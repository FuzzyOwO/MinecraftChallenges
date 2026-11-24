package gg.itzkatze.katze.randomizer;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.configuration.ChallengeSettingsManager;
import gg.itzkatze.katze.configuration.states.OnOffState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    @Test void leafLitterHasTheSameDropWhenMinedDirectlyOrThroughItsSupport() throws ReflectiveOperationException {
        RandomizerGameListener randomizer = new RandomizerGameListener(List.of(Material.DIAMOND, Material.EMERALD), Stack::new);
        UUID worldId = UUID.randomUUID();
        World world = proxy(World.class, Map.of("getUID", worldId));
        Player player = proxy(Player.class, Map.of("getUniqueId", UUID.randomUUID()));
        BlockData data = proxy(BlockData.class, Map.of());
        BlockState state = proxy(BlockState.class, Map.of());
        Location directLocation = new Location(world, 5, 65, 3);
        Location litterLocation = new Location(world, 3, 65, 3);
        Block directBlock = proxy(Block.class, Map.of("getLocation", directLocation));
        Block support = proxy(Block.class, Map.of("getLocation", new Location(world, 3, 64, 3)));
        Block litter = proxy(Block.class, Map.of("getLocation", litterLocation));
        Item directDrop = dropItem(Material.LEAF_LITTER, 2, directLocation);
        Item indirectDrop = dropItem(Material.LEAF_LITTER, 2, litterLocation);

        withRandomizerEnabled(true, () -> {
            randomizer.onBlockDrop(new BlockDropItemEvent(directBlock, state, player, List.of(directDrop)));
            randomizer.onItemSpawn(new ItemSpawnEvent(directDrop));
            assertNotEquals(Material.LEAF_LITTER, directDrop.getItemStack().getType());

            randomizer.onBlockDestroy(new BlockDestroyEvent(litter, data, data, 0, true));
            randomizer.onBlockDrop(new BlockDropItemEvent(support, state, player, List.of(indirectDrop)));
            randomizer.onItemSpawn(new ItemSpawnEvent(indirectDrop));

            assertEquals(directDrop.getItemStack().getType(), indirectDrop.getItemStack().getType());
            assertEquals(2, indirectDrop.getItemStack().getAmount());

            Item fallbackDrop = dropItem(Material.LEAF_LITTER, 2, litterLocation);
            randomizer.onItemSpawn(new ItemSpawnEvent(fallbackDrop));
            assertEquals(directDrop.getItemStack().getType(), fallbackDrop.getItemStack().getType());
        });
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {"OAK_DOOR", "SUNFLOWER", "RED_BED"})
    void bothPartsUseTheBreakersMapping(Material material) throws ReflectiveOperationException {
        UUID breakerId = UUID.randomUUID();
        UUID bystanderId = UUID.randomUUID();
        Player breaker = proxy(Player.class, Map.of("getUniqueId", breakerId));
        Map<String, Object> bystanderData = new HashMap<>();
        bystanderData.put("getUniqueId", bystanderId);
        Player bystander = proxy(Player.class, bystanderData);
        World world = proxy(World.class, Map.of("getUID", UUID.randomUUID(), "getPlayers", List.of(bystander)));
        Location dropLocation = new Location(world, 3, 64, 3);
        bystanderData.put("getLocation", dropLocation);
        Location otherLocation = new Location(world, material == Material.RED_BED ? 2 : 3,
                material == Material.RED_BED ? 64 : 65, 3);

        BlockData dropData;
        BlockData otherData;
        if (material == Material.RED_BED) {
            dropData = proxy(Bed.class, Map.of("getMaterial", material, "getPart", Bed.Part.HEAD, "getFacing", BlockFace.EAST));
            otherData = proxy(Bed.class, Map.of("getMaterial", material, "getPart", Bed.Part.FOOT, "getFacing", BlockFace.EAST));
        } else {
            Class<? extends Bisected> type = material == Material.OAK_DOOR ? Door.class : Bisected.class;
            dropData = proxy(type, Map.of("getMaterial", material, "getHalf", Bisected.Half.BOTTOM));
            otherData = proxy(type, Map.of("getMaterial", material, "getHalf", Bisected.Half.TOP));
        }
        BlockState dropState = proxy(BlockState.class, Map.of("getBlockData", dropData));
        BlockState otherState = proxy(BlockState.class, Map.of("getBlockData", otherData));
        Block dropPart = proxy(Block.class, Map.of("getLocation", dropLocation, "getBlockData", dropData, "getState", dropState));
        Block otherPart = proxy(Block.class, (instance, method, args) -> switch (method.getName()) {
            case "getRelative" -> {
                assertEquals(material == Material.RED_BED ? BlockFace.EAST : BlockFace.DOWN, args[0]);
                yield dropPart;
            }
            case "getState" -> otherState;
            case "getBlockData" -> otherData;
            case "getLocation" -> otherLocation;
            default -> throw new AssertionError(method.getName());
        });

        RandomizerGameListener randomizer = new RandomizerGameListener(List.of(), Stack::new);
        RandomizerMappings.SaveData saved = new RandomizerMappings.SaveData();
        saved.scopedMappings.put("player:" + breakerId, Map.of(material.name(), "DIAMOND"));
        saved.scopedMappings.put("player:" + bystanderId, Map.of(material.name(), "EMERALD"));
        Field mappingsField = RandomizerGameListener.class.getDeclaredField("mappings");
        mappingsField.setAccessible(true);
        ((RandomizerMappings) mappingsField.get(randomizer)).load(saved);

        withRandomizerEnabled(false, () -> {
            Item directly = dropItem(material, 1, dropLocation);
            randomizer.onBlockDrop(new BlockDropItemEvent(dropPart, dropState, breaker, List.of(directly)));

            randomizer.onBlockBreak(new BlockBreakEvent(otherPart, breaker));
            randomizer.onBlockDestroy(new BlockDestroyEvent(dropPart, dropData, dropData, 0, true));
            Item indirectly = dropItem(material, 1, dropLocation);
            randomizer.onItemSpawn(new ItemSpawnEvent(indirectly));

            assertEquals(Material.DIAMOND, directly.getItemStack().getType());
            assertEquals(directly.getItemStack().getType(), indirectly.getItemStack().getType());
            assertEquals(1, indirectly.getItemStack().getAmount());
        });
    }

    private static Item dropItem(Material material, int amount, Location location) {
        UUID id = UUID.randomUUID();
        return proxy(Item.class, new InvocationHandler() {
            private ItemStack stack = new Stack(material, amount);

            @Override public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getLocation" -> location;
                    case "getItemStack" -> stack;
                    case "setItemStack" -> { stack = (ItemStack) args[0]; yield null; }
                    default -> throw new AssertionError(method.getName());
                };
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void withRandomizerEnabled(boolean shared, Runnable test) throws ReflectiveOperationException {
        Set<Challenge> active = ChallengeManager.getActiveChallenges();
        Set<Challenge> previous = new HashSet<>(active);
        Field registryField = ChallengeRegistry.class.getDeclaredField("challenges");
        registryField.setAccessible(true);
        Map<ChallengeType, Challenge> registry = (Map<ChallengeType, Challenge>) registryField.get(null);
        ChallengeSettingsManager settings = new ChallengeSettingsManager(null, RandomizerSettings.RandomizerSetting.class);
        settings.setState(RandomizerSettings.RandomizerSetting.SHARED_MAPPINGS, shared ? OnOffState.ON : OnOffState.OFF);
        Challenge challenge = proxy(Challenge.class, Map.of("getType", ChallengeType.RANDOMIZER, "getSettingsManager", settings,
                "allowsGameplay", true, "hashCode", 1));
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        Object oldServer = serverField.get(null);
        Challenge oldChallenge = registry.put(ChallengeType.RANDOMIZER, challenge);
        try {
            active.clear();
            active.add(challenge);
            serverField.set(null, proxy(Server.class, Map.of("getCurrentTick", 1)));
            test.run();
        } finally {
            serverField.set(null, oldServer);
            if (oldChallenge == null) registry.remove(ChallengeType.RANDOMIZER);
            else registry.put(ChallengeType.RANDOMIZER, oldChallenge);
            active.clear();
            active.addAll(previous);
        }
    }

    private static <T> T proxy(Class<T> type, Map<String, Object> responses) {
        return proxy(type, (instance, method, args) -> {
            if (responses.containsKey(method.getName())) return responses.get(method.getName());
            throw new AssertionError(method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
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
