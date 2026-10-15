package dev.nebula.module.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Accent-colored block selection with an optional breathing fill. */
public final class BlockOutline extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0.42f));
    public final BoolSetting fill = add(new BoolSetting("Fill", false));
    public final NumberSetting fillOpacity = add(new NumberSetting("Fill Opacity", 0.05, 0.6, 0.18, 0.01));
    public final BoolSetting breathe = add(new BoolSetting("Breathing", false));
    public final NumberSetting lineOpacity = add(new NumberSetting("Line Opacity", 0.1, 1, 0.7, 0.05));

    public BlockOutline() {
        super("Block Outline", "Glowing accent outline on the block you look at.", Category.VISUALS);
        fillOpacity.visibleWhen(fill::get);
        breathe.visibleWhen(fill::get);
        setEnabled(true);
    }

    @SubscribeEvent
    public void onHighlight(RenderHighlightEvent.Block e) {
        if (!isEnabled() || mc().level == null) return;
        BlockPos pos = e.getTarget().getBlockPos();
        BlockState st = mc().level.getBlockState(pos);
        if (st.isAir() || !mc().level.getWorldBorder().isWithinBounds(pos)) return;
        e.setCanceled(true);

        VoxelShape shape = st.getShape(mc().level, pos, CollisionContext.of(e.getCamera().getEntity()));
        if (shape.isEmpty()) return;
        Vec3 cam = e.getCamera().getPosition();
        double ox = pos.getX() - cam.x, oy = pos.getY() - cam.y, oz = pos.getZ() - cam.z;
        PoseStack ps = e.getPoseStack();
        int c = color.get();
        float r = Col.r(c), g = Col.g(c), b = Col.b(c);

        if (fill.get()) {
            float a = fillOpacity.f();
            if (breathe.get()) a *= 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 380.0);
            VertexConsumer fv = e.getMultiBufferSource().getBuffer(RenderType.debugFilledBox());
            for (AABB box : shape.toAabbs()) {
                double grow = 0.002;
                LevelRenderer.addChainedFilledBoxVertices(ps, fv,
                        box.minX + ox - grow, box.minY + oy - grow, box.minZ + oz - grow,
                        box.maxX + ox + grow, box.maxY + oy + grow, box.maxZ + oz + grow, r, g, b, a);
            }
        }

        VertexConsumer lv = e.getMultiBufferSource().getBuffer(RenderType.lines());
        PoseStack.Pose last = ps.last();
        float la = lineOpacity.f();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len == 0) return;
            dx /= len; dy /= len; dz /= len;
            int c1 = color.get((float) (x1 + y1 + z1));
            int c2 = color.get((float) (x2 + y2 + z2));
            lv.vertex(last.pose(), (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz))
                    .color(Col.r(c1), Col.g(c1), Col.b(c1), la).normal(last.normal(), dx, dy, dz).endVertex();
            lv.vertex(last.pose(), (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz))
                    .color(Col.r(c2), Col.g(c2), Col.b(c2), la).normal(last.normal(), dx, dy, dz).endVertex();
        });
    }
}
