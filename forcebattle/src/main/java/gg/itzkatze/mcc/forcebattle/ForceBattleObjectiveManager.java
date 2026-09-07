package gg.itzkatze.mcc.forcebattle;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import gg.itzkatze.mcc.data.DamageTypeSimplified;
import gg.itzkatze.mcc.data.MccDamageType;
import gg.itzkatze.mcc.data.MobsSimplified;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.data.BiomeSimplified;
import gg.itzkatze.mcc.data.MccAdvancement;
import gg.itzkatze.mcc.data.MccStructure;
import gg.itzkatze.mcc.data.StructureSimplified;
import gg.itzkatze.mcc.player.MccPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ForceBattleObjectiveManager {

    private static ForceBattleObjectiveManager instance;

    public static ForceBattleObjectiveManager getInstance() {
        if (instance == null) {
            instance = new ForceBattleObjectiveManager();
        }
        return instance;
    }

    private final List<ObjectiveSeed> chainList = new ArrayList<>();
    private final List<ObjectiveSeed> teamChainList = new ArrayList<>();

    private ForceBattleObjectiveManager() {
    }

    /**
     * Gets the ForceBattleSettings manager for accessing challenge-specific settings
     */
    private ChallengeSettingsManager getForceBattleSettingsManager() {
        Challenge forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE);
        if (forceBattle != null && forceBattle.getSettingsManager() != null) {
            return forceBattle.getSettingsManager();
        }
        return null;
    }

    public ForceBattleObjective getNewObjective(ForceBattleTeam team) {
        if (SettingsManager.isEnabled(ForceBattleSettings.CHAIN_MODE)) {
            int progress = team.getChainProgress();
            ObjectiveSeed objective;
            try {
                objective = getTeamChainList().get(progress);
            } catch (IndexOutOfBoundsException e) {
                progress = 0;
                objective = getTeamChainList().get(progress);
            }
            return objective.toObjective();
        }

        List<MccPlayer> players = team.getPlayers();
        if (players.isEmpty()) return null;

        Random random = new Random();
        ForceBattlePlayer player = (ForceBattlePlayer) players.get(random.nextInt(players.size()));

        List<ForceBattleType> availableBattleTypes = new ArrayList<>();
        for (ForceBattleType type : ForceBattleType.values()) {
            SettingStateType state = type.getSettingState();
            if (state == PlayerTeamBothState.TEAM || state == PlayerTeamBothState.BOTH) {
                availableBattleTypes.add(type);
            }
        }

        if (availableBattleTypes.isEmpty()) return null;

        ForceBattleType battleType = availableBattleTypes.get(random.nextInt(availableBattleTypes.size()));
        return generateObjective(battleType, player, team);
    }

    public ForceBattleObjective getNewObjective(ForceBattlePlayer player) {
        if (SettingsManager.isEnabled(ForceBattleSettings.CHAIN_MODE)) {
            int progress = player.getChainProgress();
            ObjectiveSeed objective;
            try {
                objective = getChainList().get(progress);
            } catch (IndexOutOfBoundsException e) {
                progress = 0;
                objective = getChainList().get(progress);
            }
            return objective.toObjective();
        }

        List<ForceBattleType> availableBattleTypes = new ArrayList<>();
        for (ForceBattleType type : ForceBattleType.values()) {
            SettingStateType state = type.getSettingState();
            if (state == PlayerTeamBothState.PLAYER || state == PlayerTeamBothState.BOTH) {
                availableBattleTypes.add(type);
            }
        }

        if (availableBattleTypes.isEmpty()) return null;

        Random random = new Random();
        ForceBattleType battleType = availableBattleTypes.get(random.nextInt(availableBattleTypes.size()));

        return generateObjective(battleType, player, null);
    }

    private ForceBattleObjective generateObjective(ForceBattleType battleType, ForceBattlePlayer player, ForceBattleTeam team) {
        Random random = new Random();
        List<ObjectiveSeed> allPossible = getAllPossibleObjectives(battleType, player);
        if (allPossible.isEmpty()) return null;

        if (SettingsManager.isEnabled(GlobalSetting.NO_DUPLICATE_OBJECTIVES)) {
            Set<String> finished = new HashSet<>();
            if (team == null || !player.isInTeam()) {
                player.getSkippedObjectives().forEach(obj -> finished.add(obj.getObjectiveString()));
                ForceBattleObjective.finishedBy(player).forEach(obj -> finished.add(obj.getObjectiveString()));
            } else {
                for (MccPlayer teammate : team.getPlayers()) {
                    ForceBattlePlayer teammatePlayer = (ForceBattlePlayer) teammate;
                    teammatePlayer.getSkippedObjectives().forEach(obj -> finished.add(obj.getObjectiveString()));
                    ForceBattleObjective.finishedBy(teammate).forEach(obj -> finished.add(obj.getObjectiveString()));
                }
            }
            allPossible.removeIf(seed -> finished.contains(seed.getKey()));
        }

            if (allPossible.isEmpty()) {
            player.sendMessage(Component.text("No objective available for BattleType " + battleType.name() + ". Possible all completed already?")
                    .color(NamedTextColor.RED));
            if (team != null && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
                return getNewObjective(team);
            } else {
                return getNewObjective(player);
            }
        }

        ObjectiveSeed seed = allPossible.get(random.nextInt(allPossible.size()));
        return seed.toObjective();
    }

    private List<ObjectiveSeed> getAllPossibleObjectives(ForceBattleType battleType, MccPlayer mccPlayer) {
        List<ObjectiveSeed> list = new ArrayList<>();
        switch (battleType) {
            case FORCE_ITEM -> getAvailableItems().forEach(material -> list.add(new ObjectiveSeed(battleType, material)));
            case FORCE_MOB -> {
                if (SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES)) {
                    getAvailableMobsSimplified().forEach(entitySimplified -> list.add(new ObjectiveSeed(battleType, entitySimplified)));
                } else {
                    getAvailableMobs().forEach(entityType -> list.add(new ObjectiveSeed(battleType, entityType)));
                }
            }
            case FORCE_BIOME -> {
                if (SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES)) {
                    getAvailableBiomesSimplified().forEach(biomeSimplified -> list.add(new ObjectiveSeed(battleType, biomeSimplified)));
                } else {
                    getAvailableBiomes().forEach(biome -> list.add(new ObjectiveSeed(battleType, biome)));
                }
            }
            case FORCE_ADVANCEMENT -> getAvailableAdvancements().forEach(advancement -> list.add(new ObjectiveSeed(battleType, advancement)));
            case FORCE_HEIGHT -> getAvailableHeights().forEach(height -> list.add(new ObjectiveSeed(battleType, height)));
            case FORCE_COORDS -> {
                Location coords = getRandomLocation(mccPlayer);
                list.add(new ObjectiveSeed(battleType, coords));
            }
            case FORCE_STRUCTURE -> {
                if (SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES)) {
                    getAvailableStructuresSimplified().forEach(structureSimplified -> list.add(new ObjectiveSeed(battleType, structureSimplified)));
                } else {
                    getAvailableStructures().forEach(structure -> list.add(new ObjectiveSeed(battleType, structure)));
                }
            }
            case FORCE_DEATH -> {
                if (SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES)) {
                    getAvailableDeathsSimplified().forEach(deathSimplified -> list.add(new ObjectiveSeed(battleType, deathSimplified)));
                } else {
                    getAvailableDeaths().forEach(death -> list.add(new ObjectiveSeed(battleType, death)));
                }
            }
        }
        return list;
    }

    private List<ObjectiveSeed> getChainList() {
        if (chainList.isEmpty()) {
            updateChainList();
        }
        return chainList;
    }

    private List<ObjectiveSeed> getTeamChainList() {
        if (teamChainList.isEmpty()) {
            updateTeamChainList();
        }
        return teamChainList;
    }

    public void updateChainList() {
        chainList.clear();

        for (ForceBattleType type : ForceBattleType.values()) {
            SettingStateType state = type.getSettingState();

            if (state != PlayerTeamBothState.PLAYER && state != PlayerTeamBothState.BOTH) continue;

            switch (type) {
                case FORCE_ITEM -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_ITEM)) {
                        getAvailableItems().forEach(m -> chainList.add(new ObjectiveSeed(type, m)));
                    }
                }
                case FORCE_MOB -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_MOB)) {
                        getAvailableMobs().forEach(e -> chainList.add(new ObjectiveSeed(type, e)));
                    }
                }
                case FORCE_ADVANCEMENT -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_ADVANCEMENT)) {
                        getAvailableAdvancements().forEach(a -> chainList.add(new ObjectiveSeed(type, a)));
                    }
                }
                case FORCE_HEIGHT -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_HEIGHT)) {
                        getAvailableHeights().forEach(h -> chainList.add(new ObjectiveSeed(type, h)));
                    }
                }
                case FORCE_BIOME -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_BIOME)) {
                        if (SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES)) {
                            getAvailableBiomesSimplified().forEach(b -> chainList.add(new ObjectiveSeed(type, b)));
                        } else {
                            getAvailableBiomes().forEach(b -> chainList.add(new ObjectiveSeed(type, b)));
                        }
                    }
                }
                case FORCE_STRUCTURE -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_STRUCTURE)) {
                        if (SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES)) {
                            getAvailableStructuresSimplified().forEach(s -> chainList.add(new ObjectiveSeed(type, s)));
                        } else {
                            getAvailableStructures().forEach(s -> chainList.add(new ObjectiveSeed(type, s)));
                        }
                    }
                }
                case FORCE_COORDS -> {} // COORDS is not added to chain list
            }
        }

        Collections.shuffle(chainList);
    }

    public void updateTeamChainList() {
        teamChainList.clear();

        for (ForceBattleType type : ForceBattleType.values()) {
            SettingStateType state = type.getSettingState();

            if (state != PlayerTeamBothState.TEAM && state != PlayerTeamBothState.BOTH) continue;

            switch (type) {
                case FORCE_ITEM -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_ITEM)) {
                        getAvailableItems().forEach(m -> teamChainList.add(new ObjectiveSeed(type, m)));
                    }
                }
                case FORCE_MOB -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_MOB)) {
                        getAvailableMobs().forEach(e -> teamChainList.add(new ObjectiveSeed(type, e)));
                    }
                }
                case FORCE_ADVANCEMENT -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_ADVANCEMENT)) {
                        getAvailableAdvancements().forEach(a -> teamChainList.add(new ObjectiveSeed(type, a)));
                    }
                }
                case FORCE_HEIGHT -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_HEIGHT)) {
                        getAvailableHeights().forEach(h -> teamChainList.add(new ObjectiveSeed(type, h)));
                    }
                }
                case FORCE_COORDS -> {} // Coordinates cannot be pre-generated for chain mode
                case FORCE_BIOME -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_BIOME)) {
                        if (SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES)) {
                            getAvailableBiomesSimplified().forEach(b -> teamChainList.add(new ObjectiveSeed(type, b)));
                        } else {
                            getAvailableBiomes().forEach(b -> teamChainList.add(new ObjectiveSeed(type, b)));
                        }
                    }
                }
                case FORCE_STRUCTURE -> {
                    if (SettingsManager.isEnabled(ForceBattleSettings.FORCE_STRUCTURE)) {
                        if (SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES)) {
                            getAvailableStructuresSimplified().forEach(s -> teamChainList.add(new ObjectiveSeed(type, s)));
                        } else {
                            getAvailableStructures().forEach(s -> teamChainList.add(new ObjectiveSeed(type, s)));
                        }
                    }
                }
            }
        }

        Collections.shuffle(teamChainList);
    }

    public List<Material> getAvailableItems() {
        List<Material> availableItems = new ArrayList<>();

        List<String> itemsToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.items");

        // Get ForceBattle settings manager
        Challenge forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE);
        boolean excludeMusicDiscs = false;
        boolean excludeBannerPatterns = false;
        boolean excludeArmorTemplates = false;
        boolean excludePotteryShreds = false;
        boolean excludeOres = false;
        boolean excludeEndItems = false;

        if (forceBattle != null && forceBattle.getSettingsManager() != null) {
            ChallengeSettingsManager fbManager = forceBattle.getSettingsManager();
            excludeMusicDiscs = fbManager.isEnabled(ForceBattleSettings.EXCLUDE_MUSIC_DISCS);
            excludeBannerPatterns = fbManager.isEnabled(ForceBattleSettings.EXCLUDE_BANNER_PATTERNS);
            excludeArmorTemplates = fbManager.isEnabled(ForceBattleSettings.EXCLUDE_ARMOR_TEMPLATES);
            excludePotteryShreds = fbManager.isEnabled(ForceBattleSettings.EXCLUDE_POTTERY_SHERDS);
            excludeOres = fbManager.isEnabled(ForceBattleSettings.EXCLUDE_ORES);
            excludeEndItems = fbManager.isEnabled(ForceBattleSettings.EXCLUDE_END);
        }

        for (Material material : Material.values()) {
            if (!material.isItem()) continue;
            if (itemsToExclude.contains(material.name())) continue;
            if (excludeMusicDiscs && material.name().contains("DISC")) continue;
            if (excludeBannerPatterns && material.name().endsWith("BANNER_PATTERN")) continue;
            if (excludeArmorTemplates && material.name().endsWith("TEMPLATE")) continue;
            if (excludePotteryShreds && material.name().endsWith("POTTERY_SHERD")) continue;
            if (excludeOres && material.name().endsWith("ORE")) continue;
            if (excludeEndItems && ENDITEMS.contains(material)) continue;
            if (material.name().endsWith("SPAWN_EGG")) continue;
            if (material.name().endsWith("CANDLE_CAKE")) continue;
            if (material.name().startsWith("POTTED")) continue;
            if (material.name().contains("PETRIFIED")) continue;
            if (material.name().contains("WALL") && material.name().contains("TORCH")) continue;
            if (material.name().contains("WALL") && material.name().contains("SIGN")) continue;
            if (material.name().contains("WALL") && material.name().contains("HEAD")) continue;
            if (material.name().contains("WALL") && material.name().contains("CORAL")) continue;
            if (material.name().contains("WALL") && material.name().contains("BANNER")) continue;
            if (material.name().contains("WALL") && material.name().contains("SKULL")) continue;
            if (material.name().endsWith("STEM")) continue;
            if (material.name().startsWith("INFESTED")) continue;

            availableItems.add(material);
        }
        return availableItems;
    }

    public List<EntityType> getAvailableMobs() {
        List<EntityType> availableMobs = new ArrayList<>();

        ChallengeSettingsManager settingsManager = getForceBattleSettingsManager();
        boolean excludeEndMobs = settingsManager != null && settingsManager.isEnabled(ForceBattleSettings.EXCLUDE_END);

        List<String> mobsToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.mobs");

        for (EntityType entity : EntityType.values()) {
            if (!entity.isAlive() || !entity.isSpawnable()) continue;
            if (mobsToExclude.contains(entity.name())) continue;
            if (excludeEndMobs && ENDMOBS.contains(entity)) continue;

            availableMobs.add(entity);
        }
        return availableMobs;
    }

    public List<MobsSimplified> getAvailableMobsSimplified() {
        List<MobsSimplified> availableMobs = new ArrayList<>();
        ChallengeSettingsManager settingsManager = getForceBattleSettingsManager();
        boolean excludeEndMobs = settingsManager != null && settingsManager.isEnabled(ForceBattleSettings.EXCLUDE_END);

        List<String> mobsToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.mobs");

        for (MobsSimplified simplified : MobsSimplified.values()) {
            if (mobsToExclude.contains(simplified.name().toUpperCase(Locale.ROOT))) continue;
            if (excludeEndMobs && ENDMOBSSIMPLIFIED.contains(simplified)) continue;

            availableMobs.add(simplified);
        }
        return availableMobs;
    }

    public List<Biome> getAvailableBiomes() {
        List<Biome> availableBiomes = new ArrayList<>();

        ChallengeSettingsManager settingsManager = getForceBattleSettingsManager();
        boolean excludeBiomes = settingsManager != null && settingsManager.isEnabled(ForceBattleSettings.EXCLUDE_END);

        List<String> biomesToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.biomes");

        Registry<Biome> access = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        for (Biome biome : access) {
            if (biomesToExclude.contains(biome.getKey().toString())) continue;
            if (excludeBiomes && ENDBIOMES.contains(biome)) continue;

            availableBiomes.add(biome);
        }
        return availableBiomes;
    }

    public List<BiomeSimplified> getAvailableBiomesSimplified() {
        List<BiomeSimplified> availableBiomes = new ArrayList<>();

        ChallengeSettingsManager settingsManager = getForceBattleSettingsManager();
        boolean excludeBiomes = settingsManager != null && settingsManager.isEnabled(ForceBattleSettings.EXCLUDE_END);

        List<String> biomesToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.biomes");

        for (BiomeSimplified simplified : BiomeSimplified.values()) {
            if (biomesToExclude.contains(simplified.getName().toUpperCase(Locale.ROOT))) continue;
            if (excludeBiomes && ENDBIOMESSIMPLIFIED.contains(simplified)) continue;

            availableBiomes.add(simplified);
        }
        return availableBiomes;
    }

    public List<MccAdvancement> getAvailableAdvancements() {
        List<MccAdvancement> availableAdvancements = new ArrayList<>();

        List<String> advancementsToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.advancements");

        for (MccAdvancement advancement : MccAdvancement.values()) {
            if (advancementsToExclude.contains(advancement.name())) continue;

            availableAdvancements.add(advancement);
        }
        return availableAdvancements;
    }

    public List<Integer> getAvailableHeights() {
        List<Integer> availableHeights = new ArrayList<>();

        List<String> heightsToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.heights");

        for (int i = -63; i < 321; i++) {
            if (heightsToExclude.contains(String.valueOf(i))) continue;

            availableHeights.add(i);
        }
        return availableHeights;
    }

    public Location getRandomLocation(MccPlayer mccPlayer) {
        org.bukkit.entity.Player player = mccPlayer.getPlayer();
        if (player == null) return new Location(Bukkit.getWorld("world"), 0, 0, 0);

        int maxDistance = 2000;

        int offsetX = ThreadLocalRandom.current().nextInt(-maxDistance, maxDistance + 1);
        int offsetZ = ThreadLocalRandom.current().nextInt(-maxDistance, maxDistance + 1);

        Location base = player.getLocation();

        int x = (int) base.getX() + offsetX;
        int z = (int) base.getZ() + offsetZ;

        int y = player.getWorld().getHighestBlockYAt(x, z);

        return new Location(player.getWorld(), x, y, z);
    }

    public List<MccStructure> getAvailableStructures() {
        List<MccStructure> availableStructures = new ArrayList<>();
        ChallengeSettingsManager settingsManager = getForceBattleSettingsManager();
        boolean excludeEndStructures = settingsManager != null && settingsManager.isEnabled(ForceBattleSettings.EXCLUDE_END);

        List<String> structuresToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.structures");

        for (MccStructure structure : MccStructure.values()) {
            if (structuresToExclude.contains(structure.getKey().toString())) continue;
            if (excludeEndStructures && ENDSTRUCTURES.contains(structure)) continue;

            availableStructures.add(structure);
        }
        return availableStructures;
    }

    public List<StructureSimplified> getAvailableStructuresSimplified() {
        List<StructureSimplified> availableStructures = new ArrayList<>();
        ChallengeSettingsManager settingsManager = getForceBattleSettingsManager();
        boolean excludeEndStructures = settingsManager != null && settingsManager.isEnabled(ForceBattleSettings.EXCLUDE_END);

        List<String> structuresToExclude = MinecraftChallenges.get().getConfig().getStringList("exclude.structures");

        for (StructureSimplified simplified : StructureSimplified.values()) {
            if (structuresToExclude.contains(simplified.name().toUpperCase(Locale.ROOT))) continue;
            if (excludeEndStructures && ENDSTRUCTURESSIMPLIFIED.contains(simplified)) continue;

            availableStructures.add(simplified);
        }
        return availableStructures;
    }

    public List<MccDamageType> getAvailableDeaths() {
        return new ArrayList<>(Arrays.asList(MccDamageType.values()));
    }

    public List<DamageTypeSimplified> getAvailableDeathsSimplified() {
        return new ArrayList<>(Arrays.asList(DamageTypeSimplified.values()));
    }

    private final List<Material> ENDITEMS = List.of(
            Material.END_STONE, Material.END_STONE_BRICK_SLAB, Material.END_STONE_BRICKS, Material.END_STONE_BRICK_STAIRS, Material.END_STONE_BRICK_WALL,
            Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.END_GATEWAY, Material.END_ROD, Material.ELYTRA, Material.PURPUR_BLOCK, Material.PURPUR_PILLAR,
            Material.PURPUR_SLAB, Material.PURPUR_STAIRS, Material.CHORUS_FLOWER, Material.CHORUS_FRUIT, Material.CHORUS_PLANT, Material.POPPED_CHORUS_FRUIT,
            Material.DRAGON_BREATH, Material.DRAGON_EGG, Material.DRAGON_HEAD, Material.DRAGON_WALL_HEAD, Material.SHULKER_SHELL, Material.SHULKER_BOX,
            Material.BLUE_SHULKER_BOX, Material.BLACK_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.WHITE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX, Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE
    );
    private final List<EntityType> ENDMOBS = List.of(
            EntityType.ENDER_DRAGON, EntityType.SHULKER, EntityType.SHULKER_BULLET, EntityType.DRAGON_FIREBALL
    );
    private final List<MobsSimplified> ENDMOBSSIMPLIFIED = List.of(
            MobsSimplified.ENDER
    );
    private final List<Biome> ENDBIOMES = List.of(
            Biome.THE_END, Biome.END_BARRENS, Biome.END_HIGHLANDS, Biome.END_MIDLANDS, Biome.SMALL_END_ISLANDS
    );
    private final List<BiomeSimplified> ENDBIOMESSIMPLIFIED = List.of(
            BiomeSimplified.END
    );
    private final List<MccStructure> ENDSTRUCTURES = List.of(
            MccStructure.END_CITY
    );
    private final List<StructureSimplified> ENDSTRUCTURESSIMPLIFIED = List.of(
            StructureSimplified.END_CITY
    );

    private static final class ObjectiveSeed {
        private final ForceBattleType type;
        private final Object value;
        @Getter
        private final String key;

        private ObjectiveSeed(ForceBattleType type, Object value) {
            this.type = type;
            this.value = value;
            this.key = ForceBattleObjective.buildObjectiveString(type, value);
        }

        public ForceBattleObjective toObjective() {
            return new ForceBattleObjective(type, value);
        }
    }
}
