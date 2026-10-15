package dev.nebula.module.hud;

import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ClockHud extends StatHud {
    public final ModeSetting format = add(new ModeSetting("Format", "24h", "24h", "12h"));
    public final BoolSetting seconds = add(new BoolSetting("Seconds", false));
    public final BoolSetting gameTime = add(new BoolSetting("Game Time", false));

    public ClockHud() {
        super("Clock", "Real time or in-game time.", 1.0f, 0.97f);
    }

    @Override
    protected String label() {
        return gameTime.get() ? "Day" : "Time";
    }

    @Override
    protected String value(boolean editor) {
        if (gameTime.get() && mc().level != null) {
            long t = mc().level.getDayTime();
            long day = t / 24000;
            int ticks = (int) ((t + 6000) % 24000);
            int hh = ticks / 1000, mm = (ticks % 1000) * 60 / 1000;
            return String.format("%d  %02d:%02d", day, hh, mm);
        }
        String pat = (format.is("12h") ? "h:mm" : "HH:mm") + (seconds.get() ? ":ss" : "") + (format.is("12h") ? " a" : "");
        return LocalTime.now().format(DateTimeFormatter.ofPattern(pat));
    }
}
