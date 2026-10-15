package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

/** Warns on your action bar (with a sound) when a creeper is close, louder when it's about to blow. */
public final class CreeperAlert extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 3, 16, 7, 1, "m"));

    private int cooldown;

    public CreeperAlert() {
        super("Creeper Alert", "Warns about nearby creepers.", Category.CLIENT);
    }

    @Override
    public void onTick() {
        if (cooldown-- > 0) return;
        var p = mc().player;
        for (Entity e : mc().level.entitiesForRendering()) {
            if (!(e instanceof Creeper c) || c.distanceTo(p) > range.value) continue;
            boolean fusing = c.getSwellDir() > 0;
            p.displayClientMessage(Component.literal((fusing ? "CREEPER ABOUT TO EXPLODE " : "Creeper nearby ") + String.format("(%.0fm)", c.distanceTo(p)))
                    .withStyle(fusing ? ChatFormatting.RED : ChatFormatting.GREEN), true);
            if (fusing) p.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
            cooldown = fusing ? 5 : 20;
            return;
        }
    }
}
