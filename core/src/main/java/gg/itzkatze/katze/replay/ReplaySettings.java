package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ReplaySettings {
    private ReplaySettings() {}

    public static GUIClickableItem button(int slot, ChallengeType type, Setting setting, KatzeGUI menu) {
        return new GUIClickableItem(slot) {
            @Override public void run(InventoryClickEvent event, KatzePlayer player) {
                if (player.getPlayer() == null || !player.getPlayer().isOp()) {
                    player.sendMessage(Component.translatable("katze.command.no_permission"));
                    return;
                }
                if (ChallengeManager.isGameStarted()) {
                    player.sendMessage(Component.translatable("katze.replay.saving.locked"));
                    return;
                }
                var challenge = ChallengeRegistry.getChallenge(type);
                if (challenge == null || challenge.getSettingsManager() == null) return;
                var manager = challenge.getSettingsManager();
                if (!(manager.getState(setting) instanceof ReplaySaveMode mode)) return;
                manager.setState(setting, mode.next());
                SessionPersistence.requestSave(type);
                menu.open(player);
            }

            @Override public ItemStack getItem(KatzePlayer player) {
                var mode = ReplaySaveMode.from(ChallengeRegistry.getChallenge(type));
                return ItemStackCreator.getStack(Component.translatable("katze.replay.saving.title").color(KatzeColor.LIGHT_BLUE), Material.SPYGLASS,
                        List.of(mode.getDisplayName().color(KatzeColor.WHITE), Component.translatable("katze.replay.saving.hint").color(KatzeColor.LIGHT_GRAY)));
            }
        };
    }
}
