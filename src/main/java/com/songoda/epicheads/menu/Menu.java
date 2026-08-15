package com.songoda.epicheads.menu;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Menu implements InventoryHolder {
    private final Player player;
    private final int rows;
    private String title;
    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private int page = 1;
    private int pages = 1;
    private Runnable pageRenderer;

    public Menu(Player player, int rows, String title) {
        this.player = player;
        this.rows = rows;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public Player getPlayer() {
        return this.player;
    }

    public int getPage() {
        return this.page;
    }

    public void setPage(int page) {
        this.page = Math.max(1, page);
    }

    public int getPages() {
        return this.pages;
    }

    public void setPages(int pages) {
        this.pages = Math.max(1, pages);
    }

    public void changePage(int delta) {
        this.page = Math.min(this.pages, Math.max(1, this.page + delta));
        if (this.pageRenderer != null) {
            this.pageRenderer.run();
        }
    }

    public void setPageRenderer(Runnable pageRenderer) {
        this.pageRenderer = pageRenderer;
    }

    public void setTitle(String title) {
        this.title = title;
        ItemStack[] contents = this.inventory.getContents();
        Map<Integer, Consumer<InventoryClickEvent>> saved = new HashMap<>(this.actions);
        this.inventory = Bukkit.createInventory(this, this.rows * 9, title);
        this.inventory.setContents(contents);
        this.actions.clear();
        this.actions.putAll(saved);
        if (this.player.getOpenInventory().getTopInventory().getHolder() == this) {
            this.player.openInventory(this.inventory);
        }
    }

    public String getTitle() {
        return this.title;
    }

    public void setItem(int slot, ItemStack item) {
        this.actions.remove(slot);
        this.inventory.setItem(slot, item);
    }

    public void setButton(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        this.inventory.setItem(slot, item);
        if (action != null) {
            this.actions.put(slot, action);
        } else {
            this.actions.remove(slot);
        }
    }

    public void clearSlot(int slot) {
        this.actions.remove(slot);
        this.inventory.setItem(slot, null);
    }

    public void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = this.actions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    public void fillBorder(ItemStack glass2, ItemStack glass3) {
        hideTooltip(glass2);
        hideTooltip(glass3);
        int size = this.rows * 9;
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean edge = row == 0 || row == this.rows - 1 || col == 0 || col == 8;
            if (!edge) {
                continue;
            }
            boolean cornerish = (row == 0 && (col <= 2 || col >= 6)) || col == 0 || col == 8;
            setItem(slot, cornerish ? glass2.clone() : glass3.clone());
        }
    }

    public void open() {
        this.player.openInventory(this.inventory);
    }

    public static ItemStack named(Material material, String name, String... lore) {
        return named(material, name, Arrays.asList(lore));
    }

    public static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null) {
            meta.setDisplayName(name);
        }
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack named(ItemStack base, String name, List<String> lore) {
        ItemStack item = base.clone();
        ItemMeta meta = item.getItemMeta();
        if (name != null) {
            meta.setDisplayName(name);
        }
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack border(Material material) {
        ItemStack item = new ItemStack(material);
        hideTooltip(item);
        return item;
    }

    public static void hideTooltip(ItemStack item) {
        TooltipDisplay tooltipDisplay = TooltipDisplay.tooltipDisplay().hideTooltip(true).build();
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, tooltipDisplay);
    }
}
