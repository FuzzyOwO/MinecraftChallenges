package gg.itzkatze.katze.replay;

import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public final class ReplayItems {
    private ReplayItems() {}

    public static String encode(ItemStack item) {
        return item == null || item.isEmpty() ? "" : Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static List<String> encode(ItemStack[] items) {
        return Arrays.stream(items).map(ReplayItems::encode).toList();
    }

    public static ItemStack decode(String item) {
        return item == null || item.isEmpty() ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(item));
    }

    public static ItemStack[] decode(List<String> items) {
        return items.stream().map(ReplayItems::decode).toArray(ItemStack[]::new);
    }
}
