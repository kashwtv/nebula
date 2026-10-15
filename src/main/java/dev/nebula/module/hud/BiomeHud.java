package dev.nebula.module.hud;

/** The biome you're standing in. */
public final class BiomeHud extends StatHud {
    public BiomeHud() {
        super("Biome", "The biome you're standing in.", 0.0f, 0.48f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Biome";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "Plains"; return p.level().getBiome(p.blockPosition()).unwrapKey().map(k -> pretty(k.location().getPath())).orElse("?");
    }
}
