package dev.nebula.module.hud;

/** Durability left on what you're holding. */
public final class DurabilityHud extends StatHud {
    public DurabilityHud() {
        super("Durability", "Durability left on what you're holding.", 0.0f, 0.76f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Durability";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "100%"; var st = p.getMainHandItem(); if (!st.isDamageableItem()) return "-"; int left = st.getMaxDamage() - st.getDamageValue(); return left + " (" + (100 * left / st.getMaxDamage()) + "%)";
    }
}
