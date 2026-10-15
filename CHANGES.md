# Nebula changes

Built against the unchanged gradle.properties: Minecraft 1.20.1, Forge 47.4.10, official 1.20.1 mappings, Java 17.

## Totals

- Combat: 22 modules (6 new)
- Movement: 33 modules (22 new)
- Visuals: 38 modules (21 new)
- World: 8 modules (1 new)
- Player: 27 modules (18 new)
- Hud: 54 modules (37 new)
- Client: 18 modules (14 new)
- **Total: 200 modules (119 new, 81 existing)**

## v4 (2026-10-15): Auto Build for survival

- **Builds legitimately in survival.** Blocks come from your inventory. Anything in the main inventory gets swapped into the *Swap Slot*. It uses your real reach (4.5 in survival) and turns to look at each face it clicks. *Blocks Per Tick* and *Delay* set the speed.
- **Correct block states.** Before each click it tries every face, click spot and look direction until the result is exactly the schematic's block: stair facing and top/bottom half, slab half, double slabs (two clicks), log/wood axis, trapdoor half, door hinge, chest pairs, wall vs standing torches, hanging lanterns, ladders, observers, furnaces and so on. Stairs and logs no longer just take the direction you're facing. *Fix States* (on by default) right-clicks afterwards to open trapdoors, doors and gates, and puts out campfires with a shovel.
- **Build order.** It works from the bottom up and never places a floating block. It always clicks a real neighbour, and torches, carpets, lanterns and ladders wait until their support is there. It sneak-clicks when the only neighbour is a chest, table or trapdoor, so it doesn't open them.
- **Helper Blocks** (on): when no neighbour gives the right state (for example a sideways log on bare floor), it puts down a throwaway cobble/dirt (or a spare full block), places against it, then mines it away again.
- **Can't-place handling.** Fluids, technical blocks (barrier, command block, light...) and blocks with no item are skipped and listed. A block with nothing to click on and no helper option (truly floating) is skipped with a chat message after the build stalls on it.
- **Obstructions.** *Clear Obstructions* (off) mines wrong blocks with the best tool in your inventory. Otherwise they're outlined red in the preview (yellow means right block, wrong state; grey means can't be placed).
- **Materials.** Exact need/have/missing counts (double slabs count 2, door tops 0). When you run out it waits on those blocks, keeps placing everything else, and carries on by itself when you pick more up.
- **Controls.** Start/Resume, Pause, Stop and Skip block, with progress % and ETA. Pause state and progress survive closing the screen. Chat commands: `.build start|pause|resume|stop|skip|mats|copy`.
- **Screen.** It shows the full materials list for the selected schematic before you start: icon, name, need, have, missing, sorted by what's missing, scrollable, with total blocks, a "N blocks missing / You have everything" line, a "can't be placed" section, and **Copy** (copies the list to the clipboard). The status pill shows Building, Paused, Missing blocks, Move closer, Waiting or Done.
- **Build HUD** (on): a compact overlay while building with %, count, ETA, status and the top 5 materials still needed (red when missing).
- **Schematic loading.** Blocks from mods that wrap vanilla ones (for example `diagonalfences:minecraft/spruce_fence`) are mapped to the vanilla block instead of silently disappearing. Unknown ids are listed when loading.
- **Removed:** *Air Place* from Auto Build. That's not something a survival player can do.
- **Limits:** it doesn't walk. When nothing is in reach it says "Move closer" (HUD and action bar) and continues when you do. It can't waterlog blocks or place water/lava. Campfires can be put out but not lit.
- Dev only: `run/nebula_abtest` (AbHarness) builds the presets, a state test litematic (plain, rot90+mirror, rot180), `kash1.litematic`, `kash2.schem`, an obstruction case and an out-of-materials case in a survival world, then compares the world block-for-block. It also clicks through every Auto Build screen button. The harness teleports the player when the build asks you to move closer, in place of walking.
- **Test results (survival, real reach):** `kash1.litematic` 21x8x15, 943 blocks, 20 materials: 943/943 exact match including states (100%). `kash2.schem` 5x7x8, 189 blocks (183 placeable + 6 water): 183/183 placement-relevant (100%), 175/183 exact. The 8 differences are 5 campfires and 3 oak slabs that are waterlogged in the file, which can't be done without water. Block counts per type match an independent Python NBT parse for both files. Presets, the state test (plain, rot90+mirror, rot180) and the out-of-materials pause/resume all came out 100%. Selftest: 204 modules, 0 failures.

