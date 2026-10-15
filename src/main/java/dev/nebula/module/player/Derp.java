package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Harmless troll: makes your head spin, nod or jerk around for everyone else. Only the
 * rotation sent to the server changes; your own camera stays where you aim it.
 */
public final class Derp extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Random", "Random", "Spin", "Nod", "Headless"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 5, 60, 25, 5, "°"));
    public final BoolSetting pauseOnUse = add(new BoolSetting("Pause While Using", true));

    private float yaw, pitch;
    private int t;

    public Derp() {
        super("Derp", "Spins and jerks your head for others to see.", Category.PLAYER);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || p.connection == null) return;
        if (pauseOnUse.get() && (p.isUsingItem() || mc().options.keyAttack.isDown() || mc().options.keyUse.isDown())) return;
        t++;
        var r = ThreadLocalRandom.current();
        switch (mode.get()) {
            case "Spin" -> { yaw = (yaw + speed.f()) % 360; pitch = p.getXRot(); }
            case "Nod" -> { yaw = p.getYRot(); pitch = (float) Math.sin(t * speed.value / 60.0) * 80f; }
            case "Headless" -> { yaw = p.getYRot(); pitch = 180f; }
            default -> { yaw = r.nextFloat() * 360f; pitch = r.nextFloat() * 180f - 90f; }
        }
        p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, p.onGround()));
        p.yHeadRot = yaw; // shows in your own third-person view too
    }

    @Override
    public void onDisable() {
        var p = mc().player;
        if (p != null && p.connection != null) {
            p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), p.getXRot(), p.onGround()));
            p.yHeadRot = p.getYRot();
        }
    }
}
