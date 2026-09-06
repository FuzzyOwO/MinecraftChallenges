package gg.itzkatze.mcc.forcebattle;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.ChallengeObjective;
import gg.itzkatze.mcc.data.BiomeSimplified;
import gg.itzkatze.mcc.data.MccAdvancement;
import gg.itzkatze.mcc.data.MccStructure;
import gg.itzkatze.mcc.data.MobsSimplified;
import gg.itzkatze.mcc.data.StructureSimplified;
import gg.itzkatze.mcc.player.MccPlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ForceBattleObjective implements ChallengeObjective {

    private static final List<ForceBattleObjective> objectives = new ArrayList<>();
    @Getter
    private final ForceBattleType battleType;
    @Getter
    private final String objectiveString;
    @Getter
    private final Object objectiveValue;
    private int time;
    @Getter
    private MccPlayer whoFinished;
    @Setter
    private boolean hasBeenSkipped;

    public ForceBattleObjective(ForceBattleType battleType, Object objectiveValue) {
        this.battleType = battleType;
        this.objectiveValue = objectiveValue;
        this.objectiveString = buildObjectiveString(battleType, objectiveValue);
        objectives.add(this);
    }

    public static @NotNull List<ForceBattleObjective> finishedBy(MccPlayer mccPlayer) {
        return objectives.stream()
                .filter(ForceBattleObjective::isFinished)
                .filter(o -> o.whoFinished.equals(mccPlayer))
                .sorted(Comparator.comparingInt(ForceBattleObjective::getTime).reversed())
                .toList();
    }

    public static @NotNull List<ForceBattleObjective> finished() {
        return objectives.stream()
                .filter(ForceBattleObjective::isFinished)
                .sorted(Comparator.comparingInt(ForceBattleObjective::getTime).reversed())
                .toList();
    }

    public static void deleteAll() {
        objectives.clear();
    }

    public void delete() {
        objectives.remove(this);
    }

    public boolean hasBeenSkipped() {
        return hasBeenSkipped;
    }

    public int getTime() {
        if (!isFinished()) {
            return -1;
        }
        return time;
    }

    public boolean isFinished() {
        return whoFinished != null;
    }

    public void setFinished(MccPlayer whoFinished) {
        this.whoFinished = whoFinished;
        this.time = MinecraftChallenges.getTimer().getTime();
    }

    public @NotNull Material getToastIcon() {
        if (objectiveValue instanceof Material material) return material;
        if (objectiveValue instanceof EntityType || objectiveValue instanceof MobsSimplified) return Material.SPIDER_SPAWN_EGG;
        if (objectiveValue instanceof Biome || objectiveValue instanceof BiomeSimplified) return Material.GRASS_BLOCK;
        if (objectiveValue instanceof MccAdvancement) return Material.BREWING_STAND;
        if (objectiveValue instanceof Integer) return Material.SCAFFOLDING;
        if (objectiveValue instanceof Location) return Material.COMPASS;
        if (objectiveValue instanceof MccStructure || objectiveValue instanceof StructureSimplified) return Material.STRUCTURE_BLOCK;
        return Material.BOOK;
    }

    public @Nullable <T> T getObjectiveValue(@NotNull Class<T> type) {
        return type.isInstance(objectiveValue) ? type.cast(objectiveValue) : null;
    }

    static @NotNull String buildObjectiveString(@NotNull ForceBattleType battleType, @Nullable Object value) {
        if (value == null) return "";

        return switch (battleType) {
            case FORCE_ITEM -> value instanceof Material material ? material.name() : value.toString();
            case FORCE_MOB -> {
                if (value instanceof MobsSimplified simplified) yield simplified.getName();
                if (value instanceof EntityType entityType) yield entityType.name();
                yield value.toString();
            }
            case FORCE_BIOME -> {
                if (value instanceof BiomeSimplified simplified) yield simplified.getName();
                if (value instanceof Biome biome) yield biome.getKey().toString();
                yield value.toString();
            }
            case FORCE_ADVANCEMENT -> value instanceof MccAdvancement advancement ? advancement.name() : value.toString();
            case FORCE_HEIGHT -> value.toString();
            case FORCE_COORDS -> {
                if (value instanceof Location location) {
                    yield location.getBlockX() + ", " + location.getBlockZ();
                }
                yield value.toString();
            }
            case FORCE_STRUCTURE -> {
                if (value instanceof StructureSimplified simplified) yield simplified.getName();
                if (value instanceof MccStructure structure) yield structure.name();
                yield value.toString();
            }
        };
    }
}
