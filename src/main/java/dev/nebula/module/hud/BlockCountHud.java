package dev.nebula.module.hud;

/** How many placeable blocks you carry. */
public final class BlockCountHud extends StatHud {
    public BlockCountHud() {
        super("Blocks", "How many placeable blocks you carry.", 1.0f, 0.62f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Blocks";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "64"; int n = 0; for (var st : p.getInventory().items) if (st.getItem() instanceof net.minecraft.world.item.BlockItem) n += st.getCount(); return String.valueOf(n);
    }
}
