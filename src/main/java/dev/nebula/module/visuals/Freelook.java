package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.client.CameraType;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Look around freely while you keep walking straight: the camera follows your mouse but your
 * body keeps its direction. Best bound with "Hold". Optional third-person while active.
 */
public final class Freelook extends Module {
    public final BoolSetting thirdPerson = add(new BoolSetting("Third Person", true));

    private float camYaw, camPitch, lockYaw, lockPitch;
    private CameraType saved;

    public Freelook() {
        super("Freelook", "Look around without turning.", Category.VISUALS);
    }

    @Override
    public void onEnable() {
        var p = mc().player;
        if (p == null) return;
        camYaw = lockYaw = p.getYRot();
        camPitch = lockPitch = p.getXRot();
        saved = mc().options.getCameraType();
        if (thirdPerson.get()) mc().options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    /** Every frame: mouse turned the player; move that turn onto the camera and put the body back. */
    @SubscribeEvent
    public void onFrame(TickEvent.RenderTickEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.phase != TickEvent.Phase.END) return;
        camYaw += p.getYRot() - lockYaw;
        camPitch = Math.max(-90, Math.min(90, camPitch + p.getXRot() - lockPitch));
        p.setYRot(lockYaw);
        p.setXRot(lockPitch);
        p.yRotO = lockYaw;
        p.xRotO = lockPitch;
    }

    @SubscribeEvent
    public void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (!isEnabled() || mc().player == null) return;
        e.setYaw(camYaw);
        e.setPitch(camPitch);
    }

    @Override
    public void onDisable() {
        if (saved != null) mc().options.setCameraType(saved);
        saved = null;
    }
}
