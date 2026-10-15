package dev.nebula.module.hud;

/** Your chunk coordinates and position inside it. */
public final class ChunkHud extends StatHud {
    public ChunkHud() {
        super("Chunk", "Your chunk coordinates and position inside it.", 0.0f, 0.6f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Chunk";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "0, 0"; var c = p.chunkPosition(); return c.x + ", " + c.z + "  [" + (p.getBlockX() & 15) + ", " + (p.getBlockZ() & 15) + "]";
    }
}
