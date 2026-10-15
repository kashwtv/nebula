package dev.nebula.module.hud;

import dev.nebula.module.setting.BoolSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;

/** FPS readout with an optional frame-time sparkline. */
public final class FpsHud extends StatHud {
    public final BoolSetting graph = add(new BoolSetting("Graph", false));

    private final float[] samples = new float[40];
    private int head;
    private long lastSample;

    public FpsHud() {
        super("FPS", "Frames per second, optional live graph.", 0.0f, 0.26f);
        setEnabled(true);
    }

    @Override
    protected String label() {
        return "FPS";
    }

    @Override
    protected String value(boolean editor) {
        return String.valueOf(mc().getFps());
    }

    @Override
    protected int statusColor() {
        int f = mc().getFps();
        return f >= 90 ? Theme.GREEN : f >= 45 ? Theme.YELLOW : Theme.RED;
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        super.draw(g, editor);
        if (!graph.get()) return;
        long now = System.currentTimeMillis();
        if (now - lastSample > 120) {
            samples[head] = mc().getFps();
            head = (head + 1) % samples.length;
            lastSample = now;
        }
        float gw = Math.max(w, 64), gh = 16, gy = h + 2;
        if (background.get()) Skin.plate(g, 0, gy, gw, gh);
        float max = 1;
        for (float s : samples) max = Math.max(max, s);
        int n = samples.length;
        float step = (gw - 8) / (n - 1);
        float[] pts = new float[(n - 1) * 4];
        int[] cols = new int[n - 1];
        int acc = Theme.accent();
        for (int i = 0; i < n - 1; i++) {
            float a = samples[(head + i) % n] / max, b = samples[(head + i + 1) % n] / max;
            float x0 = 4 + i * step, x1 = 4 + (i + 1) * step;
            pts[i * 4] = x0;
            pts[i * 4 + 1] = gy + gh - 4 - a * (gh - 8);
            pts[i * 4 + 2] = x1;
            pts[i * 4 + 3] = gy + gh - 4 - b * (gh - 8);
            cols[i] = Col.alpha(acc, 0.35f + 0.65f * i / (n - 1f));
        }
        R2D.lines(g, pts, cols, n - 1);
        h = gy + gh;
        w = gw;
    }
}
