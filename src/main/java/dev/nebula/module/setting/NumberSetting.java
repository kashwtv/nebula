package dev.nebula.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting {
    public final double min, max, step;
    private final String unit;
    public double value;

    public NumberSetting(String name, double min, double max, double value, double step, String unit) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
        set(value);
        anim.set((float) fraction());
    }

    public NumberSetting(String name, double min, double max, double value, double step) {
        this(name, min, max, value, step, "");
    }

    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        v = Math.round(v / step) * step;
        value = Math.max(min, Math.min(max, v));
    }

    public double fraction() {
        return (value - min) / (max - min);
    }

    public float f() {
        return (float) value;
    }

    public int i() {
        return (int) Math.round(value);
    }

    public String display() {
        String s = step >= 1 ? String.valueOf((long) Math.round(value))
                : step >= 0.1 ? String.format("%.1f", value) : String.format("%.2f", value);
        return s + unit;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        set(e.getAsDouble());
        anim.set((float) fraction());
    }
}
