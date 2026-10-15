package dev.nebula.module.hud;

/** Your exact yaw and pitch. */
public final class RotationHud extends StatHud {
    public RotationHud() {
        super("Rotation", "Your exact yaw and pitch.", 0.0f, 0.52f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Yaw/Pitch";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "0 / 0"; return String.format("%.1f / %.1f", net.minecraft.util.Mth.wrapDegrees(p.getYRot()), p.getXRot());
    }
}
