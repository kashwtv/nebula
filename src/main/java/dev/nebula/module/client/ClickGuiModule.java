package dev.nebula.module.client;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.Theme;

public final class ClickGuiModule extends Module {
    public final ModeSetting theme = add(new ModeSetting("Accent", "Crimson", Theme.NAMES));
    /** Index doubles as the pixel multiplier: 1 = 1x ... 4 = 4x. */
    public final ModeSetting uiScale = add(new ModeSetting("UI Scale", "Auto", "Auto", "1x", "2x", "3x", "4x"));
    public final ModeSetting font = add(new ModeSetting("Font", "Poppins", "Poppins", "Minecraft"));
    public final BoolSetting blur = add(new BoolSetting("Blur", true));
    public final NumberSetting blurStrength = add(new NumberSetting("Blur Strength", 2, 30, 12, 1));
    public final NumberSetting dim = add(new NumberSetting("Dim", 0, 0.9, 0.45, 0.05));
    public final BoolSetting particles = add(new BoolSetting("Particles", false));
    public final BoolSetting sounds = add(new BoolSetting("Sounds", true));

    public ClickGuiModule() {
        super("Click GUI", "The panel itself: accent color, blur, sounds.", Category.CLIENT);
    }

    @Override
    public boolean toggleable() {
        return false;
    }
}
