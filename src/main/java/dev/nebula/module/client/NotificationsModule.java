package dev.nebula.module.client;

import dev.nebula.module.Category;
import dev.nebula.module.Module;

public final class NotificationsModule extends Module {
    public NotificationsModule() {
        super("Notifications", "Toast when a module is switched on or off.", Category.CLIENT);
        setEnabled(true);
    }

    @Override
    public boolean listed() {
        return false;
    }
}
