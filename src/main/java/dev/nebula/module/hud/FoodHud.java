package dev.nebula.module.hud;

/** Hunger and hidden saturation. */
public final class FoodHud extends StatHud {
    public FoodHud() {
        super("Food", "Hunger and hidden saturation.", 0.0f, 0.64f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Food";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "20 (5.0)"; return p.getFoodData().getFoodLevel() + String.format(" (%.1f)", p.getFoodData().getSaturationLevel());
    }
}
