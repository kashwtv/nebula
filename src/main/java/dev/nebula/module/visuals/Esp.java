package dev.nebula.module.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.Projection;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.render.World3D;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows entities through walls: tactical 2D brackets with health, name, distance and the
 * player's armor + held items, optional 3D boxes and tracers.
 */
public final class Esp extends Module {
    public final ModeSetting style = add(new ModeSetting("Style", "2D", "2D", "3D Box", "Both"));
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting animals = add(new BoolSetting("Passive Mobs", true));
    public final BoolSetting items = add(new BoolSetting("Items", false));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", true));
    public final BoolSetting healthBar = add(new BoolSetting("Health Bar", true));
    public final BoolSetting names = add(new BoolSetting("Names", true));
    public final BoolSetting armor = add(new BoolSetting("Armor", true));
    public final BoolSetting fill = add(new BoolSetting("Box Fill", true));
    public final NumberSetting range = add(new NumberSetting("Range", 16, 256, 128, 8, "m"));
    public final ColorSetting mobColor = add(new ColorSetting("Hostile Color", 0.98f));

    public Esp() {
        super("ESP", "See players and mobs through walls with their armor and health.", Category.VISUALS);
        mobColor.theme = false;
        fill.visibleWhen(() -> !style.is("2D"));
    }

    @Override
    public String suffix() {
        return style.get();
    }

    public List<Entity> targets() {
        List<Entity> out = new ArrayList<>();
        var p = mc().player;
        var level = mc().level;
        if (p == null || level == null) return out;
        double r2 = range.value * range.value;
        for (Entity e : level.entitiesForRendering()) {
            if (e == p || !e.isAlive() || e.distanceToSqr(p) > r2) continue;
            if (e instanceof Player pl) {
                if (players.get() && !pl.isSpectator()) out.add(e);
            } else if (e instanceof ItemEntity) {
                if (items.get()) out.add(e);
            } else if (e instanceof Enemy) {
                if (mobs.get()) out.add(e);
            } else if (e instanceof Mob) {
                if (animals.get()) out.add(e);
            }
        }
        return out;
    }

    public int colorFor(Entity e, float off) {
        if (e instanceof Enemy) return mobColor.get(off);
        if (e instanceof ItemEntity) return 0xFFFFD84A;
        return color.get(off);
    }

    private static AABB box(Entity e, float pt) {
        double x = Mth.lerp(pt, e.xOld, e.getX()), y = Mth.lerp(pt, e.yOld, e.getY()), z = Mth.lerp(pt, e.zOld, e.getZ());
        return e.getBoundingBox().move(x - e.getX(), y - e.getY(), z - e.getZ());
    }

    // ------------------------------------------------------------------ 3D

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        List<Entity> list = targets();
        if (list.isEmpty()) return;
        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        Matrix4f m = e.getPoseStack().last().pose();
        boolean box3d = !style.is("2D");

