package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Anim;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Just the bars: a big health number, segmented health with a white damage trail,
 * and slimmer hunger / armor bars, using Minecraft's own icons. No box behind it by default.
 */
public final class HealthHud extends HudModule {
    private static final ResourceLocation ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final int HP = 0xFFFF4B4B, GOLD = 0xFFFFD43B, FOOD = 0xFFE7B062, ARMOR = 0xFFB4C8DA;

    public final NumberSetting length = add(new NumberSetting("Length", 70, 220, 120, 2));
    public final BoolSetting segmented = add(new BoolSetting("Segmented", true));
    public final BoolSetting hunger = add(new BoolSetting("Hunger", true));
    public final BoolSetting armor = add(new BoolSetting("Armor", true));
    public final BoolSetting flash = add(new BoolSetting("Damage Flash", true));
    public final BoolSetting hideVanilla = add(new BoolSetting("Hide Vanilla Bars", true));

    private final Anim hp = new Anim(1), ghost = new Anim(1), food = new Anim(1), arm = new Anim(0), abs = new Anim(0);

    public HealthHud() {
        super("Health", "Clean health, hunger and armor bars.", 0.5f, 0.9f);
        background.value = false;
        setEnabled(true);
    }

    @Override
    public NumberSetting lengthSetting() {
        return length;
    }

    @SubscribeEvent
    public void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (!isEnabled() || !hideVanilla.get()) return;
        var id = e.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || (hunger.get() && id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()))
                || (armor.get() && id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id()))) {
            e.setCanceled(true);
        }
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        var p = mc().player;
        if (p == null) return;
        float max = Math.max(1, p.getMaxHealth());
        float frac = Math.min(1, p.getHealth() / max);
        float cur = hp.update(frac, 16);
        float gh = ghost.update(frac, frac < ghost.get() ? 2.4f : 30f);
        float ab = abs.update(Math.min(1, p.getAbsorptionAmount() / max), 10);
        boolean bg = background.get();

        w = length.f();
        int rows = (hunger.get() ? 1 : 0) + (armor.get() ? 1 : 0);
        h = 18 + rows * 11 + (bg ? 4 : 0);
        float pad = bg ? 4 : 0;
        if (bg) Skin.plate(g, 0, 0, w, h);

        // health number row
        float y = pad;
        g.blit(ICONS, (int) pad, (int) y, 16, 0, 9, 9);   // dark heart outline
        g.blit(ICONS, (int) pad, (int) y, p.getAbsorptionAmount() > 0 ? 160 : 52, 0, 9, 9);
        String num = fmt(p.getHealth());
        R2D.bold(g, num, pad + 12, y + 1, Theme.TEXT, true);
        float nx = pad + 12 + R2D.boldWidth(num) + 2;
        R2D.text(g, "/ " + fmt(max), nx, y + 1, Theme.MUTED, true);
        if (p.getAbsorptionAmount() > 0) {
            String a = "+" + fmt(p.getAbsorptionAmount());
            R2D.text(g, a, w - pad - R2D.width(a), y + 1, GOLD, true);
        }

        // segmented health bar
        float bx = pad, bw = w - pad * 2, by = y + 11, bh = 4;
        int segs = segmented.get() ? Math.max(1, Math.min(20, (int) Math.ceil(max / 2f))) : 1;
        segBar(g, bx, by, bw, bh, segs, cur, flash.get() ? gh : cur, HP, bg);
        if (ab > 0.001f) R2D.rect(g, bx, by + bh + 1, bw * ab, 1, 0.5f, GOLD);

        y = by + bh + 4;
        if (hunger.get()) {
            float f = food.update(p.getFoodData().getFoodLevel() / 20f, 12);
            row(g, pad, y, 52, 27, String.valueOf(p.getFoodData().getFoodLevel()), f, FOOD, bg);
            y += 11;
        }
        if (armor.get()) {
            float a = arm.update(Math.min(1, p.getArmorValue() / 20f), 12);
            row(g, pad, y, 34, 9, String.valueOf(p.getArmorValue()), a, ARMOR, bg);
        }
    }

    private void row(GuiGraphics g, float pad, float y, int u, int v, String value, float frac, int color, boolean bg) {
        g.blit(ICONS, (int) pad, (int) y, u, v, 9, 9);
        R2D.text(g, value, pad + 12, y + 1, Theme.TEXT, true);
        float bx = pad + 28, bw = w - pad - bx;
        segBar(g, bx, y + 3, bw, 3, segmented.get() ? 10 : 1, frac, frac, color, bg);
    }

    /** Bar split into gapped segments; the span between value and ghost shows white. */
    private static void segBar(GuiGraphics g, float x, float y, float w, float h, int segs, float value, float ghost, int color, boolean bg) {
        float gap = segs > 1 ? 1 : 0;
        float sw = (w - gap * (segs - 1)) / segs;
        int track = bg ? Theme.TRACK : 0x99000000;
        int shade = Col.lerp(color, 0xFF000000, 0.3f);
        for (int i = 0; i < segs; i++) {
            float sx = x + i * (sw + gap);
            float fill = Math.max(0, Math.min(1, value * segs - i));
            float gf = Math.max(0, Math.min(1, ghost * segs - i));
            R2D.rect(g, sx, y, sw, h, 0.5f, track);
            if (gf > fill) R2D.rect(g, sx + sw * fill, y, sw * (gf - fill), h, 0.5f, 0xE6FFFFFF);
            if (fill > 0) {
                R2D.rect(g, sx, y, sw * fill, h, 0.5f, color);
                R2D.rect(g, sx, y + h - Math.max(1, h * 0.35f), sw * fill, Math.max(1, h * 0.35f), 0.5f, shade);
            }
        }
    }

    private static String fmt(float v) {
        return Math.abs(v - Math.round(v)) < 0.05f ? String.valueOf(Math.round(v)) : String.format("%.1f", v);
    }
}
