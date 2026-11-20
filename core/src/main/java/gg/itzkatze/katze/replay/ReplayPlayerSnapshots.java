package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.game.GameTimer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.StringUtility;
import net.kyori.adventure.text.Component;
import gg.itzkatze.katze.player.KatzePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Objects;

final class ReplayPlayerSnapshots {
    private final Map<UUID, ReplayFrame.PlayerState> previous = new HashMap<>();

    void invalidate() { previous.clear(); }

    ReplayFrame.PlayerState capture(Player player, int tick) {
        ReplayFrame.PlayerState last = previous.get(player.getUniqueId());
        KatzePlayer wrapper = KatzePlayer.adapt(player);
        boolean inventory = last == null || tick % 10 == 0;
        List<String> contents = inventory ? ReplayItems.encode(player.getInventory().getStorageContents()) : last.inventory();
        List<String> backpack = last == null ? null : last.backpack();
        List<String> information = last == null ? List.of() : last.information();
        Map<String, List<String>> scoreboards = last == null ? Map.of() : last.scoreboards();
        if (inventory) {
            var items = wrapper.getBackpackContents();
            backpack = items == null ? null : items.stream().map(ReplayItems::encode).toList();
            information = ChallengeManager.getActiveChallenges().stream().flatMap(challenge -> challenge.getSpectatorInfo(wrapper).stream())
                    .map(ReplayText::encode).toList();
            scoreboards = new HashMap<>();
            for (var challenge : ChallengeManager.getActiveChallenges()) {
                List<Component> lines = new ArrayList<>();
                GameTimer timer = KatzeGames.getTimer();
                if (timer.getMode() == GameTimer.Mode.COUNT_DOWN) lines.add(Component.translatable("katze.replay.sidebar.remaining",
                        TextArg.text("time", StringUtility.formatTime(timer.getRemainingSeconds()))));
                lines.addAll(challenge.getReplayInfo(wrapper));
                scoreboards.put(challenge.getType().name(), lines.stream().filter(Objects::nonNull)
                        .filter(line -> !line.equals(Component.empty())).map(ReplayText::encode).toList());
            }
        }
        var maximum = player.getAttribute(Attribute.MAX_HEALTH);
        var state = new ReplayFrame.PlayerState(contents, backpack, wrapper.isInTeam() ? wrapper.getTeam().getId() : null,
                player.getHealth(), maximum == null ? 20 : maximum.getValue(), player.getFoodLevel(), player.getLevel(), player.getWorld().getName(), information, scoreboards);
        previous.put(player.getUniqueId(), state);
        return state.equals(last) ? null : state;
    }
}
