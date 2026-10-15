package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Full speed while eating, drinking, blocking, drawing a bow, or sneaking. */
public final class NoSlow extends Module {
    public final BoolSetting items = add(new BoolSetting("Using Items", true));
    public final BoolSetting sneak = add(new BoolSetting("Sneaking", false));

    public NoSlow() {
        super("No Slow", "No slowdown when using items.", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onInput(MovementInputUpdateEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getEntity() != p) return;
        var in = e.getInput();
        if (sneak.get() && p.isMovingSlowly()) {
            float f = Mth.clamp(0.3F + EnchantmentHelper.getSneakingSpeedBonus(p), 0.0F, 1.0F);
            if (f > 0) {
                in.forwardImpulse /= f;
                in.leftImpulse /= f;
            }
        }
        // vanilla multiplies by 0.2 right after this event
        if (items.get() && p.isUsingItem() && !p.isPassenger()) {
            in.forwardImpulse *= 5f;
            in.leftImpulse *= 5f;
        }
    }
}
