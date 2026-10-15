package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * Removes fall damage. "Spoof" marks you on the ground for the movement packet while
 * falling; "Packet" sends a single on-ground update once you've fallen far enough and
 * resets the counter. Elytra flight is left alone unless you allow it.
 */
public final class NoFall extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Spoof", "Spoof", "Packet"));
    public final NumberSetting minFall = add(new NumberSetting("Min Fall", 2, 5, 2.5, 0.5, "m"));
    public final BoolSetting elytra = add(new BoolSetting("While Gliding", false));

    private boolean spoofed;

    public NoFall() {
        super("No Fall", "Removes fall damage.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return mode.get();
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
        if (!isEnabled() || p.isCreative() || p.isSpectator() || (p.isFallFlying() && !elytra.get()) || p.onGround()) return;
        if (p.fallDistance < minFall.value || p.getDeltaMovement().y >= 0) return;
        if (mode.is("Packet")) {
            if (p.connection != null) p.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true));
            p.fallDistance = 0;
        } else {
            p.setOnGround(true);
            spoofed = true;
        }
    }

    @Override
    public void onDisable() {
        if (spoofed && mc().player != null) mc().player.setOnGround(false);
        spoofed = false;
    }
}
