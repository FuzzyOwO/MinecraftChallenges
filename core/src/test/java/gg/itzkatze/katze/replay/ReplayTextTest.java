package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.text.TextStyle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ReplayTextTest {
    @Test void recordingPreservesNamedArgumentsAndBothViewerLanguages() throws Exception {
        var store = MiniMessageTranslationStore.create(Key.key("katzegames", "replay-text-test"), TextStyle.miniMessage());
        store.defaultLocale(Locale.ENGLISH);
        for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.GERMAN}) {
            Properties properties = new Properties();
            try (var input = new InputStreamReader(getClass().getResourceAsStream("/lang/replay/" + locale.getLanguage() + ".properties"), StandardCharsets.UTF_8)) {
                properties.load(input);
            }
            for (String key : properties.stringPropertyNames()) store.register(key, locale, properties.getProperty(key));
        }
        GlobalTranslator.translator().addSource(store);
        try {
            String count = ReplayText.encode(Component.translatable("katze.replay.sidebar.team_collection", TextArg.number("count", 37)));
            String player = ReplayText.encode(Component.translatable("katze.replay.sidebar.opponent", TextArg.text("player", "ItzKatze")));
            String wave = ReplayText.encode(Component.translatable("katze.replay.sidebar.wave", TextArg.number("wave", 2)));
            assertEquals("Team collection: 37", plain(count, Locale.ENGLISH));
            assertEquals("Teamsammlung: 37", plain(count, Locale.GERMAN));
            assertEquals("Opponent: ItzKatze", plain(player, Locale.ENGLISH));
            assertEquals("Gegner: ItzKatze", plain(player, Locale.GERMAN));
            assertEquals("Wave: 2/3", plain(wave, Locale.ENGLISH));
            assertEquals("Welle: 2/3", plain(wave, Locale.GERMAN));
        } finally {
            GlobalTranslator.translator().removeSource(store);
        }
    }

    @Test void oldComponentRecordingsRemainReadable() {
        Component original = Component.text("Recorded objective");
        assertEquals(original, ReplayText.decode(GsonComponentSerializer.gson().serialize(original), Locale.GERMAN));
    }

    private String plain(String recorded, Locale locale) {
        return PlainTextComponentSerializer.plainText().serialize(ReplayText.decode(recorded, locale));
    }
}
