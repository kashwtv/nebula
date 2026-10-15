package dev.nebula.module.movement;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;

/**
 * Keeps walking forward for you. "Smart" also hops over single-block steps and stops at
 * drops and liquids. Releasing doesn't cancel a W key you're really holding.
 */
public final class AutoWalk extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Forward", "Forward", "Smart"));
    public final BoolSetting sprint = add(new BoolSetting("Sprint", false));
    public final BoolSetting pauseInGui = add(new BoolSetting("Pause In Menus", true));

    public AutoWalk() {
        super("Auto Walk", "Keeps walking forward for you.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    private static boolean physicallyDown(net.minecraft.client.KeyMapping k) {
        var key = k.getKey();
        long win = mc().getWindow().getWindow();
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() != -1 && InputConstants.isKeyDown(win, key.getValue());
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        if (pauseInGui.get() && mc().screen != null) { mc().options.keyUp.setDown(physicallyDown(mc().options.keyUp)); return; }
        boolean walk = true;
        if (mode.is("Smart")) {
            double yaw = Math.toRadians(p.getYRot());
            double dx = -Math.sin(yaw) * 0.6, dz = Math.cos(yaw) * 0.6;
            var bb = p.getBoundingBox();
            var ahead = bb.move(dx, 0, dz);
            var drop = ahead.setMinY(p.getY() - 3).setMaxY(p.getY() - 0.01);
            boolean liquidAhead = !p.level().getFluidState(net.minecraft.core.BlockPos.containing(p.getX() + dx, p.getY() - 0.5, p.getZ() + dz)).isEmpty();
            if (p.onGround() && (p.level().noCollision(p, drop) || liquidAhead)) walk = false;
            else if (p.onGround() && p.horizontalCollision && p.level().noCollision(p, ahead.move(0, 1.05, 0))) p.jumpFromGround();
        }
        mc().options.keyUp.setDown(walk || physicallyDown(mc().options.keyUp));
        if (sprint.get() && walk) p.setSprinting(true);
    }

    @Override
    public void onDisable() {
        mc().options.keyUp.setDown(physicallyDown(mc().options.keyUp));
    }
}
