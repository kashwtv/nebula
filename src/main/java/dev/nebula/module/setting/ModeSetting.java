package dev.nebula.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ModeSetting extends Setting {
    public final String[] modes;
    public int index;

    public ModeSetting(String name, String def, String... modes) {
        super(name);
        this.modes = modes;
        for (int i = 0; i < modes.length; i++) if (modes[i].equals(def)) index = i;
    }

    public String get() {
        return modes[index];
    }

    public boolean is(String mode) {
        return modes[index].equals(mode);
    }

    public void cycle(int dir) {
        index = Math.floorMod(index + dir, modes.length);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement e) {
        String v = e.getAsString();
        for (int i = 0; i < modes.length; i++) if (modes[i].equals(v)) index = i;
    }
}
