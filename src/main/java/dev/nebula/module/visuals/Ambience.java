package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;

/** Client-side time of day and weather. Nobody else sees it. */
public final class Ambience extends Module {
    public final ModeSetting time = add(new ModeSetting("Time", "Sunset", "Sunrise", "Day", "Sunset", "Night", "Midnight", "Custom", "Cycle"));
    public final NumberSetting custom = add(new NumberSetting("Custom Time", 0, 24000, 13000, 100));
    public final NumberSetting cycleSpeed = add(new NumberSetting("Cycle Speed", 10, 400, 80, 10, "x"));
    public final BoolSetting clearWeather = add(new BoolSetting("Clear Weather", true));

    private double cycle = 0;

    public Ambience() {
        super("Ambience", "Lock the time of day and kill the rain (client only).", Category.WORLD);
        custom.visibleWhen(() -> time.is("Custom"));
        cycleSpeed.visibleWhen(() -> time.is("Cycle"));
    }

    @Override
    public String suffix() {
        return time.get();
    }

    private long targetTime() {
        return switch (time.get()) {
            case "Sunrise" -> 23200;
            case "Day" -> 6000;
            case "Night" -> 15000;
            case "Midnight" -> 18000;
            case "Custom" -> (long) custom.value;
            case "Cycle" -> (long) cycle;
            default -> 12350;
        };
    }

    @Override
    public void onTick() {
        if (time.is("Cycle")) cycle = (cycle + cycleSpeed.value) % 24000;
        apply();
    }

    /**
     * Re-applied every frame too: the server's time packets can land between ticks,
     * and without this the sky flashes back to server time for a frame.
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onFrame(net.minecraftforge.event.TickEvent.RenderTickEvent e) {
        if (isEnabled() && e.phase == net.minecraftforge.event.TickEvent.Phase.START) apply();
    }

    private void apply() {
        var level = mc().level;
        if (level == null) return;
        level.setDayTime(targetTime());
        if (clearWeather.get()) {
            level.setRainLevel(0);
            level.setThunderLevel(0);
        }
    }
}
