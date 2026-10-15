package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Col;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Tints the fog and horizon with a color and controls fog distance. */
public final class Atmosphere extends Module {
    public final ColorSetting color = add(new ColorSetting("Fog Color", 0.42f));
    public final NumberSetting tint = add(new NumberSetting("Tint", 0, 1, 0.35, 0.05));
    public final ModeSetting fog = add(new ModeSetting("Fog", "Default", "Default", "None", "Thick", "Custom"));
    public final NumberSetting distance = add(new NumberSetting("Fog Distance", 8, 512, 96, 8, "m"));

    public Atmosphere() {
        super("Atmosphere", "Colored fog and sky glow, or no fog at all.", Category.WORLD);
        distance.visibleWhen(() -> fog.is("Custom"));
    }

    @Override
    public String suffix() {
        return fog.get();
    }

    @SubscribeEvent
    public void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (!isEnabled() || tint.value <= 0) return;
        if (e.getCamera().getFluidInCamera() != FogType.NONE) return;
        int c = color.get();
        float k = tint.f();
        e.setRed(e.getRed() + (Col.r(c) - e.getRed()) * k);
        e.setGreen(e.getGreen() + (Col.g(c) - e.getGreen()) * k);
        e.setBlue(e.getBlue() + (Col.b(c) - e.getBlue()) * k);
    }

    @SubscribeEvent
    public void onFog(ViewportEvent.RenderFog e) {
        // only touch terrain fog: changing the sky pass makes the sky flicker
        if (!isEnabled() || fog.is("Default") || e.getType() != FogType.NONE || e.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float far = e.getFarPlaneDistance();
        switch (fog.get()) {
            case "None" -> {
                e.setNearPlaneDistance(far * 2f);
                e.setFarPlaneDistance(far * 2.5f);
            }
            case "Thick" -> {
                e.setNearPlaneDistance(0);
                e.setFarPlaneDistance(Math.min(far, 48));
            }
            default -> {
                e.setNearPlaneDistance(Math.min(far, distance.f()) * 0.25f);
                e.setFarPlaneDistance(Math.min(far, distance.f()));
            }
        }
        e.setCanceled(true);
    }
}
