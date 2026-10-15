package dev.nebula.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Target filtering and angle math shared by combat modules. */
public final class Targets {
    private Targets() {}

    /**
     * Players / monsters (anything hostile, incl. slimes and the dragon) / animals.
     * Never friends, your own tamed pets or armor stands.
     */
    public static boolean valid(Entity e, boolean players, boolean monsters, boolean animals, boolean invisibles) {
        var me = Minecraft.getInstance().player;
        if (!(e instanceof LivingEntity le) || e == me || !le.isAlive() || le.isDeadOrDying()) return false;
        if (e instanceof ArmorStand) return false;
        if (e.isInvisible() && !invisibles) return false;
        if (Friends.is(e)) return false;
        if (e instanceof Player p) return players && !p.isSpectator() && !p.isCreative();
        if (e instanceof Enemy) return monsters;
        if (e instanceof net.minecraft.world.entity.TamableAnimal t && me != null && t.isOwnedBy(me)) return false;
        // every other mob: animals, villagers, golems, bats, fish...
        if (e instanceof net.minecraft.world.entity.Mob) return animals;
        return false;
    }

    /** {yaw, pitch} needed to look from the eyes at a point. */
    public static float[] angles(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(dy, h) * Mth.RAD_TO_DEG);
        return new float[]{yaw, pitch};
    }

    /** Angular distance from where the player is looking to a point. */
    public static float fovTo(Vec3 point) {
        var me = Minecraft.getInstance().player;
        float[] a = angles(me.getEyePosition(), point);
        float dy = Mth.wrapDegrees(a[0] - me.getYRot());
        float dp = a[1] - me.getXRot();
        return (float) Math.sqrt(dy * dy + dp * dp);
    }
}
