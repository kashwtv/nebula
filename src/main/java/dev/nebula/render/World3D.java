package dev.nebula.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.util.Col;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Translucent colored geometry in world space (positions relative to the camera). */
public final class World3D {
    private World3D() {}

    public static BufferBuilder begin(VertexFormat.Mode mode) {
        return begin(mode, true);
    }

    public static BufferBuilder begin(VertexFormat.Mode mode, boolean depth) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (depth) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        return b;
    }

    public static void end(BufferBuilder b) {
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    /** Six faces of a box (QUADS mode). */
    public static void boxQuads(BufferBuilder b, Matrix4f m, net.minecraft.world.phys.AABB bb, int c, float a) {
        double x0 = bb.minX, y0 = bb.minY, z0 = bb.minZ, x1 = bb.maxX, y1 = bb.maxY, z1 = bb.maxZ;
        double[][] f = {
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
                {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
        for (double[] q : f) for (int i = 0; i < 4; i++) v(b, m, q[i * 3], q[i * 3 + 1], q[i * 3 + 2], c, a);
    }

    /** Twelve edges of a box (DEBUG_LINES mode). */
    public static void boxEdges(BufferBuilder b, Matrix4f m, net.minecraft.world.phys.AABB bb, int c, float a) {
        double x0 = bb.minX, y0 = bb.minY, z0 = bb.minZ, x1 = bb.maxX, y1 = bb.maxY, z1 = bb.maxZ;
        double[][] l = {
                {x0, y0, z0, x1, y0, z0}, {x1, y0, z0, x1, y0, z1}, {x1, y0, z1, x0, y0, z1}, {x0, y0, z1, x0, y0, z0},
                {x0, y1, z0, x1, y1, z0}, {x1, y1, z0, x1, y1, z1}, {x1, y1, z1, x0, y1, z1}, {x0, y1, z1, x0, y1, z0},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x1, y0, z1, x1, y1, z1}, {x0, y0, z1, x0, y1, z1}};
        for (double[] s : l) {
            v(b, m, s[0], s[1], s[2], c, a);
            v(b, m, s[3], s[4], s[5], c, a);
        }
    }

    /** Camera-facing text label at a world position (relative to camera). */
    public static void label(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.Camera cam, double x, double y, double z,
                             String text, int color, float scale) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var buf = mc.renderBuffers().bufferSource();
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(cam.rotation());
        double dist = Math.sqrt(x * x + y * y + z * z);
        float s = (float) (0.025 * scale * Math.max(1, dist / 8));
        ps.scale(-s, -s, s);
        float w = mc.font.width(text);
        mc.font.drawInBatch(text, -w / 2, 0, color, false, ps.last().pose(), buf,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0x90000000, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        ps.popPose();
        buf.endBatch();
    }

    public static void v(BufferBuilder b, Matrix4f m, double x, double y, double z, int color) {
        b.vertex(m, (float) x, (float) y, (float) z).color(color).endVertex();
    }

    public static void v(BufferBuilder b, Matrix4f m, double x, double y, double z, int color, float alpha) {
        b.vertex(m, (float) x, (float) y, (float) z).color(Col.alpha(color, alpha)).endVertex();
    }
}
