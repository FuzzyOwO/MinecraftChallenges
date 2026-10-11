package gg.itzkatze.mcc.lobby.npc;

import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LobbyNpc {

    public abstract @NotNull String getId();

    public abstract @NotNull Component getName();

    public @Nullable ResolvableProfile getSkin() {
        return null;
    }

    public @Nullable Component getDescription() {
        return Component.text("CLICK", NamedTextColor.YELLOW, TextDecoration.BOLD);
    }

    public @NotNull List<Component> getDialogue(@NotNull MccPlayer player) {
        return List.of();
    }

    public @NotNull DialogueMode getDialogueMode() {
        return DialogueMode.EVERY_TIME;
    }

    public @Nullable MccGUI createGui(@NotNull MccPlayer player) {
        return null;
    }
}
