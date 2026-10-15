package dev.nebula.module.hud;

/** Elytra durability and glide speed. */
public final class ElytraHud extends StatHud {
    public ElytraHud() {
        super("Elytra", "Elytra durability and glide speed.", 1.0f, 0.58f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Elytra";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "432"; var st = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST); if (!(st.getItem() instanceof net.minecraft.world.item.ElytraItem)) return "-"; int left = st.getMaxDamage() - st.getDamageValue(); return left + (p.isFallFlying() ? String.format("  %.0f b/s", p.getDeltaMovement().length() * 20) : "");
    }
}
