package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;

public final class AutoWalk extends Module {
    public AutoWalk() {
        super("Auto Walk", "Keeps walking forward for you.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        mc().options.keyUp.setDown(true);
    }

    @Override
    public void onDisable() {
        mc().options.keyUp.setDown(false);
    }
}
