package gg.itzkatze.mcc.forcebattle.tasks;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.forcebattle.ForceBattleObjective;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.forcebattle.ForceBattlePlayer;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.data.MccStructure;
import gg.itzkatze.mcc.data.StructureSimplified;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.forcebattle.ForceBattleType;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.Toast;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.util.BoundingBox;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StructureTask implements ForceTask {

    private static final int MAX_CACHE_ENTRIES = 128;
    private static final int MAX_BOXES_PER_ENTRY = 512;
    private static final int MAX_IN_FLIGHT_SCANS = 8;
    private static final Map<String, Set<BoundingBox>> STRUCTURE_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Set<BoundingBox>> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            });
    private static final Set<String> IN_FLIGHT_SCANS = new HashSet<>();
    private static long cacheGeneration;

    public static void clearCache() {
        synchronized (STRUCTURE_CACHE) {
            STRUCTURE_CACHE.clear();
            IN_FLIGHT_SCANS.clear();
            cacheGeneration++;
        }
    }

    @Override
    public void runTick() {
        if (ForceBattleType.FORCE_STRUCTURE.getSettingState() == PlayerTeamBothState.OFF) return;
        if (!MinecraftChallenges.getTimer().isRunning()) return;

        Registry<Structure> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);

        for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
            ForceBattlePlayer mccPlayer = (ForceBattlePlayer) player;
            if (mccPlayer.isExcluded()) continue;

            checkPlayerObjective(mccPlayer, registry);
            checkTeamObjective(mccPlayer, registry);
        }
    }

    private void checkPlayerObjective(ForceBattlePlayer player, Registry<Structure> registry) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_STRUCTURE) return;

        if (isInsideStructure(player, objective.getObjectiveValue(), registry)) {
            completeObjective(player, objective.getObjectiveString());
            player.updateObjective(true, false);
        }
    }

    private void checkTeamObjective(ForceBattlePlayer player, Registry<Structure> registry) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_STRUCTURE) return;

        if (isInsideStructure(player, teamObjective.getObjectiveValue(), registry)) {
            completeObjective(player, teamObjective.getObjectiveString());
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private boolean isInsideStructure(ForceBattlePlayer player, Object objectiveValue, Registry<Structure> registry) {
        if (objectiveValue instanceof StructureSimplified simplified) {
            for (MccStructure structure : simplified.getStructures()) {
                Structure bukkitStruct = registry.get(structure.getKey());
                if (bukkitStruct != null && checkPlayerLocationInStructure(player, structure.getKey(), bukkitStruct)) {
                    return true;
                }
            }
            return false;
        }

        if (objectiveValue instanceof MccStructure structure) {
            Structure bukkitStruct = registry.get(structure.getKey());
            return bukkitStruct != null && checkPlayerLocationInStructure(player, structure.getKey(), bukkitStruct);
        }

        return false;
    }

    private boolean checkPlayerLocationInStructure(ForceBattlePlayer fbPlayer, NamespacedKey structureKey, Structure bukkitStruct) {
        Player player = fbPlayer.getPlayer();
        if (player == null) return false;

        String worldName = player.getWorld().getName();
        String cacheKey = worldName + ":" + structureKey;
        Set<BoundingBox> cachedBoxes;
        synchronized (STRUCTURE_CACHE) {
            Set<BoundingBox> boxes = STRUCTURE_CACHE.get(cacheKey);
            cachedBoxes = boxes == null ? null : Set.copyOf(boxes);
        }
        Location playerLoc = player.getLocation();

        if (cachedBoxes != null) {
            for (BoundingBox box : cachedBoxes) {
                if (isInsideBoundingBox(playerLoc, box)) return true;
            }
        }

        Chunk center = playerLoc.getChunk();
        String scanRegion = cacheKey + ":" + center.getX() + ":" + center.getZ();
        final long generation;
        final String scanKey;
        synchronized (STRUCTURE_CACHE) {
            generation = cacheGeneration;
            scanKey = scanRegion + ":" + generation;
            if (IN_FLIGHT_SCANS.size() >= MAX_IN_FLIGHT_SCANS || !IN_FLIGHT_SCANS.add(scanKey)) {
                return false;
            }
        }

        Bukkit.getScheduler().runTask(MinecraftChallenges.get(), () -> {
            try {
                World world = Bukkit.getWorld(worldName);
                if (world == null) return;
                int radius = 6;

                Set<BoundingBox> newBoxes = new HashSet<>();
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        Chunk chunk = world.getChunkAt(center.getX() + dx, center.getZ() + dz);
                        for (GeneratedStructure generated : chunk.getStructures(bukkitStruct)) {
                            for (StructurePiece piece : generated.getPieces()) {
                                newBoxes.add(piece.getBoundingBox());
                            }
                        }
                    }
                }

                synchronized (STRUCTURE_CACHE) {
                    if (generation != cacheGeneration || newBoxes.isEmpty()) return;
                    Set<BoundingBox> boxes =
                            STRUCTURE_CACHE.computeIfAbsent(cacheKey, key -> new HashSet<>());
                    for (BoundingBox box : newBoxes) {
                        if (boxes.size() >= MAX_BOXES_PER_ENTRY) break;
                        boxes.add(box);
                    }
                }
            } catch (Exception ignored) {}
            finally {
                synchronized (STRUCTURE_CACHE) {
                    IN_FLIGHT_SCANS.remove(scanKey);
                }
            }
        });

        return false;
    }

    private boolean isInsideBoundingBox(Location loc, BoundingBox box) {
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= box.getMinX() && x <= box.getMaxX()
                && y >= box.getMinY() && y <= box.getMaxY()
                && z >= box.getMinZ() && z <= box.getMaxZ();
    }

    private void completeObjective(MccPlayer player, String objective) {
        String formattedObjective = StringUtility.formatName(objective);
        Material toastIcon = Material.STRUCTURE_BLOCK;
        MinecraftChallenges plugin = MinecraftChallenges.get();

        if (player.isInTeam() && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
            player.getTeam().getPlayers().stream()
                    .filter(teammate -> teammate != player)
                    .filter(teammate -> !teammate.isOffline())
                    .forEach(teammate -> {
                        teammate.sendMessage(Component.text(player.getName() + " has finished " + formattedObjective + "."));
                        Toast.display(teammate.getPlayer(), toastIcon, Component.text(formattedObjective).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
                    });
        }

        player.sendMessage(Component.text("You finished " + formattedObjective + "."));
        Toast.display(player.getPlayer(), toastIcon, Component.text(formattedObjective).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
    }
}
