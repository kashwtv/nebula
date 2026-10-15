package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.FishingRodItem;

/** Reels in when the bobber dips (a bite) and casts again after a short delay. */
public final class AutoFish extends Module {
    public final NumberSetting recast = add(new NumberSetting("Recast Delay", 5, 60, 15, 1, "t"));

    private int wait, castAge;

    public AutoFish() {
        super("Auto Fish", "Catches fish for you.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (!(p.getMainHandItem().getItem() instanceof FishingRodItem) || mc().screen != null) return;
        var hook = p.fishing;
        if (hook == null) {
            if (wait-- <= 0) { mc().gameMode.useItem(p, InteractionHand.MAIN_HAND); p.swing(InteractionHand.MAIN_HAND); castAge = 0; wait = recast.i(); }
            return;
        }
        castAge++;
        // a bite yanks the bobber down sharply while it's floating in water
        if (castAge > 20 && hook.isInWater() && hook.getDeltaMovement().y < -0.08 && Math.abs(hook.getDeltaMovement().x) < 0.05) {
            mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
            wait = recast.i();
        }
    }
}
