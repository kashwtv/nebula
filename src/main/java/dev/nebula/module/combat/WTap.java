package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * After a hit, lets go of W for a tick or two like a real W-tap, which drops and
 * restarts your sprint so the next hit deals full sprint knockback.
 */
public final class WTap extends Module {
    public final NumberSetting length = add(new NumberSetting("Length", 1, 4, 1, 1, "t"));
    public final NumberSetting chance = add(new NumberSetting("Chance", 0, 100, 90, 5, "%"));

    private int ticks;

    public WTap() {
        super("W-Tap", "Sprint resets after each hit for more knockback.", Category.COMBAT);
    }

    public void onAttack() {
        var p = mc().player;
        if (!isEnabled() || p == null || !p.isSprinting()) return;
        if (ThreadLocalRandom.current().nextInt(100) >= chance.i()) return;
        ticks = length.i();
    }

    @SubscribeEvent
    public void onInput(MovementInputUpdateEvent e) {
        if (!isEnabled() || ticks <= 0 || e.getEntity() != mc().player) return;
        ticks--;
        e.getInput().forwardImpulse = 0;
        e.getInput().up = false;
    }
}
