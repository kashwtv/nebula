package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;

/**
 * Better elytra control. "Control" flies where you steer at a set speed, jump/sneak go up
 * and down, and letting go of everything hovers in place. "Boost" just adds speed along
 * where you're looking on top of normal gliding. Auto Start opens the elytra when you jump.
 */
public final class ElytraFly extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Control", "Control", "Boost"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.5, 4, 1.5, 0.1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical Speed", 0.1, 2, 0.6, 0.1));
    public final NumberSetting acceleration = add(new NumberSetting("Acceleration", 0.05, 1, 0.3, 0.05));
    public final NumberSetting boost = add(new NumberSetting("Boost", 0.01, 0.2, 0.05, 0.01));
    public final BoolSetting hover = add(new BoolSetting("Hover When Idle", true));
    public final BoolSetting autoStart = add(new BoolSetting("Auto Start", true));

    public ElytraFly() {
        super("Elytra Fly", "Control speed and hover with an elytra.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        if (!p.isFallFlying()) {
            if (autoStart.get() && !p.onGround() && p.getDeltaMovement().y < 0 && p.input.jumping && !p.isInWater()
                    && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() instanceof net.minecraft.world.item.ElytraItem
                    && p.tryToStartFallFlying()) {
                p.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(p,
                        net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
            return;
        }
        Vec3 v = p.getDeltaMovement();
        if (mode.is("Boost")) {
            if (p.input.forwardImpulse > 0) {
                Vec3 look = p.getLookAngle();
                Vec3 nv = v.add(look.scale(boost.value));
                if (nv.length() > speed.value) nv = nv.normalize().scale(speed.value);
                p.setDeltaMovement(nv);
            }
            return;
        }
        double[] d = dev.nebula.util.Move.dir(p);
        double ty = 0;
        if (p.input.jumping) ty += vertical.value;
        if (p.input.shiftKeyDown) ty -= vertical.value;
        boolean idle = d[0] == 0 && d[1] == 0 && ty == 0;
        Vec3 target = new Vec3(d[0] * speed.value, ty, d[1] * speed.value);
        if (idle && !hover.get()) target = new Vec3(v.x * 0.99, -0.03, v.z * 0.99);
        p.setDeltaMovement(v.add(target.subtract(v).scale(acceleration.value)));
        p.fallDistance = 0;
    }
}
