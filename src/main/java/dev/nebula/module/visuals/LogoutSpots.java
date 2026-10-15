package dev.nebula.module.visuals;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.render.World3D;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Marks where nearby players logged out (they left the tab list while in view). */
public final class LogoutSpots extends Module {
    private record Spot(String name, AABB box, long time) {}

    private final Map<UUID, Spot> seen = new HashMap<>();
    private final Map<UUID, Spot> spots = new HashMap<>();

    public LogoutSpots() {
        super("Logout Spots", "Marks where players logged out.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        var conn = mc().getConnection();
        if (conn == null) return;
        Map<UUID, Spot> now = new HashMap<>();
        for (Player pl : mc().level.players()) if (pl != mc().player) now.put(pl.getUUID(), new Spot(pl.getGameProfile().getName(), pl.getBoundingBox(), System.currentTimeMillis()));
        for (var en : seen.entrySet()) {
            if (!now.containsKey(en.getKey()) && conn.getPlayerInfo(en.getKey()) == null) spots.put(en.getKey(), en.getValue());
        }
        for (UUID id : now.keySet()) spots.remove(id);
        seen.clear();
        seen.putAll(now);
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || spots.isEmpty() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var cam = e.getCamera().getPosition();
        var m = e.getPoseStack().last().pose();
        var b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, false);
        for (Spot s : spots.values()) World3D.boxEdges(b, m, s.box().move(-cam.x, -cam.y, -cam.z), 0xFFFF5555, 0.9f);
        World3D.end(b);
        for (Spot s : spots.values()) {
            long mins = (System.currentTimeMillis() - s.time()) / 60000;
            var c = s.box().getCenter();
            World3D.label(e.getPoseStack(), e.getCamera(), c.x - cam.x, s.box().maxY - cam.y + 0.4, c.z - cam.z, s.name() + " (" + mins + "m ago)", 0xFFFF7777, 1f);
        }
    }

    @Override
    public void onDisable() {
        spots.clear();
        seen.clear();
    }
}
