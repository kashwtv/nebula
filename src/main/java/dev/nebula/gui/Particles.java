package dev.nebula.gui;

import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;

/** Drifting points that link up into a constellation and lean toward the cursor. */
final class Particles {
    private static final int N = 70;
    private final float[] x = new float[N], y = new float[N], vx = new float[N], vy = new float[N], size = new float[N];
    private boolean init;
    private long last = System.nanoTime();

    void render(GuiGraphics g, float w, float h, float mx, float my, float alpha) {
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1e9f, 0.05f);
        last = now;
        if (!init) {
            for (int i = 0; i < N; i++) {
                x[i] = (float) Math.random() * w;
                y[i] = (float) Math.random() * h;
                vx[i] = (float) (Math.random() - 0.5) * 14;
                vy[i] = (float) (Math.random() - 0.5) * 14;
                size[i] = 0.6f + (float) Math.random() * 1.1f;
            }
            init = true;
        }
        for (int i = 0; i < N; i++) {
            float dx = mx - x[i], dy = my - y[i];
            float d2 = dx * dx + dy * dy;
            if (d2 < 120 * 120 && d2 > 1) {
                float f = 18f / (float) Math.sqrt(d2);
                vx[i] += dx * f * dt;
                vy[i] += dy * f * dt;
            }
            vx[i] *= 1 - 0.4f * dt;
            vy[i] *= 1 - 0.4f * dt;
            float sp = (float) Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i]);
            if (sp < 4) { vx[i] *= 1.02f; vy[i] *= 1.02f; }
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            if (x[i] < -10) x[i] = w + 10; else if (x[i] > w + 10) x[i] = -10;
            if (y[i] < -10) y[i] = h + 10; else if (y[i] > h + 10) y[i] = -10;
        }
        for (int i = 0; i < N; i++) {
            R2D.circle(g, x[i], y[i], size[i] * 0.8f, Col.alpha(Theme.TEXT, 0.18f * alpha));
        }
    }
}
