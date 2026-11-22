package gg.itzkatze.katze.game.entry;

import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.GUIItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class ActiveGamesGUI extends KatzeGUI {
    private static final int PAGE_SIZE = 45;
    private final ActiveGameProvider provider;
    private final GameEntryService entries;
    private final int page;

    public ActiveGamesGUI(ActiveGameProvider provider, GameEntryService entries) {
        this(provider, entries, 0);
    }

    private ActiveGamesGUI(ActiveGameProvider provider, GameEntryService entries, int page) {
        super(Component.translatable("katze.game.limbo.browser"), 54);
        this.provider = provider;
        this.entries = entries;
        this.page = page;
    }

    @Override
    public void setItems(KatzePlayer player) {
        clear();
        List<ActiveGameView> views = provider.activeGames();
        if (views.isEmpty()) {
            set(new GUIItem(13) {
                @Override public ItemStack getItem(KatzePlayer viewer) {
                    return ItemStackCreator.createNamedItemStack(Material.BARRIER, Component.translatable("katze.game.limbo.empty"));
                }
            });
            return;
        }
        int start = Math.min(page * PAGE_SIZE, Math.max(0, views.size() - 1));
        for (int index = start; index < Math.min(start + PAGE_SIZE, views.size()); index++) {
            ActiveGameView view = views.get(index);
            set(new GUIClickableItem(index - start) {
                @Override public ItemStack getItem(KatzePlayer viewer) {
                    Component modes = Component.join(JoinConfiguration.separator(Component.text(", ")),
                            view.modes().stream().map(type -> type.displayName()).toList());
                    Material icon = view.modes().isEmpty() ? Material.COMPASS : view.modes().getFirst().getMaterial().material();
                    Player player = viewer.getPlayer();
                    UUID playerId = player.getUniqueId();

                    boolean canRejoin = playerId != null
                            && entries.currentRoundId() != null
                            && entries.currentRoundId().equals(view.roundId())
                            && !entries.roundFinished()
                            && entries.isEligible(playerId);
                    Component loreText;
                    if (canRejoin) {
                        loreText = Component.translatable("katze.game.limbo.rejoin");
                    } else {
                        loreText = Component.translatable("katze.game.limbo.spectate");
                    }
                    return ItemStackCreator.getStack(modes, icon, 1, view.phase(),
                            Component.translatable("katze.game.limbo.participants", TextArg.number("count", view.participants())),
                            loreText);
                }
                @Override public void run(InventoryClickEvent event, KatzePlayer viewer) {
                    if (viewer.getPlayer() == null) return;
                    Player player = viewer.getPlayer();
                    UUID playerId = player.getUniqueId();

                    boolean canRejoin = playerId != null
                            && entries.currentRoundId() != null
                            && entries.currentRoundId().equals(view.roundId())
                            && !entries.roundFinished()
                            && entries.isEligible(playerId);
                    EntryResult result;
                    if (canRejoin) {
                        result = entries.enter(player, view.roundId(), EntryReason.REJOIN);
                    } else {
                        result = entries.enter(player, view.roundId(), EntryReason.SPECTATE);
                    }
                    if (result == EntryResult.DENIED) {
                        viewer.sendMessage(Component.translatable("katze.game.limbo.unavailable"));
                    }
                }
            });
        }
        if (page > 0) set(pageButton(45, page - 1, "katze.gui.back"));
        if (start + PAGE_SIZE < views.size()) set(pageButton(53, page + 1, "katze.gui.forth"));
    }

    @Override public boolean allowItemMoving() { return false; }

    private GUIClickableItem pageButton(int slot, int target, String label) {
        return new GUIClickableItem(slot) {
            @Override public ItemStack getItem(KatzePlayer player) {
                return ItemStackCreator.createNamedItemStack(Material.ARROW, Component.translatable(label));
            }
            @Override public void run(InventoryClickEvent event, KatzePlayer player) {
                new ActiveGamesGUI(provider, entries, target).open(player);
            }
        };
    }
}
