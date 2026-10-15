package dev.nebula.module.hud;

/** Sprinting, sneaking, flying or walking. */
public final class SprintHud extends StatHud {
    public SprintHud() {
        super("Movement State", "Sprinting, sneaking, flying or walking.", 1.0f, 0.66f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "State";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "Sprinting"; return p.isFallFlying() ? "Gliding" : p.getAbilities().flying ? "Flying" : p.isSwimming() ? "Swimming" : p.isCrouching() ? "Sneaking" : p.isSprinting() ? "Sprinting" : "Walking";
    }
}
