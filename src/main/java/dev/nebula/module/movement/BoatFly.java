package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

/**
 * Fly the boat (or any ridden vehicle you control). Steer with your camera, move with the
 * movement keys, jump to rise and sprint key to sink (sneak still dismounts).
 */
public final class BoatFly extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.2, 3, 1, 0.1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical Speed", 0.1, 2, 0.4, 0.1));
    public final NumberSetting acceleration = add(new NumberSetting("Acceleration", 0.1, 1, 0.4, 0.05));
    public final NumberSetting idleFall = add(new NumberSetting("Idle Fall", 0, 0.1, 0.0, 0.01));
    public final BoolSetting boatsOnly = add(new BoolSetting("Boats Only", true));
    public final BoolSetting yawLock = add(new BoolSetting("Face Camera", true));

    public BoatFly() {
        super("Boat Fly", "Fly with boats and other vehicles.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        var veh = p.getVehicle();
        if (veh == null || veh.getControllingPassenger() != p || (boatsOnly.get() && !(veh instanceof Boat))) return;
        if (yawLock.get()) veh.setYRot(p.getYRot());
        double[] d = dev.nebula.util.Move.dir(p);
        double ty = -idleFall.value;
        if (mc().options.keyJump.isDown()) ty = vertical.value;
        else if (mc().options.keySprint.isDown()) ty = -vertical.value;
        Vec3 target = new Vec3(d[0] * speed.value, ty, d[1] * speed.value);
        Vec3 v = veh.getDeltaMovement();
        veh.setDeltaMovement(v.add(target.subtract(v).scale(acceleration.value)));
        veh.fallDistance = 0;
    }
}
