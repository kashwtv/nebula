package dev.nebula.module.visuals;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import dev.nebula.util.Ease;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Shockwave rings expanding on the ground every time you jump. */
public final class JumpCircles extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final NumberSetting size = add(new NumberSetting("Size", 0.5, 3, 1.4, 0.1));
    public final NumberSetting duration = add(new NumberSetting("Duration", 300, 2000, 900, 50, "ms"));

    private record Circle(Vec3 pos, long start) {}

    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround = true;

    public JumpCircles() {
        super("Jump Circles", "Glowing shockwave rings when you jump.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        if (wasOnGround && !p.onGround() && p.getDeltaMovement().y > 0.1) {
            circles.add(new Circle(p.position(), System.currentTimeMillis()));
        }
        wasOnGround = p.onGround();
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || circles.isEmpty()) return;
        long now = System.currentTimeMillis();
        circles.removeIf(c -> now - c.start > duration.value);
        if (circles.isEmpty()) return;
        Vec3 cam = e.getCamera().getPosition();
        Matrix4f m = e.getPoseStack().last().pose();
        BufferBuilder b = World3D.begin(VertexFormat.Mode.QUADS);
        int seg = 72;
        for (Circle c : circles) {
            float t = (float) ((now - c.start) / duration.value);
            float r = size.f() * Ease.outExpo(t);
            float a = 1 - Ease.inCubic(t);
            double cx = c.pos.x - cam.x, cy = c.pos.y + 0.02 - cam.y, cz = c.pos.z - cam.z;
            float inner = r * 0.6f, outer = r + 0.05f + 0.05f * (1 - t);
            for (int i = 0; i < seg; i++) {
                double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
                int col0 = color.get((float) (i * 6.0 / seg)), col1 = color.get((float) ((i + 1) * 6.0 / seg));
                double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                // inner fade band
                World3D.v(b, m, cx + c0 * inner, cy, cz + s0 * inner, col0, 0);
                World3D.v(b, m, cx + c1 * inner, cy, cz + s1 * inner, col1, 0);
                World3D.v(b, m, cx + c1 * r, cy, cz + s1 * r, col1, a * 0.55f);
                World3D.v(b, m, cx + c0 * r, cy, cz + s0 * r, col0, a * 0.55f);
                // hot outer edge
                World3D.v(b, m, cx + c0 * r, cy, cz + s0 * r, col0, a);
                World3D.v(b, m, cx + c1 * r, cy, cz + s1 * r, col1, a);
                World3D.v(b, m, cx + c1 * outer, cy, cz + s1 * outer, col1, 0);
                World3D.v(b, m, cx + c0 * outer, cy, cz + s0 * outer, col0, 0);
            }
        }
        World3D.end(b);
    }
}
