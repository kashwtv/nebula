package dev.nebula.module.hud;

/** How far you've been falling. */
public final class FallHud extends StatHud {
    public FallHud() {
        super("Fall Distance", "How far you've been falling.", 0.0f, 0.48f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Fall";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; return p == null ? "0.0m" : String.format("%.1fm", p.fallDistance);
    }
}
