package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.text.Texts;
import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

final class ReplayHotbar {
    private static final NamespacedKey CONTROL = new NamespacedKey("katzegames", "replay_control");

    private ReplayHotbar() {}

    static void install(Player player, ReplaySession session) {
        item(player, 0, Material.ARROW, Component.translatable("katze.replay.back"));
        item(player, 1, session.paused() ? Material.LIME_DYE : Material.RED_DYE, Component.translatable(session.paused() ? "katze.replay.resume" : "katze.replay.pause"));
        item(player, 2, Material.ARROW, Component.translatable("katze.replay.forward"));
        item(player, 3, Material.CLOCK, Component.translatable("katze.replay.speed", TextArg.text("speed", Double.toString(session.speed()))));
        item(player, 4, Material.PLAYER_HEAD, Component.translatable("katze.replay.players"));
        for (int slot = 5; slot <= 7; slot++) player.getInventory().setItem(slot, null);
        if (session.battle().game() == SavedBattle.Game.MOB_ARMY) item(player, 5, Material.IRON_SWORD, Component.translatable("katze.replay.jump_combat"));
        item(player, 8, Material.BARRIER, Component.translatable("katze.replay.leave"));
    }

    private static void item(Player player, int slot, Material material, Component name) {
        ItemStack item = ItemStackCreator.getStack(name, material, List.of(Component.translatable("katze.replay.use_control")));
        item.editMeta(meta -> meta.getPersistentDataContainer().set(CONTROL, PersistentDataType.INTEGER, slot));
        player.getInventory().setItem(slot, Texts.render(item, player));
    }

    static int control(ItemStack item) {
        return item == null || !item.hasItemMeta() ? -1 : item.getItemMeta().getPersistentDataContainer().getOrDefault(CONTROL, PersistentDataType.INTEGER, -1);
    }
}
