package dev.nebula.module.movement;

import dev.nebula.gui.Notifications;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Survival flight. "Motion" gives direct, smooth control; "Creative" uses creative-style
 * flying. Double-tap jump to turn it off, just like stopping creative flight.
 * Anti-Kick dips you slightly so servers with allow-flight off don't kick you.
 */
public final class Flight extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Motion", "Motion", "Creative"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.1, 5, 1, 0.1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical Speed", 0.1, 3, 0.6, 0.1));
    public final NumberSetting smoothing = add(new NumberSetting("Smoothing", 0, 0.9, 0.35, 0.05));
    public final BoolSetting doubleTapOff = add(new BoolSetting("Double-Tap Jump Off", true));
    public final BoolSetting antiKick = add(new BoolSetting("Anti Kick", true));

    private int ticks;
    private long lastJumpPress;
    private Vec3 vel = Vec3.ZERO;

    public Flight() {
        super("Flight", "Fly in survival. Double-tap jump to stop.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    @Override
    public void onEnable() {
        ticks = 0;
        vel = Vec3.ZERO;
        var p = mc().player;
        if (p != null && mode.is("Creative")) {
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key e) {
        if (!isEnabled() || !doubleTapOff.get() || mc().screen != null || e.getAction() != GLFW.GLFW_PRESS) return;
        if (!mc().options.keyJump.matches(e.getKey(), e.getScanCode())) return;
        long now = System.currentTimeMillis();
        if (now - lastJumpPress < 300) {
            setEnabled(false);
            Notifications.push(this);
            lastJumpPress = 0;
        } else {
            lastJumpPress = now;
        }
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        ticks++;
        if (mode.is("Creative")) {
            p.getAbilities().mayfly = true;
            p.getAbilities().setFlyingSpeed(speed.f() * 0.05f);
            if (antiKick.get() && p.getAbilities().flying && ticks % 40 == 0) p.setDeltaMovement(p.getDeltaMovement().add(0, -0.04, 0));
            return;
        }
        p.getAbilities().flying = false;
        var in = p.input;
        float fwd = in.forwardImpulse, str = in.leftImpulse;
        double yaw = Math.toRadians(p.getYRot());
        double s = speed.value * (mc().options.keySprint.isDown() ? 1.4 : 1.0) * 0.5;
        double tx = (-Math.sin(yaw) * fwd + Math.cos(yaw) * str) * s;
        double tz = (Math.cos(yaw) * fwd + Math.sin(yaw) * str) * s;
        double ty = 0;
        if (in.jumping) ty += vertical.value;
        if (in.shiftKeyDown) ty -= vertical.value;
        Vec3 target = new Vec3(tx, ty, tz);
        double k = 1 - smoothing.value;
        vel = vel.add(target.subtract(vel).scale(k));
        double vy = vel.y;
        if (antiKick.get() && Math.abs(ty) < 1e-6 && ticks % 40 == 0) vy = -0.04;
        p.setDeltaMovement(vel.x, vy, vel.z);
        p.fallDistance = 0;
    }

    @Override
    public void onDisable() {
        var p = mc().player;
        if (p == null) return;
        boolean canFly = p.isCreative() || p.isSpectator();
        p.getAbilities().mayfly = canFly;
        if (!canFly) p.getAbilities().flying = false;
        p.getAbilities().setFlyingSpeed(0.05f);
        p.onUpdateAbilities();
        p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
        vel = Vec3.ZERO;
    }
}
