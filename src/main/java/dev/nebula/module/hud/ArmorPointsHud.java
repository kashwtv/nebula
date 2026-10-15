package dev.nebula.module.hud;

/** Your total armor and toughness. */
public final class ArmorPointsHud extends StatHud {
    public ArmorPointsHud() {
        super("Armor Points", "Your total armor and toughness.", 1.0f, 0.7f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Armor";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "20"; return p.getArmorValue() + String.format(" (+%.0f)", p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
    }
}
