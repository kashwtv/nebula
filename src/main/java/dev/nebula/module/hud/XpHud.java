package dev.nebula.module.hud;

/** Your level and progress to the next one. */
public final class XpHud extends StatHud {
    public XpHud() {
        super("Experience", "Your level and progress to the next one.", 0.0f, 0.64f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "XP";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "0"; return p.experienceLevel + String.format(" (%d%%)", (int) (p.experienceProgress * 100));
    }
}
