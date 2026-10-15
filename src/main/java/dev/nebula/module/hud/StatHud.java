package dev.nebula.module.hud;

import dev.nebula.module.HudModule;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.render.R2D;
import dev.nebula.render.Skin;
import dev.nebula.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** A compact readout: "Label value". */
public abstract class StatHud extends HudModule {
    public final BoolSetting showLabel = add(new BoolSetting("Label", true));
    public final ModeSetting valueColor = add(new ModeSetting("Value Color", "White", "White", "Accent", "Status"));

    protected StatHud(String name, String description, float fx, float fy) {
        super(name, description, fx, fy);
    }

    protected abstract String label();

    protected abstract String value(boolean editor);

    /** Color for "Status" mode, e.g. green = good ping. */
    protected int statusColor() {
        return Theme.TEXT;
    }

    protected int valueColor() {
        return switch (valueColor.get()) {
            case "Accent" -> Theme.accent();
            case "Status" -> statusColor();
            default -> Theme.TEXT;
        };
    }

    @Override
    public void draw(GuiGraphics g, boolean editor) {
        String l = label(), v = value(editor);
        float lw = showLabel.get() ? R2D.width(l) + 4 : 0;
        w = 6 + lw + R2D.width(v) + 6;
        h = 13;
        if (background.get()) Skin.plate(g, 0, 0, w, h);
        if (lw > 0) R2D.text(g, l, 6, 3, Theme.MUTED);
        R2D.text(g, v, 6 + lw, 3, valueColor());
    }
}
