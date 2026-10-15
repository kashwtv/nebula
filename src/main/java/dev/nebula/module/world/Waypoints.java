package dev.nebula.module.world;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.render.World3D;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Beams at your last death spot and your spawn/bed area in the current world, with distance,
 * so you can always find your stuff again.
 */
public final class Waypoints extends Module {
    public final BoolSetting death = add(new BoolSetting("Last Death", true));
    public final BoolSetting spawn = add(new BoolSetting("World Spawn", true));

    private Vec3 deathPos;
    private String deathDim;
    private boolean wasDead;

    public Waypoints() {
        super("Waypoints", "Beams at your death point and spawn.", Category.WORLD);
    }

    @Override
    public void update() {
        var p = mc().player;
        if (p == null) return;
        boolean dead = p.isDeadOrDying();
        if (dead && !wasDead) { deathPos = p.position(); deathDim = p.level().dimension().location().toString(); }
        wasDead = dead;
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        String dim = p.level().dimension().location().toString();
        if (death.get() && deathPos != null && dim.equals(deathDim)) beam(e, deathPos, "Death", 0xFFFF5555);
        if (spawn.get()) {
            var sp = mc().level.getSharedSpawnPos();
            if (dim.equals("minecraft:overworld")) beam(e, Vec3.atBottomCenterOf(sp), "Spawn", 0xFF55FF55);
        }
    }

    private void beam(RenderLevelStageEvent e, Vec3 pos, String name, int c) {
        var cam = e.getCamera().getPosition();
        var m = e.getPoseStack().last().pose();
        double x = pos.x - cam.x, z = pos.z - cam.z;
        var b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, false);
        World3D.v(b, m, x, pos.y - cam.y, z, c, 0.9f);
        World3D.v(b, m, x, 320 - cam.y, z, c, 0.2f);
        World3D.end(b);
        double dist = mc().player.position().distanceTo(pos);
        // keep far labels on screen by drawing them at most 64 blocks away along the same line
        double k = dist > 64 ? 64 / dist : 1;
        World3D.label(e.getPoseStack(), e.getCamera(), x * k, (pos.y + 2 - cam.y) * k, z * k, name + String.format(" %.0fm", dist), c, 1.2f);
    }
}