## v3 (2026-10-15)

- **Structure Finder picker:** the panel (`.sf`) now has a tick list of every structure type for the current dimension on the left (All / None), **Search Selected** searches only the ticked ones, **Search All** does everything. Chat matches it: `.find <type>`, or several at once `.find village,stronghold,monument`.
- **End portal room (Structure Finder):** after a stronghold is found (setting *Find Portal Room*, on by default), or with `.portal` / the *Portal Rm* button, it finds the stronghold's portal room and reports it separately: `Stronghold (start): X ~ Z` and `End PORTAL ROOM: X Y Z (12 frames, N with eyes)`. Green beam + "End Portal Room" label in-world.
  - Singleplayer: reads the stronghold's structure start from the world generator, takes its PortalRoom piece and counts the End Portal Frame blocks inside it (exact).
  - Multiplayer: spirals out over the chunks your client has loaded around the stronghold start, looking for End Portal Frames. If none are loaded yet it tells you to go near the stronghold and run `.portal` again.
- **Flight:** new **Creative** mode (no drift, stops the moment you let go), **Vertical Boost** (sprint multiplies up/down speed), **Smooth Stop** toggle, **Pause In Menus** (hovers in place while a screen is open). Flight now switches off on logout and gives your abilities back. No anticheat bypass.
- **Auto Build (new, World):** builds schematics block by block from your inventory.
  - Formats: Litematica `.litematic` (all regions), WorldEdit/Sponge `.schem` (v1-v3), vanilla structure-block `.nbt`. Old MCEdit numeric `.schematic` isn't supported (re-save as `.schem`).
  - Presets: `cobble_hut`, `nether_portal`, `pillar`, `pixel_heart`, `bridge`.
  - Settings: Blocks Per Tick (1-4), Delay (ticks), Swap Slot (where blocks from the main inventory are swapped in), Ghost Preview, Air Place, Pause In Menus.
  - Builds bottom-up, nearest first, only within reach. Skips spots where a different block is in the way (shown red in the preview), warns once per missing item, turns itself off when finished. Directional blocks (stairs, logs) take the direction you're facing when placed.
  - **How to load a schematic:**
    1. Put the file in `.minecraft/schematics/` (CurseForge: `<instance>/schematics/`; Litematica already saves there).
    2. In game: `.build list` shows files and presets.
    3. `.build load myhouse.litematic` (or `.build load preset:cobble_hut`; a name prefix is enough). The ghost preview appears 2 blocks in front of you.
    4. Move it: stand where you want it and `.build here` (or `.build here x y z`). `.build rotate` (90° steps), `.build mirror`.
    5. `.build mats` lists the blocks you still need. Put them in your inventory.
    6. `.build start` (or turn on Auto Build) / `.build stop`. `.build clear` unloads.
- **Anti Lava (new, Player):** no fire/lava damage in singleplayer or a world you host (the integrated server cancels fire-type damage to you). On someone else's server damage is decided by the server, so there it only hides the fire overlay/burning and makes lava swimming easier (Lava Swim, Rise Speed: jump to rise, sneak to sink, no fast sinking).
- Dev only: `run/nebula_buildtest` runs BuildHarness (portal room vs real frames, loader round trips, AutoBuild placing obsidian, Flight modes, Anti Lava). Inert in the release jar.

