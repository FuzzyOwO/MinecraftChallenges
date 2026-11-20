package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.configuration.states.ConfigurableSettingState;
import net.kyori.adventure.text.Component;

import java.util.Locale;

public record ReplaySaveMode(Scope scope, boolean hasBattlePhase) implements ConfigurableSettingState {
    public enum Scope { ALL, BATTLE, OFF }

    public static ReplaySaveMode fullGame() { return new ReplaySaveMode(Scope.ALL, false); }
    public static ReplaySaveMode withBattlePhase() { return new ReplaySaveMode(Scope.ALL, true); }

    public static ReplaySaveMode from(Challenge challenge) {
        if (challenge != null && challenge.getSettingsManager() != null) {
            var manager = challenge.getSettingsManager();
            for (var setting : manager.getAllSettings()) {
                if (manager.getState(setting) instanceof ReplaySaveMode mode) return mode;
            }
        }
        return new ReplaySaveMode(Scope.OFF, false);
    }

    public boolean records(boolean battleStarted) { return scope == Scope.ALL || scope == Scope.BATTLE && battleStarted; }

    @Override public String serialize() { return scope.name(); }

    @Override public ReplaySaveMode parse(String raw) {
        try {
            Scope selected = Scope.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return selected == Scope.BATTLE && !hasBattlePhase ? null : new ReplaySaveMode(selected, hasBattlePhase);
        } catch (IllegalArgumentException e) { return null; }
    }

    @Override public Component getDisplayName() { return Component.translatable("katze.replay.saving." + scope.name().toLowerCase(Locale.ROOT)); }

    @Override public ReplaySaveMode next() {
        return new ReplaySaveMode(switch (scope) {
            case ALL -> hasBattlePhase ? Scope.BATTLE : Scope.OFF;
            case BATTLE -> Scope.OFF;
            case OFF -> Scope.ALL;
        }, hasBattlePhase);
    }
}
