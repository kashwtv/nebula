package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.R2D;
import dev.nebula.util.Anim;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Pixel-exact crosshair in the style competitive players run: small static cross,
 * tight gap, 1px black outline. Everything is measured in real screen pixels.
 */
public final class Crosshair extends Module {
    public final ModeSetting style = add(new ModeSetting("Style", "Cross", "Cross", "T-Shape", "Circle", "Dot"));
    public final ColorSetting color = add(new ColorSetting("Color", 0.35f));
    public final NumberSetting length = add(new NumberSetting("Length", 1, 20, 5, 1, "px"));
    public final NumberSetting gap = add(new NumberSetting("Gap", 0, 12, 3, 1, "px"));
    public final NumberSetting thickness = add(new NumberSetting("Thickness", 1, 6, 2, 1, "px"));
    public final NumberSetting outlineSize = add(new NumberSetting("Outline", 0, 3, 1, 1, "px"));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.2, 1, 1, 0.05));
    public final BoolSetting dot = add(new BoolSetting("Center Dot", false));
    public final BoolSetting dynamic = add(new BoolSetting("Dynamic Spread", false));
    public final BoolSetting thirdPerson = add(new BoolSetting("Show In 3rd Person", false));

    private final Anim spread = new Anim(0);

    public Crosshair() {
        super("Crosshair", "Clean pro-style crosshair, every pixel adjustable.", Category.VISUALS);
        color.theme = false;
        color.sat = 0.9f;
        setEnabled(true);
    }

    @SubscribeEvent
    public void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (isEnabled() && e.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) e.setCanceled(true);
    }

    public void draw(GuiGraphics g, float w, float h) {
        var p = mc().player;
        if (p == null || mc().options.renderDebug) return;
        if (!mc().options.getCameraType().isFirstPerson() && !thirdPerson.get()) return;
        float px = (float) (1.0 / mc().getWindow().getGuiScale());
        // center on a whole physical pixel
        float cx = (float) Math.floor(w / 2f / px) * px, cy = (float) Math.floor(h / 2f / px) * px;
        float target = 0;
        if (dynamic.get()) {
            target += (1 - p.getAttackStrengthScale(0f)) * 5;
            if (p.isSprinting()) target += 2;
            if (!p.onGround()) target += 2;
        }
        int sp = Math.round(spread.update(target, 14));
        float len = length.i() * px, th = thickness.i() * px, gp = (gap.i() + sp) * px, ol = outlineSize.i() * px;
        int c = Col.alpha(color.get(), opacity.f());
        int o = Col.alpha(0xFF000000, opacity.f() * 0.9f);
        // keep odd and even thicknesses centered on the pixel grid
        float half = (float) Math.floor(thickness.i() / 2f) * px;

        switch (style.get()) {
            case "Circle" -> {
                float r = gp + len / 2;
                if (ol > 0) R2D.ring(g, cx, cy, r + ol, th + ol * 2, o);
                R2D.ring(g, cx, cy, r, th, c);
            }
            case "Dot" -> { }
            default -> {
                boolean tShape = style.is("T-Shape");
                float[][] arms = {
                        {cx - gp - len, cy - half, len, th}, {cx + gp + px, cy - half, len, th},
                        {cx - half, cy + gp + px, th, len}, {cx - half, cy - gp - len, th, len}};
                for (int i = 0; i < arms.length; i++) {
                    if (tShape && i == 3) continue;
                    float[] a = arms[i];
                    if (ol > 0) R2D.rect(g, a[0] - ol, a[1] - ol, a[2] + ol * 2, a[3] + ol * 2, 0, o);
                }
                for (int i = 0; i < arms.length; i++) {
                    if (tShape && i == 3) continue;
                    float[] a = arms[i];
                    R2D.rect(g, a[0], a[1], a[2], a[3], 0, c);
                }
            }
        }
        if (dot.get() || style.is("Dot")) {
            float d = Math.max(px, th);
            if (ol > 0) R2D.rect(g, cx - half - ol, cy - half - ol, d + ol * 2, d + ol * 2, 0, o);
            R2D.rect(g, cx - half, cy - half, d, d, 0, c);
        }
    }
}
