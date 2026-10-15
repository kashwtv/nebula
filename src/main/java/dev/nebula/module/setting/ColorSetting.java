package dev.nebula.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;

/** Either follows the theme accent, or a custom hue (with optional rainbow cycling). */
public final class ColorSetting extends Setting {
    public boolean theme = true;
    public boolean rainbow = false;
    public float hue;
    public float sat = 0.8f;

    public ColorSetting(String name, float hue) {
        super(name);
        this.hue = hue;
    }

    public int get() {
        return get(0);
    }

    public int get(float offset) {
        if (theme) return Theme.accent(offset);
        if (rainbow) return Col.hsv(hue + System.currentTimeMillis() / 6000f + offset * 0.1f, sat, 1f);
        return Col.hsv(hue, sat, 1f);
    }

    @Override
    public JsonElement save() {
        JsonObject o = new JsonObject();
        o.addProperty("theme", theme);
        o.addProperty("rainbow", rainbow);
        o.addProperty("hue", hue);
        o.addProperty("sat", sat);
        return o;
    }

    @Override
    public void load(JsonElement e) {
        JsonObject o = e.getAsJsonObject();
        if (o.has("theme")) theme = o.get("theme").getAsBoolean();
        if (o.has("rainbow")) rainbow = o.get("rainbow").getAsBoolean();
        if (o.has("hue")) hue = o.get("hue").getAsFloat();
        if (o.has("sat")) sat = o.get("sat").getAsFloat();
    }
}
