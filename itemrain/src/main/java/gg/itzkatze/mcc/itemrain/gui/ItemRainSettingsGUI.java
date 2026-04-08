package gg.itzkatze.mcc.itemrain.gui;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.configuration.states.OnOffState;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.gui.impl.SettingsGUI;
import gg.itzkatze.mcc.gui.sign.NumberSignInput;
import gg.itzkatze.mcc.itemrain.ItemRainSettings;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemRainSettingsGUI extends MccGUI {

    public ItemRainSettingsGUI() {
        super("Item Rain Settings", 36);
    }

    @Override
    public void setItems(MccPlayer player) {
        fill(ItemStackCreator.fillerItem());

        set(new GUIClickableItem(13) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer player) {
                ItemRainSettings.ItemIntervalState state = getIntervalState();
                int current = state == null ? 10 : state.getSeconds();
                NumberSignInput.open(
                        player,
                        "Seconds (1-60)",
                        current,
                        1,
                        60,
                        ItemRainSettingsGUI.this::applySeconds,
                        (mccPlayer, message) -> {
                            mccPlayer.sendMessage(Component.text(message).color(NamedTextColor.RED));
                            new ItemRainSettingsGUI().open(mccPlayer);
                        }
                );
            }

            @Override
            public ItemStack getItem(MccPlayer p) {
                ItemRainSettings.ItemIntervalState state = getIntervalState();
                Component status = state == null ? Component.text("Unknown") : state.getDisplayName();
                return ItemStackCreator.getStack(
                        Component.text("Item Interval").color(NamedTextColor.AQUA),
                        Material.CLOCK,
                        1,
                        List.of(
                                Component.text("Current: ").color(NamedTextColor.GRAY).append(status),
                                Component.empty(),
                                Component.text("Click to set seconds").color(NamedTextColor.YELLOW)
                        )
                );
            }
        });

        set(new GUIClickableItem(15) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer player) {
                SettingStateType current = getState(ItemRainSettings.ItemRainSetting.SKY_DROP_MODE);
                SettingStateType next = current == null ? OnOffState.ON : current.next();
                updateSetting(ItemRainSettings.ItemRainSetting.SKY_DROP_MODE, next);
                new ItemRainSettingsGUI().open(player);
            }

            @Override
            public ItemStack getItem(MccPlayer p) {
                SettingStateType state = getState(ItemRainSettings.ItemRainSetting.SKY_DROP_MODE);
                Component status = state == null ? Component.text("Unknown") : state.getDisplayName();
                return ItemStackCreator.getStack(
                        Component.text("Sky Drop Mode").color(NamedTextColor.AQUA),
                        Material.ELYTRA,
                        1,
                        List.of(
                                Component.text("Current: ").color(NamedTextColor.GRAY).append(status),
                                Component.empty(),
                                Component.text("Click to toggle").color(NamedTextColor.YELLOW)
                        )
                );
            }
        });

        set(new GUIClickableItem(17) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer player) {
                ItemRainSettings.DropRadiusState state = getRadiusState();
                int current = state == null ? 5 : state.getRadius();
                NumberSignInput.open(
                        player,
                        "Radius (0-32)",
                        current,
                        0,
                        32,
                        ItemRainSettingsGUI.this::applyRadius,
                        (mccPlayer, message) -> {
                            mccPlayer.sendMessage(Component.text(message).color(NamedTextColor.RED));
                            new ItemRainSettingsGUI().open(mccPlayer);
                        }
                );
            }

            @Override
            public ItemStack getItem(MccPlayer p) {
                ItemRainSettings.DropRadiusState state = getRadiusState();
                Component status = state == null ? Component.text("Unknown") : state.getDisplayName();
                return ItemStackCreator.getStack(
                        Component.text("Sky Drop Radius").color(NamedTextColor.AQUA),
                        Material.WIND_CHARGE,
                        1,
                        List.of(
                                Component.text("Current: ").color(NamedTextColor.GRAY).append(status),
                                Component.empty(),
                                Component.text("Click to set radius").color(NamedTextColor.YELLOW)
                        )
                );
            }
        });

        set(GUIClickableItem.getGoBackItem(27, new SettingsGUI()));
    }

    private ItemRainSettings.ItemIntervalState getIntervalState() {
        SettingStateType state = getState(ItemRainSettings.ItemRainSetting.ITEM_INTERVAL);
        if (state instanceof ItemRainSettings.ItemIntervalState intervalState) {
            return intervalState;
        }
        return null;
    }

    private ItemRainSettings.DropRadiusState getRadiusState() {
        SettingStateType state = getState(ItemRainSettings.ItemRainSetting.SKY_DROP_RADIUS);
        if (state instanceof ItemRainSettings.DropRadiusState radiusState) {
            return radiusState;
        }
        return null;
    }

    private SettingStateType getState(ItemRainSettings.ItemRainSetting setting) {
        Challenge challenge = ChallengeRegistry.getChallenge(ChallengeType.ITEM_RAIN);
        if (challenge == null || challenge.getSettingsManager() == null) return null;
        return challenge.getSettingsManager().getState(setting);
    }

    private void updateSetting(ItemRainSettings.ItemRainSetting setting, SettingStateType state) {
        Challenge challenge = ChallengeRegistry.getChallenge(ChallengeType.ITEM_RAIN);
        if (challenge == null || challenge.getSettingsManager() == null) return;
        ChallengeSettingsManager manager = challenge.getSettingsManager();
        manager.setState(setting, state);
    }

    private void applySeconds(MccPlayer player, int seconds) {
        ItemRainSettings.ItemIntervalState state = ItemRainSettings.ItemIntervalState.fromSeconds(seconds);
        if (state == null) {
            player.sendMessage(Component.text("Unsupported value.").color(NamedTextColor.RED));
            new ItemRainSettingsGUI().open(player);
            return;
        }
        updateSetting(ItemRainSettings.ItemRainSetting.ITEM_INTERVAL, state);
        new ItemRainSettingsGUI().open(player);
    }

    private void applyRadius(MccPlayer player, int radius) {
        ItemRainSettings.DropRadiusState state = ItemRainSettings.DropRadiusState.fromRadius(radius);
        if (state == null) {
            player.sendMessage(Component.text("Unsupported value.").color(NamedTextColor.RED));
            new ItemRainSettingsGUI().open(player);
            return;
        }
        updateSetting(ItemRainSettings.ItemRainSetting.SKY_DROP_RADIUS, state);
        new ItemRainSettingsGUI().open(player);
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
