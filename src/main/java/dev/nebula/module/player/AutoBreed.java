package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;

import java.util.HashMap;
import java.util.Map;

/** Feeds nearby adult animals the food you're holding so they breed. */
public final class AutoBreed extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 2, 4.5, 4, 0.5, "m"));

    private final Map<Integer, Long> fed = new HashMap<>();

    public AutoBreed() {
        super("Auto Breed", "Feeds animals so they breed.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var held = p.getMainHandItem();
        long now = System.currentTimeMillis();
        for (Entity e : mc().level.entitiesForRendering()) {
            if (!(e instanceof Animal a) || a.isBaby() || !a.isFood(held) || a.distanceTo(p) > range.value) continue;
            if (now - fed.getOrDefault(a.getId(), 0L) < 300_000) continue; // vanilla breeding cooldown is 5 minutes
            mc().gameMode.interact(p, a, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
            fed.put(a.getId(), now);
            return;
        }
    }
}
