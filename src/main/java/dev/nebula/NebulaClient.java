package dev.nebula;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nebula.config.ConfigIO;
import dev.nebula.gui.HudRenderer;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.render.Shaders;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

public final class NebulaClient {
    public static final String CATEGORY = "key.categories.nebula";
    public static final KeyMapping OPEN_GUI = new KeyMapping("key.nebula.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY);
    public static final KeyMapping OPEN_HUD = new KeyMapping("key.nebula.hud", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping ZOOM = new KeyMapping("key.nebula.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

    private NebulaClient() {}

    static void init(IEventBus modBus) {
        ModuleManager.init();
        ModuleManager.snapshotDefaults();
        modBus.addListener(NebulaClient::onKeys);
        modBus.addListener(NebulaClient::onOverlays);
        modBus.addListener(NebulaClient::onSetup);
        modBus.addListener(Shaders::onRegister);
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        MinecraftForge.EVENT_BUS.register(ChatCommands.class);
        if (!net.minecraftforge.fml.loading.FMLEnvironment.production) MinecraftForge.EVENT_BUS.register(DevAutomation.class);
        for (Module m : ModuleManager.MODULES) {
            if (hasSubscribers(m.getClass())) MinecraftForge.EVENT_BUS.register(m);
        }
    }

    private static boolean hasSubscribers(Class<?> c) {
        for (Method m : c.getMethods()) if (m.isAnnotationPresent(SubscribeEvent.class)) return true;
        return false;
    }

    private static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(OPEN_GUI);
        e.register(OPEN_HUD);
        e.register(ZOOM);
    }

    private static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("hud", HudRenderer::render);
    }

    private static void onSetup(FMLClientSetupEvent e) {
        e.enqueueWork(ConfigIO::load);
    }
}
