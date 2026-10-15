package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/** Jump again in mid-air. "Max Jumps" 0 means unlimited. */
public final class AirJump extends Module {
    public final NumberSetting maxJumps = add(new NumberSetting("Max Jumps", 0, 10, 1, 1));

    private int used;

    public AirJump() {
        super("Air Jump", "Jump again while in the air.", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key e) {
        var p = mc().player;
        if (!isEnabled() || p == null || mc().screen != null || e.getAction() != GLFW.GLFW_PRESS) return;
        if (!mc().options.keyJump.matches(e.getKey(), e.getScanCode())) return;
        if (p.onGround() || p.isInWater() || p.getAbilities().flying || p.isFallFlying() || p.isPassenger()) return;
        if (maxJumps.i() > 0 && used >= maxJumps.i()) return;
        p.jumpFromGround();
        p.fallDistance = 0;
        used++;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p != null && p.onGround()) used = 0;
    }
}
