package dev.nebula.module.hud;

/** Golden hearts you have. */
public final class AbsorptionHud extends StatHud {
    public AbsorptionHud() {
        super("Absorption", "Golden hearts you have.", 1.0f, 0.5f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Absorb";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; return p == null ? "0" : String.format("%.0f", p.getAbsorptionAmount());
    }
}
