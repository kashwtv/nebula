package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;

/** Scrolling heading strip that fades out at the edges. */
public final class CompassHud extends HudModule {
    public final NumberSetting width = add(new NumberSetting("Length", 80, 300, 150, 2));
    public final NumberSetting span = add(new NumberSetting("View Angle", 60, 220, 130, 10, "°"));
    public final BoolSetting degrees = add(new BoolSetting("Degrees", true));
    public final BoolSetting espDots = add(new BoolSetting("ESP Dots", true));

    private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    @Override
    public NumberSetting lengthSetting() {
        return width;
    }

    public CompassHud() {
        super("Compass", "Scrolling heading strip.", 0.5f, 0.0f);
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        var p = mc().player;
        if (p == null) return;
        w = width.f();
        h = degrees.get() ? 24 : 14;
        if (background.get()) Skin.plate(g, 0, 0, w, h);
        float yaw = ((p.getViewYRot(1f) % 360) + 360) % 360;
        float pxPerDeg = w / span.f();
        float cx = w / 2f;
        float px = R2D.px();
        R2D.pushScissor(g, 1, 0, w - 2, h);
        int start = (int) Math.floor((yaw - span.f() / 2) / 15f) * 15 - 15;
        for (int d = start; d <= yaw + span.f() / 2 + 15; d += 15) {
            float x = cx + (d - yaw) * pxPerDeg;
            float edge = 1 - Math.min(1, Math.abs(x - cx) / (w / 2f));
            edge = edge * edge * (3 - 2 * edge);
            int norm = ((d % 360) + 360) % 360;
            boolean major = norm % 45 == 0;
            if (major) {
                String n = NAMES[norm / 45];
                int c = n.equals("N") ? Theme.accent() : n.length() == 1 ? Theme.TEXT : Theme.MUTED;
                R2D.textCentered(g, n, x + 0.5f, 3.5f, Col.alpha(c, edge));
            } else {
                R2D.rect(g, x - px / 2, 5, px, 4, 0, Col.alpha(Theme.DIM, edge));
            }
        }
        R2D.popScissor(g);
        if (espDots.get()) drawDots(g, p, yaw, pxPerDeg, cx);
        R2D.rect(g, cx - px / 2, 0, px, 2.5f, 0, Theme.TEXT);
        if (degrees.get()) {
            String s = String.valueOf(Math.round(yaw) % 360);
            R2D.textCentered(g, s, cx + 0.5f, 14, Theme.MUTED);
        }
    }

    /** While ESP is on: a dot per tracked player/mob in the direction they are. */
    private void drawDots(GuiGraphics g, net.minecraft.world.entity.player.Player p, float yaw, float pxPerDeg, float cx) {
        var esp = dev.nebula.module.ModuleManager.get(dev.nebula.module.visuals.Esp.class);
        if (esp == null || !esp.isEnabled()) return;
        var eye = p.getEyePosition();
        float half = span.f() / 2;
        float y = 12.5f;
        for (var en : esp.targets()) {
            if (en instanceof net.minecraft.world.entity.item.ItemEntity) continue;
            float[] a = dev.nebula.util.Targets.angles(eye, en.position());
            float delta = net.minecraft.util.Mth.wrapDegrees(a[0] - yaw);
            boolean offScreen = Math.abs(delta) > half - 2;
            float d = Math.max(-half + 2, Math.min(half - 2, delta));
            float x = cx + d * pxPerDeg;
            double dist = p.distanceTo(en);
            float r = (float) Math.max(1.2, 2.6 - dist / 60.0);
            int c = esp.colorFor(en, 0);
            if (offScreen) {
                // pinned to the edge: a small faded marker so you know someone is behind you
                R2D.circle(g, x, y, 1.2f, Col.alpha(c, 0.55f));
            } else {
                R2D.circle(g, x, y, r + 0.8f, 0xA0000000);
                R2D.circle(g, x, y, r, c);
            }
        }
    }
}
