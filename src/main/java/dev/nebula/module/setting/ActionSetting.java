package dev.nebula.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** A pill button in the module's settings; runs an action when clicked. Not saved. */
public final class ActionSetting extends Setting {
    public final Runnable action;

    public ActionSetting(String name, Runnable action) {
        super(name);
        this.action = action;
    }

    @Override
    public JsonElement save() { return JsonNull.INSTANCE; }

    @Override
    public void load(JsonElement e) {}
}
