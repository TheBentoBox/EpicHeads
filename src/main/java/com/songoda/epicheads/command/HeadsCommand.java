package com.songoda.epicheads.command;

import com.songoda.epicheads.EpicHeads;
import com.songoda.epicheads.database.DataHelper;
import com.songoda.epicheads.gui.GUIHeads;
import com.songoda.epicheads.gui.GUIOverview;
import com.songoda.epicheads.head.Category;
import com.songoda.epicheads.head.Head;
import com.songoda.epicheads.head.HeadManager;
import com.songoda.epicheads.utils.Methods;
import com.songoda.epicheads.utils.SkullUtils;
import com.songoda.epicheads.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class HeadsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "add", "base64", "give", "givetoken", "help", "reload", "search", "url");

    private final EpicHeads plugin;

    public HeadsCommand(EpicHeads plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sendHelp(sender);
                return true;
            }
            if (!player.hasPermission("epicheads.menu")) {
                this.plugin.getLocale().getMessage("event.general.nopermission").sendPrefixedMessage(player);
                return true;
            }
            new GUIOverview(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return switch (sub) {
            case "add" -> add(sender, rest);
            case "base64" -> base64(sender);
            case "give" -> give(sender, rest);
            case "givetoken" -> giveToken(sender, rest);
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            case "reload" -> reload(sender);
            case "search" -> search(sender);
            case "url" -> url(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean deny(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return false;
        }
        this.plugin.getLocale().getMessage("event.general.nopermission").sendPrefixedMessage(sender);
        return true;
    }

    private boolean add(CommandSender sender, String[] args) {
        if (deny(sender, "epicheads.add")) {
            return true;
        }
        if (args.length != 3) {
            Text.send(sender, "&cUsage: /heads add <url> <name> <category>");
            return true;
        }

        String url = args[0];
        String name = args[1].replace("_", " ");
        String categoryStr = args[2].replace("_", " ");

        HeadManager headManager = this.plugin.getHeadManager();
        if (headManager.getLocalHeads().stream().anyMatch(head -> head.getUrl().equals(url))) {
            this.plugin.getLocale().getMessage("command.add.exists").sendPrefixedMessage(sender);
            return true;
        }

        Category category = headManager.getOrCreateCategoryByName(categoryStr);
        Head head = new Head(headManager.getNextLocalId(), name, url, category, true);
        headManager.addLocalHead(head);
        DataHelper.createLocalHead(head);

        this.plugin.getLocale()
                .getMessage("command.add.success")
                .processPlaceholder("name", name)
                .sendPrefixedMessage(sender);
        return true;
    }

    private boolean base64(CommandSender sender) {
        if (!(sender instanceof Player player) || deny(sender, "epicheads.base64")) {
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta() || !(item.getItemMeta() instanceof SkullMeta)) {
            return true;
        }
        String encoded = SkullUtils.getProfileValue(item);
        if (encoded == null) {
            return true;
        }
        this.plugin.getLocale().newMessage(encoded).sendPrefixedMessage(player);
        return true;
    }

    private boolean url(CommandSender sender) {
        if (!(sender instanceof Player player) || deny(sender, "epicheads.url")) {
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta() || !(item.getItemMeta() instanceof SkullMeta)) {
            return true;
        }
        String encoded = SkullUtils.getProfileValue(item);
        if (encoded == null) {
            return true;
        }
        String texture = SkullUtils.getDecodedTexture(encoded);
        this.plugin.getLocale().newMessage("http://textures.minecraft.net/texture/" + texture).sendPrefixedMessage(player);
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (deny(sender, "epicheads.give")) {
            return true;
        }
        if (args.length != 3) {
            Text.send(sender, "&cUsage: /heads give <player/all> <global/local> <head_id>");
            return true;
        }

        String playerStr = args[0].toLowerCase(Locale.ROOT);
        Player player = Bukkit.getPlayer(playerStr);
        String archive = args[1];
        int headId;
        try {
            headId = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            return true;
        }

        if (player == null && !playerStr.equals("all")) {
            this.plugin.getLocale().getMessage("command.give.notonline")
                    .processPlaceholder("name", args[0]).sendPrefixedMessage(sender);
            return true;
        }

        List<Head> heads;
        if (archive.equalsIgnoreCase("global")) {
            heads = this.plugin.getHeadManager().getGlobalHeads();
        } else if (archive.equalsIgnoreCase("local")) {
            heads = this.plugin.getHeadManager().getLocalHeads();
        } else {
            Text.send(sender, "&cUsage: /heads give <player/all> <global/local> <head_id>");
            return true;
        }

        Optional<Head> head = heads.stream().filter(h -> h.getId() == headId).findFirst();
        if (head.isEmpty()) {
            this.plugin.getLocale().getMessage("command.give.notfound")
                    .processPlaceholder("name", String.valueOf(headId)).sendPrefixedMessage(sender);
            return true;
        }

        ItemStack item = head.get().asItemStack();
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Collections.emptyList());
        item.setItemMeta(meta);

        if (playerStr.equals("all")) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl == sender) {
                    continue;
                }
                pl.getInventory().addItem(item);
                this.plugin.getLocale().getMessage("command.give.receive")
                        .processPlaceholder("name", head.get().getName()).sendPrefixedMessage(pl);
            }
            this.plugin.getLocale().getMessage("command.give.success")
                    .processPlaceholder("player", this.plugin.getLocale().getMessage("general.word.everyone").toText())
                    .processPlaceholder("name", head.get().getName())
                    .sendPrefixedMessage(sender);
        } else {
            player.getInventory().addItem(item);
            this.plugin.getLocale().getMessage("command.give.receive")
                    .processPlaceholder("name", head.get().getName()).sendPrefixedMessage(player);
            this.plugin.getLocale().getMessage("command.give.success")
                    .processPlaceholder("player", player.getName())
                    .processPlaceholder("name", head.get().getName())
                    .sendPrefixedMessage(sender);
        }
        return true;
    }

    private boolean giveToken(CommandSender sender, String[] args) {
        if (deny(sender, "epicheads.givetoken")) {
            return true;
        }
        if (args.length != 2) {
            Text.send(sender, "&cUsage: /heads givetoken <player> <amount>");
            return true;
        }
        Player player = Bukkit.getPlayer(args[0]);
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            return true;
        }
        if (player == null) {
            this.plugin.getLocale().getMessage("command.give.notonline")
                    .processPlaceholder("name", args[0]).sendPrefixedMessage(sender);
            return true;
        }
        player.getInventory().addItem(Methods.createToken(amount));
        this.plugin.getLocale().getMessage("command.givetoken.receive")
                .processPlaceholder("amount", amount).sendPrefixedMessage(player);
        if (player != sender) {
            this.plugin.getLocale().getMessage("command.givetoken.success")
                    .processPlaceholder("player", player.getName())
                    .processPlaceholder("amount", amount).sendPrefixedMessage(sender);
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (deny(sender, "epicheads.admin")) {
            return true;
        }
        this.plugin.reloadPlugin();
        this.plugin.getLocale().newMessage("&7Configuration and Language files reloaded.").sendPrefixedMessage(sender);
        return true;
    }

    private boolean search(CommandSender sender) {
        if (!(sender instanceof Player player) || deny(sender, "epicheads.search")) {
            return true;
        }
        GUIHeads.doSearch(this.plugin, player);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        Text.send(sender, "&c&l" + this.plugin.getPluginMeta().getName() + " &8» &7Version "
                + this.plugin.getPluginMeta().getVersion());
        sender.sendMessage("");
        Text.send(sender, "&7Welcome to EpicHeads! To get started try using the command /heads to access the heads panel.");
        sender.sendMessage("");
        Text.send(sender, "&6Commands:");
        sendHelpLine(sender, "epicheads.menu", "/heads", "Displays heads overview.");
        sendHelpLine(sender, "epicheads.add", "/heads add <url> <name> <category>", "Adds a head to your local database.");
        sendHelpLine(sender, "epicheads.base64", "/heads base64", "Gives you the base64 code of the head you are holding.");
        sendHelpLine(sender, "epicheads.give", "/heads give <player/all> <global/local> <head_id>", "Gives a head to a player.");
        sendHelpLine(sender, "epicheads.givetoken", "/heads givetoken <player> <amount>", "Gives player head tokens.");
        sendHelpLine(sender, null, "/heads help", "Displays this page.");
        sendHelpLine(sender, "epicheads.admin", "/heads reload", "Reload the configuration and language files.");
        sendHelpLine(sender, "epicheads.search", "/heads search", "Opens a gui displaying your search results.");
        sendHelpLine(sender, "epicheads.url", "/heads url", "Gives you the texture url for the head you are holding.");
        sender.sendMessage("");
    }

    private void sendHelpLine(CommandSender sender, String permission, String syntax, String description) {
        if (permission != null && !sender.hasPermission(permission)) {
            return;
        }
        sender.sendMessage(org.bukkit.ChatColor.DARK_GRAY + "- " + org.bukkit.ChatColor.YELLOW + syntax
                + org.bukkit.ChatColor.GRAY + " - " + description);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            names.add("all");
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return List.of("global", "local");
        }
        return Collections.emptyList();
    }
}
