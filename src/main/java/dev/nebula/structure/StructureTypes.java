package dev.nebula.structure;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

import java.util.Arrays;
import java.util.List;

/** Every vanilla 1.20.1 structure grouped into finder types, by dimension. */
public final class StructureTypes {
    public record Type(String name, String id, ResourceKey<Level> dim, int color, List<ResourceLocation> structures) {}

    private static Type t(String name, String id, ResourceKey<Level> dim, int color, String... ids) {
        return new Type(name, id, dim, color, Arrays.stream(ids).map(s -> new ResourceLocation("minecraft", s)).toList());
    }

    public static final List<Type> ALL = List.of(
            t("Stronghold", "stronghold", Level.OVERWORLD, 0xFF9B9BFF, "stronghold"),
            t("Village", "village", Level.OVERWORLD, 0xFF6BD36B, "village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga"),
            t("Ocean Monument", "monument", Level.OVERWORLD, 0xFF4FD6D6, "monument"),
            t("Woodland Mansion", "mansion", Level.OVERWORLD, 0xFFB5793F, "mansion"),
            t("Ancient City", "ancient_city", Level.OVERWORLD, 0xFF2F8C8C, "ancient_city"),
            t("Desert Temple", "desert_pyramid", Level.OVERWORLD, 0xFFE8D27A, "desert_pyramid"),
            t("Jungle Temple", "jungle_pyramid", Level.OVERWORLD, 0xFF3FA83F, "jungle_pyramid"),
            t("Igloo", "igloo", Level.OVERWORLD, 0xFFE0F4FF, "igloo"),
            t("Swamp Hut", "swamp_hut", Level.OVERWORLD, 0xFF6B8E4E, "swamp_hut"),
            t("Pillager Outpost", "pillager_outpost", Level.OVERWORLD, 0xFF8A8A8A, "pillager_outpost"),
            t("Shipwreck", "shipwreck", Level.OVERWORLD, 0xFF9C6B3F, "shipwreck", "shipwreck_beached"),
            t("Ocean Ruins", "ocean_ruin", Level.OVERWORLD, 0xFF5F9EA0, "ocean_ruin_warm", "ocean_ruin_cold"),
            t("Buried Treasure", "buried_treasure", Level.OVERWORLD, 0xFFFFD700, "buried_treasure"),
            t("Mineshaft", "mineshaft", Level.OVERWORLD, 0xFFA0522D, "mineshaft", "mineshaft_mesa"),
            t("Trail Ruins", "trail_ruins", Level.OVERWORLD, 0xFFC08A5A, "trail_ruins"),
            t("Ruined Portal", "ruined_portal", Level.OVERWORLD, 0xFFB06BFF, "ruined_portal", "ruined_portal_desert", "ruined_portal_jungle", "ruined_portal_swamp", "ruined_portal_mountain", "ruined_portal_ocean"),
            t("Nether Fortress", "fortress", Level.NETHER, 0xFFD84B4B, "fortress"),
            t("Bastion Remnant", "bastion_remnant", Level.NETHER, 0xFF505050, "bastion_remnant"),
            t("Ruined Portal (Nether)", "ruined_portal_nether", Level.NETHER, 0xFFB06BFF, "ruined_portal_nether"),
            t("Nether Fossil", "nether_fossil", Level.NETHER, 0xFFEDE6D0, "nether_fossil"),
            t("End City", "end_city", Level.END, 0xFFE6B3FF, "end_city"));

    public static List<Type> forDim(ResourceKey<Level> dim) {
        return ALL.stream().filter(t -> t.dim().equals(dim)).toList();
    }

    public static Type byId(String id) {
        for (Type t : ALL) if (t.id().equalsIgnoreCase(id) || t.name().replace(" ", "").equalsIgnoreCase(id.replace("_", ""))) return t;
        for (Type t : ALL) if (t.id().startsWith(id.toLowerCase())) return t;
        return null;
    }

    private StructureTypes() {}
}
