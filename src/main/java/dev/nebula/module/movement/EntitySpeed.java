package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.entity.vehicle.Boat;

/** Faster horses, pigs, striders and other rideable mobs (not boats; see Boat Fly). */
public final class EntitySpeed extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.2, 2, 0.6, 0.05));

    public EntitySpeed() {
        super("Entity Speed", "Ride mobs faster.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var v = p.getVehicle();
        if (v == null || v instanceof Boat || v.getControllingPassenger() != p) return;
        double[] d = dev.nebula.util.Move.dir(p);
        if (d[0] == 0 && d[1] == 0) return;
        var m = v.getDeltaMovement();
        v.setDeltaMovement(m.x + (d[0] * speed.value - m.x) * 0.4, m.y, m.z + (d[1] * speed.value - m.z) * 0.4);
    }
}
