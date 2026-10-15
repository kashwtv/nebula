package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Place;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** Fires a rocket from your hotbar whenever your elytra speed drops below the threshold. */
public final class AutoRocket extends Module {
    public final NumberSetting minSpeed = add(new NumberSetting("Min Speed", 0.2, 2, 0.8, 0.1));
    public final NumberSetting delay = add(new NumberSetting("Delay", 10, 100, 30, 5, "t"));

    private int cooldown;

    public AutoRocket() {
        super("Auto Rocket", "Boosts your elytra with rockets.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (cooldown-- > 0 || !p.isFallFlying() || p.getDeltaMovement().length() > minSpeed.value) return;
        int slot = Place.findItem(Items.FIREWORK_ROCKET);
        if (slot < 0) return;
        int prev = p.getInventory().selected;
        p.getInventory().selected = slot;
        mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
        p.getInventory().selected = prev;
        cooldown = delay.i();
    }
}
