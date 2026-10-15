package dev.nebula.module.hud;

/** Which way you're facing, with yaw. */
public final class DirectionHud extends StatHud {
    public DirectionHud() {
        super("Direction", "Which way you're facing, with yaw.", 0.0f, 0.52f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Facing";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "North"; return pretty(p.getDirection().getName()) + String.format(" %.0f°", net.minecraft.util.Mth.wrapDegrees(p.getYRot()));
    }
}