## v2 (2026-10-15)

- **Structure Finder (new, World):** nearest N of every vanilla 1.20.1 structure. Overworld: stronghold, village (plains/desert/savanna/snowy/taiga), ocean monument, woodland mansion, ancient city, desert & jungle temple, igloo, swamp hut, pillager outpost, shipwreck (+beached), ocean ruins (warm/cold), buried treasure, mineshaft (+mesa), trail ruins, ruined portal (all 6 variants). Nether: fortress, bastion remnant, nether ruined portal, nether fossil. End: end city. (Trial chambers are 1.21, not included.)
  - Singleplayer: uses the integrated server's own generator, structure state and template manager (same data as /locate), queried on the server thread.
  - Multiplayer: `.seed <seed>` then it rebuilds the vanilla *Default* world offline from the game's own worldgen data (noise settings, multi-noise biome source, structure sets, template pools, structure NBTs, biome tags) and runs the same checks world generation does: RandomSpread / ConcentricRings placement, frequency + exclusion zones, the structure set's weighted pick, and the structure's biome/terrain check. If that data can't be built it falls back to grid-only positions marked **approx**.
  - Settings: Per Type (1-10), Search Rings, Beams, Tracers, Render Nearest Only, Auto Refresh (re-search after moving 512 blocks).
  - Chat commands (local, never sent to the server): `.seed [seed]`, `.find <type|all> [n]`, `.find list`, `.structures` / `.sf` opens the panel (dimension tabs, Search, seed box, click a row to pin its beam; Nether rows also show overworld coords).
- **Kill Aura synced with Reach:** with Reach on, Kill Aura's range is your actual entity reach (the `forge:entity_reach` attribute Reach sets), not its own Range. With Reach off it uses Range as before. In both cases it is clamped so it never targets past your current entity reach.
- **Nebula Updater (part of this project, `updater/`):** `nebula-updater.jar` + `Nebula Updater.bat`, config `updater.json`; see `updater/README.md`. The public repo will be **kashwtv/nebula**, with GitHub Releases hosting `nebula*.jar`; the updater's default URL is `https://api.github.com/repos/kashwtv/nebula/releases/latest`. `.github/workflows/release.yml` builds the mod jar and the updater on `v*` tags and attaches both to the release.
- Dev only: `run/nebula_structtest` (list of seeds) makes the dev client create a world per seed and compare the finder against the server's /locate (`LocateHarness`). Inert in the release jar.

