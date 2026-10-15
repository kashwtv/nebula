package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/** Press jump while touching a wall in mid-air to kick off it. */
public final class WallJump extends Module {
    public final NumberSetting push = add(new NumberSetting("Push", 0.1, 0.8, 0.35, 0.05));

    public WallJump() {
        super("Wall Jump", "Jump off walls in mid-air.", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key e) {
        var p = mc().player;
        if (!isEnabled() || p == null || mc().screen != null || e.getAction() != GLFW.GLFW_PRESS || !mc().options.keyJump.matches(e.getKey(), e.getScanCode())) return;
        if (p.onGround() || !p.horizontalCollision) return;
        double yaw = Math.toRadians(p.getYRot());
        p.setDeltaMovement(Math.sin(yaw) * push.value, 0.42, -Math.cos(yaw) * push.value); // away from the wall you face
        p.fallDistance = 0;
    }
}
