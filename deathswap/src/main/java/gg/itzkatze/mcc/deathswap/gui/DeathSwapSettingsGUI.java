package gg.itzkatze.mcc.deathswap.gui;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import gg.itzkatze.mcc.deathswap.DeathSwapSettings;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.gui.sign.NumberSignInput;
import gg.itzkatze.mcc.gui.impl.SettingsGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeathSwapSettingsGUI extends MccGUI {

    public DeathSwapSettingsGUI() {
        super("Death Swap Settings", 27);
    }

    @Override
    public void setItems(MccPlayer player) {
        fill(ItemStackCreator.fillerItem());

        set(new GUIClickableItem(13) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer player) {
                DeathSwapSettings.SwapIntervalState state = getIntervalState();
                int current = state == null ? 5 : state.getMinutes();
                NumberSignInput.open(
                        player,
                        "Minutes (1-60)",
                        current,
                        1,
                        60,
                        DeathSwapSettingsGUI.this::applyMinutes,
                        (mccPlayer, message) -> {
                            mccPlayer.sendMessage(Component.text(message).color(NamedTextColor.RED));
                            new DeathSwapSettingsGUI().open(mccPlayer);
                        }
                );
            }

            @Override
            public ItemStack getItem(MccPlayer p) {
                DeathSwapSettings.SwapIntervalState state = getIntervalState();
                Component status = state == null ? Component.text("Unknown") : state.getDisplayName();
                return ItemStackCreator.getStack(
                        Component.text("Swap Interval").color(NamedTextColor.AQUA),
                        Material.CLOCK,
                        1,
                        List.of(
                                Component.text("Current: ").color(NamedTextColor.GRAY).append(status),
                                Component.empty(),
                                Component.text("Click to set minutes").color(NamedTextColor.YELLOW)
                        )
                );
            }
        });

        set(GUIClickableItem.getGoBackItem(18, new SettingsGUI()));
    }

    private DeathSwapSettings.SwapIntervalState getIntervalState() {
        Challenge challenge = ChallengeRegistry.getChallenge(ChallengeType.DEATH_SWAP);
        if (challenge == null || challenge.getSettingsManager() == null) return null;
        ChallengeSettingsManager manager = challenge.getSettingsManager();
        SettingStateType state = manager.getState(DeathSwapSettings.DeathSwapSetting.SWAP_INTERVAL);
        if (state instanceof DeathSwapSettings.SwapIntervalState intervalState) {
            return intervalState;
        }
        return null;
    }

    private void updateSetting(SettingStateType state) {
        Challenge challenge = ChallengeRegistry.getChallenge(ChallengeType.DEATH_SWAP);
        if (challenge == null || challenge.getSettingsManager() == null) return;
        ChallengeSettingsManager manager = challenge.getSettingsManager();
        manager.setState(DeathSwapSettings.DeathSwapSetting.SWAP_INTERVAL, state);
    }

    private void applyMinutes(MccPlayer player, int minutes) {
        DeathSwapSettings.SwapIntervalState state = DeathSwapSettings.SwapIntervalState.fromMinutes(minutes);
        if (state == null) {
            player.sendMessage(Component.text("Unsupported value.").color(NamedTextColor.RED));
            new DeathSwapSettingsGUI().open(player);
            return;
        }
        updateSetting(state);
        new DeathSwapSettingsGUI().open(player);
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
