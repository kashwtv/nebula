package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.concurrent.ThreadLocalRandom;

/** Takes less knockback. Scales the hit's push on the tick it lands. */
public final class Velocity extends Module {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal", 0, 100, 70, 5, "%"));
    public final NumberSetting vertical = add(new NumberSetting("Vertical", 0, 100, 100, 5, "%"));
    public final NumberSetting chance = add(new NumberSetting("Chance", 0, 100, 80, 5, "%"));

    public Velocity() {
        super("Velocity", "Reduces knockback by a percentage you choose.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return horizontal.i() + "% " + vertical.i() + "%";
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (!isEnabled() || e.phase != TickEvent.Phase.START || e.side != LogicalSide.CLIENT || e.player != mc().player) return;
        var p = mc().player;
        if (p.hurtTime != p.hurtDuration || p.hurtDuration == 0) return;
        if (ThreadLocalRandom.current().nextInt(100) >= chance.i()) return;
        var v = p.getDeltaMovement();
        p.setDeltaMovement(v.x * horizontal.value / 100, v.y * vertical.value / 100, v.z * horizontal.value / 100);
    }
}
