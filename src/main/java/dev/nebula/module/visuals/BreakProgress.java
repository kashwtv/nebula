package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.util.Reflect;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Shows a percentage on the block you're mining. */
public final class BreakProgress extends Module {
    public BreakProgress() {
        super("Break Progress", "Shows how far a block is mined.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        var gm = mc().gameMode;
        if (!isEnabled() || gm == null || !gm.isDestroying() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!(mc().hitResult instanceof BlockHitResult b)) return;
        Object v = Reflect.get(MultiPlayerGameMode.class, gm, "f_105193_"); // destroyProgress
        if (!(v instanceof Float f)) return;
        var cam = e.getCamera().getPosition();
        var pos = b.getBlockPos();
        int c = dev.nebula.util.Col.health(f);
        dev.nebula.render.World3D.label(e.getPoseStack(), e.getCamera(), pos.getX() + 0.5 - cam.x, pos.getY() + 0.5 - cam.y, pos.getZ() + 0.5 - cam.z,
                (int) (f * 100) + "%", c, 1f);
    }
}
