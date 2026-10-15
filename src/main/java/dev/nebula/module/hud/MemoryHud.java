package dev.nebula.module.hud;

/** Java memory in use. */
public final class MemoryHud extends StatHud {
    public MemoryHud() {
        super("Memory", "Java memory in use.", 0.0f, 0.6f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "RAM";
    }

    @Override
    protected String value(boolean editor) {
        var r = Runtime.getRuntime(); long used = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20; return used + "/" + max + "MB";
    }
}
