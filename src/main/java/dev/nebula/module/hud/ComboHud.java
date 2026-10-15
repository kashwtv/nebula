package dev.nebula.module.hud;

/** Your current hit combo. */
public final class ComboHud extends StatHud {
    public ComboHud() {
        super("Combo", "Your current hit combo.", 1.0f, 0.5f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Combo";
    }

    @Override
    protected String value(boolean editor) {
        return String.valueOf(dev.nebula.util.Combat.combo);
    }
}
