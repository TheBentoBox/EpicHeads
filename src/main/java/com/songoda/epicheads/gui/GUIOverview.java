package com.songoda.epicheads.gui;

import com.songoda.epicheads.EpicHeads;
import com.songoda.epicheads.head.Category;
import com.songoda.epicheads.head.Head;
import com.songoda.epicheads.menu.Menu;
import com.songoda.epicheads.settings.Settings;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GUIOverview {
    private final EpicHeads plugin;
    private final Player player;
    private final Menu menu;

    public GUIOverview(Player player) {
        this.plugin = EpicHeads.getInstance();
        this.player = player;
        this.menu = new Menu(player, 5, this.plugin.getLocale().getMessage("gui.overview.title")
                .processPlaceholder("count", this.plugin.getHeadManager().getHeads().size())
                .toText());
        this.menu.setPageRenderer(this::showPage);
        showPage();
        this.menu.open();
    }

    private void showPage() {
        this.menu.setButton(4, Menu.named(Material.GOLDEN_APPLE,
                        this.plugin.getLocale().getMessage("gui.overview.viewfavorites").toText(),
                        this.plugin.getLocale().getMessage("gui.overview.favoriteslore").getMessageLines('|')),
                event -> new GUIHeads(this.plugin, this.player, null, GUIHeads.QueryTypes.FAVORITES,
                        this.plugin.getPlayerManager().getPlayer(this.player).getFavoritesAsHeads()));

        ItemStack glass2 = Menu.border(Settings.glassType(2));
        ItemStack glass3 = Menu.border(Settings.glassType(3));
        int lastRow = 4 * 9;
        this.menu.setItem(0, glass2.clone());
        this.menu.setItem(8, glass2.clone());
        this.menu.setItem(lastRow, glass2.clone());
        this.menu.setItem(lastRow + 8, glass2.clone());
        this.menu.setItem(9, glass2.clone());
        this.menu.setItem(17, glass2.clone());
        this.menu.setItem(27, glass2.clone());
        this.menu.setItem(35, glass2.clone());
        this.menu.setItem(1, glass2.clone());
        this.menu.setItem(7, glass2.clone());
        this.menu.setItem(lastRow + 1, glass2.clone());
        this.menu.setItem(lastRow + 7, glass2.clone());
        this.menu.setItem(2, glass3.clone());
        this.menu.setItem(6, glass3.clone());
        this.menu.setItem(lastRow + 2, glass3.clone());
        this.menu.setItem(lastRow + 6, glass3.clone());

        List<Category> categories = this.plugin.getHeadManager()
                .getCategories()
                .stream()
                .filter(category -> this.player.hasPermission("epicheads.category." + category.getName().replace(" ", "_")))
                .filter(category -> !this.plugin.getHeadManager().getHeadsByCategory(category).isEmpty())
                .collect(Collectors.toList());

        this.menu.setPages((int) Math.ceil(categories.size() / 21.0));
        int skip = (this.menu.getPage() - 1) * 21;
        List<Category> pageCategories = categories.stream().skip(skip).limit(21).collect(Collectors.toList());

        int nextSlot = 10;
        for (int slot = 10; slot <= 34; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) {
                continue;
            }
            this.menu.clearSlot(slot);
        }

        for (Category category : pageCategories) {
            List<Head> heads = this.plugin.getHeadManager().getHeadsByCategory(category);
            Head firstHead = heads.get(0);

            ItemStack buttonItem = firstHead.asItemStack();
            this.menu.setButton(nextSlot, Menu.named(buttonItem,
                            this.plugin.getLocale().getMessage("gui.overview.headname")
                                    .processPlaceholder("name", Color.getRandomColor() + category.getName())
                                    .toText(),
                            Collections.singletonList(this.plugin.getLocale().getMessage("gui.overview.headlore")
                                    .processPlaceholder("count", String.format("%,d", category.getCount()))
                                    .toText())),
                    event -> new GUIHeads(this.plugin, this.player, null, GUIHeads.QueryTypes.CATEGORY, heads));

            nextSlot++;
            if (nextSlot % 9 == 8) {
                nextSlot += 2;
            }
        }

        if (this.menu.getPage() > 1) {
            this.menu.setButton(37, Menu.named(Material.ARROW,
                    this.plugin.getLocale().getMessage("gui.general.previous").toText()),
                    event -> this.menu.changePage(-1));
        } else {
            this.menu.clearSlot(37);
        }
        if (this.menu.getPage() < this.menu.getPages()) {
            this.menu.setButton(43, Menu.named(Material.ARROW,
                    this.plugin.getLocale().getMessage("gui.general.next").toText()),
                    event -> this.menu.changePage(1));
        } else {
            this.menu.clearSlot(43);
        }

        this.menu.setButton(40, Menu.named(Material.COMPASS,
                        this.plugin.getLocale().getMessage("gui.overview.search").toText()),
                event -> GUIHeads.doSearch(this.plugin, this.player));
    }

    public enum Color {
        C9("&9&l"),
        CA("&a&l"),
        CB("&b&l"),
        C8("&8&l"),
        CD("&d&l"),
        CC("&c&l"),
        C6("&6&l");

        final String color;

        Color(String color) {
            this.color = color;
        }

        public String getColor() {
            return this.color;
        }

        public static String getRandomColor() {
            Random random = new Random();
            return values()[random.nextInt(values().length)].getColor();
        }
    }
}
