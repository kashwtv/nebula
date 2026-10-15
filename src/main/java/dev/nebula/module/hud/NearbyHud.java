package dev.nebula.module.hud;

/** How many players are within 64 blocks. */
public final class NearbyHud extends StatHud {
    public NearbyHud() {
        super("Nearby Players", "How many players are within 64 blocks.", 0.0f, 0.8f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Nearby";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null || mc().level == null) return "0"; int n = 0; for (var o : mc().level.players()) if (o != p && o.distanceTo(p) < 64) n++; return String.valueOf(n);
    }
}
