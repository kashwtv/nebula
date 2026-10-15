package dev.nebula.module.client;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;

/** Not a toggle: clicking it in the panel opens the HUD layout editor. */
public final class HudEditorModule extends Module {
    public final BoolSetting snap = add(new BoolSetting("Snapping", true));
    public final BoolSetting grid = add(new BoolSetting("Grid", true));

    public HudEditorModule() {
        super("HUD Editor", "Click to drag and resize HUD elements.", Category.CLIENT);
    }

    @Override
    public boolean toggleable() {
        return false;
    }
}