## Highlights
- **Reach (new, Combat):** Entity Reach 3-6, Block Reach 3-6, Change Block Reach. Uses Forge ENTITY_REACH / BLOCK_REACH transient attribute modifiers on your own player, removed on disable. Kill Aura's range stretches to it. The server checks reach with its own attributes, so on your server also run e.g. `/attribute <you> forge:entity_reach base set 6` and `/attribute <you> forge:block_reach base set 6`, or long hits get dropped by the server.
- **Kill Aura (reworked):** filters Players/Hostiles/Neutrals/Animals/Invisibles (friends always excluded), Priority Angle/Distance/Health, FOV up to 360, eased rotation with Aim Speed, Timing Cooldown (Min Charge) or CPS with Randomness, Require Weapon, Rotate toggle, Target HUD now shows the aura's target.
- **Flight (reworked):** modes Vanilla/Motion/Glide/Jetpack, Speed, Vertical Speed, Acceleration easing, Glide Fall, Jetpack Thrust, Sprint Boost, Anti Kick (vanilla floating-kick avoidance: small dip every 2 s), double-tap jump off, saves and restores mayfly/flying/fly speed.
- **Movement fixes:** Speed (Ground/Hop/Strafe Hop, acceleration, Speed potion aware, pause while sneaking), Step (restores your original step height; only on ground), Spider (eased climb, sneak to cling), Jesus (Bounce mode, rise speed, can jump off the surface), No Fall (Spoof/Packet, gliding option, clean disable), No Slow (Keep Speed %, Keep Sprint), Parkour (edge lead, min drop, only sprinting), Safe Walk (lookahead, safe drop, no sneak while jumping), Auto Walk (Smart mode hops steps and stops at drops/liquids, sprint, pause in menus; no longer releases a W key you're really holding).
- **Config name clash fixed:** the Speed HUD and Speed module (and the Reach HUD and new Reach module) shared one config key. The HUDs are now named "Speedometer" and "Reach Display". Their old saved HUD positions won't carry over.
- **Troll modules:** Derp, Copycat, Dance, Twerk.
- **Dev self-test:** DevAutomation (dev workspace only, never in the release jar) toggles every module at the title screen when `run/nebula_selftest` exists.

## Modules by category

### Combat (22)
- Aim Assist: Speed, FOV, Range, Aim At, Priority, Vertical, Only While Clicking, Weapon Only, Through Walls, Sticky Target, Players, Hostile Mobs, Passive Mobs, Invisibles
- Triggerbot: Sword Charge, Axe Charge, Other Charge, Crits, Min Delay, Max Delay, Weapon Only, Players, Hostile Mobs, Passive Mobs, Invisibles, Pause While Using
- Bow Aim: Speed, FOV, Range, Lead Moving Targets, Through Walls, Players, Hostile Mobs, Passive Mobs
- Auto Totem: Mode, Health, Delay
- Velocity: Horizontal, Vertical, Chance
- W-Tap: Length, Chance
- Auto Weapon: Prefer
- Kill Aura **[CHANGED]**: Range, FOV, Priority, Aim Speed, Timing, Min Charge, CPS, Randomness, Players, Hostiles, Neutrals, Animals, Invisibles, Require Weapon, Swing Only When Aimed, Through Walls, Rotate
- Criticals: Mode
- Auto Clicker: Min CPS, Max CPS, Respect Cooldown, Only On Entities
- Auto Log: Health, Only Without Totems, Only If Player Near
- Auto Gap: Below Health, Prefer Enchanted
- Auto Bow: Release At
- Auto XP: Repair Below, Delay, Only While Sneaking
- Surround: Blocks Per Tick, Floor, Off When You Move
- Auto Trap: Mode, Range, Blocks Per Tick
- Reach **[NEW]**: Entity Reach, Block Reach, Change Block Reach
- Auto Pot **[NEW]**: Health, Delay
- Auto Shield **[NEW]**: Range
- No Miss Cooldown **[NEW]**
- Shield Breaker **[NEW]**
- Target Strafe **[NEW]**: Radius, Only While Jump Held

### Movement (33)
- Sprint: Toggle Sneak
- Flight **[CHANGED]**: Mode, Speed, Vertical Speed, Acceleration, Glide Fall, Jetpack Thrust, Sprint Boost, Anti Kick, Double-Tap Jump Off, Stop On Disable
- Speed **[CHANGED]**: Mode, Speed, Acceleration, Air Friction, Pause While Sneaking, Stop On Release
- No Fall **[CHANGED]**: Mode, Min Fall, While Gliding
- Step **[CHANGED]**: Height, Only On Ground, Sneak Disables
- Spider **[CHANGED]**: Climb Speed, Acceleration, Sneak To Cling
- No Slow **[CHANGED]**: Using Items, Sneaking, Keep Speed, Keep Sprint
- Safe Walk **[CHANGED]**: Only Backwards, Only Holding Blocks, Lookahead, Safe Drop
- Parkour **[CHANGED]**: Edge Lead, Min Drop, Only Sprinting
- Auto Walk **[CHANGED]**: Mode, Sprint, Pause In Menus
- Jesus **[CHANGED]**: Mode, Lava, Rise Speed
- Long Jump **[NEW]**: Boost, Decay, Extra Height, Auto Jump, Disable On Land
- High Jump **[NEW]**: Height, No Fall From Jump
- Air Jump **[NEW]**: Max Jumps
- Omni Sprint **[NEW]**: Respect Hunger
- Elytra Fly **[NEW]**: Mode, Speed, Vertical Speed, Acceleration, Boost, Hover When Idle, Auto Start
- Boat Fly **[NEW]**: Speed, Vertical Speed, Acceleration, Idle Fall, Boats Only, Face Camera
- Fast Ladder **[NEW]**: Up Speed, Down Speed
- Strafe **[NEW]**: Control, Only In Air, Stop On Release
- Anti Levitation **[NEW]**
- Anti Void **[NEW]**: Mode
- Auto Jump **[NEW]**: Only While Moving
- Sneak **[NEW]**
- Entity Speed **[NEW]**: Speed
- Fast Swim **[NEW]**: Speed, Vertical
- Ice Speed **[NEW]**: Speed
- No Jump Delay **[NEW]**
- Reverse Step **[NEW]**: Max Drop
- Scaffold **[NEW]**: Extend, Tower (Jump)
- Auto MLG **[NEW]**: Min Fall
- Auto Rocket **[NEW]**: Min Speed, Delay
- Parachute **[NEW]**: Max Fall Speed, Open After
- Wall Jump **[NEW]**: Push

### Visuals (38)
- ESP: Style, Color, Players, Hostile Mobs, Passive Mobs, Items, Tracers, Health Bar, Names, Armor, Box Fill, Range, Hostile Color
- Fullbright: Mode, Brightness
- Trajectories: Color, Landing Marker, Highlight Hit
- Crosshair: Style, Color, Length, Gap, Thickness, Outline, Opacity, Center Dot, Dynamic Spread, Show In 3rd Person
- Hit Marker: Color, Size, Sound
- Damage Numbers: Color, Size, Range, Show Heals, Through Walls
- Kill Effects: Effect, Sound
- Block Outline: Color, Fill, Fill Opacity, Breathing, Line Opacity
- Motion Blur: Strength
- Trails: Particle, Color, Density, Dust Size, Only Moving
- Jump Circles: Color, Size, Duration
- Hit Particles: Mode, Color, Amount
- China Hat: Color, Radius, Height, Opacity, First Person
- Clean View: No Fire, No Water, No Suffocation, No Pumpkin, No Vignette, No Portal, No Spyglass
- Low HP Warning: Below HP, Intensity
- Zoom: Zoom, Key Mode, Animation Speed, Smooth Camera, Scroll Adjust
- Module List: Sort, Text Color, Suffixes, Row Background, Accent Bar, Row Height, Background Opacity
- Chunk Borders **[NEW]**: Color, Radius, Height
- Particle Multiplier **[NEW]**: Multiplier, Crit, Sharpness, Every Hit
- Custom FOV **[NEW]**: FOV, Static (No Sprint Zoom)
- Freelook **[NEW]**: Third Person
- Glow **[NEW]**: Players, Hostiles, Passives, Items, Range
- Hitboxes **[NEW]**
- Item ESP **[NEW]**: Color, Labels, Rare Only, Range
- Light Overlay **[NEW]**: Radius
- Logout Spots **[NEW]**
- Nametags **[NEW]**: Health, Distance, Mobs Too, Always Show
- Anti Blind **[NEW]**: Blindness, Darkness, Nausea
- No Bob **[NEW]**: No View Bob, No Hurt Tilt
- Hide HUD Clutter **[NEW]**: Boss Bars, Scoreboard, Titles, Potion Icons
- TNT Timer **[NEW]**
- Tracers **[NEW]**: Players, Hostiles, Passives, Distance Color, Color, Range, Width
- View Model **[NEW]**: X, Y, Z, Scale, Tilt, Swing
- Shader FX **[NEW]**: Effect
- Better Tooltips **[NEW]**: Durability, Food Values, Inventory Total
- Hide Hand **[NEW]**: Main Hand, Off Hand
- Self Nametag **[NEW]**
- Break Progress **[NEW]**

### World (8)
- Block Finder: Blocks, Range, Scan Speed, Max Shown, Fill, Tracers, Through Walls
- Storage Finder: Chests, Barrels, Shulkers, Ender Chests, Hoppers / Droppers, Furnaces, Spawners, Fill, Tracers, Base Finder, Base Threshold
- Portal Finder: Range, Beams, Highlight Blocks, Notify
- Freecam: Speed
- Breadcrumbs: Color, Length, Fade After, Through Walls
- Ambience: Time, Custom Time, Cycle Speed, Clear Weather
- Atmosphere: Fog Color, Tint, Fog, Fog Distance
- Waypoints **[NEW]**: Last Death, World Spawn

### Player (27)
- Fast Place: Delay, Blocks, Crystals, XP Bottles, Everything
- Fast Break: Delay
- Auto Tool: Swap Back, Save Low Tools
- Auto Armor: Delay, Keep Elytra
- Auto Eat: Eat Below, Allow Gapples, Pause In Combat
- Chest Stealer: Delay, Random Delay, Close When Done
- Inventory Move: Jump
- Auto Respawn
- Anti AFK: Interval, Jump, Turn, Swing
- Derp **[NEW]**: Mode, Speed, Pause While Using
- Copycat **[NEW]**: Find Range, Follow Distance, Turn Speed, Copy Look Direction, Sprint To Catch Up
- Dance **[NEW]**: Tempo, Sneak, Jump, Spin
- Auto Bonemeal **[NEW]**
- Auto Breed **[NEW]**: Range
- Auto Drop **[NEW]**: Rotten Flesh, Seeds, Cobble/Diorite/Granite/Andesite, Dirt/Gravel, Poison Potato/Spider Eye
- Auto Farm **[NEW]**: Range, Replant
- Auto Fish **[NEW]**: Recast Delay
- Auto Mine **[NEW]**
- Auto Replenish **[NEW]**: Threshold
- Auto Shear **[NEW]**: Range
- Fast Use **[NEW]**: Delay
- Middle Click Pearl **[NEW]**
- Auto Right Click **[NEW]**: CPS
- Elytra Swap **[NEW]**
- No Break Delay **[NEW]**
- Auto Torch **[NEW]**: Max Light
- Twerk **[NEW]**: Speed

### Hud (54)
- Watermark: Version, FPS, Time, Server
- Coordinates: Decimals, Nether / Overworld, Facing, Biome, Chunk
- Armor: Layout, Durability, Held Items, Bars, Low Warning
- Health: Length, Segmented, Hunger, Armor, Damage Flash, Hide Vanilla Bars
- FPS: Graph
- CPS: Right Click
- Ping
- Speedometer **[CHANGED]**: Unit
- Reach Display **[CHANGED]**
- Clock: Format, Seconds, Game Time
- Compass: Length, View Angle, Degrees, ESP Dots
- Potions: Bars, Hide Vanilla, Blink When Ending
- Keystrokes: Key Size, Press Color, Mouse, Space Bar, CPS
- Target HUD **[CHANGED]**: 3D Model, Armor, HP Difference
- Inventory: Slot Backgrounds
- Item Counter: Layout, Hide Empty, Totems, Golden Apples, Ender Pearls, Arrows, XP Bottles, Crystals, Obsidian
- Session Stats: Combo, Hits
- Arrows **[NEW]**
- Biome **[NEW]**
- Chunk **[NEW]**
- Day Counter **[NEW]**
- Dimension **[NEW]**
- Direction **[NEW]**
- Durability **[NEW]**
- Entities **[NEW]**
- Fall Distance **[NEW]**
- Food **[NEW]**
- Light Level **[NEW]**
- Looking At **[NEW]**
- Memory **[NEW]**
- Moon Phase **[NEW]**
- Nearby Players **[NEW]**
- Nether Coords **[NEW]**
- Radar **[NEW]**: Size, Range, Mobs
- Rotation **[NEW]**
- Server **[NEW]**
- TPS **[NEW]**
- Weather **[NEW]**
- World Time **[NEW]**
- Experience **[NEW]**
- Absorption **[NEW]**
- Armor Points **[NEW]**
- Blocks **[NEW]**
- Combo **[NEW]**
- Difficulty **[NEW]**
- Elytra **[NEW]**
- Game Mode **[NEW]**
- K/D **[NEW]**
- Vertical Speed **[NEW]**
- Playtime **[NEW]**
- Movement State **[NEW]**
- Target Distance **[NEW]**
- Lag Detector **[NEW]**
- Ore Hint **[NEW]**

### Client (18)
- Anti Spam **[NEW]**: Window
- Armor Alert **[NEW]**: Warn Below
- Auto Reconnect **[NEW]**: Delay
- Chat Timestamps **[NEW]**: Seconds
- Death Coords **[NEW]**
- Unfocused FPS **[NEW]**: Background FPS
- Visual Range **[NEW]**: Leave Messages, Sound
- Death Screenshot **[NEW]**
- Mention Notify **[NEW]**
- Chat Logger **[NEW]**
- Creeper Alert **[NEW]**: Range
- Hit Sound **[NEW]**: Sound, Volume
- Pickup Notify **[NEW]**
- Projectile Alert **[NEW]**
- Click GUI: Accent, UI Scale, Font, Blur, Blur Strength, Dim, Particles, Sounds
- HUD Editor: Snapping, Grid
- HUD Style: Opacity, Corner Radius, Border, Shadow, Accent Line, Text Shadow
- Notifications

## Other changed files
- util/Move.java (new movement helper)
- module/ModuleManager.java (registration)
- module/hud/TargetHud.java (Kill Aura target)
- DevAutomation.java (self-test)
- module/hud/ReachHud.java, SpeedHud.java (renamed)

## Stuck-keys fix
- ClientEvents: on ClientPlayerNetworkEvent.LoggingOut, Auto Walk, Copycat, Twerk, Auto Mine, Dance, Auto Shield and Auto Right Click are switched off (unless bound as "Always") and KeyMapping.releaseAll() releases every held key.
- Twerk, Dance and Copycat now pause and let go of their keys while a menu is open. Auto Mine, Auto Walk and Auto Shield already did this.
- The dev self-test now also checks the logout release.

## Structure Finder UI, Auto Build UI, Anti Hunger
- New `ActionSetting` (pill button in Click GUI settings).
- Structure Finder settings: "Structures" chip multi-select (same chip renderer as Block Finder's Blocks, "N selected"), Range slider, Results slider, Search Now / Open Panel buttons. Selected chips drive what is searched and which beams render; the "Portal Room" chip replaces the old "Find Portal Room" toggle.
- Structure Finder panel (`.sf`) rebuilt in Nebula's card style: chip picker + sliders left, dimension chips + results right (click a row to pin its beam), styled seed field.
- New Auto Build screen (`.build`, keybind B "Open Auto Build", or the module's "Open UI" button): schematic library (.minecraft/schematics + presets), Open folder, Add schematic (native TinyFileDialogs picker, copies the file in), rotatable/zoomable isometric preview with size and block count, materials have/need, Place Here / Rotate / Mirror / Preview in world / Start / Stop, progress bar.
- New Anti Hunger (Player): singleplayer/hosted worlds keep food, saturation full on the integrated server. On other servers hunger is server-controlled; the only option is an honest "No Sprint On Servers" toggle (nothing spoofed).
- Self-test also opens both new screens and checks Anti Hunger is registered. Dev commands: screen sf|build, abload, sfsearch, resize, quit.
