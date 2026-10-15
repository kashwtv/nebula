package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Swinging at air doesn't reset your attack cooldown: missed clicks are simply ignored. */
public final class NoMissCooldown extends Module {
    public NoMissCooldown() {
        super("No Miss Cooldown", "Missed swings don't reset your cooldown.", Category.COMBAT);
    }

    @SubscribeEvent
    public void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (isEnabled() && e.isAttack() && mc().hitResult != null && mc().hitResult.getType() == HitResult.Type.MISS) {
            e.setSwingHand(false);
            e.setCanceled(true);
        }
    }
}
