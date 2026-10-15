package dev.nebula.module.visuals;

import com.mojang.math.Axis;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;
import dev.nebula.util.Combat;
import dev.nebula.util.Ease;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** CoD-style X that pops at the crosshair when you land a hit. */
public final class HitMarker extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final NumberSetting size = add(new NumberSetting("Size", 3, 14, 7, 1, "px"));
    public final BoolSetting sound = add(new BoolSetting("Sound", true));

    public HitMarker() {
        super("Hit Marker", "An X flashes on your crosshair when you hit something.", Category.VISUALS);
        setEnabled(true);
    }

    public void onHit() {
        if (isEnabled() && sound.get())
            mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.9f, 0.18f));
    }

    public void draw(GuiGraphics g, float w, float h) {
        long t = Combat.sinceHit();
        if (t > 380) return;
        float k = t / 380f;
        float pop = Ease.outBack(Math.min(1, t / 120f));
        float alpha = 1 - Ease.inCubic(k);
        float px = (float) (1.0 / mc().getWindow().getGuiScale());
        float cx = w / 2f, cy = h / 2f;
        float inner = (3 + 3 * pop) * px, len = size.f() * px * (0.6f + 0.4f * pop);
        float th = 2 * px;
        int c = Col.alpha(Col.lerp(0xFFFFFFFF, color.get(), k * 1.6f), alpha);
        for (int i = 0; i < 4; i++) {
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(45 + 90 * i));
            R2D.rect(g, inner - px, -th / 2 - px, len + px * 2, th + px * 2, 0, Col.alpha(0xB0000000, alpha));
            R2D.rect(g, inner, -th / 2, len, th, th / 2, c);
            g.pose().popPose();
        }
    }
}
