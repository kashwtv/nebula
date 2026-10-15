package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.util.Move;

/**
 * Sprint speed in every direction, not just forward. Vanilla only sprints forward, so when
 * you move sideways or backwards this sets your ground speed to sprint speed instead.
 */
public final class OmniSprint extends Module {
    public final BoolSetting hungerCheck = add(new BoolSetting("Respect Hunger", true));

    public OmniSprint() {
        super("Omni Sprint", "Sprint sideways and backwards too.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || !p.onGround() || !Move.moving(p) || p.isShiftKeyDown() || p.isUsingItem() || p.isInWater() || p.isPassenger()) return;
        if (hungerCheck.get() && p.getFoodData().getFoodLevel() <= 6 && !p.getAbilities().mayfly) return;
        if (p.input.forwardImpulse > 0) { p.setSprinting(true); return; }
        double s = Move.SPRINT;
        var eff = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
        if (eff != null) s *= 1 + 0.2 * (eff.getAmplifier() + 1);
        Move.easeSpeed(p, s, 0.5);
    }
}
