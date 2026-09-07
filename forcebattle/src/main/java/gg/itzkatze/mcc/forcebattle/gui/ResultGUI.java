package gg.itzkatze.mcc.forcebattle.gui;

import gg.itzkatze.mcc.data.DamageTypeSimplified;
import gg.itzkatze.mcc.data.MccDamageType;
import gg.itzkatze.mcc.data.MccStructure;
import gg.itzkatze.mcc.data.MobsSimplified;
import gg.itzkatze.mcc.forcebattle.ForceBattleObjective;
import gg.itzkatze.mcc.data.BiomeSimplified;
import gg.itzkatze.mcc.data.MccAdvancement;
import gg.itzkatze.mcc.data.StructureSimplified;
import gg.itzkatze.mcc.team.MccTeam;
import lombok.Getter;
import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.GUIItem;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.time.Duration;

public class ResultGUI extends MccGUI {

    private static final int SPACES_ON_PAGE = 45;

    private final ResultType resultType;
    private final int page;
    private final MccPlayer targetPlayer;
    private final MccTeam targetTeam;

    private final boolean animated;
    private final boolean skipLoad;

    private final Map<MccPlayer, Integer> playerPageCache = new HashMap<>();
    private final Map<MccPlayer, List<ForceBattleObjective>> objectiveList = new HashMap<>();
    private final Map<MccPlayer, BukkitRunnable> activeAnimations = new HashMap<>();

    public ResultGUI(ResultType resultType, int page, MccPlayer targetPlayer, MccTeam targetTeam, boolean animated, boolean skipLoad) {
        super(generateTitle(resultType, targetPlayer, targetTeam, animated), 54);
        this.page = page;
        this.resultType = resultType;
        this.targetPlayer = targetPlayer;
        this.targetTeam = targetTeam;
        this.animated = animated;
        this.skipLoad = skipLoad;
    }

    public ResultGUI(ResultType resultType, int page, MccPlayer targetPlayer, MccTeam targetTeam, boolean animated) {
        this(resultType, page, targetPlayer, targetTeam, animated, false);
    }

    public ResultGUI(MccPlayer targetPlayer, boolean animated) {
        this(ResultType.PLAYER, 1, targetPlayer, null, animated, false);
    }

    public ResultGUI(MccTeam targetTeam, boolean animated) {
        this(ResultType.TEAM, 1, null, targetTeam, animated, false);
    }

    public ResultGUI() {
        this(ResultType.ALL, 1, null, null, false, false);
    }

    @Override
    public void open(MccPlayer viewer) {
        if (skipLoad) {
            super.open(viewer);

            List<ForceBattleObjective> objectives = objectiveList.get(viewer);
            if (objectives == null) return;

            if (!animated) setItems(viewer);
            return;
        }

        Collection<ForceBattleObjective> collection = switch (resultType) {
            case PLAYER -> ForceBattleObjective.finishedBy(targetPlayer);
            case TEAM -> ResultCommandUtils.combineTeamObjectives(targetTeam);
            case ALL -> ForceBattleObjective.finished();
        };

        List<ForceBattleObjective> objectives = new ArrayList<>(collection);
        objectiveList.put(viewer, objectives);
        playerPageCache.put(viewer, page);

        super.open(viewer);

        if (animated) startAnimation(viewer, objectives, 0);
        else setItems(viewer);
    }

