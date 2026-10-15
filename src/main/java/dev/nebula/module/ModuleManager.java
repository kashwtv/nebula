package dev.nebula.module;

import dev.nebula.module.client.ClickGuiModule;
import dev.nebula.module.client.HudEditorModule;
import dev.nebula.module.client.HudStyleModule;
import dev.nebula.module.client.NotificationsModule;
import dev.nebula.module.combat.*;
import dev.nebula.module.hud.*;
import dev.nebula.module.movement.*;
import dev.nebula.module.player.*;
import dev.nebula.module.world.*;
import dev.nebula.module.utility.*;
import dev.nebula.module.visuals.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Class<?>, Module> BY_CLASS = new HashMap<>();

    private ModuleManager() {}

    public static void init() {
        // Combat
        add(new AimAssist());
        add(new Triggerbot());
        add(new BowAim());
        add(new AutoTotem());
        add(new Velocity());
        add(new WTap());
        add(new AutoWeapon());
        add(new KillAura());
        add(new Criticals());
        add(new AutoClicker());
        add(new AutoLog());
        add(new AutoGap());
        add(new AutoBow());
        add(new AutoXp());
        add(new Surround());
        add(new AutoTrap());
        // Movement
        add(new ToggleSprint());
        add(new Flight());
        add(new Speed());
        add(new NoFall());
        add(new Step());
        add(new Spider());
        add(new NoSlow());
        add(new SafeWalk());
        add(new Parkour());
        add(new AutoWalk());
        add(new Jesus());
        // Visuals
        add(new Esp());
        add(new Fullbright());
        add(new Trajectories());
        add(new Crosshair());
        add(new HitMarker());
        add(new DamageNumbers());
        add(new KillEffects());
        add(new BlockOutline());
        add(new MotionBlur());
        add(new Trails());
        add(new JumpCircles());
        add(new HitParticles());
        add(new ChinaHat());
        add(new CleanView());
        add(new LowHpWarning());
        add(new Zoom());
        // World
        add(new BlockFinder());
        add(new StorageFinder());
        add(new PortalFinder());
        add(new Freecam());
        add(new Breadcrumbs());
        add(new Ambience());
        add(new Atmosphere());
        // Player
        add(new FastPlace());
        add(new FastBreak());
        add(new AutoTool());
        add(new AutoArmor());
        add(new AutoEat());
        add(new ChestStealer());
        add(new InventoryMove());
        add(new AutoRespawn());
        add(new AntiAfk());
        // HUD
        add(new Watermark());
        add(new ModuleList());
        add(new Coordinates());
        add(new ArmorHud());
        add(new HealthHud());
        add(new FpsHud());
        add(new CpsHud());
        add(new PingHud());
        add(new SpeedHud());
        add(new ReachHud());
        add(new ClockHud());
        add(new CompassHud());
        add(new PotionHud());
        add(new Keystrokes());
        add(new TargetHud());
        add(new InventoryHud());
        add(new ItemCounter());
        add(new SessionStats());
        // Client
        add(new ClickGuiModule());
        add(new HudEditorModule());
        add(new HudStyleModule());
        add(new NotificationsModule());
    }

    /** Call once after init so "Reset to defaults" knows the original values. */
    public static void snapshotDefaults() {
        for (Module m : MODULES) m.snapshotDefaults();
    }

    private static void add(Module m) {
        MODULES.add(m);
        BY_CLASS.put(m.getClass(), m);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> c) {
        return (T) BY_CLASS.get(c);
    }

    public static List<Module> of(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }

    public static List<HudModule> huds() {
        List<HudModule> out = new ArrayList<>();
        for (Module m : MODULES) if (m instanceof HudModule h) out.add(h);
        return out;
    }
}
