package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;

/**
 * Keeps you fed. Hunger is decided by the server: in singleplayer / a LAN world you host, the integrated
 * server's copy of you gets food and saturation topped up every tick. On someone else's server nothing is
 * spoofed; the only (honest) client-side help is optionally not sprinting, since sprinting drains food fastest.
 */
public final class AntiHunger extends Module {
    public final BoolSetting saturation = add(new BoolSetting("Full Saturation", true));
    public final BoolSetting noSprintMp = add(new BoolSetting("No Sprint On Servers", false));

    public AntiHunger() {
        super("Anti Hunger", "Full food in singleplayer/hosted worlds; servers control hunger elsewhere.", Category.PLAYER);
    }

    @Override
    public String suffix() {
        return mc().hasSingleplayerServer() ? "SP" : "server";
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        var server = mc().getSingleplayerServer();
        if (server != null) {
            var uuid = p.getUUID();
            server.execute(() -> {
                var sp = server.getPlayerList().getPlayer(uuid);
                if (sp == null) return;
                var f = sp.getFoodData();
                f.setFoodLevel(20);
                if (saturation.get()) f.setSaturation(20f);
                f.setExhaustion(0f);
            });
        } else if (noSprintMp.get() && p.isSprinting()) {
            p.setSprinting(false);
        }
    }
}
