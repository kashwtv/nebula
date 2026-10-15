package dev.nebula.module.hud;

/** Clear, rain or thunder. */
public final class WeatherHud extends StatHud {
    public WeatherHud() {
        super("Weather", "Clear, rain or thunder.", 0.0f, 0.84f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Weather";
    }

    @Override
    protected String value(boolean editor) {
        if (mc().level == null) return "Clear"; return mc().level.isThundering() ? "Thunder" : mc().level.isRaining() ? "Rain" : "Clear";
    }
}
