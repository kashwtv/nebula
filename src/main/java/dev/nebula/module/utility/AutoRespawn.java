package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.util.Combat;
import net.minecraft.client.gui.screens.DeathScreen;

public final class AutoRespawn extends Module {
    private boolean wasDead;

    public AutoRespawn() {
        super("Auto Respawn", "Skips the death screen.", Category.PLAYER);
    }

    @Override
    public void update() {
        var p = mc().player;
        boolean dead = p != null && p.isDeadOrDying();
        if (dead && !wasDead) Combat.deaths++;
        wasDead = dead;
        if (isEnabled() && dead && mc().screen instanceof DeathScreen) {
            p.respawn();
            mc().setScreen(null);
        }
    }
}
