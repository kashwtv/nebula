package dev.nebula.module.world;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Detaches the camera so you can fly around and look at things while your body stays put.
 * Your player doesn't move and can't break or place anything while it's on.
 */
public final class Freecam extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.1, 5, 1, 0.1));

    private ArmorStand cam;
    private float yaw, pitch;

    public Freecam() {
        super("Freecam", "Fly the camera around while your body stays still.", Category.WORLD);
    }

    @Override
    public void onEnable() {
        var p = mc().player;
        if (p == null || mc().level == null) {
            setEnabled(false);
            return;
        }
        cam = new ArmorStand(EntityType.ARMOR_STAND, mc().level);
        cam.setInvisible(true);
        cam.setNoGravity(true);
        cam.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        cam.xo = cam.getX();
        cam.yo = cam.getY();
        cam.zo = cam.getZ();
        yaw = p.getYRot();
        pitch = p.getXRot();
        mc().setCameraEntity(cam);
    }

    @Override
    public void onDisable() {
        if (mc().player != null) mc().setCameraEntity(mc().player);
        if (cam != null && mc().player != null) {
            mc().player.setYRot(yaw);
            mc().player.setXRot(pitch);
        }
        cam = null;
    }

    @Override
    public void onTick() {
        if (cam == null) return;
        if (cam.level() != mc().level) {
            setEnabled(false);
            return;
        }
        var o = mc().options;
        float fwd = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        float str = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        float up = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
        double s = speed.value * (o.keySprint.isDown() ? 2 : 1);
        double yr = Math.toRadians(cam.getYRot());
        double vx = (-Math.sin(yr) * fwd + Math.cos(yr) * str) * s;
        double vz = (Math.cos(yr) * fwd + Math.sin(yr) * str) * s;
        cam.xo = cam.getX();
        cam.yo = cam.getY();
        cam.zo = cam.getZ();
        cam.setPos(cam.getX() + vx, cam.getY() + up * s, cam.getZ() + vz);
    }

    /** Mouse look turns the camera, not your body. */
    @SubscribeEvent
    public void onFrame(TickEvent.RenderTickEvent e) {
        if (cam == null || mc().player == null) return;
        var p = mc().player;
        if (e.phase == TickEvent.Phase.START) {
            cam.yRotO = cam.getYRot();
            cam.xRotO = cam.getXRot();
            cam.setYRot(p.getYRot());
            cam.setXRot(p.getXRot());
            cam.yRotO = cam.getYRot();
            cam.xRotO = cam.getXRot();
        }
    }

    @SubscribeEvent
    public void onInput(MovementInputUpdateEvent e) {
        if (cam == null || e.getEntity() != mc().player) return;
        var in = e.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public void onInteract(InputEvent.InteractionKeyMappingTriggered e) {
        if (cam == null) return;
        e.setCanceled(true);
        e.setSwingHand(false);
    }

    @SubscribeEvent
    public void onHand(RenderHandEvent e) {
        if (cam != null) e.setCanceled(true);
    }
}
