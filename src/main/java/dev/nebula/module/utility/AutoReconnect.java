package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Reconnects to the last server a few seconds after you get disconnected. */
public final class AutoReconnect extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay", 1, 30, 5, 1, "s"));

    private ServerData last;
    private long at;

    public AutoReconnect() {
        super("Auto Reconnect", "Rejoins the server after a disconnect.", Category.CLIENT);
    }

    @Override
    public void update() {
        if (!isEnabled()) return;
        if (mc().getCurrentServer() != null) last = mc().getCurrentServer();
        if (!(mc().screen instanceof DisconnectedScreen) || last == null) { at = 0; return; }
        long now = System.currentTimeMillis();
        if (at == 0) at = now + (long) (delay.value * 1000);
        if (now < at) return;
        at = 0;
        ConnectScreen.startConnecting(new TitleScreen(), mc(), ServerAddress.parseString(last.ip), last, false);
    }
}
