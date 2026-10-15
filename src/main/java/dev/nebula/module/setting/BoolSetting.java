package dev.nebula.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting {
    public boolean value;

    public BoolSetting(String name, boolean value) {
        super(name);
        this.value = value;
        anim.set(value ? 1 : 0);
    }

    public boolean get() {
        return value;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        value = e.getAsBoolean();
        anim.set(value ? 1 : 0);
    }
}
