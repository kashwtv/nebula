package dev.nebula.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.LinkedHashSet;
import java.util.Set;

/** Pick any number of options; shown as clickable chips. */
public final class MultiSetting extends Setting {
    public final String[] options;
    public final Set<String> selected = new LinkedHashSet<>();

    public MultiSetting(String name, String[] options, String... defaults) {
        super(name);
        this.options = options;
        for (String d : defaults) selected.add(d);
    }

    public boolean has(String o) {
        return selected.contains(o);
    }

    public void toggle(String o) {
        if (!selected.remove(o)) selected.add(o);
    }

    @Override
    public JsonElement save() {
        JsonArray a = new JsonArray();
        for (String s : selected) a.add(s);
        return a;
    }

    @Override
    public void load(JsonElement e) {
        selected.clear();
        for (JsonElement x : e.getAsJsonArray()) {
            String v = x.getAsString();
            for (String o : options) if (o.equals(v)) selected.add(v);
        }
    }
}
