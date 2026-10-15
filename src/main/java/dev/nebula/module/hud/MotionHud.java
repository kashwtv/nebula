package dev.nebula.module.hud;

/** How fast you're rising or falling. */
public final class MotionHud extends StatHud {
    public MotionHud() {
        super("Vertical Speed", "How fast you're rising or falling.", 1.0f, 0.86f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Y Speed";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; return p == null ? "0.0" : String.format("%+.1f b/s", p.getDeltaMovement().y * 20);
    }
}
