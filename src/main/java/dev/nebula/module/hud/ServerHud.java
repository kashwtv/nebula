package dev.nebula.module.hud;

/** The server address you're on. */
public final class ServerHud extends StatHud {
    public ServerHud() {
        super("Server", "The server address you're on.", 0.0f, 0.56f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Server";
    }

    @Override
    protected String value(boolean editor) {
        var s = mc().getCurrentServer(); return s != null ? s.ip : (mc().hasSingleplayerServer() ? "Singleplayer" : "-");
    }
}
