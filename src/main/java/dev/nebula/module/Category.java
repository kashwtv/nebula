package dev.nebula.module;

public enum Category {
    COMBAT("Combat", "⚔"),     // ⚔
    MOVEMENT("Movement", "➤"), // ➤
    VISUALS("Visuals", "◈"),   // ◈
    WORLD("World", "◎"),       // ◎
    PLAYER("Player", "✦"),     // ✦
    HUD("HUD", "▣"),           // ▣
    CLIENT("Client", "⚙");     // ⚙

    public final String label;
    public final String icon;

    Category(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }
}
