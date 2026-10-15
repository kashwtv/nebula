package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.util.Place;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.phys.EntityHitResult;

/** Swaps to an axe when the player you're aiming at raises a shield (axes disable shields). */
public final class ShieldBreaker extends Module {
    public ShieldBreaker() {
        super("Shield Breaker", "Switches to an axe against blocking players.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (!(mc().hitResult instanceof EntityHitResult eh) || !(eh.getEntity() instanceof Player t)) return;
        if (!t.isBlocking()) return;
        int slot = Place.findHotbar(s -> s.getItem() instanceof AxeItem);
        if (slot >= 0) mc().player.getInventory().selected = slot;
    }
}
