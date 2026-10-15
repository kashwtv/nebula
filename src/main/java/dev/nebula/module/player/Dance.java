package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Harmless troll / victory emote: loops sneak, jump and a spin. The spin is only sent to
 * the server, so your camera doesn't whirl around.
 */
public final class Dance extends Module {
    public final NumberSetting tempo = add(new NumberSetting("Tempo", 2, 20, 6, 1, "t"));
    public final BoolSetting sneak = add(new BoolSetting("Sneak", true));
    public final BoolSetting jump = add(new BoolSetting("Jump", true));
    public final BoolSetting spin = add(new BoolSetting("Spin", true));

    private int t;
    private float yaw;

    public Dance() {
        super("Dance", "Sneak, jump and spin on a loop.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        if (mc().screen != null) { mc().options.keyShift.setDown(false); return; }
        t++;
        int beat = t / tempo.i();
        if (sneak.get()) mc().options.keyShift.setDown(beat % 2 == 0);
        if (jump.get() && beat % 4 == 3 && t % tempo.i() == 0 && p.onGround()) p.jumpFromGround();
        if (spin.get() && p.connection != null) {
            yaw = (yaw + 360f / (tempo.f() * 4)) % 360;
            p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, p.getXRot(), p.onGround()));
            p.yHeadRot = yaw;
        }
    }

    @Override
    public void onDisable() {
        mc().options.keyShift.setDown(false);
        var p = mc().player;
        if (p != null && p.connection != null) {
            p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), p.getXRot(), p.onGround()));
            p.yHeadRot = p.getYRot();
        }
    }
}
