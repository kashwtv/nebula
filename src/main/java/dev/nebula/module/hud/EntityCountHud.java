package dev.nebula.module.hud;

/** How many entities are loaded around you. */
public final class EntityCountHud extends StatHud {
    public EntityCountHud() {
        super("Entities", "How many entities are loaded around you.", 0.0f, 0.84f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Entities";
    }

    @Override
    protected String value(boolean editor) {
        if (mc().level == null) return "0"; int n = 0; for (var e : mc().level.entitiesForRendering()) n++; return String.valueOf(n);
    }
}
