package dev.nebula.module.hud;

/** Matching coordinates in the other dimension. */
public final class NetherCoordsHud extends StatHud {
    public NetherCoordsHud() {
        super("Nether Coords", "Matching coordinates in the other dimension.", 0.0f, 0.68f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Other Dim";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null || mc().level == null) return "0, 0"; boolean nether = mc().level.dimension() == net.minecraft.world.level.Level.NETHER; double f = nether ? 8 : 0.125; return (int) (p.getX() * f) + ", " + (int) (p.getZ() * f);
    }
}
