package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;

/** Harmless troll: rapid sneak spam, the classic Minecraft "twerk". */
public final class Twerk extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 1, 10, 2, 1, "t"));

    private int t;

    public Twerk() {
        super("Twerk", "Spam sneak on a loop.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc().screen != null) { mc().options.keyShift.setDown(false); return; }
        t++;
        mc().options.keyShift.setDown((t / speed.i()) % 2 == 0);
    }

    @Override
    public void onDisable() {
        mc().options.keyShift.setDown(false);
    }
}
