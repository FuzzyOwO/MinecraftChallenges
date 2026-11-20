package gg.itzkatze.katze.replay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.itzkatze.katze.text.Texts;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;

import java.util.Locale;

final class ReplayText {
    private ReplayText() {}

    static String encode(Component component) {
        JsonObject translations = new JsonObject();
        translations.add("en", GsonComponentSerializer.gson().serializeToTree(GlobalTranslator.render(component, Locale.ENGLISH)));
        translations.add("de", GsonComponentSerializer.gson().serializeToTree(GlobalTranslator.render(component, Locale.GERMAN)));
        JsonObject recorded = new JsonObject();
        recorded.add("replay_locales", translations);
        return recorded.toString();
    }

    static Component decode(String recorded, Locale locale) {
        JsonElement json = JsonParser.parseString(recorded);
        if (json.isJsonObject() && json.getAsJsonObject().has("replay_locales")) {
            json = json.getAsJsonObject().getAsJsonObject("replay_locales").get(Texts.isGerman(locale) ? "de" : "en");
        }
        return GsonComponentSerializer.gson().deserializeFromTree(json);
    }
}
