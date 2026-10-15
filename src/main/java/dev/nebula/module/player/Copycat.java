package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Harmless troll: follows a friend around like a shadow and mirrors their sneaks and jumps.
 * The target is whoever you're looking at when you turn it on, else the nearest player.
 */
public final class Copycat extends Module {
    public final NumberSetting range = add(new NumberSetting("Find Range", 4, 64, 24, 1, "m"));
    public final NumberSetting distance = add(new NumberSetting("Follow Distance", 1, 8, 2.5, 0.5, "m"));
    public final NumberSetting turn = add(new NumberSetting("Turn Speed", 0.1, 1, 0.35, 0.05));
    public final BoolSetting mirrorLook = add(new BoolSetting("Copy Look Direction", false));
    public final BoolSetting sprint = add(new BoolSetting("Sprint To Catch Up", true));

    private Player target;
    private double lastY;

    public Copycat() {
        super("Copycat", "Follow a player and copy their moves.", Category.PLAYER);
    }

    @Override
    public String suffix() {
        return target != null ? target.getGameProfile().getName() : null;
    }

    @Override
    public void onEnable() {
        target = null;
        var p = mc().player;
        if (p == null || mc().level == null) return;
        if (mc().hitResult instanceof EntityHitResult eh && eh.getEntity() instanceof Player pl) target = pl;
        else {
            double best = range.value;
            for (Player pl : mc().level.players()) {
                if (pl == p || pl.isSpectator()) continue;
                double d = p.distanceTo(pl);
                if (d < best) { best = d; target = pl; }
            }
        }
        if (target != null) lastY = target.getY();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var o = mc().options;
        if (mc().screen != null) { release(); return; }
        if (target == null || target.isRemoved() || !target.isAlive() || p.distanceTo(target) > range.value * 2) {
            release();
            target = null;
            onEnable();
            return;
        }
        double dx = target.getX() - p.getX(), dz = target.getZ() - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = mirrorLook.get() && dist <= distance.value ? target.getYRot() : (float) Math.toDegrees(Math.atan2(-dx, dz));
        p.setYRot(p.getYRot() + Mth.wrapDegrees(wantYaw - p.getYRot()) * turn.f());
        if (mirrorLook.get()) p.setXRot(p.getXRot() + (target.getXRot() - p.getXRot()) * turn.f());
        boolean walk = dist > distance.value;
        o.keyUp.setDown(walk);
        if (sprint.get() && dist > distance.value * 2) p.setSprinting(true);
        o.keyShift.setDown(target.isCrouching());
        boolean targetJumped = target.getY() > lastY + 0.2 && !target.onGround();
        if ((targetJumped || (walk && p.horizontalCollision)) && p.onGround()) p.jumpFromGround();
        lastY = target.getY();
    }

    private void release() {
        var o = mc().options;
        o.keyUp.setDown(false);
        o.keyShift.setDown(false);
    }

    @Override
    public void onDisable() {
        release();
        target = null;
    }
}
