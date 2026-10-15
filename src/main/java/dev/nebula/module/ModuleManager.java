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
        add(new Reach());
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
        add(new LongJump());
        add(new HighJump());
        add(new AirJump());
        add(new OmniSprint());
        add(new ElytraFly());
        add(new BoatFly());
        add(new FastLadder());
        add(new Strafe());
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
        add(new StructureFinder());
        add(new AutoBuild());
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
        add(new AntiLava());
        add(new ChestStealer());
        add(new InventoryMove());
        add(new AutoRespawn());
        add(new AntiAfk());
        add(new Derp());
        add(new Copycat());
        add(new Dance());
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
        // Added in the big module pass
        add(new AutoPot());
        add(new AutoShield());
        add(new NoMissCooldown());
        add(new ShieldBreaker());
        add(new TargetStrafe());
        add(new AntiLevitation());
        add(new AntiVoid());
        add(new AutoJump());
        add(new AutoSneak());
        add(new EntitySpeed());
        add(new FastSwim());
        add(new IceSpeed());
        add(new NoJumpDelay());
        add(new ReverseStep());
        add(new Scaffold());
        add(new AutoBonemeal());
        add(new AutoBreed());
        add(new AutoDrop());
        add(new AutoFarm());
        add(new AutoFish());
        add(new AutoMine());
        add(new AutoReplenish());
        add(new AutoShear());
        add(new FastUse());
        add(new MiddleClickPearl());
        add(new AntiSpam());
        add(new ArmorAlert());
        add(new AutoReconnect());
        add(new ChatTimestamps());
        add(new DeathCoords());
        add(new UnfocusedFps());
        add(new VisualRange());
        add(new ChunkBorders());
        add(new CritParticles());
        add(new CustomFov());
        add(new Freelook());
        add(new Glow());
        add(new HitboxView());
        add(new ItemEsp());
        add(new LightOverlay());
        add(new LogoutSpots());
        add(new Nametags());
        add(new NoBlindness());
        add(new NoBob());
        add(new NoBossBar());
        add(new TntTimer());
        add(new Tracers());
        add(new ViewModel());
        add(new ShaderFx());
        add(new Waypoints());
        add(new ArrowHud());
        add(new BiomeHud());
        add(new ChunkHud());
        add(new DayHud());
        add(new DimensionHud());
        add(new DirectionHud());
        add(new DurabilityHud());
        add(new EntityCountHud());
        add(new FallHud());
        add(new FoodHud());
        add(new LightHud());
        add(new LookingAtHud());
        add(new MemoryHud());
        add(new MoonHud());
        add(new NearbyHud());
        add(new NetherCoordsHud());
        add(new RadarHud());
        add(new RotationHud());
        add(new ServerHud());
        add(new TpsHud());
        add(new WeatherHud());
        add(new WorldTimeHud());
        add(new XpHud());
        add(new AutoMlg());
        add(new AutoRocket());
        add(new Parachute());
        add(new WallJump());
        add(new AutoRightClick());
        add(new ElytraSwap());
        add(new NoBreakDelay());
        add(new DeathScreenshot());
        add(new MentionNotify());
        add(new BetterTooltips());
        add(new HideHand());
        add(new SelfNametag());
        add(new AbsorptionHud());
        add(new ArmorPointsHud());
        add(new BlockCountHud());
        add(new ComboHud());
        add(new DifficultyHud());
        add(new ElytraHud());
        add(new GameModeHud());
        add(new KdHud());
        add(new MotionHud());
        add(new PlaytimeHud());
        add(new SprintHud());
        add(new TargetDistanceHud());
        add(new AutoTorch());
        add(new Twerk());
        add(new ChatLogger());
        add(new CreeperAlert());
        add(new HitSound());
        add(new ItemPickupNotify());
        add(new ProjectileAlert());
        add(new BreakProgress());
        add(new LagHud());
        add(new OreHintHud());
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
