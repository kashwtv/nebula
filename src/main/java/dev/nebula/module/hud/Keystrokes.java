package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Anim;
import dev.nebula.util.Clicks;
import dev.nebula.util.Col;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;

/** WASD, mouse and space with a quick fill when pressed. */
public final class Keystrokes extends HudModule {
    public final NumberSetting size = add(new NumberSetting("Key Size", 14, 30, 19, 1));
    public final ModeSetting pressColor = add(new ModeSetting("Press Color", "White", "White", "Accent"));
    public final BoolSetting mouse = add(new BoolSetting("Mouse", true));
    public final BoolSetting space = add(new BoolSetting("Space Bar", true));
    public final BoolSetting cps = add(new BoolSetting("CPS", true));

    private final Map<String, Anim> anims = new HashMap<>();

    public Keystrokes() {
        super("Keystrokes", "Your WASD, mouse and space presses.", 0.0f, 0.72f);
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        var o = mc().options;
        float s = size.f(), gap = 2;
        w = s * 3 + gap * 2;
        float y = 0;
        key(g, "W", o.keyUp, s + gap, y, s, s, null);
        y += s + gap;
        key(g, "A", o.keyLeft, 0, y, s, s, null);
        key(g, "S", o.keyDown, s + gap, y, s, s, null);
        key(g, "D", o.keyRight, (s + gap) * 2, y, s, s, null);
        y += s + gap;
        if (mouse.get()) {
            float mw = (w - gap) / 2, mh = s * 0.85f;
            key(g, "LMB", o.keyAttack, 0, y, mw, mh, cps.get() && Clicks.left() > 0 ? Clicks.left() + " CPS" : null);
            key(g, "RMB", o.keyUse, mw + gap, y, mw, mh, cps.get() && Clicks.right() > 0 ? Clicks.right() + " CPS" : null);
            y += mh + gap;
        }
        if (space.get()) {
            float sh = s * 0.5f;
            float t = key(g, "", o.keyJump, 0, y, w, sh, null);
            float bw = w * 0.36f;
            R2D.rect(g, (w - bw) / 2, y + sh / 2 - 0.6f, bw, 1.2f, 0.6f, Col.lerp(Theme.MUTED, Theme.onAccent(), t));
            y += sh + gap;
        }
        h = y - gap;
    }

    private float key(GuiGraphics g, String label, KeyMapping km, float x, float y, float kw, float kh, String sub) {
        float t = anims.computeIfAbsent(label + km.getName(), k -> new Anim(0)).update(km.isDown() ? 1 : 0, 22);
        int fill = pressColor.is("Accent") ? Theme.accent() : 0xFFEDEDEF;
        Skin.plate(g, x, y, kw, kh);
        if (t > 0.01f) R2D.rect(g, x, y, kw, kh, Skin.radius(), Col.alpha(fill, 0.92f * t));
        int tc = Col.lerp(Theme.TEXT, Theme.onAccent(), t);
        float cx = x + kw / 2, cy = y + kh / 2;
        if (!label.isEmpty()) {
            if (sub != null) {
                R2D.textCentered(g, sub, cx + 0.5f, cy - 3.5f, tc);
            } else {
                R2D.textCentered(g, label, cx + 0.5f, cy - 3.5f, tc);
            }
        }
        return t;
    }
}
