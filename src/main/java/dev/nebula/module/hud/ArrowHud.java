package dev.nebula.module.hud;

/** Arrows in your inventory. */
public final class ArrowHud extends StatHud {
    public ArrowHud() {
        super("Arrows", "Arrows in your inventory.", 0.0f, 0.72f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Arrows";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "0"; int n = 0; for (var st : p.getInventory().items) if (st.getItem() instanceof net.minecraft.world.item.ArrowItem) n += st.getCount(); return String.valueOf(n);
    }
}
