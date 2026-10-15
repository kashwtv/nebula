package dev.nebula.module.visuals;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

/** Translucent spinning cone hat over your head in third person. */
public final class ChinaHat extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final NumberSetting radius = add(new NumberSetting("Radius", 0.3, 1.2, 0.7, 0.05));
    public final NumberSetting height = add(new NumberSetting("Height", 0.1, 0.8, 0.3, 0.05));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.1, 1, 0.45, 0.05));
    public final BoolSetting firstPerson = add(new BoolSetting("First Person", false));

    public ChinaHat() {
        super("China Hat", "A glowing cone hat floating over your head.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var p = mc().player;
        if (p == null || p.isInvisible()) return;
        if (mc().options.getCameraType().isFirstPerson() && !firstPerson.get()) return;
        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        double x = Mth.lerp(pt, p.xOld, p.getX()) - cam.x;
        double y = Mth.lerp(pt, p.yOld, p.getY()) + p.getBbHeight() + 0.08 - cam.y;
        double z = Mth.lerp(pt, p.zOld, p.getZ()) - cam.z;
        float r = radius.f(), hh = height.f(), op = opacity.f();
        double spin = (System.currentTimeMillis() % 6000) / 6000.0 * Math.PI * 2;
        int seg = 48;
        Matrix4f m = e.getPoseStack().last().pose();

        BufferBuilder b = World3D.begin(VertexFormat.Mode.TRIANGLES);
        for (int i = 0; i < seg; i++) {
            double a0 = spin + Math.PI * 2 * i / seg, a1 = spin + Math.PI * 2 * (i + 1) / seg;
            int c0 = color.get((float) (i * 6.0 / seg)), c1 = color.get((float) ((i + 1) * 6.0 / seg));
            World3D.v(b, m, x, y + hh, z, color.get(0), op * 0.35f);
            World3D.v(b, m, x + Math.cos(a0) * r, y, z + Math.sin(a0) * r, c0, op);
            World3D.v(b, m, x + Math.cos(a1) * r, y, z + Math.sin(a1) * r, c1, op);
        }
        World3D.end(b);

        // bright rim
        BufferBuilder rim = World3D.begin(VertexFormat.Mode.QUADS);
        for (int i = 0; i < seg; i++) {
            double a0 = spin + Math.PI * 2 * i / seg, a1 = spin + Math.PI * 2 * (i + 1) / seg;
            int c0 = color.get((float) (i * 6.0 / seg)), c1 = color.get((float) ((i + 1) * 6.0 / seg));
            float r2 = r + 0.025f;
            World3D.v(rim, m, x + Math.cos(a0) * r, y, z + Math.sin(a0) * r, c0, 1);
            World3D.v(rim, m, x + Math.cos(a1) * r, y, z + Math.sin(a1) * r, c1, 1);
            World3D.v(rim, m, x + Math.cos(a1) * r2, y - 0.012, z + Math.sin(a1) * r2, c1, 0);
            World3D.v(rim, m, x + Math.cos(a0) * r2, y - 0.012, z + Math.sin(a0) * r2, c0, 0);
        }
        World3D.end(rim);
    }
}
