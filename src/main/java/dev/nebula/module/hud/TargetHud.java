package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Anim;
import dev.nebula.util.Col;
import dev.nebula.util.Combat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** Who you're fighting: live model, health with a white damage trail, armor and the HP difference. */
public final class TargetHud extends HudModule {
    public final dev.nebula.module.setting.NumberSetting length = add(new dev.nebula.module.setting.NumberSetting("Length", 110, 240, 140, 2));
    public final BoolSetting model = add(new BoolSetting("3D Model", true));
    public final BoolSetting armor = add(new BoolSetting("Armor", true));
    public final BoolSetting difference = add(new BoolSetting("HP Difference", true));

    private final Anim hp = new Anim(1), ghost = new Anim(1), appear = new Anim(0);
    private LivingEntity shown;

    public TargetHud() {
        super("Target HUD", "The player or mob you're fighting.", 0.62f, 0.62f);
        setEnabled(true);
    }

    @Override
    public dev.nebula.module.setting.NumberSetting lengthSetting() {
        return length;
    }

    private LivingEntity current() {
        if (Combat.lastTarget != null && Combat.lastTarget.isAlive() && Combat.sinceHit() < 4000) return Combat.lastTarget;
        if (mc().hitResult instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity le) return le;
        return null;
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        LivingEntity t = current();
        if (t == null && editor) t = mc().player;
        float a = appear.update(t != null ? 1 : 0, 12);
        if (t != null) shown = t;
        w = length.f();
        h = 42;
        if (shown == null || a < 0.01f) return;
        LivingEntity e = shown;
        float prev = R2D.alpha;
        R2D.alpha = prev * a;
        g.pose().pushPose();
        g.pose().translate(0, (1 - a) * 6, 0);

        Skin.plate(g, 0, 0, w, h);
        float left = 7;
        if (model.get()) {
            R2D.rect(g, 4, 4, 30, h - 8, 3, 0x40000000);
            R2D.pushScissor(g, 4, 4, 30, h - 8);
            int size = (int) Math.max(6, 26 / Math.max(e.getBbHeight(), 0.6f));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, 19, (int) (h - 6), size, -30, -10, e);
            R2D.popScissor(g);
            left = 40;
        }
        String name = e.getDisplayName().getString();
        R2D.bold(g, name, left, 6, Theme.TEXT);
        var me = mc().player;
        if (me != null && me != e) R2D.textRight(g, String.format("%.1fm", me.distanceTo(e)), w - 7, 6, Theme.DIM);

        float max = Math.max(1, e.getMaxHealth());
        float frac = Math.min(1, e.getHealth() / max);
        float cur = hp.update(frac, 14);
        float gh = ghost.update(frac, frac < ghost.get() ? 2.2f : 30f);
        float bx = left, bw = w - left - 7, by = 18;
        R2D.rect(g, bx, by, bw, 3, 1.5f, Theme.TRACK);
        if (gh > cur) R2D.rect(g, bx + bw * cur, by, bw * (gh - cur), 3, 1.5f, 0xD0FFFFFF);
        int hc = Col.health(cur);
        if (cur > 0) R2D.rect(g, bx, by, Math.max(3, bw * cur), 3, 1.5f, hc);

        String hpText = String.format("%.1f ❤", e.getHealth() + e.getAbsorptionAmount());
        R2D.text(g, hpText, bx, 25, Theme.TEXT);
        if (difference.get() && me != null && me != e) {
            float diff = (me.getHealth() + me.getAbsorptionAmount()) - (e.getHealth() + e.getAbsorptionAmount());
            String s = (diff >= 0 ? "+" : "") + String.format("%.1f", diff);
            R2D.textRight(g, s, w - 7, 25, diff >= 0 ? Theme.GREEN : Theme.RED);
        }
        if (armor.get() && e instanceof Player pl) {
            ItemStack[] st = {pl.getMainHandItem(), pl.getItemBySlot(EquipmentSlot.HEAD), pl.getItemBySlot(EquipmentSlot.CHEST),
                    pl.getItemBySlot(EquipmentSlot.LEGS), pl.getItemBySlot(EquipmentSlot.FEET)};
            g.pose().pushPose();
            g.pose().translate(bx + 38, 24, 0);
            g.pose().scale(0.55f, 0.55f, 1);
            int x = 0;
            for (ItemStack s : st) {
                if (s.isEmpty()) continue;
                g.renderItem(s, x, 0);
                x += 17;
            }
            g.pose().popPose();
        }
        g.pose().popPose();
        R2D.alpha = prev;
    }
}
