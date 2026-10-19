package gg.itzkatze.mcc.lobby.cosmetics;

import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.GUIItem;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CosmeticShopGui extends MccGUI {

    private static final int BLOCK_TAB_SLOT = 20;
    private static final int BALANCE_SLOT = 4;
    private static final int SOUND_TAB_SLOT = 22;
    private static final int MUSIC_TAB_SLOT = 24;
    private static final int PREVIOUS_SLOT = 45;
    private static final int BACK_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int[] ENTRY_SLOTS = entrySlots();
    private boolean overview = true;


    // A null cosmetic is the free Default entry of the current tab.
    private record Entry(@Nullable Cosmetic cosmetic) {
    }

    private CosmeticCategory category = CosmeticCategory.BLOCK;
    private int page;

    public CosmeticShopGui() {
        super("Cosmetic Shop", 54);
    }

    private static int[] entrySlots() {
        int[] slots = new int[28];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 7; column++) {
                slots[row * 7 + column] = (row + 1) * 9 + column + 1;
            }
        }
        return slots;
    }

    @Override
    public void setItems(MccPlayer player) {
        clear();
        fill(ItemStackCreator.fillerItem());

        set(new GUIItem(BALANCE_SLOT) {
            @Override
            public ItemStack getItem(MccPlayer viewer) {
                return ItemStackCreator.getStack(Component.text("Points: " + CosmeticStore.get().points(viewer.getUuid()), NamedTextColor.GOLD), Material.GOLD_NUGGET,
                        List.of(Component.text("Every block you land on in the Jump & Run earns one point.", NamedTextColor.GRAY)));
            }
        });

        if (overview) {
            set(overviewCategory(CosmeticCategory.BLOCK, BLOCK_TAB_SLOT));
            set(overviewCategory(CosmeticCategory.SOUND, SOUND_TAB_SLOT));
            set(overviewCategory(CosmeticCategory.MUSIC, MUSIC_TAB_SLOT));
            set(GUIClickableItem.getCloseItem(CLOSE_SLOT));
            return;
        }

        set(new GUIClickableItem(BACK_SLOT) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer clicker) {
                overview = true;
                page = 0;
                play(clicker, ShopSounds.CLICK);
                redraw(clicker);
            }

            @Override
            public ItemStack getItem(MccPlayer viewer) {
                return ItemStackCreator.getStack(
                        Component.text("Back to Categories", NamedTextColor.YELLOW),
                        Material.ARROW,
                        List.of()
                );
            }
        });

        List<Entry> entries = entries();
        int pages = Math.max(1, Math.ceilDiv(entries.size(), ENTRY_SLOTS.length));
        page = Math.clamp(page, 0, pages - 1);
        for (int i = 0; i < ENTRY_SLOTS.length; i++) {
            int index = page * ENTRY_SLOTS.length + i;
            if (index >= entries.size()) break;
            set(entry(ENTRY_SLOTS[i], entries.get(index)));
        }
        if (page > 0) {
            set(arrow(PREVIOUS_SLOT, "Previous page", -1, pages));
        }
        if (page < pages - 1) {
            set(arrow(NEXT_SLOT, "Next page", 1, pages));
        }
        set(GUIClickableItem.getCloseItem(CLOSE_SLOT));
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }

    private void redraw(@NotNull MccPlayer player) {
        clear();
        setItems(player);
        updateInventory(player);
    }

    private @NotNull List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(null));
        CosmeticStore.get().catalog().in(category).stream()
                .sorted(Comparator.comparingInt(Cosmetic::price))
                .forEach(cosmetic -> entries.add(new Entry(cosmetic)));
        return entries;
    }

    private @NotNull GUIClickableItem tab(@NotNull CosmeticCategory tab, int slot) {
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer clicker) {
                category = tab;
                page = 0;
                play(clicker, ShopSounds.CLICK);
                redraw(clicker);
            }

            @Override
            public ItemStack getItem(MccPlayer viewer) {
                boolean selected = tab == category;
                ItemStack stack = ItemStackCreator.getStack(Component.text(tab.title(), selected ? NamedTextColor.YELLOW : NamedTextColor.GRAY, TextDecoration.BOLD), tab.icon(), List.of());
                return selected ? glowing(stack) : stack;
            }
        };
    }

    private @NotNull GUIClickableItem arrow(int slot, @NotNull String name, int step, int pages) {
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer clicker) {
                page += step;
                play(clicker, ShopSounds.CLICK);
                redraw(clicker);
            }

            @Override
            public ItemStack getItem(MccPlayer viewer) {
                return ItemStackCreator.getStack(Component.text(name, NamedTextColor.YELLOW), Material.ARROW, List.of(Component.text("Page " + (page + 1) + " of " + pages, NamedTextColor.GRAY)));
            }
        };
    }

    private @NotNull GUIClickableItem entry(int slot, @NotNull Entry entry) {
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer clicker) {
                ClickType type = event.getClick();
                if (type == ClickType.LEFT || type == ClickType.SHIFT_LEFT) {
                    choose(entry, clicker);
                } else if (type == ClickType.RIGHT || type == ClickType.SHIFT_RIGHT) {
                    preview(entry, clicker);
                }
            }

            @Override
            public ItemStack getItem(MccPlayer viewer) {
                return render(entry, viewer);
            }
        };
    }

    private @NotNull GUIClickableItem overviewCategory(
            @NotNull CosmeticCategory selectedCategory,
            int slot
    ) {
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer clicker) {
                category = selectedCategory;
                page = 0;
                overview = false;
                play(clicker, ShopSounds.CLICK);
                redraw(clicker);
            }

            @Override
            public ItemStack getItem(MccPlayer viewer) {
                return ItemStackCreator.getStack(
                        Component.text(
                                selectedCategory.title(),
                                NamedTextColor.YELLOW,
                                TextDecoration.BOLD
                        ),
                        selectedCategory.icon(),
                        List.of(Component.text("Click to Open", NamedTextColor.GRAY))
                );
            }
        };
    }

    private @NotNull ItemStack render(@NotNull Entry entry, @NotNull MccPlayer viewer) {
        CosmeticStore store = CosmeticStore.get();
        UUID id = viewer.getUuid();
        Cosmetic cosmetic = entry.cosmetic();
        boolean owned = cosmetic == null || store.owns(id, cosmetic);
        boolean equipped = cosmetic == null ? store.equipped(id, category).isEmpty() : owned && isEquipped(store, id, cosmetic);

        List<Component> lore = new ArrayList<>();
        if (!owned) {
            int balance = store.points(id);
            lore.add(Component.text("Price: " + PointsText.amount(cosmetic.price()), NamedTextColor.GOLD));
            if (cosmetic.isAvailable(store.today())) {
                lore.add(balance >= cosmetic.price() ? Component.text("Click to buy", NamedTextColor.YELLOW) : Component.text(PointsText.missing(cosmetic.price() - balance), NamedTextColor.RED));
            }
        } else if (equipped) {
            lore.add(Component.text("Equipped", NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("Click to equip", NamedTextColor.YELLOW));
        }
        Season season = cosmetic == null ? null : cosmetic.season();
        if (season != null) {
            lore.add(season.availability(store.today()));
        }
        if (previewOf(entry) != null) {
            lore.add(Component.text("Right-click to preview", NamedTextColor.GRAY));
        }

        Component name = cosmetic != null
                ? cosmetic.styledName()
                : Component.text(
                        switch (category) {
                            case BLOCK -> "Default: Colored Concrete";
                            case SOUND -> "Default: XP Orb";
                            case MUSIC -> "Default: No Music";
                        },
                        NamedTextColor.WHITE
                );

        Material icon = cosmetic != null
                ? cosmetic.icon()
                : switch (category) {
                    case BLOCK -> Material.LIME_CONCRETE;
                    case SOUND -> Material.EXPERIENCE_BOTTLE;
                    case MUSIC -> Material.BARRIER;
                };

        ItemStack stack = ItemStackCreator.getStack(name, icon, lore);
        return equipped ? glowing(stack) : stack;
    }

    private void choose(@NotNull Entry entry, @NotNull MccPlayer clicker) {
        Player player = clicker.getPlayer();
        if (player == null) return;

        CosmeticStore store = CosmeticStore.get();
        UUID id = player.getUniqueId();
        Cosmetic cosmetic = entry.cosmetic();
        if (cosmetic == null) {
            if (store.equipped(id, category).isPresent()) {
                store.unequip(player, category);
                play(clicker, ShopSounds.CLICK);
                redraw(clicker);
            }
        } else if (store.owns(id, cosmetic)) {
            if (!isEquipped(store, id, cosmetic)) {
                store.equip(player, cosmetic);
                play(clicker, ShopSounds.CLICK);
                redraw(clicker);
            }
        } else if (cosmetic.season() != null && !cosmetic.isAvailable(store.today())) {
            player.sendMessage(cosmetic.season().returnsMessage(cosmetic));
            play(clicker, ShopSounds.DENY);
        } else if (store.points(id) >= cosmetic.price()) {
            play(clicker, ShopSounds.CLICK);
            new CosmeticConfirmGui(cosmetic, this).open(clicker);
        } else {
            player.sendMessage(Component.text(PointsText.missing(cosmetic.price() - store.points(id)), NamedTextColor.RED));
            play(clicker, ShopSounds.DENY);
        }
    }

    private void preview(@NotNull Entry entry, @NotNull MccPlayer clicker) {
        Sound sound = previewOf(entry);
        if (sound != null) {
            play(clicker, sound);
        }
    }

    private @Nullable Sound previewOf(@NotNull Entry entry) {
        Cosmetic cosmetic = entry.cosmetic();
        if (cosmetic == null) {
            return category == CosmeticCategory.SOUND ? SoundCosmetic.DEFAULT : null;
        }
        return cosmetic instanceof SoundCosmetic sound ? sound.sound() : null;
    }

    private static boolean isEquipped(@NotNull CosmeticStore store, @NotNull UUID id, @NotNull Cosmetic cosmetic) {
        return store.equipped(id, cosmetic.category()).map(equipped -> equipped.id().equals(cosmetic.id())).orElse(false);
    }

    private static void play(@NotNull MccPlayer player, @NotNull Sound sound) {
        Player bukkit = player.getPlayer();
        if (bukkit != null) {
            bukkit.playSound(sound);
        }
    }

    private static @NotNull ItemStack glowing(@NotNull ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        stack.setItemMeta(meta);
        return stack;
    }
}
