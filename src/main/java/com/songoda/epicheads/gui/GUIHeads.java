package com.songoda.epicheads.gui;

import com.songoda.epicheads.EpicHeads;
import com.songoda.epicheads.database.DataHelper;
import com.songoda.epicheads.head.Category;
import com.songoda.epicheads.head.Head;
import com.songoda.epicheads.menu.Menu;
import com.songoda.epicheads.players.EPlayer;
import com.songoda.epicheads.settings.Settings;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GUIHeads {
    private final EpicHeads plugin;
    private final Player player;
    private final Menu menu;

    private List<Head> heads;

    private String query;
    private final QueryTypes type;

    public GUIHeads(EpicHeads plugin, Player player, String query, QueryTypes type, List<Head> heads) {
        this.plugin = plugin;
        this.player = player;
        this.query = query;
        this.type = type;

        List<String> favorites = plugin.getPlayerManager().getPlayer(player).getFavorites();
        this.heads = heads.stream()
                .sorted(Comparator.comparingInt(head -> (favorites.contains(head.getUrl()) ? 0 : 1)))
                .collect(Collectors.toList());

        this.menu = new Menu(player, 6, "Heads");
        this.menu.setPageRenderer(this::showPage);
        showPage();
        this.menu.open();
    }

    private void updateTitle() {
        int numHeads = this.heads.size();
        if (numHeads == 0) {
            this.plugin.getLocale().getMessage("general.search.nonefound").sendPrefixedMessage(this.player);
            return;
        }
        Category category = this.heads.get(0).getCategory();

        String name = switch (this.type) {
            case SEARCH -> this.plugin.getLocale().getMessage("general.word.query").toText() + ": " + this.query;
            case CATEGORY -> category.getName();
            case FAVORITES -> this.plugin.getLocale().getMessage("general.word.favorites").toText();
        };

        this.menu.setPages((int) Math.ceil(numHeads / 45.0));
        this.menu.setTitle(name + " (" + numHeads + ")");
    }

    private void showPage() {
        updateTitle();

        List<Head> sortedHeads = this.heads.stream()
                .sorted(Comparator.<Head>comparingDouble(Head::getAverageRating).reversed())
                .collect(Collectors.toList());

        List<Head> pageHeads = sortedHeads.stream().skip((this.menu.getPage() - 1) * 45L).limit(45)
                .collect(Collectors.toList());

        int page = this.menu.getPage();
        int pages = this.menu.getPages();

        setPageButton(0, page - 3, pages);
        setPageButton(1, page - 2, pages);
        setPageButton(2, page - 1, pages);

        this.menu.setButton(3, Menu.named(Material.COMPASS,
                        this.plugin.getLocale().getMessage("gui.heads.search").toText()),
                event -> doSearch(this.plugin, event.getWhoClicked() instanceof Player p ? p : this.player));

        this.menu.setButton(4, Menu.named(Material.MAP, this.plugin.getLocale().getMessage("gui.heads.categories").toText()),
                event -> new GUIOverview(this.player));

        if (pageHeads.size() > 1) {
            this.menu.setButton(5, Menu.named(Material.COMPASS,
                            this.plugin.getLocale().getMessage("gui.heads.refine").toText()),
                    event -> {
                        this.player.closeInventory();
                        this.plugin.getChatInput().prompt(this.player,
                                this.plugin.getLocale().getPrefix() + " " + this.plugin.getLocale().getMessage("general.search.refine").toText(),
                                message -> {
                                    this.menu.setPage(1);
                                    this.heads = this.heads.stream().filter(head -> head.getName().toLowerCase()
                                            .contains(message.toLowerCase())).collect(Collectors.toList());
                                    if (this.query == null) {
                                        this.query = message;
                                    } else {
                                        this.query += ", " + message;
                                    }
                                    showPage();
                                    this.menu.open();
                                },
                                () -> this.plugin.getLocale().getMessage("general.search.canceled").sendPrefixedMessage(this.player));
                    });
        } else {
            this.menu.clearSlot(5);
        }

        setPageButton(6, page + 1, pages);
        setPageButton(7, page + 2, pages);
        setPageButton(8, page + 3, pages);

        List<String> favorites = this.plugin.getPlayerManager().getPlayer(this.player).getFavorites();

        double cost = Settings.headCost();
        boolean free = this.player.hasPermission("epicheads.bypasscost")
                || (Settings.freeInCreative() && this.player.getGameMode() == GameMode.CREATIVE);
        int i = 0;
        for (; i < pageHeads.size(); i++) {
            Head head = pageHeads.get(i);

            if (head.getName() == null) {
                continue;
            }

            ItemStack item = head.asItemStack(favorites.contains(head.getUrl()), free);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = item.getItemMeta().getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add("");
            lore.add(this.plugin.getLocale().getMessage("gui.heads.leftclick").toText());
            if (this.player.hasPermission("epicheads.delete")) {
                lore.add(this.plugin.getLocale().getMessage("gui.heads.delete").toText());
            }
            meta.setLore(lore);
            item.setItemMeta(meta);

            this.menu.setButton(i + 9, item, event -> {
                if (event.getClick() == ClickType.MIDDLE && this.player.hasPermission("epicheads.delete")) {
                    this.plugin.getHeadManager().disableHead(head);
                    DataHelper.disableHead(head);
                    this.heads.remove(head);
                    showPage();
                    return;
                } else if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    EPlayer ePlayer = this.plugin.getPlayerManager().getPlayer(this.player);
                    boolean isFav = ePlayer.getFavorites().contains(head.getUrl());
                    if (isFav) {
                        ePlayer.removeFavorite(head.getUrl());
                    } else {
                        ePlayer.addFavorite(head.getUrl());
                    }
                    showPage();
                    return;
                }
                if (!free) {
                    if (this.plugin.getEconomies().isEnabled()) {
                        if (this.plugin.getEconomies().hasBalance(this.player, cost)) {
                            this.plugin.getEconomies().withdraw(this.player, cost);
                        } else {
                            this.player.sendMessage(this.plugin.getLocale().getMessage("event.buyhead.cannotafford").toText());
                            return;
                        }
                    } else {
                        this.player.sendMessage("Economy plugin not setup correctly...");
                        return;
                    }
                }

                ItemStack headItem = item.clone();
                meta.setLore(new ArrayList<>());
                headItem.setItemMeta(meta);

                this.player.getInventory().addItem(headItem);
            });
        }
        for (; i < 45; i++) {
            this.menu.clearSlot(i + 9);
        }
    }

    private void setPageButton(int slot, int targetPage, int pages) {
        if (targetPage >= 1 && targetPage <= pages) {
            this.menu.setButton(slot, Menu.named(Material.ARROW, String.valueOf(targetPage),
                            ChatColor.RED + this.plugin.getLocale().getMessage("general.word.page").toText() + " " + targetPage),
                    event -> {
                        this.menu.setPage(targetPage);
                        showPage();
                    });
        } else {
            this.menu.clearSlot(slot);
        }
    }

    public static void doSearch(EpicHeads plugin, Player player) {
        plugin.getChatInput().prompt(player,
                plugin.getLocale().getPrefix() + " " + plugin.getLocale().getMessage("general.search.global").toText(),
                query -> {
                    List<Head> searchHeads = plugin.getHeadManager().getHeads().stream()
                            .filter(head -> head.getName().toLowerCase().contains(query.toLowerCase()))
                            .filter(head -> player.hasPermission("epicheads.category." + head.getCategory().getName().replace(" ", "_")))
                            .collect(Collectors.toList());
                    new GUIHeads(plugin, player, query, QueryTypes.SEARCH, searchHeads);
                },
                () -> plugin.getLocale().getMessage("general.search.canceled").sendPrefixedMessage(player));
    }

    public enum QueryTypes {
        SEARCH, CATEGORY, FAVORITES
    }
}
