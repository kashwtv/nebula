package dev.nebula.module.visuals;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/** Marks blocks where hostile mobs can spawn (block light 0, solid top, room above) with an X. */
public final class LightOverlay extends Module {
    public final NumberSetting radius = add(new NumberSetting("Radius", 4, 24, 12, 1, "m"));

    private final List<BlockPos> spots = new ArrayList<>();
    private int timer;

    public LightOverlay() {
        super("Light Overlay", "Shows where mobs can spawn.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        if (timer-- > 0) return;
        timer = 10;
        spots.clear();
        var p = mc().player;
        var lvl = mc().level;
        int r = radius.i();
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -4, -r), c.offset(r, 4, r))) {
            BlockPos below = pos.below();
            var st = lvl.getBlockState(below);
            if (!st.isFaceSturdy(lvl, below, Direction.UP) || !lvl.getBlockState(pos).getCollisionShape(lvl, pos).isEmpty()
                    || !lvl.getBlockState(pos.above()).getCollisionShape(lvl, pos.above()).isEmpty() || !lvl.getFluidState(pos).isEmpty()) continue;
            if (lvl.getBrightness(LightLayer.BLOCK, pos) == 0) spots.add(pos.immutable());
        }
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!isEnabled() || spots.isEmpty() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var cam = e.getCamera().getPosition();
        var m = e.getPoseStack().last().pose();
        var b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, true);
        for (BlockPos pos : spots) {
            double x = pos.getX() - cam.x, y = pos.getY() - cam.y + 0.02, z = pos.getZ() - cam.z;
            int c = mc().level.getBrightness(LightLayer.SKY, pos) > 7 ? 0xFFFFD040 : 0xFFFF4040;
            World3D.v(b, m, x + 0.1, y, z + 0.1, c, 0.8f); World3D.v(b, m, x + 0.9, y, z + 0.9, c, 0.8f);
            World3D.v(b, m, x + 0.9, y, z + 0.1, c, 0.8f); World3D.v(b, m, x + 0.1, y, z + 0.9, c, 0.8f);
        }
        World3D.end(b);
    }

    @Override
    public void onDisable() {
        spots.clear();
    }
}
