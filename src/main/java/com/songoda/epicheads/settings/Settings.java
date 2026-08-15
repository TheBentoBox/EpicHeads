package com.songoda.epicheads.settings;

import com.songoda.epicheads.EpicHeads;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class Settings {
    private Settings() {
    }

    private static FileConfiguration config() {
        return EpicHeads.getInstance().getConfig();
    }

    public static int autosaveSeconds() {
        return config().getInt("Main.Auto Save Interval In Seconds", 15);
    }

    public static boolean freeInCreative() {
        return config().getBoolean("Main.Heads Free In Creative Mode", false);
    }

    public static boolean dropMobHeads() {
        return config().getBoolean("Main.Drop Mob Heads", true);
    }

    public static boolean dropPlayerHeads() {
        return config().getBoolean("Main.Drop Player Heads", true);
    }

    public static String dropChance() {
        return config().getString("Main.Head Drop Chance", "25%");
    }

    public static String economyPlugin() {
        return config().getString("Economy.Economy", "Vault");
    }

    public static double headCost() {
        return config().getDouble("Economy.Head Cost", 24.99);
    }

    public static String itemTokenType() {
        return config().getString("Economy.Item.Type", "PLAYER_HEAD");
    }

    public static int itemTokenId() {
        return config().getInt("Economy.Item.Head ID", 14395);
    }

    public static String itemTokenName() {
        return config().getString("Economy.Item.Name", "&6Player Head Token");
    }

    public static List<String> itemTokenLore() {
        return config().getStringList("Economy.Item.Lore");
    }

    public static String languageMode() {
        return config().getString("System.Language Mode", "en_US");
    }

    public static Material glassType(int index) {
        String key = "Interfaces.Glass Type " + index;
        String value = config().getString(key, index == 1 ? "GRAY_STAINED_GLASS_PANE"
                : index == 2 ? "BLUE_STAINED_GLASS_PANE" : "LIGHT_BLUE_STAINED_GLASS_PANE");
        try {
            int color = Integer.parseInt(value);
            return glassPaneForColor(color);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Material.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Material.GRAY_STAINED_GLASS_PANE;
        }
    }

    public static void setupConfig(EpicHeads plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        migrateGlass(config, "Interfaces.Glass Type 1");
        migrateGlass(config, "Interfaces.Glass Type 2");
        migrateGlass(config, "Interfaces.Glass Type 3");
        plugin.saveConfig();
    }

    private static void migrateGlass(FileConfiguration config, String key) {
        String value = config.getString(key);
        if (value == null) {
            return;
        }
        try {
            int color = Integer.parseInt(value);
            config.set(key, glassPaneForColor(color).name());
        } catch (NumberFormatException ignored) {
        }
    }

    private static Material glassPaneForColor(int color) {
        return switch (color) {
            case 0 -> Material.WHITE_STAINED_GLASS_PANE;
            case 1 -> Material.ORANGE_STAINED_GLASS_PANE;
            case 2 -> Material.MAGENTA_STAINED_GLASS_PANE;
            case 3 -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case 4 -> Material.YELLOW_STAINED_GLASS_PANE;
            case 5 -> Material.LIME_STAINED_GLASS_PANE;
            case 6 -> Material.PINK_STAINED_GLASS_PANE;
            case 7 -> Material.GRAY_STAINED_GLASS_PANE;
            case 8 -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case 9 -> Material.CYAN_STAINED_GLASS_PANE;
            case 10 -> Material.PURPLE_STAINED_GLASS_PANE;
            case 11 -> Material.BLUE_STAINED_GLASS_PANE;
            case 12 -> Material.BROWN_STAINED_GLASS_PANE;
            case 13 -> Material.GREEN_STAINED_GLASS_PANE;
            case 14 -> Material.RED_STAINED_GLASS_PANE;
            case 15 -> Material.BLACK_STAINED_GLASS_PANE;
            default -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }
}
