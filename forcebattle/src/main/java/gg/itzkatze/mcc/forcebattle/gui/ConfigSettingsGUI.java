package gg.itzkatze.mcc.forcebattle.gui;

import gg.itzkatze.mcc.configuration.states.OnOffState;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import lombok.Getter;
import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.forcebattle.ForceBattleSettings;
import gg.itzkatze.mcc.configuration.Setting;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ConfigSettingsGUI extends MccGUI {

    public ConfigSettingsGUI() {
        super("Config Settings", 27);
    }

    @Getter
    public enum ConfigSettingButton {
        MUSIC_DISCS(10, Material.MUSIC_DISC_CAT, ForceBattleSettings.EXCLUDE_MUSIC_DISCS, "Exclude Music Discs",
                "Click to toggle excluding Music Discs.", "Exclude Music Discs"),
        BANNER_PATTERNS(11, Material.FLOWER_BANNER_PATTERN, ForceBattleSettings.EXCLUDE_BANNER_PATTERNS, "Exclude Banner Patterns",
                "Click to toggle excluding Banner Patterns.", "Exclude Banner Patterns"),
        ARMOR_TEMPLATES(12, Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, ForceBattleSettings.EXCLUDE_ARMOR_TEMPLATES, "Exclude Armor Templates",
                "Click to toggle excluding Armor Templates.", "Exclude Armor Templates"),
        POTTERY_SHERDS(13, Material.ANGLER_POTTERY_SHERD, ForceBattleSettings.EXCLUDE_POTTERY_SHERDS, "Exclude Pottery Sherds",
                "Click to toggle excluding Pottery Sherds.", "Exclude Pottery Sherds"),
        ORES(14, Material.DIAMOND_ORE, ForceBattleSettings.EXCLUDE_ORES, "Exclude Ores",
                "Click to toggle excluding Ores.", "Exclude Ores"),
        END(15, Material.END_STONE, ForceBattleSettings.EXCLUDE_END, "Exclude End Objectives",
                "Click to toggle excluding End Objectives.", "Exclude End Items"),
        TRIAL_ITEMS(16, Material.TRIAL_SPAWNER, ForceBattleSettings.EXCLUDE_TRIAL_ITEMS, "Exclude Trial Items",
                "Click to toggle excluding Trial Items.", "Exclude Trial Items");

        private final int slot;
        private final Material material;
        private final Setting setting;
        private final String nameText;
        private final String toggleLine;
        private final String changedMsg;

        ConfigSettingButton(int slot, Material material, Setting setting, String nameText, String toggleLine, String changedMsg) {
            this.slot = slot;
            this.material = material;
            this.setting = setting;
            this.nameText = nameText;
            this.toggleLine = toggleLine;
            this.changedMsg = changedMsg;
        }

        public ItemStack createItem() {
            List<Component> loreLines = new ArrayList<>();
            if (toggleLine != null && !toggleLine.isBlank()) {
                loreLines.add(Component.text(toggleLine).color(NamedTextColor.GRAY));
            }
            loreLines.add(Component.empty());
            loreLines.add(Component.text("Current Status").color(NamedTextColor.BLUE)
                    .append(Component.text(": ").color(NamedTextColor.DARK_GRAY))
                    .append(SettingsManager.getSettingState(setting).getDisplayName()));

            return ItemStackCreator.getStack(
                    Component.text(nameText).color(NamedTextColor.BLUE),
                    material,
                    1,
                    loreLines
            );
        }

        public void handleClick(MccPlayer whoClicked) {
            SettingStateType currentState = SettingsManager.getSettingState(setting);
            SettingStateType nextState = null;

            // For OnOff settings (most global settings), toggle between ON/OFF
            if (currentState instanceof OnOffState onOff) {
                nextState = onOff.next();
            }
            // For multi-state settings, cycle through states
            else if (currentState instanceof PlayerTeamBothState settingState) {
                nextState = settingState.next();
            }

            if (nextState != null) {
                SettingsManager.setState(setting, nextState);

                if (changedMsg != null) {
                    MinecraftChallenges.broadcast(
                            Component.text(changedMsg + " ").color(NamedTextColor.BLUE)
                                    .append(Component.text("has been set to ").color(NamedTextColor.GRAY))
                                    .append(nextState.getDisplayName())
                                    .append(Component.text("!").color(NamedTextColor.GRAY)));
                }
            }

            new ConfigSettingsGUI().open(whoClicked);
        }
    }

    @Override
    public void setItems(MccPlayer player) {
        fill(ItemStackCreator.fillerItem());
        set(GUIClickableItem.getGoBackItem(0, new ForceBattleSettingsGUI()));

        for (ConfigSettingButton button : ConfigSettingButton.values()) {
            set(new GUIClickableItem(button.getSlot()) {
                @Override
                public void run(InventoryClickEvent event, MccPlayer player) {
                    button.handleClick(player);
                }

                @Override
                public ItemStack getItem(MccPlayer player) {
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
