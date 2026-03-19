package gg.itzkatze.mcc.forcebattle;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.forcebattle.tasks.*;
import gg.itzkatze.mcc.util.Joker;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.game.SpawnManager;
import gg.itzkatze.mcc.team.MccTeam;
import org.bukkit.event.Listener;
import gg.itzkatze.mcc.event.PlayerTeamJoinEvent;
import gg.itzkatze.mcc.event.PlayerTeamLeaveEvent;
import gg.itzkatze.mcc.event.TimerEndEvent;
import gg.itzkatze.mcc.event.TimerStartEvent;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.Toast;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ForceBattleGameListener implements Listener {

    public static boolean startPhase = true;

    public ForceBattleGameListener() {
        // Tasks are now managed by the Challenge system
    }

    /**
     * Runs all ForceBattle-specific tasks.
     */
    public void runTasks() {
        new ItemTask().runTick();
        new BiomeTask().runTick();
        new AdvancementTask().runTick();
        new HeightTask().runTick();
        new CoordsTask().runTick();
        new StructureTask().runTick();
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (MinecraftChallenges.getTimer().isRunning()) {
            UUID uuid = event.getUniqueId();
            if (!MccPlayer.getAllUUIDs().contains(uuid)) {

                Component contestants = Component.empty();

                if (!MccTeam.teams.isEmpty()) {
                    for (MccTeam team : MccTeam.teams) {
                        if (team instanceof ForceBattleTeam fbTeam) {
                            String playersList = fbTeam.getPlayers().stream()
                                    .map(MccPlayer::getName)
                                    .reduce((a, b) -> a + ", " + b)
                                    .orElse("");
                            Component line = Component.text("• " + fbTeam.getId() + ": ", NamedTextColor.AQUA)
                                    .append(Component.text(playersList, NamedTextColor.WHITE));
                            contestants = contestants.append(line).append(Component.text("\n"));
                        }
                    }
                } else {
                    for (MccPlayer player : MccPlayer.PLAYERS) {
                        Component line = Component.text("• ", NamedTextColor.AQUA)
                                .append(Component.text(player.getName(), NamedTextColor.WHITE));
                        contestants = contestants.append(line).append(Component.text("\n"));
                    }
                }

                String formattedTime = StringUtility.formatTime(MinecraftChallenges.getTimer().getTime());

                Component kickMessage = Component.text("✖ A game is currently running! ✖", NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text("\n\n"))
                        .append(Component.text("⏳ Time remaining: ", NamedTextColor.GRAY))
                        .append(Component.text(formattedTime, NamedTextColor.GOLD))
                        .append(Component.text("\n\n"))
                        .append(Component.text("👥 ", NamedTextColor.AQUA))
                        .append(Component.text("Current Teams / Players:", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                        .append(Component.text("\n"))
                        .append(contestants.colorIfAbsent(NamedTextColor.WHITE));

                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        kickMessage
                );
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        MccPlayer mccPlayer = MccPlayer.adapt(event.getPlayer());

        updateAllTablists();

        if (!MinecraftChallenges.getTimer().isRunning() && startPhase) {
            SpawnManager.createSpawnIfNeeded();
            mccPlayer.getPlayer().setGameMode(GameMode.ADVENTURE);
            mccPlayer.getPlayer().teleport(SpawnManager.getSpawn());
        }

        boolean firstStartup = MinecraftChallenges.get().getConfig().getBoolean("first-startup", true);
        if (firstStartup && PermissionDefault.OP.getValue(event.getPlayer().isOp())) {
            mccPlayer.sendMessage(Component.text("----\nIt seems to be the first time you start the plugin.\n-> Use /menu to adjust settings and /timer toggle to start the battle.\n----"));
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        updateAllTablists();
    }

    @EventHandler
    public void onGameStart(TimerStartEvent event) {
        startPhase = false;
        SpawnManager.destroySpawn();

        updateAllTablists();

        for (MccPlayer mccPlayer : MccPlayer.getOnlinePlayers()) {
            ForceBattlePlayer player = (ForceBattlePlayer) mccPlayer;
            if (player.getPlayer() == null) return;

            if (player.getObjective() == null) {
                player.updateObjective(false, false);
            }
            if (player.isInTeam() && player.getTeam().getObjective() == null) {
                player.getTeam().updateObjective(player, false, false);
            }
            player.getPlayer().playSound(player.getPlayer(), Sound.EVENT_RAID_HORN, SoundCategory.MASTER, 100, 1);
            player.getPlayer().setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.getPlayer().setHealth(20);
            player.getPlayer().setSaturation(20);
            player.getPlayer().setFoodLevel(20);
            player.getPlayer().setFireTicks(0);

            player.getPlayer().getInventory().addItem(Joker.getPlayerSkipItem(MinecraftChallenges.get().getConfig().getInt("playerSkips", 3)));
            player.getPlayer().getInventory().addItem(Joker.getSwapItem(MinecraftChallenges.get().getConfig().getInt("swaps", 1)));

            if (SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE) && player.isInTeam()) {
                player.getPlayer().getInventory().addItem(Joker.getTeamSkipItem(MinecraftChallenges.get().getConfig().getInt("teamSkips", 1)));
            }

            player.getPlayer().teleport(Bukkit.getWorld("world").getSpawnLocation());
        }

        for (MccTeam team : MccTeam.teams) {
            if (team instanceof ForceBattleTeam fbTeam) {
                fbTeam.createBackpackGUIs();
            }
        }
    }

    @EventHandler
    public void onGameEnd(TimerEndEvent event) {
        MinecraftChallenges.broadcast(Component.text("The battle is over. /displayresults (player/team) | /result (player/team)."));

        for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
            if (player.getPlayer() == null) return;

            if (player.isInTeam()) ((ForceBattleTeam) player.getTeam()).setObjective(null);
            player.setCurrentObjective(null, false, false);
            player.getPlayer().teleport(Bukkit.getWorld("world").getSpawnLocation());
            player.getPlayer().setHealth(20);
            player.getPlayer().setSaturation(20);
        }
    }

    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!MinecraftChallenges.getTimer().isRunning() || SettingsManager.isEnabled(GlobalSetting.NO_HUNGER)) {
            event.setCancelled(true);

            player.setFoodLevel(20);
            player.setSaturation(20);
        }
    }


    @EventHandler
    public void onTeamJoin(PlayerTeamJoinEvent event) {
        updateAllTablists();
    }

    @EventHandler
    public void onTeamLeave(PlayerTeamLeaveEvent event) {
        updateAllTablists();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (ForceBattleType.FORCE_MOB.getSettingState() == PlayerTeamBothState.OFF) return;

        Player damager = null;

        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (event.getDamager() instanceof Fireball fireball) {
            if (fireball.getShooter() instanceof Player player) {
                damager = player;
            }
        } else if (event.getDamager() instanceof Player player) {
            damager = player;
        } else return;

        ForceBattlePlayer mccPlayer = (ForceBattlePlayer) MccPlayer.adapt(damager);
        if (mccPlayer.isExcluded()) return;

        if (target.getHealth() - event.getDamage() > 0) return;

        checkPlayerObjective(mccPlayer, target);
        checkTeamObjective(mccPlayer, target);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!MinecraftChallenges.getTimer().isRunning() || SettingsManager.isEnabled(GlobalSetting.NO_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    private void updateAllTablists() {
        MccPlayer.getOnlinePlayers().forEach(player -> {
            if (player.isInTeam()) {
                player.getPlayer().setPlayerListOrder(player.getTeam().getId());
            } else {
                player.getPlayer().setPlayerListOrder(999);
            }
        });
    }

    private void checkPlayerObjective(ForceBattlePlayer player, LivingEntity target) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_MOB) return;

        EntityType required = objective.getObjectiveValue(EntityType.class);
        if (required != null && target.getType() == required) {
            completeObjective(player, objective.getObjectiveString());
            player.updateObjective(true, false);
        }
    }

    private void checkTeamObjective(ForceBattlePlayer player, LivingEntity target) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_MOB) return;

        EntityType required = teamObjective.getObjectiveValue(EntityType.class);
        if (required != null && target.getType() == required) {
            completeObjective(player, teamObjective.getObjectiveString());
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private void completeObjective(ForceBattlePlayer player, String objective) {
        String formattedObjective = StringUtility.formatName(objective);
        Material toastIcon = Material.DIAMOND_SWORD;
        MinecraftChallenges plugin = MinecraftChallenges.get();

        if (player.isInTeam() && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
            player.getTeam().getPlayers().stream()
                    .filter(teammate -> teammate != player)
                    .forEach(teammate -> {
                        teammate.sendMessage(Component.text(player.getName() + " has finished " + formattedObjective + "."));
                        Toast.display(teammate.getPlayer(), toastIcon, Component.text(formattedObjective).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
                    });
        }

        player.sendMessage(Component.text("You finished " + formattedObjective + "."));
        Toast.display(player.getPlayer(), toastIcon, Component.text(formattedObjective).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
    }

    @EventHandler
    public void onPlayerSkip(@NotNull PlayerInteractEvent event) {
        if (event.getHand() == null || !event.getHand().equals(EquipmentSlot.HAND)) {return;}
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {return;}
        if (event.getItem() == null || !Joker.isPlayerSkipItem(event.getItem())) {return;}

        ForceBattlePlayer player = (ForceBattlePlayer) MccPlayer.adapt(event.getPlayer());
        if (player.isExcluded()) return;

        event.setCancelled(true);

        if (!MinecraftChallenges.getTimer().isRunning()) {
            player.sendMessage(Component.text("Can't skip while timer is paused."));
            return;
        }
        if (player.getObjective() == null) {
            player.sendMessage(Component.text("You don't have an objective to skip."));
            return;
        }

        if (event.getPlayer().hasCooldown(event.getItem().getType())) return;
        event.getPlayer().setCooldown(event.getItem().getType(), 2);

        ForceBattleObjective oldObjective = player.getObjective();

        player.sendMessage(Component.text("You skipped " + StringUtility.formatName(oldObjective.getObjectiveString()) + "."));
        player.updateObjective(true, true);

        player.addSkippedObjectiveToList(oldObjective);

        decreaseItemAmount(event);

        if (oldObjective.getBattleType() == ForceBattleType.FORCE_ITEM) {
            Material material = oldObjective.getObjectiveValue(Material.class);
            if (material != null) {
                ItemStack itemStack = new ItemStack(material);
                player.getPlayer().getWorld().dropItemNaturally(player.getPlayer().getLocation(), itemStack);
            }
        }
    }

    @EventHandler
    public void onTeamSkip(@NotNull PlayerInteractEvent event) {
        if (event.getHand() == null || !event.getHand().equals(EquipmentSlot.HAND)) {return;}
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {return;}
        if (event.getItem() == null || !Joker.isTeamSkipItem(event.getItem())) {return;}

        ForceBattlePlayer mccPlayer = (ForceBattlePlayer) MccPlayer.adapt(event.getPlayer());
        ForceBattleTeam team = (ForceBattleTeam) mccPlayer.getTeam();
        if (mccPlayer.isExcluded()) return;

        event.setCancelled(true);

        if (!MinecraftChallenges.getTimer().isRunning()) {
            mccPlayer.sendMessage(Component.text("Can't skip while timer is paused."));
            return;
        }
        if (team.getObjective() == null) {
            mccPlayer.sendMessage(Component.text("You don't have an objective to skip."));
            return;
        }

        if (event.getPlayer().hasCooldown(event.getItem().getType())) return;
        event.getPlayer().setCooldown(event.getItem().getType(), 2);

        ForceBattleObjective oldObjective = team.getObjective();

        team.getPlayers().forEach(player -> {
            if (player != mccPlayer) {
                player.sendMessage(Component.text(player.getName() + " has skipped " + StringUtility.formatName(oldObjective.getObjectiveString()) + "."));
            }
        });

        mccPlayer.sendMessage(Component.text("You skipped " + StringUtility.formatName(oldObjective.getObjectiveString()) + "."));

        team.updateObjective(mccPlayer, true, true);

        mccPlayer.addSkippedObjectiveToList(oldObjective);

        decreaseItemAmount(event);

        if (oldObjective.getBattleType() == ForceBattleType.FORCE_ITEM) {
            Material material = oldObjective.getObjectiveValue(Material.class);
            if (material != null) {
                ItemStack itemStack = new ItemStack(material);
                mccPlayer.getPlayer().getWorld().dropItemNaturally(mccPlayer.getPlayer().getLocation(), itemStack);
            }
        }
    }

    @EventHandler
    public void onPlayerSwap(@NotNull PlayerInteractEvent event) {
        if (event.getHand() == null || !event.getHand().equals(EquipmentSlot.HAND)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (event.getItem() == null || !Joker.isSwapItem(event.getItem())) return;

        ForceBattlePlayer mccPlayer = (ForceBattlePlayer) MccPlayer.adapt(event.getPlayer());
        if (mccPlayer.isExcluded()) return;
        event.setCancelled(true);

        if (!MinecraftChallenges.getTimer().isRunning()) {
            mccPlayer.sendMessage(Component.text("Can't swap while timer is paused."));
            return;
        }

        if (mccPlayer.getObjective() == null) {
            mccPlayer.sendMessage(Component.text("There is no player to swap your objective with."));
            return;
        }

        List<MccPlayer> availablePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (event.getPlayer().equals(player)) continue;
            ForceBattlePlayer toSwap = (ForceBattlePlayer) MccPlayer.adapt(player);
            ForceBattlePlayer sender = (ForceBattlePlayer) MccPlayer.adapt(event.getPlayer());
            if (toSwap.getObjective() == null) continue;
            if (toSwap.getTeam() == sender.getTeam() && toSwap.isInTeam() && sender.isInTeam()) continue;
            availablePlayers.add(toSwap);
        }

        if (availablePlayers.isEmpty()) {
            mccPlayer.sendMessage(Component.text("There is no player to swap your objective with."));
            return;
        }

        if (event.getPlayer().hasCooldown(event.getItem().getType())) return;
        event.getPlayer().setCooldown(event.getItem().getType(), 2);

        decreaseItemAmount(event);
        Random random = new Random();
        ForceBattlePlayer targetPlayer = (ForceBattlePlayer) availablePlayers.get(random.nextInt(availablePlayers.size()));

        ForceBattleObjective playerObjective = mccPlayer.getObjective();
        ForceBattleObjective targetObjective = targetPlayer.getObjective();

        mccPlayer.setCurrentObjective(targetObjective, false, false);
        targetPlayer.setCurrentObjective(playerObjective, false, false);

        targetPlayer.sendMessage(Component.text(mccPlayer.getName() + " swapped their objective with yours."));

        mccPlayer.sendMessage(Component.text("Your objective has been swapped with " + targetPlayer.getName() + "."));

        targetPlayer.getPlayer().playSound(targetPlayer.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 1, 20);
    }

    private void decreaseItemAmount(@NotNull PlayerInteractEvent event) {
        Inventory inventory = event.getPlayer().getInventory();
        int slot = event.getPlayer().getInventory().getHeldItemSlot();
        ItemStack stack = inventory.getItem(slot);
        if (stack != null) {
            stack.setAmount(stack.getAmount() - 1);
            inventory.setItem(slot, stack);
            event.getPlayer().setCooldown(stack.getType(), 20);
        }
    }

}
