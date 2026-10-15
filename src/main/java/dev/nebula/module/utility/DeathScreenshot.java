package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.client.Screenshot;

/** Takes a screenshot automatically the moment you die. */
public final class DeathScreenshot extends Module {
    private boolean wasDead;

    public DeathScreenshot() {
        super("Death Screenshot", "Screenshots your deaths.", Category.CLIENT);
    }

    @Override
    public void onTick() {
        boolean dead = mc().player.isDeadOrDying();
        if (dead && !wasDead) Screenshot.grab(mc().gameDirectory, mc().getMainRenderTarget(), c -> mc().execute(() -> mc().gui.getChat().addMessage(c)));
        wasDead = dead;
    }
}
