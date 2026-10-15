package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * Tells the server you're touching the ground while falling, so fall distance never
 * builds up. Elytra flight is left alone.
 */
public final class NoFall extends Module {
    public final NumberSetting minFall = add(new NumberSetting("Min Fall", 2, 5, 2.5, 0.5, "m"));

    private boolean spoofed;

    public NoFall() {
        super("No Fall", "Removes fall damage.", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.side != LogicalSide.CLIENT || e.player != mc().player) return;
        var p = mc().player;
        if (e.phase == TickEvent.Phase.START) {
            // undo last tick's spoof so local physics stay normal
            if (spoofed) {
                p.setOnGround(false);
                spoofed = false;
            }
            return;
        }
        if (!isEnabled() || p.isCreative() || p.isFallFlying() || p.onGround()) return;
        if (p.fallDistance >= minFall.value && p.getDeltaMovement().y < 0) {
            p.setOnGround(true);
            spoofed = true;
        }
    }
}
