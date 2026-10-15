package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Makes your hits crits. "Packet" sends a tiny hop to the server right before the swing;
 * "Jump" does a real small jump first (looks legit, works anywhere).
 */
public final class Criticals extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Packet", "Packet", "Jump"));

    public Criticals() {
        super("Criticals", "Every hit lands as a critical hit.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    @SubscribeEvent
    public void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (isEnabled() && e.isAttack() && mc().hitResult instanceof EntityHitResult) beforeAttack();
    }

    /** Also called by Kill Aura and Triggerbot right before they swing. */
    public void beforeAttack() {
        var p = mc().player;
        if (!isEnabled() || p == null || !p.onGround() || p.isInWater() || p.onClimbable() || p.isPassenger()) return;
        if (mode.is("Jump")) {
            p.jumpFromGround();
            return;
        }
        var c = mc().getConnection();
        if (c == null) return;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        c.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false));
        c.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false));
    }
}
