package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ShearsItem;

/** Shears nearby woolly sheep while you hold shears. */
public final class AutoShear extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 2, 4.5, 4, 0.5, "m"));

    private int cooldown;

    public AutoShear() {
        super("Auto Shear", "Shears sheep around you.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (cooldown-- > 0 || !(p.getMainHandItem().getItem() instanceof ShearsItem)) return;
        for (Entity e : mc().level.entitiesForRendering()) {
            if (e instanceof Sheep s && s.readyForShearing() && s.distanceTo(p) <= range.value) {
                mc().gameMode.interact(p, s, InteractionHand.MAIN_HAND);
                p.swing(InteractionHand.MAIN_HAND);
                cooldown = 5;
                return;
            }
        }
    }
}
