package dev.nebula.module.setting;

import com.google.gson.JsonElement;
import dev.nebula.util.Anim;

import java.util.function.BooleanSupplier;

public abstract class Setting {
    public final String name;
    public final Anim anim = new Anim(0);
    private BooleanSupplier visible = () -> true;

    protected Setting(String name) {
        this.name = name;
    }

    public Setting visibleWhen(BooleanSupplier v) {
        this.visible = v;
        return this;
    }

    public boolean visible() {
        return visible.getAsBoolean();
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement e);
}
