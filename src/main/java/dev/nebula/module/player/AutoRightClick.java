package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

/** Right-clicks for you at a set rate (placing, using, farming machines). */
public final class AutoRightClick extends Module {
    public final NumberSetting cps = add(new NumberSetting("CPS", 1, 20, 8, 0.5));

    private long next;

    public AutoRightClick() {
        super("Auto Right Click", "Clicks right mouse for you.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        long now = System.currentTimeMillis();
        if (now < next || mc().screen != null) return;
        next = now + (long) (1000 / cps.value * ThreadLocalRandom.current().nextDouble(0.9, 1.1));
        var hr = mc().hitResult;
        if (hr instanceof BlockHitResult b && hr.getType() == HitResult.Type.BLOCK) {
            if (mc().gameMode.useItemOn(p, InteractionHand.MAIN_HAND, b).consumesAction()) { p.swing(InteractionHand.MAIN_HAND); return; }
        }
        mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
    }
}
