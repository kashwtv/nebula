package dev.nebula.module.hud;

/** Distance to what your crosshair is on. */
public final class TargetDistanceHud extends StatHud {
    public TargetDistanceHud() {
        super("Target Distance", "Distance to what your crosshair is on.", 1.0f, 0.54f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Dist";
    }

    @Override
    protected String value(boolean editor) {
        var hr = mc().hitResult; var p = mc().player; if (hr == null || p == null || hr.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return "-"; return String.format("%.1fm", hr.getLocation().distanceTo(p.getEyePosition()));
    }
}
