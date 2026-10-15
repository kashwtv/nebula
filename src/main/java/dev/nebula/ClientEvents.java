package dev.nebula;

import dev.nebula.config.ConfigIO;
import dev.nebula.gui.ClickGui;
import dev.nebula.gui.HudEditor;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.visuals.HitMarker;
import dev.nebula.module.visuals.HitParticles;
import dev.nebula.render.Projection;
import dev.nebula.util.Clicks;
import dev.nebula.util.Combat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/** Global client hooks that feed modules. */
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (NebulaClient.OPEN_GUI.consumeClick()) {
            if (mc.screen == null && mc.player != null) mc.setScreen(new ClickGui());
        }
        while (NebulaClient.OPEN_HUD.consumeClick()) {
            if (mc.screen == null && mc.player != null) mc.setScreen(new HudEditor());
        }
        ConfigIO.autosave();
        for (Module m : ModuleManager.MODULES) {
            try {
                if (m.bindMode == 2 && !m.isEnabled() && mc.player != null) m.setEnabled(true);
                m.update();
                if (m.isEnabled() && mc.player != null) m.onTick();
            } catch (Throwable t) {
                Nebula.LOG.error("Module {} crashed in tick; disabling it", m.name, t);
                m.setEnabled(false);
            }
        }
    }

    /** Modules that hold movement/attack keys for you: switched off when you leave a world. */
    private static final Class<?>[] KEY_HOLDERS = {
            dev.nebula.module.movement.AutoWalk.class, dev.nebula.module.player.Copycat.class, dev.nebula.module.player.Twerk.class,
            dev.nebula.module.player.AutoMine.class, dev.nebula.module.player.Dance.class, dev.nebula.module.combat.AutoShield.class,
            dev.nebula.module.player.AutoRightClick.class};

    /** Leaving a world: turn off key-holding modules and release every key we may have pressed. */
    @SubscribeEvent
    public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) {
        for (Module m : ModuleManager.MODULES) {
            for (Class<?> c : KEY_HOLDERS) {
                if (c.isInstance(m) && m.isEnabled() && m.bindMode != 2) {
                    try { m.setEnabled(false); } catch (Throwable t) { Nebula.LOG.warn("disable {} on logout failed", m.name, t); }
                }
            }
        }
        net.minecraft.client.KeyMapping.releaseAll();
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || e.getKey() == GLFW.GLFW_KEY_UNKNOWN) return;
        boolean changed = false;
        for (Module m : ModuleManager.MODULES) {
            if (m.key != e.getKey() || !m.toggleable()) continue;
            if (m.bindMode == 2) continue;
            if (m.bindMode == 1) {
                if (e.getAction() == GLFW.GLFW_PRESS) m.setEnabled(true);
                else if (e.getAction() == GLFW.GLFW_RELEASE) m.setEnabled(false);
            } else if (e.getAction() == GLFW.GLFW_PRESS) {
                m.toggle();
                changed = true;
            }
        }
        if (changed) ConfigIO.save();
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && e.getAction() == GLFW.GLFW_PRESS) Clicks.click(e.getButton());
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.getEntity() != mc.player || !(e.getTarget() instanceof LivingEntity target)) return;
        Combat.onHit(target);
        ModuleManager.get(dev.nebula.module.combat.WTap.class).onAttack();
        ModuleManager.get(HitMarker.class).onHit();
        ModuleManager.get(HitParticles.class).spawn(target);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) Projection.capture(e);
    }
}
