package dev.nebula.module.hud;

/** Block light at your feet (mobs spawn at 0). */
public final class LightHud extends StatHud {
    public LightHud() {
        super("Light Level", "Block light at your feet (mobs spawn at 0).", 0.0f, 0.48f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Light";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player; if (p == null) return "15"; return String.valueOf(p.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p.blockPosition()));
    }
}
