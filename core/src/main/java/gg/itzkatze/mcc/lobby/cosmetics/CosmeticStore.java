package gg.itzkatze.mcc.lobby.cosmetics;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.util.JsonFiles;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class CosmeticStore {

    public interface Delayer {
        void runLater(@NotNull Runnable task, long ticks);
    }

    private static final Logger logger = LoggerFactory.getLogger("MinecraftChallenges/" + CosmeticStore.class.getSimpleName());
    private static final String FILE_NAME = "data/cosmetics.json";
    private static final int FILE_VERSION = 1;
    private static final long WRITE_DELAY_TICKS = 100;
    private static final DateTimeFormatter QUARANTINE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int SHORT_ID_LENGTH = 8;

    private static @Nullable CosmeticStore installed;

    private final Path file;
    private final CosmeticCatalog catalog;
    private final Delayer delayer;
    private final Clock clock;
    private final Map<UUID, CosmeticProfile> profiles = new LinkedHashMap<>();
    private final List<Consumer<UUID>> balanceListeners = new ArrayList<>();
    private boolean dirty;
    private boolean writePending;
    private int writes;

    public CosmeticStore(@NotNull Path file, @NotNull CosmeticCatalog catalog, @NotNull Delayer delayer) {
        this(file, catalog, delayer, Clock.systemDefaultZone());
    }

    public CosmeticStore(@NotNull Path file, @NotNull CosmeticCatalog catalog, @NotNull Delayer delayer, @NotNull Clock clock) {
        this.file = file;
        this.catalog = catalog;
        this.delayer = delayer;
        this.clock = clock;
        readFile();
    }

    public static @NotNull CosmeticStore get() {
        if (installed == null) {
            throw new IllegalStateException("The cosmetic store has not been loaded");
        }
        return installed;
    }

    public static void install(@Nullable CosmeticStore store) {
        installed = store;
    }

    public static void load() {
        MinecraftChallenges plugin = MinecraftChallenges.get();
        Path file = new File(plugin.getDataDirectory(), FILE_NAME).toPath();
        install(new CosmeticStore(file, CosmeticCatalog.standard(), (task, ticks) -> Bukkit.getScheduler().runTaskLater(plugin, task, ticks)));
    }

    public static void flushInstalled() {
        if (installed != null) {
            installed.flush();
        }
    }

    public @NotNull CosmeticCatalog catalog() {
        return catalog;
    }

    public @NotNull LocalDate today() {
        return LocalDate.now(clock);
    }

    public int points(@NotNull UUID player) {
        CosmeticProfile profile = profiles.get(player);
        return profile == null ? 0 : profile.points();
    }

    public int earned(@NotNull UUID player) {
        CosmeticProfile profile = profiles.get(player);
        return profile == null ? 0 : profile.earned();
    }

    public int best(@NotNull UUID player) {
        CosmeticProfile profile = profiles.get(player);
        return profile == null ? 0 : profile.best();
    }

    public boolean owns(@NotNull UUID player, @NotNull Cosmetic cosmetic) {
        CosmeticProfile profile = profiles.get(player);
        return profile != null && profile.owns(cosmetic.id());
    }

    public @NotNull Optional<Cosmetic> equipped(@NotNull UUID player, @NotNull CosmeticCategory category) {
        CosmeticProfile profile = profiles.get(player);
        return profile == null ? Optional.empty() : profile.equipped(category, catalog);
    }

    public @NotNull Optional<BlockCosmetic> equippedBlock(@NotNull UUID player) {
        return equipped(player, CosmeticCategory.BLOCK).filter(BlockCosmetic.class::isInstance).map(BlockCosmetic.class::cast);
    }

    public @NotNull Optional<SoundCosmetic> equippedSound(@NotNull UUID player) {
        return equipped(player, CosmeticCategory.SOUND).filter(SoundCosmetic.class::isInstance).map(SoundCosmetic.class::cast);
    }

    public @NotNull List<Standing> top(@NotNull LeaderboardMetric metric, int limit) {
        return profiles.entrySet().stream()
                .filter(entry -> metric.read(entry.getValue()) > 0)
                .sorted(Comparator.<Map.Entry<UUID, CosmeticProfile>>comparingInt(entry -> metric.read(entry.getValue())).reversed()
                        .thenComparing(CosmeticStore::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Map.Entry::getKey))
                .limit(Math.max(limit, 0))
                .map(entry -> new Standing(displayName(entry), metric.read(entry.getValue())))
                .toList();
    }

    public void recordLanding(@NotNull Player player, int runScore) {
        UUID id = player.getUniqueId();
        CosmeticProfile profile = profiles.computeIfAbsent(id, key -> new CosmeticProfile());
        int before = profile.points();
        profile.rename(player.getName());
        profile.recordLanding(runScore);
        markDirty();
        if (profile.points() != before) {
            notifyBalance(id);
        }
    }

    public @NotNull PurchaseResult buy(@NotNull Player player, @NotNull Cosmetic cosmetic) {
        CosmeticProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) return cosmetic.isAvailable(today()) ? PurchaseResult.NOT_ENOUGH_POINTS : PurchaseResult.OUT_OF_SEASON;

        PurchaseResult result = profile.buy(cosmetic, catalog, today());
        if (result == PurchaseResult.BOUGHT) {
            profile.rename(player.getName());
            markDirty();
            notifyBalance(player.getUniqueId());
        }
        return result;
    }

    public boolean spend(@NotNull Player player, int amount) {
        CosmeticProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) return false;

        int before = profile.points();
        if (!profile.spend(amount)) return false;

        changed(player, profile, before);
        return true;
    }

    public void addPoints(@NotNull Player player, int amount) {
        if (amount <= 0) return;

        CosmeticProfile profile = profiles.computeIfAbsent(player.getUniqueId(), key -> new CosmeticProfile());
        int before = profile.points();
        profile.addPoints(amount);
        changed(player, profile, before);
    }

    public boolean grant(@NotNull Player player, @NotNull Cosmetic cosmetic) {
        CosmeticProfile profile = profiles.computeIfAbsent(player.getUniqueId(), key -> new CosmeticProfile());
        if (!profile.grant(cosmetic)) return false;

        changed(player, profile, profile.points());
        return true;
    }

    public int addEventPoints(@NotNull Player player, int amount) {
        CosmeticProfile profile = profiles.computeIfAbsent(player.getUniqueId(), key -> new CosmeticProfile());
        int before = profile.points();
        int applied = profile.addEventPoints(amount);
        changed(player, profile, before);
        return applied;
    }

    public boolean equip(@NotNull Player player, @NotNull Cosmetic cosmetic) {
        CosmeticProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || !profile.equip(cosmetic, catalog)) return false;

        profile.rename(player.getName());
        markDirty();
        return true;
    }

    public void unequip(@NotNull Player player, @NotNull CosmeticCategory category) {
        CosmeticProfile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.equipped(category, catalog).isEmpty()) return;

        profile.unequip(category, catalog);
        profile.rename(player.getName());
        markDirty();
    }

    public void addBalanceListener(@NotNull Consumer<UUID> listener) {
        balanceListeners.add(listener);
    }

    public void flush() {
        write();
    }

    boolean isDirty() {
        return dirty;
    }

    int writes() {
        return writes;
    }

    private static @NotNull String displayName(@NotNull Map.Entry<UUID, CosmeticProfile> entry) {
        String name = entry.getValue().name();
        return name == null || name.isBlank() ? entry.getKey().toString().substring(0, SHORT_ID_LENGTH) : name;
    }

    private void changed(@NotNull Player player, @NotNull CosmeticProfile profile, int pointsBefore) {
        profile.rename(player.getName());
        markDirty();
        if (profile.points() != pointsBefore) {
            notifyBalance(player.getUniqueId());
        }
    }

    private void notifyBalance(@NotNull UUID player) {
        for (Consumer<UUID> listener : List.copyOf(balanceListeners)) {
            try {
                listener.accept(player);
            } catch (RuntimeException e) {
                logger.error("A cosmetic balance listener failed", e);
            }
        }
    }

    private void markDirty() {
        dirty = true;
        if (!writePending) {
            writePending = true;
            delayer.runLater(this::writeScheduled, WRITE_DELAY_TICKS);
        }
    }

    private void writeScheduled() {
        writePending = false;
        write();
    }

    private void write() {
        if (!dirty) return;

        FileData data = new FileData();
        profiles.forEach((id, profile) -> data.players.put(id.toString(), profile));
        try {
            JsonFiles.write(file, data);
            dirty = false;
            writes++;
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to save {}: {}", file, e.getMessage());
        }
    }

    private void readFile() {
        try {
            FileData data = JsonFiles.read(file, FileData.class);
            if (data != null && data.players != null) {
                data.players.forEach(this::adopt);
            }
        } catch (IOException | RuntimeException e) {
            profiles.clear();
            quarantine(e);
        }
    }

    private void adopt(@NotNull String key, @Nullable CosmeticProfile profile) {
        if (profile == null) return;

        UUID id;
        try {
            id = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            logger.warn("Ignoring the entry \"{}\" of {}: it is not a UUID.", key, file);
            return;
        }
        profile.normalize(catalog);
        profiles.put(id, profile);
    }

    private void quarantine(@NotNull Exception cause) {
        Path aside = file.resolveSibling(file.getFileName() + ".corrupt-" + LocalDateTime.now().format(QUARANTINE_TIME));
        try {
            Files.move(file, aside);
            logger.warn("Could not read {} ({}). Moved it to {} and started with an empty cosmetic store.", file, cause.getMessage(), aside.getFileName());
        } catch (IOException e) {
            logger.error("Could not read {} ({}) and could not move it aside ({}). Starting with an empty cosmetic store.", file, cause.getMessage(), e.getMessage());
        }
    }

    private static final class FileData {
        private int version = FILE_VERSION;
        private Map<String, CosmeticProfile> players = new LinkedHashMap<>();
    }
}
