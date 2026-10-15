package dev.nebula.module;

import dev.nebula.gui.Notifications;
import dev.nebula.module.setting.Setting;
import dev.nebula.util.Anim;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    public int key = -1;
    /** Bind behaviour: 0 = press toggles, 1 = on only while held, 2 = always on. */
    public int bindMode;
    public static final String[] BIND_MODES = {"Toggle", "Hold", "Always"};
    /** Pinned to the Favorites tab. */
    public boolean favorite;
    private final java.util.Map<String, com.google.gson.JsonElement> defaults = new java.util.HashMap<>();
    public final Anim favAnim = new Anim(0);
    public boolean expanded;

    // UI animation state
    public final Anim hoverAnim = new Anim(0);
    public final Anim toggleAnim = new Anim(0);
    public final Anim expandAnim = new Anim(0);
    public final Anim listAnim = new Anim(0);

    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    protected <S extends Setting> S add(S s) {
        settings.add(s);
        return s;
    }

    /** Remember current setting values as this module's defaults. */
    public void snapshotDefaults() {
        defaults.clear();
        for (Setting s : settings) defaults.put(s.name, s.save());
    }

    public void resetSettings() {
        for (Setting s : settings) {
            var d = defaults.get(s.name);
            if (d != null) s.load(d);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean e) {
        if (e == enabled || !toggleable()) return;
        enabled = e;
        if (e) onEnable();
        else onDisable();
    }

    /** Toggle from user input: also shows a notification. */
    public void toggle() {
        if (bindMode == 2 && enabled) return; // "Always" modules stay on
        setEnabled(!enabled);
        Notifications.push(this);
    }

    /** Modules like Click GUI just hold settings and can't be switched off. */
    public boolean toggleable() {
        return true;
    }

    /** Whether it shows up in the on-screen module list. */
    public boolean listed() {
        return true;
    }

    /** Extra info shown in the module list, e.g. the current mode. */
    public String suffix() {
        return null;
    }

    public void onEnable() {}

    public void onDisable() {}

    /** Called every tick while enabled. */
    public void onTick() {}

    /** Called every tick whether enabled or not (fade-outs, trackers). */
    public void update() {}
}
