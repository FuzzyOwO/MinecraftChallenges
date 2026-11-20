package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class ReplayPlayersGUI extends KatzeGUI {
    private final ReplaySession session;
    private final int page;

    public ReplayPlayersGUI(ReplaySession session) { this(session, 0); }

    private ReplayPlayersGUI(ReplaySession session, int page) {
        super(Component.translatable("katze.replay.players"), 54);
        this.session = session;
        this.page = page;
    }

    @Override
    public void setItems(KatzePlayer viewer) {
        fill(ItemStackCreator.fillerItem());
        var players = session.players();
        for (int i = 0; i < 45 && page * 45 + i < players.size(); i++) {
            ReplayFrame.Actor actor = players.get(page * 45 + i);
            set(new GUIClickableItem(i) {
                @Override public void run(InventoryClickEvent event, KatzePlayer player) {
                    session.inspect(actor.id());
                }
                @Override public ItemStack getItem(KatzePlayer player) {
                    return ItemStackCreator.getPlayerSkull(actor.id(), Component.text(actor.name()), 1, Component.translatable("katze.replay.inspect"));
                }
            });
        }
        if (page > 0) set(GUIClickableItem.getGoBackItem(45, new ReplayPlayersGUI(session, page - 1)));
        if ((page + 1) * 45 < players.size()) set(GUIClickableItem.getGoForthItem(53, new ReplayPlayersGUI(session, page + 1)));
        set(GUIClickableItem.getCloseItem(49));
    }

    @Override public boolean allowItemMoving() { return false; }
}
