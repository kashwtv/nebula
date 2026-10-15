package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;

public final class ToggleSprint extends Module {
    public final BoolSetting sneak = add(new BoolSetting("Toggle Sneak", false));

    public ToggleSprint() {
        super("Sprint", "Always sprint. Optionally keep sneak held too.", Category.MOVEMENT);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        if (mc().player == null) return;
        mc().options.keySprint.setDown(true);
        if (sneak.get()) mc().options.keyShift.setDown(true);
    }

    @Override
    public void onDisable() {
        mc().options.keySprint.setDown(false);
        if (sneak.get()) mc().options.keyShift.setDown(false);
    }
}
