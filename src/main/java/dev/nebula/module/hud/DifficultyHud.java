package dev.nebula.module.hud;

/** The world difficulty. */
public final class DifficultyHud extends StatHud {
    public DifficultyHud() {
        super("Difficulty", "The world difficulty.", 1.0f, 0.82f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Difficulty";
    }

    @Override
    protected String value(boolean editor) {
        return mc().level == null ? "Normal" : pretty(mc().level.getDifficulty().getKey());
    }
}
