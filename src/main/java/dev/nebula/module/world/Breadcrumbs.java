package dev.nebula.module.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.ArrayDeque;

/** Draws the path you've walked as a fading line, so you can always find your way back. */
public final class Breadcrumbs extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final NumberSetting length = add(new NumberSetting("Length", 50, 3000, 600, 50));
    public final NumberSetting lifetime = add(new NumberSetting("Fade After", 3, 300, 20, 1, "s"));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", false));

    private record Crumb(Vec3 pos, long time) {}

    private final ArrayDeque<Crumb> crumbs = new ArrayDeque<>();
    private Object lastLevel;

    public Breadcrumbs() {
        super("Breadcrumbs", "A line showing the path you've walked.", Category.WORLD);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        if (lastLevel != mc().level) {
            crumbs.clear();
            lastLevel = mc().level;
        }
        Vec3 pos = p.position().add(0, 0.1, 0);
        if (crumbs.isEmpty() || crumbs.peekLast().pos.distanceToSqr(pos) > 0.04) crumbs.add(new Crumb(pos, System.currentTimeMillis()));
        long cut = System.currentTimeMillis() - (long) (lifetime.value * 1000);
        while (!crumbs.isEmpty() && (crumbs.size() > length.i() || crumbs.peekFirst().time < cut)) crumbs.pollFirst();
    }

    @Override
    public void onDisable() {
        crumbs.clear();
    }

    /** Old crumbs keep expiring even while you stand still. */
    @Override
    public void update() {
        long cut = System.currentTimeMillis() - (long) (lifetime.value * 1000);
        while (!crumbs.isEmpty() && crumbs.peekFirst().time < cut) crumbs.pollFirst();
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || crumbs.size() < 2) return;
        Vec3 cam = e.getCamera().getPosition();
        Matrix4f m = e.getPoseStack().last().pose();
        RenderSystem.lineWidth(2.5f);
        BufferBuilder b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, !throughWalls.get());
        long now = System.currentTimeMillis();
        double life = lifetime.value * 1000;
        Crumb prev = null;
        int i = 0;
        for (Crumb c : crumbs) {
            if (prev != null && prev.pos.distanceToSqr(c.pos) < 16) {
                float a = (float) Math.max(0, 1 - (now - c.time) / life);
                int col = color.get(i * 0.02f);
                World3D.v(b, m, prev.pos.x - cam.x, prev.pos.y - cam.y, prev.pos.z - cam.z, col, a * 0.9f);
                World3D.v(b, m, c.pos.x - cam.x, c.pos.y - cam.y, c.pos.z - cam.z, col, a * 0.9f);
            }
            prev = c;
            i++;
        }
        World3D.end(b);
    }
}
