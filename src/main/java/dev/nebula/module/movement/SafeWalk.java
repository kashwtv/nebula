package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Sneaks for you right at block edges so you can't walk off (great for bridging). */
public final class SafeWalk extends Module {
    public final BoolSetting onlyBackwards = add(new BoolSetting("Only Backwards", false));
    public final BoolSetting onlyWithBlocks = add(new BoolSetting("Only Holding Blocks", false));

    public SafeWalk() {
        super("Safe Walk", "Auto-sneak at edges so you never fall off.", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onInput(MovementInputUpdateEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getEntity() != p || !p.onGround() || p.getAbilities().flying) return;
        var in = e.getInput();
        if (onlyBackwards.get() && in.forwardImpulse >= 0) return;
        if (onlyWithBlocks.get() && !(p.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem)) return;
        var v = p.getDeltaMovement();
        // Look a little ahead of where we're moving; if there's nothing under us there, sneak.
        var probe = p.getBoundingBox().move(v.x * 2.5, -0.6, v.z * 2.5).deflate(0.08, 0, 0.08);
        if (p.level().noCollision(p, probe)) in.shiftKeyDown = true;
    }
}
