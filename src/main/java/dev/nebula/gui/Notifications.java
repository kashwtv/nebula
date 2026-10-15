package dev.nebula.gui;

import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.client.NotificationsModule;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.util.Anim;
import dev.nebula.util.Col;
import dev.nebula.util.Ease;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Small cards that slide in bottom-right; a hairline at the bottom drains as time runs out. */
public final class Notifications {
    private static final long LIFE = 1800, IN = 360, OUT = 280;
    private static final float H = 26;

    private static final class Toast {
        final String title, sub;
        final boolean on;
        final long start = System.currentTimeMillis();
        final Anim y = new Anim(-1);

        Toast(String title, String sub, boolean on) {
            this.title = title;
            this.sub = sub;
            this.on = on;
        }
    }

    private static final List<Toast> TOASTS = new ArrayList<>();

    private Notifications() {}

    public static void push(Module m) {
        NotificationsModule nm = ModuleManager.get(NotificationsModule.class);
        if (nm == null || (!nm.isEnabled() && m != nm)) return;
        push(m.name, m.isEnabled() ? "Enabled" : "Disabled", m.isEnabled());
    }

    public static void push(String title, String sub, boolean on) {
        TOASTS.removeIf(t -> t.title.equals(title));
        TOASTS.add(new Toast(title, sub, on));
        if (TOASTS.size() > 5) TOASTS.remove(0);
    }

    public static void render(GuiGraphics g, float vw, float vh) {
        long now = System.currentTimeMillis();
        TOASTS.removeIf(t -> now - t.start > LIFE + OUT);
        float baseY = vh - 10 - H;
        int slot = 0;
        float px = R2D.px();
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            Toast t = TOASTS.get(i);
            long age = now - t.start;
            float in = Ease.outCubic(age / (float) IN);
            float out = age > LIFE ? Ease.inCubic((age - LIFE) / (float) OUT) : 0;
            float w = Math.max(R2D.width(t.title), R2D.width(t.sub)) + 26;
            float targetY = baseY - slot * (H + 4);
            if (t.y.get() < 0) t.y.set(targetY);
            float y = t.y.update(targetY, 16);
            float x = vw - w - 10 + (1 - in) * 24 + out * 24;
            float prev = R2D.alpha;
            R2D.alpha = prev * in * (1 - out);

            int c = t.on ? Theme.accent() : Theme.DIM;
            R2D.glow(g, x, y + 1, w, H, 4, 10, 0x70000000);
            R2D.rect(g, x, y, w, H, 4, Theme.SURFACE);
            R2D.outline(g, x, y, w, H, 4, px, Theme.BORDER);
            R2D.circle(g, x + 9, y + H / 2, 2.2f, c);
            R2D.text(g, t.title, x + 17, y + 4, Theme.TEXT);
            R2D.text(g, t.sub, x + 17, y + 14, Theme.MUTED);
            float life = 1 - Ease.clamp01(age / (float) LIFE);
            R2D.rect(g, x + 4, y + H - px * 2, (w - 8) * life, px * 1.5f, 0, Col.alpha(c, 0.8f));

            R2D.alpha = prev;
            slot++;
        }
    }
}
