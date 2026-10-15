package dev.nebula.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nebula.Nebula;
import dev.nebula.gui.ClickGui;
import dev.nebula.module.HudModule;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.setting.Setting;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/** Everything persists to config/nebula.json. */
public final class ConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loading;
    private static String lastWritten = "";
    private static int ticks;
    private static boolean hooked;

    private ConfigIO() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("nebula.json");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        loading = true;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : ModuleManager.MODULES) {
                if (!mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                try {
                    if (o.has("key")) m.key = o.get("key").getAsInt();
                    if (o.has("bind")) m.bindMode = o.get("bind").getAsInt();
                    else if (o.has("hold") && o.get("hold").getAsBoolean()) m.bindMode = 1;
                    if (o.has("fav")) m.favorite = o.get("fav").getAsBoolean();
                    if (o.has("settings")) {
                        JsonObject so = o.getAsJsonObject("settings");
                        for (Setting s : m.settings) {
                            JsonElement e = so.get(s.name);
                            if (e != null) {
                                try {
                                    s.load(e);
                                } catch (Exception ex) {
                                    Nebula.LOG.warn("Bad setting {}.{}", m.name, s.name);
                                }
                            }
                        }
                    }
                    if (m instanceof HudModule h) {
                        if (o.has("fx")) h.fx = o.get("fx").getAsFloat();
                        if (o.has("fy")) h.fy = o.get("fy").getAsFloat();
                        if (o.has("scale")) h.scale = o.get("scale").getAsFloat();
                    }
                    if (o.has("enabled") && m.bindMode != 1) m.setEnabled(o.get("enabled").getAsBoolean());
                } catch (Exception ex) {
                    Nebula.LOG.warn("Bad config for module {}", m.name, ex);
                }
            }
            if (root.has("friends")) {
                dev.nebula.util.Friends.NAMES.clear();
                for (JsonElement fr : root.getAsJsonArray("friends")) dev.nebula.util.Friends.NAMES.add(fr.getAsString());
            }
            if (root.has("gui")) {
                JsonObject o = root.getAsJsonObject("gui");
                if (o.has("view")) ClickGui.view = o.get("view").getAsString();
                ClickGui.offX = o.get("x").getAsFloat();
                ClickGui.offY = o.get("y").getAsFloat();
            }
        } catch (Exception ex) {
            Nebula.LOG.error("Could not read nebula.json", ex);
        } finally {
            loading = false;
        }
    }

    /** Called every client tick; writes the file within a second of any change. */
    public static void autosave() {
        if (!hooked) {
            hooked = true;
            Runtime.getRuntime().addShutdownHook(new Thread(ConfigIO::save, "nebula-config-save"));
        }
        if (++ticks % 5 == 0) save();
    }

    public static synchronized void save() {
        if (loading || ModuleManager.MODULES.isEmpty()) return;
        try {
            String json = GSON.toJson(build());
            if (json.equals(lastWritten)) return;
            Files.writeString(file(), json);
            lastWritten = json;
        } catch (Exception ex) {
            Nebula.LOG.error("Could not write nebula.json", ex);
        }
    }

    private static JsonObject build() {
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : ModuleManager.MODULES) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.key);
            o.addProperty("bind", m.bindMode);
            o.addProperty("fav", m.favorite);
            JsonObject so = new JsonObject();
            for (Setting s : m.settings) so.add(s.name, s.save());
            o.add("settings", so);
            if (m instanceof HudModule h) {
                o.addProperty("fx", h.fx);
                o.addProperty("fy", h.fy);
                o.addProperty("scale", h.scale);
            }
            mods.add(m.name, o);
        }
        root.add("modules", mods);
        com.google.gson.JsonArray friends = new com.google.gson.JsonArray();
        for (String f : dev.nebula.util.Friends.NAMES) friends.add(f);
        root.add("friends", friends);
        JsonObject gui = new JsonObject();
        gui.addProperty("view", ClickGui.view);
        gui.addProperty("x", ClickGui.offX);
        gui.addProperty("y", ClickGui.offY);
        root.add("gui", gui);

        return root;
    }
}
