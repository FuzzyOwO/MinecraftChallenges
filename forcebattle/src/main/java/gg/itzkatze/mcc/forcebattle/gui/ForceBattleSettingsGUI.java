package gg.itzkatze.mcc.forcebattle.gui;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import lombok.Getter;
import gg.itzkatze.mcc.forcebattle.ForceBattle;
import gg.itzkatze.mcc.forcebattle.ForceBattleSettings;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.configuration.Setting;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.game.GameLifecycleManager;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.sign.NumberSignInput;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ForceBattleSettingsGUI extends MccGUI {

    public ForceBattleSettingsGUI() {
        super("Settings", 36);
    }

    @Getter
    public enum SettingButton {
        FORCE_ITEM(0, Material.ITEM_FRAME, ForceBattleSettings.FORCE_ITEM, true, "Force Item",
                "Click to toggle ForceItem.", "ForceItem"),
        FORCE_MOB(1, Material.DIAMOND_SWORD, ForceBattleSettings.FORCE_MOB, true, "Force Mob",
                "Click to toggle ForceMob.", "ForceMob"),
        FORCE_BIOME(2, Material.GRASS_BLOCK, ForceBattleSettings.FORCE_BIOME, true, "Force Biome",
                "Click to toggle ForceBiome.", "ForceBiome"),
        FORCE_ADVANCEMENT(3, Material.BREWING_STAND, ForceBattleSettings.FORCE_ADVANCEMENT, true, "Force Advancement",
                "Click to toggle ForceAdvancement.", "ForceAdvancement"),
        FORCE_HEIGHT(4, Material.SCAFFOLDING, ForceBattleSettings.FORCE_HEIGHT, true, "Force Height",
                "Click to toggle ForceHeight.", "ForceHeight"),
        FORCE_COORDS(5, Material.FILLED_MAP, ForceBattleSettings.FORCE_COORDS, true, "Force Coords",
                "Click to toggle ForceCoords.", "ForceCoords"),
        FORCE_STRUCTURE(6, Material.STRUCTURE_BLOCK, ForceBattleSettings.FORCE_STRUCTURE, true, "Force Structure",
                "Click to toggle ForceStructure.", "ForceStructure"),
        FORCE_DEATH(7, Material.SKELETON_SKULL, ForceBattleSettings.FORCE_DEATH, true, "Force Death",
                "Click to toggle ForceDeath.", "ForceDeath"),


        NO_HUNGER(18, Material.COOKED_BEEF, GlobalSetting.NO_HUNGER, false, "No Hunger",
                "Click to toggle no Hunger.", "No Hunger"),
        NO_DAMAGE(19, Material.IRON_SWORD, GlobalSetting.NO_DAMAGE, false, "No Damage",
                "Click to toggle no Damage.", "No Damage"),
        MOBILE_CRAFTING(20, Material.CRAFTING_TABLE, GlobalSetting.MOBILE_CRAFTING, false, "Mobile Crafting",
                "Click to toggle mobile crafting.", "Mobile Crafting"),
        BATTLE_DURATION(21, Material.CLOCK, null, false, "Battle Duration",
                "Click to set battle timer duration in minutes.", "Battle Duration"),
        CHAIN_MODE(27, Material.IRON_CHAIN, ForceBattleSettings.CHAIN_MODE, false, "Chain Mode",
                "Click to toggle Chain Mode.", "Chain Mode"),
        BACKPACK(28, Material.CHEST, ForceBattleSettings.BACKPACK, false, "Backpacks",
                "Click to toggle Backpacks.", "Backpacks"),
        HIDE_POINTS(29, Material.IRON_TRAPDOOR, GlobalSetting.HIDE_POINTS, false, "Hide Points",
                "Click to toggle Hide Points.", "Hide Points"),
        HIDE_OBJECTIVES(30, Material.DARK_OAK_DOOR, GlobalSetting.HIDE_OBJECTIVES, false, "Hide Objectives",
                "Click to toggle Hide Objectives.", "Hide Objectives"),
        NO_DUPLICATE_OBJECTIVES(31, Material.NAME_TAG, GlobalSetting.NO_DUPLICATE_OBJECTIVES, false, "No Duplicate Objectives",
                "Click to toggle no duplicate objectives.", "No duplicate objectives"),
        SIMPLIFIED_OBJECTIVES(32, Material.DIRT, GlobalSetting.SIMPLIFIED_OBJECTIVES, false, "Simplified Objectives",
                "Click to toggle simplified objectives.", "Simplified objectives"),
        EXTRA_TEAM_OBJECTIVE(33, Material.ALLAY_SPAWN_EGG, GlobalSetting.EXTRA_TEAM_OBJECTIVE, false, "Extra Team Objective",
                "Click to toggle the extra team objective.", "Extra team objective"),
        SHOW_SCOREBOARD(34, Material.ARMOR_STAND, GlobalSetting.SHOW_SCOREBOARD, false, "Show Scoreboard",
                "Click to toggle the scoreboard.", "Show scoreboard"),

        RESET(35, Material.BARRIER, null, false, "RESET", null, null);

        private final int slot;
        private final Material material;
        private final Setting setting;
        private final boolean multiState;
        private final String nameText;
        private final String toggleLine;
        private final String changedMsg;

        SettingButton(int slot, Material material, Setting setting, boolean multiState,
                      String nameText, String toggleLine, String changedMsg) {
            this.slot = slot;
            this.material = material;
            this.setting = setting;
            this.multiState = multiState;
            this.nameText = nameText;
            this.toggleLine = toggleLine;
            this.changedMsg = changedMsg;
        }

        public ItemStack createItem() {
            if (this == BATTLE_DURATION) {
                int minutes = MinecraftChallenges.get().getConfig().getInt(
                        ForceBattle.BATTLE_DURATION_MINUTES_KEY,
                        ForceBattle.DEFAULT_BATTLE_DURATION_MINUTES
                );
                List<Component> loreLines = new ArrayList<>();
                loreLines.add(Component.text(toggleLine).color(NamedTextColor.GRAY));
                loreLines.add(Component.empty());
                loreLines.add(Component.text("Current").color(NamedTextColor.BLUE)
                        .append(Component.text(": ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(minutes + " min").color(NamedTextColor.AQUA)));
                return ItemStackCreator.getStack(
                        Component.text(nameText).color(NamedTextColor.BLUE),
                        material,
                        1,
                        loreLines
                );
            }

            if (setting == null) {
                return ItemStackCreator.createNamedItemStack(
                        material,
                        Component.text(nameText).color(NamedTextColor.RED)
                );
            }

            List<Component> loreLines = new ArrayList<>();
            if (toggleLine != null && !toggleLine.isBlank()) {
                loreLines.add(Component.text(toggleLine).color(NamedTextColor.GRAY));
            }
            loreLines.add(Component.empty());
            loreLines.add(Component.text("Current Status").color(NamedTextColor.BLUE)
                    .append(Component.text(": ").color(NamedTextColor.DARK_GRAY))
                    .append(getSettingState().getDisplayName()));

            return ItemStackCreator.getStack(
                    Component.text(nameText).color(NamedTextColor.BLUE),
                    material,
                    1,
                    loreLines
            );
        }

        private SettingStateType getSettingState() {
            if (setting instanceof GlobalSetting globalSetting) {
                return SettingsManager.getState(globalSetting);
            } else if (setting instanceof ForceBattleSettings fbSetting) {
                Challenge forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE);
                if (forceBattle != null && forceBattle.getSettingsManager() != null) {
                    ChallengeSettingsManager manager = forceBattle.getSettingsManager();
                    return manager.getState(fbSetting);
                }
            }
            return PlayerTeamBothState.OFF;
        }

        public void handleClick(MccPlayer player) {
            if (this == RESET) {
                GameLifecycleManager.resetGame();
                return;
            }

            if (this == BATTLE_DURATION) {
                int current = MinecraftChallenges.get().getConfig().getInt(
                        ForceBattle.BATTLE_DURATION_MINUTES_KEY,
                        ForceBattle.DEFAULT_BATTLE_DURATION_MINUTES
                );
                NumberSignInput.open(
                        player,
                        "Battle Duration (min)",
                        current,
                        1,
                        1440,
                        (mccPlayer, value) -> {
                            MinecraftChallenges.get().getConfig().set(ForceBattle.BATTLE_DURATION_MINUTES_KEY, value);
                            MinecraftChallenges.get().saveConfig();
                            MinecraftChallenges.broadcast(
                                    Component.text("Battle Duration ").color(NamedTextColor.BLUE)
                                            .append(Component.text("has been set to ").color(NamedTextColor.GRAY))
                                            .append(Component.text(value + " min").color(NamedTextColor.AQUA))
                                            .append(Component.text("!").color(NamedTextColor.GRAY))
                            );
                            new ForceBattleSettingsGUI().open(mccPlayer);
                        },
                        (mccPlayer, error) -> {
                            mccPlayer.sendMessage(Component.text(error).color(NamedTextColor.RED));
                            new ForceBattleSettingsGUI().open(mccPlayer);
                        }
                );
                return;
            }

            if (setting == null) return;

            SettingStateType next = getSettingState().next();

            if (setting instanceof GlobalSetting globalSetting) {
                SettingsManager.setState(globalSetting, next);
            } else if (setting instanceof ForceBattleSettings fbSetting) {
                Challenge forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE);
                if (forceBattle != null && forceBattle.getSettingsManager() != null) {
                    ChallengeSettingsManager manager = forceBattle.getSettingsManager();
                    manager.setState(fbSetting, next);
                }
            }

            String settingName = changedMsg.replace("_", " ");
            settingName = settingName.substring(0, 1).toUpperCase() + settingName.substring(1);
            MinecraftChallenges.broadcast(
                    Component.text(settingName + " ").color(NamedTextColor.BLUE)
                            .append(Component.text("has been set to ").color(NamedTextColor.GRAY))
                            .append(getSettingState().getDisplayName())
                            .append(Component.text("!").color(NamedTextColor.GRAY)));

            new ForceBattleSettingsGUI().open(player);
        }
    }

    @Override
    public void setItems(MccPlayer player) {
        fill(ItemStackCreator.fillerItem());
        set(GUIClickableItem.getGoForthItem(8, new ConfigSettingsGUI()));


        for (SettingButton button : SettingButton.values()) {
            set(new GUIClickableItem(button.getSlot()) {
                @Override
                public void run(final InventoryClickEvent event, final MccPlayer player) {
                    button.handleClick(player);
                }

                @Override
                public ItemStack getItem(MccPlayer p) {
                    return button.createItem();
                }
            });
        }
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }

}
