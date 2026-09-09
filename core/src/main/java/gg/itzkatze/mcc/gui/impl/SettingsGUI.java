package gg.itzkatze.mcc.gui.impl;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.challenges.ChallengeSettings;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.GUIMaterial;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI extends MccGUI {

    public SettingsGUI() {
        super("Settings", 54);
    }

    @Override
    public void setItems(MccPlayer player) {
        fill(ItemStackCreator.fillerItem());

        List<Challenge> challenges = new ArrayList<>(ChallengeRegistry.getAllChallenges());
        int slot = 0;

        for (Challenge challenge : challenges) {
            if (slot >= 45) break;

            set(new GUIClickableItem(slot) {
                @Override
                public void run(InventoryClickEvent event, MccPlayer player) {
                    if (event.getClick() == ClickType.RIGHT && challenge instanceof ChallengeSettings settings && settings.hasSettings()) {
                        MccGUI settingsGUI = settings.getSettingsGUI();
                        if (settingsGUI != null) {
                            settingsGUI.open(player);
                        }
                    } else {
                        if (ChallengeManager.isChallengeActive(challenge.getType())) {
                            ChallengeManager.unloadChallenge(challenge);
                            player.sendMessage(Component.text("Disabled challenge: ").color(NamedTextColor.RED)
                                    .append(Component.text(challenge.getType().getName()).color(NamedTextColor.RED)));
                        } else {
                            ChallengeManager.loadChallenge(challenge);
                            player.sendMessage(Component.text("Enabled challenge: ").color(NamedTextColor.GREEN)
                                    .append(Component.text(challenge.getType().getName()).color(NamedTextColor.GREEN)));
                        }
                        new SettingsGUI().open(player);
                    }
                }

                @Override
                public ItemStack getItem(MccPlayer p) {
                    boolean active = ChallengeManager.isChallengeActive(challenge.getType());
                    GUIMaterial material = challenge.getType().getMaterial();
                    Component displayName = Component.text(challenge.getType().getName());

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Status: ").color(NamedTextColor.GRAY)
                            .append(Component.text(active ? "Enabled" : "Disabled")
                                    .color(active ? NamedTextColor.GREEN : NamedTextColor.RED)));

                    if (challenge.hasSettings()) {
                        lore.add(Component.empty());
                        lore.add(Component.text("Left-click: ").color(NamedTextColor.YELLOW)
                                .append(Component.text("Enable/Disable").color(NamedTextColor.WHITE)));
                        lore.add(Component.text("Right-click: ").color(NamedTextColor.YELLOW)
                                .append(Component.text("Open Settings").color(NamedTextColor.WHITE)));
                    } else {
                        lore.add(Component.empty());
                        lore.add(Component.text("Click: ").color(NamedTextColor.YELLOW)
                                .append(Component.text("Enable/Disable").color(NamedTextColor.WHITE)));
                    }

                    lore.add(Component.empty());
                    lore.add(Component.text("Description: ").color(NamedTextColor.GRAY)
                            .append(Component.text(challenge.getType().getDescription()).color(NamedTextColor.WHITE)));

                    if (material.hasSkull()) {
                        return ItemStackCreator.getSkull(material.skull(), displayName, 1, lore);
                    } else {
                        return ItemStackCreator.getStack(displayName, material.material(), 1, lore);
                    }
                }
            });

            slot++;
        }
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
