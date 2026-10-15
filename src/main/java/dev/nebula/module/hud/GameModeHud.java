package dev.nebula.module.hud;

/** Your current game mode. */
public final class GameModeHud extends StatHud {
    public GameModeHud() {
        super("Game Mode", "Your current game mode.", 1.0f, 0.78f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Mode";
    }

    @Override
    protected String value(boolean editor) {
        var gm = mc().gameMode; return gm == null ? "Survival" : pretty(gm.getPlayerMode().getName());
    }
}
