package gg.itzkatze.mcc.forcebattle.gui;

import gg.itzkatze.mcc.forcebattle.ForceBattleTeam;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.GUIItem;
import gg.itzkatze.mcc.player.MccPlayer;

public class TeamBackpackGUI extends MccGUI {
    private final int index;
    private final ForceBattleTeam team;

    public TeamBackpackGUI(int index, int size, ForceBattleTeam team) {
        super("Team Backpack | Page: " + (index + 1), size);
        this.index = index;
        this.team = team;
    }

    @Override
    public void setItems(final MccPlayer player) {
        int inventoryPages = team.getBACKPACK_INVENTORIES().size();

        if (index < 0 || index >= inventoryPages) return;

        int nextArrowSlot = size - 1;
        int prevArrowSlot = size - 9;

        synchronized (items) {
            items.removeIf(item -> {
                if (item.slot == nextArrowSlot || item.slot == prevArrowSlot) {
                    return item instanceof GUIClickableItem;
                }
                return false;
            });
        }

        GUIItem nextItem = get(nextArrowSlot);
        if (nextItem instanceof GUIClickableItem) {
            getInventory().setItem(nextArrowSlot, null);
        }

        GUIItem prevItem = get(prevArrowSlot);
        if (prevItem instanceof GUIClickableItem) {
            getInventory().setItem(prevArrowSlot, null);
        }

        if (index < inventoryPages - 1) set(GUIClickableItem.getGoForthItem(size - 1, team.getBACKPACK_INVENTORIES().get(index + 1)));
        if (index > 0) set(GUIClickableItem.getGoBackItem(size - 9, team.getBACKPACK_INVENTORIES().get(index - 1)));
    }

    @Override
    public boolean allowItemMoving() {
        return true;
    }
}
