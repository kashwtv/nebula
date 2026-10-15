package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Always sneaking, without holding the key. */
public final class AutoSneak extends Module {
    public AutoSneak() {
        super("Sneak", "Always sneak.", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onInput(MovementInputUpdateEvent e) {
        if (isEnabled() && e.getEntity() == mc().player) e.getInput().shiftKeyDown = true;
    }
}
