package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.R2D;
import dev.nebula.util.Anim;
import net.minecraft.client.gui.GuiGraphics;

/** Heartbeat-pulsing red vignette when your health gets low. */
public final class LowHpWarning extends Module {
    public final NumberSetting threshold = add(new NumberSetting("Below HP", 2, 16, 8, 1));
    public final NumberSetting intensity = add(new NumberSetting("Intensity", 0.2, 1, 0.7, 0.05));

    private final Anim fade = new Anim(0);

    public LowHpWarning() {
        super("Low HP Warning", "Red heartbeat vignette when you're about to die.", Category.VISUALS);
        setEnabled(true);
    }

    public void draw(GuiGraphics g, float w, float h) {
        var p = mc().player;
        float target = 0;
        if (isEnabled() && p != null && !p.isCreative() && !p.isSpectator() && p.getHealth() <= threshold.f()) {
            target = 1 - (p.getHealth() / threshold.f()) * 0.6f;
        }
        float f = fade.update(target, 4);
        if (f < 0.01f) return;
        // double-thump heartbeat
        double t = (System.currentTimeMillis() % 1100) / 1100.0;
        float beat = (float) (Math.exp(-Math.pow((t - 0.08) * 18, 2)) + 0.7 * Math.exp(-Math.pow((t - 0.28) * 18, 2)));
        float a = f * intensity.f() * (0.55f + 0.45f * beat);
        int red = ((int) (a * 255) << 24) | 0xE0102A;
        int clear = 0x00E0102A;
        float ew = w * 0.22f, eh = h * 0.28f;
        R2D.hGradient(g, 0, 0, ew, h, 0, red, clear);
        R2D.hGradient(g, w - ew, 0, ew, h, 0, clear, red);
        R2D.vGradient(g, 0, 0, w, eh, 0, red, clear);
        R2D.vGradient(g, 0, h - eh, w, eh, 0, clear, red);
    }
}
