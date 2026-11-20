package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.scoreboard.ScoreboardContent;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ReplayScoreboard implements ScoreboardContent {
    private final ReplaySession session;

    ReplayScoreboard(ReplaySession session) { this.session = session; }

    @Override public Component getTitle() {
        return Component.translatable("katze.replay.sidebar.title", TextArg.text("game", session.battle().game().displayName()));
    }

    @Override public Component[] getLines(KatzePlayer viewer) {
        return lines(session.battle(), session.perspective(), session.playerState(session.perspective()), session.seconds(), session.duration() / 20, session.paused(), session.speed(), viewer.getPlayer().locale());
    }

    static Component[] lines(SavedBattle battle, UUID perspective, ReplayFrame.PlayerState state, int seconds, int duration, boolean paused, double speed) {
        return lines(battle, perspective, state, seconds, duration, paused, speed, Locale.ENGLISH);
    }

    static Component[] lines(SavedBattle battle, UUID perspective, ReplayFrame.PlayerState state, int seconds, int duration, boolean paused, double speed, Locale locale) {
        SavedBattle.Player player = battle.players().stream().filter(entry -> entry.id().equals(perspective)).findFirst().orElse(null);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("katze.replay.sidebar.progress", TextArg.text("time", StringUtility.formatTime(seconds)), TextArg.text("duration", StringUtility.formatTime(duration))));
        lines.add(Component.translatable(paused ? "katze.replay.sidebar.paused" : "katze.replay.sidebar.playing", TextArg.text("speed", Double.toString(speed))));
        if (player != null) lines.add(state != null && state.team() != null
                ? Component.translatable("katze.replay.sidebar.player_team", TextArg.text("player", player.name()), TextArg.number("team", state.team()))
                : Component.translatable("katze.replay.sidebar.player", TextArg.text("player", player.name())));
        if (state != null) lines.add(Component.translatable("katze.replay.sidebar.health", TextArg.decimal("health", state.health()), TextArg.decimal("max", state.maxHealth())));

        List<String> recorded = state == null ? List.of() : state.scoreboards().getOrDefault(battle.game().name(), List.of());
        if (recorded.isEmpty() && state != null) recorded = state.information();
        List<Component> details = recorded.stream().map(line -> ReplayText.decode(line, locale))
                .filter(line -> !PlainTextComponentSerializer.plainText().serialize(line).isBlank()).toList();
        if (!details.isEmpty()) lines.addAll(details);
        else {
            lines.add(Component.translatable("katze.replay.sidebar.players", TextArg.number("count", battle.players().size())));
            finalResults(lines, battle, player);
        }
        return lines.stream().limit(15).toArray(Component[]::new);
    }

    private static void finalResults(List<Component> lines, SavedBattle battle, SavedBattle.Player player) {
        lines.add(Component.translatable("katze.replay.sidebar.final"));
        if (battle.game() == SavedBattle.Game.BINGO && battle.bingo() != null) {
            for (int side = 0; side < battle.bingo().competitors().size(); side++) {
                int index = side;
                lines.add(Component.translatable("katze.replay.sidebar.competitor", TextArg.text("name", battle.bingo().competitors().get(side)),
                        TextArg.number("points", battle.bingo().cells().stream().filter(cell -> cell.owner() == index).count())));
            }
            return;
        }
        if (player == null) return;
        if (battle.game() == SavedBattle.Game.FORCE_BATTLE || battle.game() == SavedBattle.Game.ALL_ITEMS) {
            lines.add(Component.translatable("katze.results." + (battle.game() == SavedBattle.Game.ALL_ITEMS ? "discoveries" : "points"), TextArg.number("points", player.points())));
        }
        for (SavedBattle.Statistic statistic : player.statistics()) {
            if (statistic.key().equals("forfeited")) continue;
            if (statistic.key().equals("battle_ticks")) {
                lines.add(Component.translatable("katze.results.stat.battle_time", TextArg.text("value", StringUtility.formatTime(statistic.value() / 20))));
            } else if (statistic.key().equals("collected") && !player.personalCollection() && player.teamId() != null) {
                lines.add(Component.translatable("katze.replay.sidebar.team_collection", TextArg.number("count", statistic.value())));
            } else {
                String value = statistic.key().equals("survived") ? StringUtility.formatTime(statistic.value())
                        : statistic.total() >= 0 ? statistic.value() + "/" + statistic.total() : Integer.toString(statistic.value());
                lines.add(Component.translatable("katze.results.stat." + statistic.key(), TextArg.text("value", value)));
            }
        }
    }
}
