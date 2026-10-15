package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;

/** Jump higher. Height is in blocks; the launch speed is worked out from vanilla gravity. */
public final class HighJump extends Module {
    public final NumberSetting height = add(new NumberSetting("Height", 1.5, 10, 3, 0.5, "m"));
    public final BoolSetting noFall = add(new BoolSetting("No Fall From Jump", true));

    private boolean wasGround = true, inJump;

    public HighJump() {
        super("High Jump", "Jump higher than normal.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return height.display();
    }

    /** Smallest upward speed that reaches h blocks with vanilla gravity (0.08) and drag (0.98). */
    private static double launch(double h) {
        double lo = 0.42, hi = 3.0;
        for (int i = 0; i < 30; i++) {
            double mid = (lo + hi) / 2, vy = mid, y = 0;
            while (vy > 0) { y += vy; vy = (vy - 0.08) * 0.98; }
            if (y < h) lo = mid; else hi = mid;
        }
        return hi;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || p.isInWater() || p.getAbilities().flying || p.isPassenger()) return;
        var v = p.getDeltaMovement();
        if (wasGround && !p.onGround() && v.y > 0.3 && p.input.jumping) {
            p.setDeltaMovement(v.x, launch(height.value), v.z);
            inJump = true;
        }
        if (inJump && noFall.get()) p.fallDistance = 0;
        if (p.onGround() && !wasGround) inJump = false;
        wasGround = p.onGround();
    }
}
