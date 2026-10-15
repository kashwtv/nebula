package dev.nebula.module.client;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;

public final class HudStyleModule extends Module {
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0, 1, 0.72, 0.01));
    public final NumberSetting radius = add(new NumberSetting("Corner Radius", 0, 6, 3, 0.5));
    public final BoolSetting border = add(new BoolSetting("Border", true));
    public final BoolSetting shadow = add(new BoolSetting("Shadow", true));
    public final BoolSetting accentLine = add(new BoolSetting("Accent Line", false));
    public final BoolSetting textShadow = add(new BoolSetting("Text Shadow", false));

    public HudStyleModule() {
        super("HUD Style", "Look of every HUD element.", Category.CLIENT);
    }

    @Override
    public boolean toggleable() {
        return false;
    }
}
