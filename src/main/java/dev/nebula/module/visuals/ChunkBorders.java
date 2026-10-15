package dev.nebula.module.visuals;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Draws the edges of the chunk you're in (and its neighbours) as a wall grid. */
public final class ChunkBorders extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.12f));
    public final NumberSetting radius = add(new NumberSetting("Radius", 0, 3, 1, 1, " chunks"));
    public final NumberSetting height = add(new NumberSetting("Height", 4, 64, 16, 4, "m"));

    public ChunkBorders() {
        super("Chunk Borders", "Shows chunk edges around you.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var cam = e.getCamera().getPosition();
        var m = e.getPoseStack().last().pose();
        int cx = p.chunkPosition().x, cz = p.chunkPosition().z, r = radius.i();
        double y0 = Math.floor(p.getY()) - height.value / 2 - cam.y, y1 = y0 + height.value;
        int c = color.get();
        var b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, true);
        for (int i = cx - r; i <= cx + r + 1; i++) for (int j = cz - r; j <= cz + r + 1; j++) {
            double x = i * 16 - cam.x, z = j * 16 - cam.z;
            boolean own = (i == cx || i == cx + 1) && (j == cz || j == cz + 1);
            World3D.v(b, m, x, y0, z, c, own ? 0.9f : 0.4f);
            World3D.v(b, m, x, y1, z, c, own ? 0.9f : 0.4f);
        }
        for (double y = Math.ceil(y0 + cam.y); y <= y1 + cam.y; y += 4) {
            double yy = y - cam.y;
            double x0 = cx * 16 - cam.x, z0 = cz * 16 - cam.z;
            double[][] s = {{x0, z0, x0 + 16, z0}, {x0 + 16, z0, x0 + 16, z0 + 16}, {x0 + 16, z0 + 16, x0, z0 + 16}, {x0, z0 + 16, x0, z0}};
            for (double[] l : s) { World3D.v(b, m, l[0], yy, l[1], c, 0.5f); World3D.v(b, m, l[2], yy, l[3], c, 0.5f); }
        }
        World3D.end(b);
    }
}
