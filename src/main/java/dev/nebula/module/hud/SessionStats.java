package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import dev.nebula.util.Combat;
import net.minecraft.client.gui.GuiGraphics;

/** Playtime, kills, deaths, K/D and combo for this session. */
public final class SessionStats extends HudModule {
    public final BoolSetting combo = add(new BoolSetting("Combo", true));
    public final BoolSetting hits = add(new BoolSetting("Hits", true));

    public SessionStats() {
        super("Session Stats", "Kills, deaths, K/D and playtime this session.", 0.0f, 0.62f);
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        long secs = (System.currentTimeMillis() - Combat.SESSION_START) / 1000;
        String time = String.format("%d:%02d:%02d", secs / 3600, (secs / 60) % 60, secs % 60);
        String kd = Combat.deaths == 0 ? String.valueOf(Combat.kills) : String.format("%.2f", Combat.kills / (double) Combat.deaths);
        String[][] rows = {
                {"Time", time}, {"Kills", String.valueOf(Combat.kills)}, {"Deaths", String.valueOf(Combat.deaths)}, {"K/D", kd},
                {"Hits", hits.get() ? String.valueOf(Combat.hits) : null},
                {"Combo", combo.get() ? (Combat.sinceHit() < 1500 ? Combat.combo + "x" : "-") : null}};
        float lw = 0, vw = 0;
        int n = 0;
        for (String[] r : rows) {
            if (r[1] == null) continue;
            lw = Math.max(lw, R2D.width(r[0]));
            vw = Math.max(vw, R2D.width(r[1]));
            n++;
        }
        w = Math.max(80, 12 + lw + 14 + vw);
        h = 16 + n * 10 + 2;
        Skin.plate(g, 0, 0, w, h);
        R2D.bold(g, "Session", 6, 4, Theme.TEXT);
        R2D.rect(g, 6, 14, w - 12, R2D.px(), 0, 0x18FFFFFF);
        float y = 17;
        for (String[] r : rows) {
            if (r[1] == null) continue;
            R2D.text(g, r[0], 6, y + 1, Theme.MUTED);
            R2D.textRight(g, r[1], w - 6, y + 1, Theme.TEXT);
            y += 10;
        }
    }
}
