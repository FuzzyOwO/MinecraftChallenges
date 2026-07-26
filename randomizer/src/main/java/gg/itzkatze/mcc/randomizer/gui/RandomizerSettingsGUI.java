package gg.itzkatze.mcc.randomizer.gui;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.gui.impl.SettingsGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.randomizer.RandomizerSettings;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RandomizerSettingsGUI extends MccGUI {

    public RandomizerSettingsGUI() {
        super("Randomizer Settings", 27);
    }

    private enum SettingButton {
        BLOCK_DROPS(10, Material.GRASS_BLOCK, "Block Drops", RandomizerSettings.RandomizerSetting.BLOCK_DROPS,
                List.of(Component.text("Randomize mined block drops").color(NamedTextColor.GRAY))),
        MOB_DROPS(12, Material.ROTTEN_FLESH, "Mob Drops", RandomizerSettings.RandomizerSetting.MOB_DROPS,
                List.of(Component.text("Randomize mob drops").color(NamedTextColor.GRAY))),
        CHEST_LOOT(14, Material.CHEST, "Chest Loot", RandomizerSettings.RandomizerSetting.CHEST_LOOT,
                List.of(Component.text("Randomize generated chest loot").color(NamedTextColor.GRAY))),
        SHARED_MAPPINGS(16, Material.PLAYER_HEAD, "Shared Mappings", RandomizerSettings.RandomizerSetting.SHARED_MAPPINGS,
                List.of(
                        Component.text("On: every player shares the same mapping").color(NamedTextColor.GRAY),
                        Component.text("Off: each player gets their own mapping").color(NamedTextColor.GRAY)
                ));

        private final int slot;
        private final Material material;
        private final String label;
        private final RandomizerSettings.RandomizerSetting setting;
        private final List<Component> description;

        SettingButton(int slot, Material material, String label, RandomizerSettings.RandomizerSetting setting,
                      List<Component> description) {
            this.slot = slot;
            this.material = material;
            this.label = label;
            this.setting = setting;
            this.description = description;
        }
    }

    @Override
    public void setItems(MccPlayer player) {
        fill(ItemStackCreator.fillerItem());

        for (SettingButton button : SettingButton.values()) {
            set(new GUIClickableItem(button.slot) {
                @Override
                public void run(InventoryClickEvent event, MccPlayer player) {
                    SettingStateType state = getState(button.setting);
                    if (state != null) {
                        updateSetting(button.setting, state.next());
                    }
                    new RandomizerSettingsGUI().open(player);
                }

                @Override
                public ItemStack getItem(MccPlayer p) {
                    SettingStateType state = getState(button.setting);
                    Component status = state == null ? Component.text("Unknown") : state.getDisplayName();
                    List<Component> lore = new java.util.ArrayList<>(button.description);
                    lore.add(Component.empty());
                    lore.add(Component.text("Current: ").color(NamedTextColor.GRAY).append(status));
                    lore.add(Component.empty());
                    lore.add(Component.text("Click to toggle").color(NamedTextColor.YELLOW));
                    return ItemStackCreator.getStack(
                            Component.text(button.label).color(NamedTextColor.AQUA),
                            button.material,
                            1,
                            lore
                    );
                }
            });
        }

        set(GUIClickableItem.getGoBackItem(18, new SettingsGUI()));
    }

    private SettingStateType getState(RandomizerSettings.RandomizerSetting setting) {
        Challenge challenge = ChallengeRegistry.getChallenge(ChallengeType.RANDOMIZER);
        if (challenge == null || challenge.getSettingsManager() == null) return null;
        return challenge.getSettingsManager().getState(setting);
    }

    private void updateSetting(RandomizerSettings.RandomizerSetting setting, SettingStateType state) {
        Challenge challenge = ChallengeRegistry.getChallenge(ChallengeType.RANDOMIZER);
        if (challenge == null || challenge.getSettingsManager() == null) return;
        ChallengeSettingsManager manager = challenge.getSettingsManager();
        manager.setState(setting, state);
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
