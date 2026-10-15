package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Less (or no) slowdown while eating, drinking, blocking, drawing a bow, or sneaking.
 * "Keep Speed" picks how much of your normal speed you keep (100% = no slowdown).
 */
public final class NoSlow extends Module {
    public final BoolSetting items = add(new BoolSetting("Using Items", true));
    public final BoolSetting sneak = add(new BoolSetting("Sneaking", false));
    public final NumberSetting keep = add(new NumberSetting("Keep Speed", 20, 100, 100, 5, "%"));
    public final BoolSetting keepSprint = add(new BoolSetting("Keep Sprint", true));

    private boolean wasSprinting;

    public NoSlow() {
        super("No Slow", "No slowdown when using items.", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onInput(MovementInputUpdateEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getEntity() != p) return;
        var in = e.getInput();
        double k = keep.value / 100.0;
        if (sneak.get() && p.isMovingSlowly()) {
            float f = Mth.clamp(0.3F + EnchantmentHelper.getSneakingSpeedBonus(p), 0.0F, 1.0F);
            if (f > 0) {
                float m = (float) Math.max(1, k / f);
                in.forwardImpulse *= m;
                in.leftImpulse *= m;
            }
        }
        // vanilla multiplies by 0.2 right after this event
        boolean using = items.get() && p.isUsingItem() && !p.isPassenger();
        if (using) {
            float m = (float) Math.max(1, k / 0.2);
            in.forwardImpulse *= m;
            in.leftImpulse *= m;
            if (keepSprint.get() && wasSprinting && in.forwardImpulse > 0) p.setSprinting(true);
        }
        if (!using) wasSprinting = p.isSprinting();
    }
}
