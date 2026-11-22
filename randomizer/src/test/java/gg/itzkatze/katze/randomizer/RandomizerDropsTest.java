package gg.itzkatze.katze.randomizer;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RandomizerDropsTest {

    private final RandomizerGameListener listener = new RandomizerGameListener(List.of(Material.DIAMOND), Stack::new);

    @Test void blockLootMobLootAndChestLootUseTheSameReplacement() {
        ItemStack mined = listener.randomizeBlockDrop(new Stack(Material.COAL, 4), null);
        List<ItemStack> chest = new ArrayList<>(List.of(new Stack(Material.COAL, 17)));
        List<ItemStack> mob = new ArrayList<>(List.of(new Stack(Material.COAL, 2)));

        listener.randomize(chest, null);
        listener.randomize(mob, null);

        assertEquals(Material.DIAMOND, mined.getType());
        assertEquals(mined.getType(), chest.getFirst().getType());
        assertEquals(mined.getType(), mob.getFirst().getType());
        assertEquals(4, mined.getAmount());
        assertEquals(17, chest.getFirst().getAmount());
        assertEquals(2, mob.getFirst().getAmount());
    }

    @Test void dirtAndGrassUseTheirActualDirtDrop() {
        ItemStack dirt = listener.randomizeBlockDrop(new Stack(Material.DIRT, 1), null);
        ItemStack grass = listener.randomizeBlockDrop(new Stack(Material.DIRT, 1), null);

        assertEquals(Material.DIAMOND, dirt.getType());
        assertEquals(dirt.getType(), grass.getType());
    }

    private static final class Stack extends ItemStack {
        private final Material material;
        private int amount;

        private Stack(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        @Override public Material getType() { return material; }
        @Override public int getAmount() { return amount; }
        @Override public boolean isEmpty() { return amount <= 0 || material == Material.AIR; }
        @Override public void setAmount(int amount) { this.amount = amount; }
        @Override public boolean isSimilar(ItemStack other) { return other != null && material == other.getType(); }
        @Override public ItemStack clone() { return new Stack(material, amount); }
    }
}
