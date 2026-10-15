package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Plays a satisfying sound (only for you) every time you hit something. */
public final class HitSound extends Module {
    public final ModeSetting sound = add(new ModeSetting("Sound", "Ding", "Ding", "Click", "Bass", "Pop"));
    public final NumberSetting volume = add(new NumberSetting("Volume", 0.1, 1, 0.6, 0.1));

    public HitSound() {
        super("Hit Sound", "Sound on every hit.", Category.CLIENT);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getEntity() != p) return;
        SoundEvent s = switch (sound.get()) {
            case "Click" -> SoundEvents.UI_BUTTON_CLICK.value();
            case "Bass" -> SoundEvents.NOTE_BLOCK_BASS.value();
            case "Pop" -> SoundEvents.ITEM_PICKUP;
            default -> SoundEvents.ARROW_HIT_PLAYER;
        };
        p.playSound(s, volume.f(), 1f);
    }
}