        if (box3d && fill.get()) {
            BufferBuilder b = World3D.begin(VertexFormat.Mode.QUADS, false);
            for (Entity en : list) quads(b, m, box(en, pt).move(-cam.x, -cam.y, -cam.z), colorFor(en, 0), 0.12f);
            World3D.end(b);
        }
        if (box3d || tracers.get()) {
            RenderSystem.lineWidth(2f);
            BufferBuilder b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, false);
            Vector3f look = e.getCamera().getLookVector();
            for (Entity en : list) {
                AABB bb = box(en, pt).move(-cam.x, -cam.y, -cam.z);
                int c = colorFor(en, 0);
                if (box3d) edges(b, m, bb, c);
                if (tracers.get()) {
                    World3D.v(b, m, look.x() * 0.6, look.y() * 0.6, look.z() * 0.6, c, 0.9f);
                    World3D.v(b, m, (bb.minX + bb.maxX) / 2, bb.minY + (bb.maxY - bb.minY) * 0.6, (bb.minZ + bb.maxZ) / 2, c, 0.9f);
                }
            }
            World3D.end(b);
        }
    }

    private static void quads(BufferBuilder b, Matrix4f m, AABB bb, int c, float a) {
        double x0 = bb.minX, y0 = bb.minY, z0 = bb.minZ, x1 = bb.maxX, y1 = bb.maxY, z1 = bb.maxZ;
        double[][] f = {
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
                {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
        for (double[] q : f) for (int i = 0; i < 4; i++) World3D.v(b, m, q[i * 3], q[i * 3 + 1], q[i * 3 + 2], c, a);
    }

    private static void edges(BufferBuilder b, Matrix4f m, AABB bb, int c) {
        double x0 = bb.minX, y0 = bb.minY, z0 = bb.minZ, x1 = bb.maxX, y1 = bb.maxY, z1 = bb.maxZ;
        double[][] l = {
                {x0, y0, z0, x1, y0, z0}, {x1, y0, z0, x1, y0, z1}, {x1, y0, z1, x0, y0, z1}, {x0, y0, z1, x0, y0, z0},
                {x0, y1, z0, x1, y1, z0}, {x1, y1, z0, x1, y1, z1}, {x1, y1, z1, x0, y1, z1}, {x0, y1, z1, x0, y1, z0},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x1, y0, z1, x1, y1, z1}, {x0, y0, z1, x0, y1, z1}};
        for (double[] s : l) {
            World3D.v(b, m, s[0], s[1], s[2], c, 1);
            World3D.v(b, m, s[3], s[4], s[5], c, 1);
        }
    }

    // ------------------------------------------------------------------ 2D

    /** Called from the HUD in vanilla GUI coordinates. */
    public void drawOverlay(GuiGraphics g, float w, float h) {
        if (!isEnabled() || mc().options.hideGui) return;
        boolean box2d = !style.is("3D Box");
        float pt = Projection.partialTick;
        float px = (float) (1.0 / mc().getWindow().getGuiScale());
        float us = R2D.uiScale();
        var me = mc().player;
        for (Entity en : targets()) {
            AABB bb = box(en, pt).inflate(0.08, 0.05, 0.08);
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean ok = true;
            for (int i = 0; i < 8 && ok; i++) {
                float[] s = Projection.toScreen((i & 1) == 0 ? bb.minX : bb.maxX, (i & 2) == 0 ? bb.minY : bb.maxY, (i & 4) == 0 ? bb.minZ : bb.maxZ);
                if (s == null) { ok = false; break; }
                minX = Math.min(minX, s[0]); minY = Math.min(minY, s[1]);
                maxX = Math.max(maxX, s[0]); maxY = Math.max(maxY, s[1]);
            }
            if (!ok || maxX < 0 || minX > w || maxY < 0 || minY > h) continue;
            float bw = maxX - minX, bh = maxY - minY;
            int c = colorFor(en, 0), c2 = colorFor(en, 1.5f);

            if (box2d) {
                float len = Math.max(3 * px, Math.min(bw, bh) / 4);
                float th = 1.5f * px;
                // black under-stroke then color
                bracketBox(g, minX - px, minY - px, bw + 2 * px, bh + 2 * px, len + px, th + 2 * px, 0xB0000000, 0xB0000000);
                bracketBox(g, minX, minY, bw, bh, len, th, c, c2);
            }

            float labelBottom = minY - 2 * px;
            if (en instanceof LivingEntity le) {
                if (healthBar.get()) {
                    float frac = Mth.clamp(le.getHealth() / Math.max(1, le.getMaxHealth()), 0, 1);
                    float bx = minX - 5 * px, bwid = 2 * px;
                    R2D.rect(g, bx - px, minY - px, bwid + 2 * px, bh + 2 * px, 0, 0xC0000000);
                    R2D.vGradient(g, bx, minY + bh * (1 - frac), bwid, bh * frac, 0, Col.health(frac), Col.health(frac * 0.7f));
                    float abs = le.getAbsorptionAmount();
                    if (abs > 0) {
                        float af = Math.min(1, abs / Math.max(1, le.getMaxHealth()));
                        R2D.rect(g, bx - 3 * px, minY + bh * (1 - af), px, bh * af, 0, 0xFFFFD84A);
                    }
                }
                if (names.get()) labelBottom = label(g, en, le, (minX + maxX) / 2, labelBottom, us, me, c);
                if (armor.get() && en instanceof Player pl) armorRow(g, pl, (minX + maxX) / 2, labelBottom, us);
            } else if (en instanceof ItemEntity it && names.get()) {
                ItemStack st = it.getItem();
                String s = st.getHoverName().getString() + (st.getCount() > 1 ? " x" + st.getCount() : "");
                g.pose().pushPose();
                g.pose().translate((minX + maxX) / 2, labelBottom, 0);
                g.pose().scale(us * 0.8f, us * 0.8f, 1);
                float tw = R2D.width(s);
                R2D.rect(g, -tw / 2 - 3, -11, tw + 6, 10, 1.5f, 0xC0080A0C);
                R2D.text(g, s, -tw / 2, -10, 0xFFFFD84A);
                g.pose().popPose();
            }
        }
    }

    private static void bracketBox(GuiGraphics g, float x, float y, float w, float h, float len, float th, int c, int c2) {
        len = Math.min(len, Math.min(w, h) / 2);
        R2D.rect(g, x, y, len, th, 0, c);
        R2D.rect(g, x, y, th, len, 0, c);
        R2D.rect(g, x + w - len, y, len, th, 0, c2);
        R2D.rect(g, x + w - th, y, th, len, 0, c2);
        R2D.rect(g, x, y + h - th, len, th, 0, c);
        R2D.rect(g, x, y + h - len, th, len, 0, c);
        R2D.rect(g, x + w - len, y + h - th, len, th, 0, c2);
        R2D.rect(g, x + w - th, y + h - len, th, len, 0, c2);
    }

    /** Name + HP + distance pill. Returns the new top edge for stacking. */
    private float label(GuiGraphics g, Entity en, LivingEntity le, float cx, float bottom, float us, Entity me, int c) {
        String name = en.getDisplayName().getString();
        String hp = String.format("%.1f", le.getHealth() + le.getAbsorptionAmount());
        String dist = me == null ? "" : (int) me.distanceTo(en) + "m";
        float s = us * 0.8f;
        float nw = R2D.width(name), hw = R2D.width(hp), dw = R2D.width(dist);
        float tw = nw + hw + dw + 14;
        g.pose().pushPose();
        g.pose().translate(cx, bottom, 0);
        g.pose().scale(s, s, 1);
        float x = -tw / 2;
        R2D.rect(g, x, -12, tw, 11, 1.5f, 0xD0080A0C);
        R2D.rect(g, x, -12, 1.5f, 11, 0, c);
        R2D.text(g, name, x + 4, -10.5f, Theme.TEXT);
        float frac = le.getHealth() / Math.max(1, le.getMaxHealth());
        R2D.text(g, hp, x + 8 + nw, -10.5f, Col.health(frac));
        R2D.text(g, dist, x + 12 + nw + hw, -10.5f, Theme.MUTED);
        g.pose().popPose();
        return bottom - 13 * s;
    }

    private static void armorRow(GuiGraphics g, Player pl, float cx, float bottom, float us) {
        ItemStack[] stacks = {
                pl.getMainHandItem(), pl.getItemBySlot(EquipmentSlot.HEAD), pl.getItemBySlot(EquipmentSlot.CHEST),
                pl.getItemBySlot(EquipmentSlot.LEGS), pl.getItemBySlot(EquipmentSlot.FEET), pl.getOffhandItem()};
        int n = 0;
        for (ItemStack st : stacks) if (!st.isEmpty()) n++;
        if (n == 0) return;
        float s = us * 0.62f;
        float rowW = n * 17;
        g.pose().pushPose();
        g.pose().translate(cx, bottom, 0);
        g.pose().scale(s, s, 1);
        float x = -rowW / 2;
        R2D.rect(g, x - 2, -19, rowW + 3, 18, 2, 0x90080A0C);
        for (ItemStack st : stacks) {
            if (st.isEmpty()) continue;
            g.renderItem(st, (int) x, -18);
            g.renderItemDecorations(R2D.font(), st, (int) x, -18);
            x += 17;
        }
        g.pose().popPose();
    }
}
