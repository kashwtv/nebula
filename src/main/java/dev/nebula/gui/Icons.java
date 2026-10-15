package dev.nebula.gui;

import com.mojang.math.Axis;
import dev.nebula.render.R2D;
import net.minecraft.client.gui.GuiGraphics;

/** Small vector icons drawn from the same primitives as the rest of the UI, so they stay crisp. */
final class Icons {
    private Icons() {}

    /** A thick line between two points. */
    private static void bar(GuiGraphics g, float x1, float y1, float x2, float y2, float th, int c) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        g.pose().pushPose();
        g.pose().translate(x1, y1, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        R2D.rect(g, 0, -th / 2, len, th, th / 2, c);
        g.pose().popPose();
    }

    static void draw(GuiGraphics g, String key, float cx, float cy, int c) {
        float t = 1.2f;
        switch (key) {
            case "COMBAT" -> {
                R2D.ring(g, cx, cy, 3.4f, t, c);
                R2D.rect(g, cx - t / 2, cy - 6, t, 2.4f, 0, c);
                R2D.rect(g, cx - t / 2, cy + 3.6f, t, 2.4f, 0, c);
                R2D.rect(g, cx - 6, cy - t / 2, 2.4f, t, 0, c);
                R2D.rect(g, cx + 3.6f, cy - t / 2, 2.4f, t, 0, c);
                R2D.circle(g, cx, cy, 0.9f, c);
            }
            case "MOVEMENT" -> {
                for (int i = 0; i < 2; i++) {
                    float ox = cx - 2.5f + i * 3.6f;
                    bar(g, ox - 1.5f, cy - 3.2f, ox + 1.6f, cy, t, c);
                    bar(g, ox + 1.6f, cy, ox - 1.5f, cy + 3.2f, t, c);
                }
            }
            case "VISUALS" -> {
                R2D.outline(g, cx - 5.5f, cy - 3.2f, 11, 6.4f, 3.2f, t, c);
                R2D.circle(g, cx, cy, 1.7f, c);
            }
            case "WORLD" -> {
                R2D.ring(g, cx, cy, 4.6f, t, c);
                R2D.outline(g, cx - 2.2f, cy - 5.2f, 4.4f, 10.4f, 2.2f, t * 0.9f, c);
                R2D.rect(g, cx - 4.6f, cy - t / 2, 9.2f, t, 0, c);
            }
            case "PLAYER" -> {
                R2D.circle(g, cx, cy - 2.4f, 2.1f, c);
                R2D.rect(g, cx - 3.8f, cy + 1.2f, 7.6f, 3.8f, 1.9f, c);
            }
            case "HUD" -> {
                R2D.outline(g, cx - 5.2f, cy - 4.2f, 10.4f, 8.4f, 1.6f, t, c);
                R2D.rect(g, cx - 3.2f, cy - 2.2f, 3, 2, 0.5f, c);
                R2D.rect(g, cx - 3.2f, cy + 1, 6.4f, 1.1f, 0.5f, c);
            }
            case "CLIENT" -> {
                for (int i = 0; i < 6; i++) {
                    g.pose().pushPose();
                    g.pose().translate(cx, cy, 0);
                    g.pose().mulPose(Axis.ZP.rotationDegrees(i * 60));
                    R2D.rect(g, -t / 2 - 0.3f, -5.4f, t + 0.6f, 2.4f, 0.4f, c);
                    g.pose().popPose();
                }
                R2D.ring(g, cx, cy, 3.2f, 1.6f, c);
            }
            case "FAVORITES" -> star(g, cx, cy, 4.6f, c, true);
            case "ACTIVE" -> {
                R2D.ring(g, cx, cy, 4f, t, c);
                R2D.circle(g, cx, cy, 2f, c);
            }
            default -> R2D.circle(g, cx, cy, 2, c);
        }
    }

    /** Diamond "favorite" mark; outlined when not set. */
    static void star(GuiGraphics g, float cx, float cy, float size, int c, boolean filled) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(45));
        float s = size / 1.414f * 1.1f;
        if (filled) R2D.rect(g, -s / 2, -s / 2, s, s, 0.8f, c);
        else R2D.outline(g, -s / 2, -s / 2, s, s, 0.8f, 1.1f, c);
        g.pose().popPose();
    }
}
