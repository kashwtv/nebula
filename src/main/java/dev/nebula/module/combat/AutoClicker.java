package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.ThreadLocalRandom;

/** Clicks for you while you hold attack, at a randomized CPS (or only at full cooldown). */
public final class AutoClicker extends Module {
    public final NumberSetting minCps = add(new NumberSetting("Min CPS", 1, 20, 8, 1));
    public final NumberSetting maxCps = add(new NumberSetting("Max CPS", 1, 20, 12, 1));
    public final BoolSetting cooldown = add(new BoolSetting("Respect Cooldown", true));
    public final BoolSetting onlyEntities = add(new BoolSetting("Only On Entities", false));

    private long next;

    public AutoClicker() {
        super("Auto Clicker", "Holds attack for you at a natural CPS.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return minCps.i() + "-" + maxCps.i();
    }

    @SubscribeEvent
    public void onFrame(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !isEnabled()) return;
        var p = mc().player;
        if (p == null || mc().screen != null || mc().gameMode == null || !mc().options.keyAttack.isDown()) return;
        long now = System.currentTimeMillis();
        if (now < next) return;
        if (onlyEntities.get() && !(mc().hitResult instanceof EntityHitResult)) return;
        if (cooldown.get() && p.getAttackStrengthScale(0.5f) < 0.95f) return;
        int lo = Math.min(minCps.i(), maxCps.i()), hi = Math.max(minCps.i(), maxCps.i());
        double cps = lo == hi ? lo : ThreadLocalRandom.current().nextDouble(lo, hi + 1);
        next = now + (long) (1000 / Math.max(1, cps));
        if (mc().hitResult instanceof EntityHitResult eh) {
            mc().gameMode.attack(p, eh.getEntity());
        }
        p.swing(InteractionHand.MAIN_HAND);
    }
}
