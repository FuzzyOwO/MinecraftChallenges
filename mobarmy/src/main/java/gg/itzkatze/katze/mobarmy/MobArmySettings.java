package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.replay.ReplaySaveMode;
import gg.itzkatze.katze.configuration.states.ConfigurableSettingState;
import gg.itzkatze.katze.configuration.states.OnOffState;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import gg.itzkatze.katze.text.TextArg;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

enum MobArmySettings implements Setting {
    STARTER_EQUIPMENT(OnOffState.ON),
    BACKPACK(OnOffState.ON),
    COLLECTION_DURATION(new Duration(90)),
    MOB_LIMIT(new MobLimit(100)),
    REPLAY_SAVING(ReplaySaveMode.withBattlePhase());

    private final SettingStateType initial;

    MobArmySettings(SettingStateType initial) {this.initial = initial;}

    @Override
    public String getId() {return name();}

    @Override
    public Component getDisplayName() {return Component.translatable(this == REPLAY_SAVING ? "katze.replay.saving.title" : "katze.mobarmy.setting." + name().toLowerCase(Locale.ROOT));}

    @Override
    public SettingStateType getDefaultState() {return initial;}

    @Override
    public boolean shouldUpdateOnChange() {return false;}

    record Duration(int minutes) implements ConfigurableSettingState {

        Duration {if (minutes < 1 || minutes > 1440) throw new IllegalArgumentException("Duration must be 1–1440 minutes");}

        @Override
        public @NonNull String serialize() {return Integer.toString(minutes);}

        @Override
        public SettingStateType parse(@NonNull String raw) {
            try {return new Duration(Integer.parseInt(raw));} catch (IllegalArgumentException exception) {return null;}
        }

        @Override
        public @NonNull Component getDisplayName() {return Component.translatable("katze.mobarmy.minutes", TextArg.number("minutes", minutes));}

        @Override
        public @NonNull SettingStateType next() {return this;}

    }

    record MobLimit(int amount) implements ConfigurableSettingState {

        MobLimit {if (amount < 1 || amount > 1000) throw new IllegalArgumentException("Mob limit must be 1–1000");}

        @Override
        public @NonNull String serialize() {return Integer.toString(amount);}

        @Override
        public SettingStateType parse(@NonNull String raw) {
            try {return new MobLimit(Integer.parseInt(raw));} catch (IllegalArgumentException exception) {return null;}
        }

        @Override
        public @NonNull Component getDisplayName() {return Component.translatable("katze.mobarmy.mob_limit", TextArg.number("amount", amount));}

        @Override
        public @NonNull SettingStateType next() {return new MobLimit(amount >= 1000 ? 1 : amount + 1);}

    }
}
