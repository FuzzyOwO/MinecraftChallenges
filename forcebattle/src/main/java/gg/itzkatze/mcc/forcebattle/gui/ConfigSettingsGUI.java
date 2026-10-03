package gg.itzkatze.mcc.forcebattle.gui;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import gg.itzkatze.mcc.forcebattle.ForceBattleSettings;
import gg.itzkatze.mcc.forcebattle.exclusion.ObjectiveExclusion;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigSettingsGUI extends MccGUI {

    private static final int[] TOGGLE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    public ConfigSettingsGUI() {
        super("Config Settings", 27);
    }

    static @NotNull Map<Integer, ObjectiveExclusion> layout() {
        Map<Integer, ObjectiveExclusion> layout = new LinkedHashMap<>();
        List<ObjectiveExclusion> toggleable = ObjectiveExclusion.toggleable();
        for (int i = 0; i < Math.min(toggleable.size(), TOGGLE_SLOTS.length); i++) {
            layout.put(TOGGLE_SLOTS[i], toggleable.get(i));
        }
        return layout;
    }

    static void toggle(@NotNull ObjectiveExclusion exclusion) {
        ForceBattleSettings setting = Objects.requireNonNull(exclusion.getSetting());
        SettingStateType nextState = SettingsManager.getSettingState(setting).next();
        SettingsManager.setState(setting, nextState);

        MinecraftChallenges.broadcast(
                Component.text(setting.getDisplayName() + " ").color(NamedTextColor.BLUE)
                        .append(Component.text("has been set to ").color(NamedTextColor.GRAY))
                        .append(nextState.getDisplayName())
                        .append(Component.text("!").color(NamedTextColor.GRAY)));
    }

    private static @NotNull ItemStack createItem(@NotNull ObjectiveExclusion exclusion) {
        ForceBattleSettings setting = Objects.requireNonNull(exclusion.getSetting());
        List<Component> loreLines = List.of(
                Component.text("Click to toggle.").color(NamedTextColor.GRAY),
                Component.text(exclusion.getDescription()).color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Current Status").color(NamedTextColor.BLUE)
                        .append(Component.text(": ").color(NamedTextColor.DARK_GRAY))
                        .append(SettingsManager.getSettingState(setting).getDisplayName()));

        return ItemStackCreator.getStack(
                Component.text(setting.getDisplayName()).color(NamedTextColor.BLUE),
                exclusion.getIcon(),
                1,
                loreLines
        );
    }

    @Override
    public void setItems(MccPlayer player) {
        fill(ItemStackCreator.fillerItem());
        set(GUIClickableItem.getGoBackItem(0, new ForceBattleSettingsGUI()));

        layout().forEach((slot, exclusion) -> set(new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer player) {
                toggle(exclusion);
                new ConfigSettingsGUI().open(player);
            }

            @Override
            public ItemStack getItem(MccPlayer player) {
                return createItem(exclusion);
            }
        }));
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }

}
