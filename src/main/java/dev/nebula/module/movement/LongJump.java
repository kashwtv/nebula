package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Move;

/**
 * Jump much further: when you jump while moving, you get a horizontal boost that fades out
 * through the air. Optionally turns itself off after one landing.
 */
public final class LongJump extends Module {
    public final NumberSetting boost = add(new NumberSetting("Boost", 1, 4, 2, 0.1, "x"));
    public final NumberSetting decay = add(new NumberSetting("Decay", 0.8, 0.99, 0.94, 0.01));
    public final NumberSetting extraHeight = add(new NumberSetting("Extra Height", 0, 0.3, 0.05, 0.01));
    public final BoolSetting autoJump = add(new BoolSetting("Auto Jump", false));
    public final BoolSetting disableOnLand = add(new BoolSetting("Disable On Land", false));

    private boolean wasGround = true, jumping;
    private double speed;

    public LongJump() {
        super("Long Jump", "Jump much further.", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        jumping = false;
        wasGround = true;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || p.isInWater() || p.getAbilities().flying || p.isFallFlying() || p.isPassenger()) return;
        if (autoJump.get() && p.onGround() && Move.moving(p)) p.jumpFromGround();
        var v = p.getDeltaMovement();
        if (wasGround && !p.onGround() && v.y > 0 && Move.moving(p)) {
            jumping = true;
            speed = Move.base(p) * boost.value;
            p.setDeltaMovement(v.x, v.y + extraHeight.value, v.z);
            Move.setSpeed(p, speed);
        } else if (jumping && !p.onGround()) {
            speed = Math.max(Move.base(p), speed * decay.value);
            Move.setSpeed(p, speed);
        } else if (jumping && p.onGround()) {
            jumping = false;
            if (disableOnLand.get()) { setEnabled(false); dev.nebula.gui.Notifications.push(this); }
        }
        wasGround = p.onGround();
    }
}
