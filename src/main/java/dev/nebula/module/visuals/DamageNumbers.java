package dev.nebula.module.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Col;
import dev.nebula.util.Ease;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Floating numbers pop off entities when they take damage (or heal). */
public final class DamageNumbers extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final NumberSetting size = add(new NumberSetting("Size", 0.5, 3, 1.2, 0.1));
    public final NumberSetting range = add(new NumberSetting("Range", 8, 64, 24, 4, "m"));
    public final BoolSetting heals = add(new BoolSetting("Show Heals", true));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", true));

    private record Pop(Vec3 pos, String text, int color, long start, double drift) {}

    private final Map<Integer, Float> health = new HashMap<>();
    private final List<Pop> pops = new ArrayList<>();

    public DamageNumbers() {
        super("Damage Numbers", "Floating damage numbers when anything gets hit.", Category.VISUALS);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        var level = mc().level;
        var me = mc().player;
        if (level == null || me == null) return;
        Map<Integer, Float> next = new HashMap<>();
        double r2 = range.value * range.value;
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || e.distanceToSqr(me) > r2) continue;
            float hp = le.getHealth();
            next.put(e.getId(), hp);
            Float prev = health.get(e.getId());
            if (prev == null) continue;
            float diff = hp - prev;
            Vec3 at = new Vec3(e.getX() + (Math.random() - 0.5) * e.getBbWidth(), e.getY() + e.getBbHeight() * 0.8, e.getZ() + (Math.random() - 0.5) * e.getBbWidth());
            if (diff < -0.05f) {
                float d = -diff;
                int c = d >= 6 ? 0xFFFF3B5C : d >= 3 ? 0xFFFFD84A : color.get();
                pops.add(new Pop(at, fmt(d), c, System.currentTimeMillis(), (Math.random() - 0.5) * 0.6));
            } else if (diff > 0.4f && heals.get()) {
                pops.add(new Pop(at, "+" + fmt(diff), 0xFF19F59A, System.currentTimeMillis(), (Math.random() - 0.5) * 0.6));
            }
        }
        health.clear();
        health.putAll(next);
    }

    @Override
    public void onDisable() {
        health.clear();
        pops.clear();
    }

    private static String fmt(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.1f", v);
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || pops.isEmpty()) return;
        long now = System.currentTimeMillis();
        pops.removeIf(p -> now - p.start > 1100);
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc().renderBuffers().bufferSource();
        Font font = mc().font;
        for (Pop p : pops) {
            float t = (now - p.start) / 1100f;
            float pop = Ease.outBack(Math.min(1, t * 5));
            float alpha = 1 - Ease.inCubic(Math.max(0, (t - 0.55f) / 0.45f));
            double rise = Ease.outCubic(t) * 0.9;
            ps.pushPose();
            ps.translate(p.pos.x - cp.x + p.drift * t, p.pos.y - cp.y + rise, p.pos.z - cp.z);
            ps.mulPose(cam.rotation());
            float s = 0.028f * size.f() * pop;
            ps.scale(-s, -s, s);
            float tw = font.width(p.text);
            int c = Col.alpha(p.color, alpha);
            if (((c >>> 24) & 0xFF) > 5) {
                Font.DisplayMode mode = throughWalls.get() ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
                font.drawInBatch(p.text, -tw / 2 + 1, 1, Col.alpha(0xFF000000, alpha * 0.6f), false, ps.last().pose(), buf, mode, 0, LightTexture.FULL_BRIGHT);
                font.drawInBatch(p.text, -tw / 2, 0, c, false, ps.last().pose(), buf, mode, 0, LightTexture.FULL_BRIGHT);
            }
            ps.popPose();
        }
        buf.endBatch();
    }
}