    private void startAnimation(MccPlayer viewer, List<ForceBattleObjective> objectives, int startIndex) {
        fill(ItemStackCreator.fillerItem());

        BukkitRunnable task = new BukkitRunnable() {
            int index = startIndex;

            @Override
            public void run() {
                if (viewer.isOffline() || !viewer.getPlayer().getOpenInventory().getTopInventory().equals(getInventory())) {
                    cancel();
                    activeAnimations.remove(viewer);
                    return;
                }

                if (index >= objectives.size()) {
                    cancel();
                    activeAnimations.remove(viewer);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            viewer.getPlayer().playSound(viewer.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100, 1f);
                            viewer.getPlayer().closeInventory();
                            showResultSummary(viewer);
                        }
                    }.runTaskLater(MinecraftChallenges.get(), 20L);
                    return;
                }

                int currentPage = (index / SPACES_ON_PAGE) + 1;
                int slot = 9 + (index % SPACES_ON_PAGE);

                ForceBattleObjective objective = objectives.get(index);

                if (currentPage != page) {
                    ResultGUI nextPageGUI = new ResultGUI(resultType, currentPage, targetPlayer, targetTeam, true, true);
                    nextPageGUI.objectiveList.put(viewer, objectives);
                    nextPageGUI.playerPageCache.put(viewer, currentPage);

                    nextPageGUI.open(viewer);
                    nextPageGUI.startAnimation(viewer, objectives, index);

                    cancel();
                    return;
                }

                set(new GUIItem(slot) {
                    @Override
                    public ItemStack getItem(MccPlayer player) {
                        return ResultCommandUtils.buildObjectiveItem(objective, false);
                    }
                });

                updateInventory(viewer);
                index++;
            }
        };

        final int animationDelayTicks = 15;
        task.runTaskTimer(MinecraftChallenges.get(), 0L, animationDelayTicks);
        activeAnimations.put(viewer, task);
    }

    @Override
    public void setItems(MccPlayer viewer) {
        if (animated) return;

        int currentPage = playerPageCache.getOrDefault(viewer, 1);
        List<ForceBattleObjective> objectives = objectiveList.get(viewer);
        if (objectives == null) return;

        fill(ItemStackCreator.fillerItem());

        int totalPages = (int) Math.ceil((double) objectives.size() / SPACES_ON_PAGE);

        if (currentPage > 1) {
            set(GUIClickableItem.getGoBackItem(0, new ResultGUI(resultType, currentPage - 1, targetPlayer, targetTeam, false)));
        }

        if (currentPage < totalPages) {
            set(GUIClickableItem.getGoForthItem(8, new ResultGUI(resultType, currentPage + 1, targetPlayer, targetTeam, false)));
        }

        int startIndex = (currentPage - 1) * SPACES_ON_PAGE;
        for (int i = 0; i < SPACES_ON_PAGE; i++) {
            int index = startIndex + i;
            int slot = 9 + i;
            if (index >= objectives.size()) break;

            ForceBattleObjective objective = objectives.get(index);
            set(new GUIItem(slot) {
                @Override
                public ItemStack getItem(MccPlayer player) {
                    return ResultCommandUtils.buildObjectiveItem(objective, true);
                }
            });
        }
    }

    private void showResultSummary(MccPlayer viewer) {
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500));
        switch (resultType) {
            case PLAYER -> {
                if (targetPlayer != null) {
                    viewer.sendMessage(ResultUtils.buildPlayerResultMessage(targetPlayer));

                    viewer.getPlayer().showTitle(Title.title(
                            Component.text(targetPlayer.getName(), NamedTextColor.GOLD),
                            Component.text("Score: ", NamedTextColor.YELLOW)
                                    .append(Component.text(targetPlayer.getPoints(), NamedTextColor.AQUA)),
                            times
                    ));
                }
            }
            case TEAM -> {
                if (targetTeam != null) {
                    viewer.sendMessage(ResultUtils.buildTeamResultMessage(targetTeam));

                    viewer.getPlayer().showTitle(Title.title(
                            Component.text("Team " + targetTeam.getId(), NamedTextColor.GOLD),
                            Component.text("Score: ", NamedTextColor.YELLOW)
                                    .append(Component.text(targetTeam.getPoints(), NamedTextColor.AQUA)),
                            times
                    ));
                }
            }
            case ALL -> {
                viewer.getPlayer().showTitle(Title.title(
                        Component.text("All Results", NamedTextColor.AQUA),
                        Component.text("Animation complete", NamedTextColor.GRAY),
                        times
                ));
            }
        }
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }

    public void onClose(MccPlayer player, InventoryCloseEvent event) {
        BukkitRunnable task = activeAnimations.remove(player);
        if (task != null) task.cancel();

        objectiveList.remove(player);
        playerPageCache.remove(player);
    }

    private static String generateTitle(ResultType type, MccPlayer player, MccTeam team, boolean animated) {
        if (animated) {
            return "Results";
        }

        return switch (type) {
            case PLAYER -> "Results | " + (player != null ? player.getName() : "Unknown");
            case TEAM -> "Results | Team " + (team != null ? team.getId() : "N/A");
            default -> "Results | All Players";
        };
    }

    @Getter
    public enum ResultType {
        ALL("gui.result_title_all"),
        TEAM("gui.result_title_team"),
        PLAYER("gui.result_title_player");

        private final String titleKey;

        ResultType(String titleKey) {
            this.titleKey = titleKey;
        }
    }

    public static class ResultCommandUtils {

        public static @NotNull List<ForceBattleObjective> combineTeamObjectives(@NotNull MccTeam team) {
            List<ForceBattleObjective> objectives = new ArrayList<>();
            team.getPlayers().forEach(teamPlayer -> objectives.addAll(ForceBattleObjective.finishedBy(teamPlayer)));
            objectives.sort(Comparator.comparingInt(ForceBattleObjective::getTime).reversed());
            return objectives;
        }

        public static @NotNull ItemStack buildObjectiveItem(@NotNull ForceBattleObjective objective, boolean displayNames) {
            Material material = Material.STONE;
            Object obj = objective.getObjectiveValue();

            if (obj instanceof Material m) material = m;
            else if (obj instanceof EntityType || obj instanceof MobsSimplified) material = Material.SPIDER_SPAWN_EGG;
            else if (obj instanceof Biome || obj instanceof BiomeSimplified) material = Material.GRASS_BLOCK;
            else if (obj instanceof MccAdvancement) material = Material.BREWING_STAND;
            else if (obj instanceof Integer) material = Material.SCAFFOLDING;
            else if (obj instanceof Location) material = Material.DIAMOND_BOOTS;
            else if (obj instanceof MccStructure || obj instanceof StructureSimplified) material = Material.STRUCTURE_BLOCK;
            else if (obj instanceof MccDamageType || obj instanceof DamageTypeSimplified) material =  Material.SKELETON_SKULL;

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Time: ").color(NamedTextColor.GRAY)
                    .append(Component.text(StringUtility.formatTime(objective.getTime()))
                            .color(NamedTextColor.BLUE)));

            if (displayNames) {
                lore.add(Component.text("Player: ").color(NamedTextColor.GRAY)
                        .append(Component.text(objective.getWhoFinished().getName())
                                .color(NamedTextColor.BLUE)));
            }
            if (objective.hasBeenSkipped()) {
                lore.add(Component.empty());
                lore.add(Component.text("SKIPPED").color(NamedTextColor.RED));
            }

            Component name = Component.text(objective.getBattleType().getPrefix() + ": ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(StringUtility.formatName(objective.getObjectiveString()))
                            .color(NamedTextColor.BLUE));

            return ItemStackCreator.getStack(name, material, lore);
        }
    }
}
