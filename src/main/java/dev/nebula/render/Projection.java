package dev.nebula.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Captures the world camera each frame so HUD code can map world positions to screen positions. */
public final class Projection {
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f PROJ = new Matrix4f();
    private static Vec3 cam = Vec3.ZERO;
    public static float partialTick;

    private Projection() {}

    public static void capture(RenderLevelStageEvent e) {
        VIEW.set(e.getPoseStack().last().pose());
        PROJ.set(e.getProjectionMatrix());
        cam = e.getCamera().getPosition();
        partialTick = e.getPartialTick();
    }

    public static Vec3 camera() {
        return cam;
    }

    /**
     * World position to GUI-scaled screen coords. Returns null when behind the camera.
     * Result is {x, y} in vanilla GUI units.
     */
    public static float[] toScreen(double x, double y, double z) {
        Vector4f v = new Vector4f((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z), 1f);
        VIEW.transform(v);
        PROJ.transform(v);
        if (v.w <= 0.01f) return null;
        float nx = v.x / v.w, ny = v.y / v.w;
        var win = Minecraft.getInstance().getWindow();
        return new float[]{(nx * 0.5f + 0.5f) * win.getGuiScaledWidth(), (0.5f - ny * 0.5f) * win.getGuiScaledHeight()};
    }
}
