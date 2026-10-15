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
 * Survival flight with four modes:
 * "Vanilla" is creative-style flying, "Motion" gives direct control with separate horizontal
 * and vertical speeds, "Glide" lets you fall slowly while steering, and "Jetpack" thrusts you
 * up while jump is held. Everything eases in and out using Acceleration, sprint boosts speed,
 * and Anti Kick is the vanilla floating-kick avoidance (a tiny dip every couple of seconds).
 * Your abilities and fly speed are put back exactly as they were when you turn it off.
 */
public final class Flight extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Motion", "Vanilla", "Creative", "Motion", "Glide", "Jetpack"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.1, 5, 1, 0.1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical Speed", 0.1, 3, 0.6, 0.1));
    public final NumberSetting acceleration = add(new NumberSetting("Acceleration", 0.1, 1, 0.65, 0.05));
    public final NumberSetting glideFall = add(new NumberSetting("Glide Fall", 0.01, 0.3, 0.05, 0.01));
    public final NumberSetting thrust = add(new NumberSetting("Jetpack Thrust", 0.05, 0.5, 0.15, 0.01));
    public final NumberSetting sprintBoost = add(new NumberSetting("Sprint Boost", 1, 3, 1.5, 0.1, "x"));
    public final NumberSetting vBoost = add(new NumberSetting("Vertical Boost", 1, 4, 2, 0.1, "x"));
    public final BoolSetting smoothStop = add(new BoolSetting("Smooth Stop", true));
    public final BoolSetting pauseInMenus = add(new BoolSetting("Pause In Menus", true));
    public final BoolSetting antiKick = add(new BoolSetting("Anti Kick", true));
    public final BoolSetting doubleTapOff = add(new BoolSetting("Double-Tap Jump Off", true));
    public final BoolSetting stopOnDisable = add(new BoolSetting("Stop On Disable", true));

    private int ticks;
    private long lastJumpPress;
    private Vec3 vel = Vec3.ZERO;
    // saved ability state, restored on disable
    private boolean savedMayfly, savedFlying, saved;
    private float savedFlySpeed = 0.05f;

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
        var p = mc().player;
        vel = p != null ? p.getDeltaMovement() : Vec3.ZERO;
        saved = false;
        if (p != null) {
            savedMayfly = p.getAbilities().mayfly;
            savedFlying = p.getAbilities().flying;
            savedFlySpeed = p.getAbilities().getFlyingSpeed();
            saved = true;
            if (mode.is("Vanilla") || mode.is("Creative")) {
                p.getAbilities().mayfly = true;
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
            }
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key e) {
        // Jetpack and Glide use jump constantly, so double-tap off only applies to the other modes
        if (!isEnabled() || !doubleTapOff.get() || mc().screen != null || e.getAction() != GLFW.GLFW_PRESS) return;
        if (mode.is("Jetpack") || mode.is("Glide")) return;
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

    /** Vertical boost: sprint makes climbing/descending faster. */
    private double vb() {
        return mc().options.keySprint.isDown() ? vBoost.value : 1.0;
    }

    private double boost() {
        return mc().options.keySprint.isDown() ? sprintBoost.value : 1.0;
    }

    /** Horizontal target velocity from the movement keys. */
    private Vec3 horizontal(double s) {
        var p = mc().player;
        double[] d = dev.nebula.util.Move.dir(p);
        return new Vec3(d[0] * s, 0, d[1] * s);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        ticks++;
        boolean kickTick = antiKick.get() && ticks % 40 == 0;
        // menus open: hover in place, ignore movement keys
        if (pauseInMenus.get() && mc().screen != null) {
            vel = Vec3.ZERO;
            if (!mode.is("Vanilla") && !mode.is("Creative")) p.setDeltaMovement(0, mode.is("Glide") ? -glideFall.value : 0, 0);
            else if (p.getAbilities().flying) p.setDeltaMovement(0, 0, 0);
            p.fallDistance = 0;
            return;
        }
        if (mode.is("Creative")) {
            p.getAbilities().mayfly = true;
            if (!p.getAbilities().flying && !p.onGround()) p.getAbilities().flying = true;
            p.getAbilities().setFlyingSpeed((float) (speed.value * boost() * 0.05));
            // creative-style: no drift, stop when keys are released; vertical boost while sprinting
            boolean moving = p.input.forwardImpulse != 0 || p.input.leftImpulse != 0;
            Vec3 v = p.getDeltaMovement();
            double vy = v.y;
            if (p.input.jumping) vy = vertical.value * vb();
            else if (p.input.shiftKeyDown) vy = -vertical.value * vb();
            else vy = kickTick ? -0.04 : 0;
            Vec3 h = moving ? horizontal(speed.value * 0.5 * boost()) : Vec3.ZERO;
            p.setDeltaMovement(h.x, vy, h.z);
            p.fallDistance = 0;
            return;
        }
        if (mode.is("Vanilla")) {
            p.getAbilities().mayfly = true;
            if (!p.getAbilities().flying && !p.onGround()) p.getAbilities().flying = true;
            p.getAbilities().setFlyingSpeed((float) (speed.value * boost() * 0.05));
            if (kickTick && p.getAbilities().flying && !p.input.jumping) p.setDeltaMovement(p.getDeltaMovement().add(0, -0.04, 0));
            return;
        }
        if (p.getAbilities().flying && !(saved && savedFlying)) p.getAbilities().flying = false;
        var in = p.input;
        double k = acceleration.value;
        Vec3 target;
        switch (mode.get()) {
            case "Glide" -> {
                Vec3 h = horizontal(speed.value * 0.5 * boost());
                double ty = in.shiftKeyDown ? -vertical.value * vb() : -glideFall.value;
                target = new Vec3(h.x, ty, h.z);
            }
            case "Jetpack" -> {
                Vec3 h = horizontal(speed.value * 0.4 * boost());
                Vec3 v = p.getDeltaMovement();
                double vy = in.jumping ? Math.min(vertical.value, v.y + thrust.value) : v.y; // gravity handles the fall
                vel = vel.add(new Vec3(h.x, 0, h.z).subtract(vel.x, 0, vel.z).scale(k));
                p.setDeltaMovement(vel.x, vy, vel.z);
                if (in.jumping || vy > -0.5) p.fallDistance = 0;
                return;
            }
            default -> { // Motion
                Vec3 h = horizontal(speed.value * 0.5 * boost());
                double ty = 0;
                if (in.jumping) ty += vertical.value * vb();
                if (in.shiftKeyDown) ty -= vertical.value * vb();
                target = new Vec3(h.x, ty, h.z);
            }
        }
        // with Smooth Stop off, letting go of the keys stops you dead instead of easing out
        boolean idle = in.forwardImpulse == 0 && in.leftImpulse == 0;
        if (!smoothStop.get() && idle) vel = new Vec3(0, vel.y, 0);
        vel = vel.add(target.subtract(vel).scale(k));
        double vy = vel.y;
        if (kickTick && vy > -0.04 && !in.jumping) vy = -0.04;
        p.setDeltaMovement(vel.x, vy, vel.z);
        p.fallDistance = 0;
    }

    @Override
    public void onDisable() {
        var p = mc().player;
        vel = Vec3.ZERO;
        if (p == null) return;
        boolean canFly = p.isCreative() || p.isSpectator();
        if (saved) {
            p.getAbilities().mayfly = savedMayfly || canFly;
            p.getAbilities().flying = savedFlying && p.getAbilities().mayfly;
            p.getAbilities().setFlyingSpeed(savedFlySpeed);
        } else {
            p.getAbilities().mayfly = canFly;
            if (!canFly) p.getAbilities().flying = false;
            p.getAbilities().setFlyingSpeed(0.05f);
        }
        p.onUpdateAbilities();
        if (stopOnDisable.get()) p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
        saved = false;
    }
}
