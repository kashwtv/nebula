package dev.nebula.module.hud;

import dev.nebula.Nebula;
import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Wordmark with a few quick stats. */
public final class Watermark extends HudModule {
    public final BoolSetting version = add(new BoolSetting("Version", false));
    public final BoolSetting fps = add(new BoolSetting("FPS", true));
    public final BoolSetting time = add(new BoolSetting("Time", true));
    public final BoolSetting server = add(new BoolSetting("Server", false));

    public Watermark() {
        super("Watermark", "Nebula wordmark with quick stats.", 0.0f, 0.0f);
        setEnabled(true);
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        StringBuilder info = new StringBuilder();
        if (fps.get()) info.append(mc().getFps()).append(" fps");
        if (time.get()) {
            if (!info.isEmpty()) info.append("   ");
            info.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (server.get()) {
            String ip = mc().getCurrentServer() != null ? mc().getCurrentServer().ip : "Singleplayer";
            if (!info.isEmpty()) info.append("   ");
            info.append(ip);
        }
        String word = "Nebula";
        float ww = R2D.boldWidth(word);
        float vw = version.get() ? R2D.width(Nebula.VERSION) + 4 : 0;
        float iw = info.isEmpty() ? 0 : R2D.width(info.toString()) + 13;
        w = 8 + 6 + ww + vw + iw + 7;
        h = 15;
        Skin.plate(g, 0, 0, w, h);
        R2D.rect(g, 7, h / 2 - 2.5f, 3, 5, 1, Theme.accent());
        R2D.bold(g, word, 14, 4, Theme.TEXT);
        float x = 14 + ww;
        if (version.get()) {
            R2D.text(g, Nebula.VERSION, x + 4, 4, Theme.DIM);
            x += vw;
        }
        if (iw > 0) {
            R2D.rect(g, x + 6, 4, R2D.px(), h - 8, 0, 0x30FFFFFF);
            R2D.text(g, info.toString(), x + 12, 4, Theme.MUTED);
        }
    }
}
