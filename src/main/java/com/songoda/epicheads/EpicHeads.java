package com.songoda.epicheads;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.songoda.epicheads.command.HeadsCommand;
import com.songoda.epicheads.database.DataHelper;
import com.songoda.epicheads.database.Database;
import com.songoda.epicheads.economy.Economies;
import com.songoda.epicheads.head.Category;
import com.songoda.epicheads.head.Head;
import com.songoda.epicheads.head.HeadManager;
import com.songoda.epicheads.input.ChatInput;
import com.songoda.epicheads.listeners.DeathListeners;
import com.songoda.epicheads.listeners.ItemListeners;
import com.songoda.epicheads.listeners.LoginListeners;
import com.songoda.epicheads.locale.Locale;
import com.songoda.epicheads.menu.MenuListener;
import com.songoda.epicheads.players.EPlayer;
import com.songoda.epicheads.players.PlayerManager;
import com.songoda.epicheads.settings.Settings;
import com.songoda.epicheads.utils.storage.Storage;
import com.songoda.epicheads.utils.storage.StorageRow;
import com.songoda.epicheads.utils.storage.types.StorageYaml;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EpicHeads extends JavaPlugin {
    private HeadManager headManager;
    private PlayerManager playerManager;
    private Locale locale;
    private ChatInput chatInput;
    private Economies economies;
    private Database database;

    private boolean doneLoadingHeads = false;

    public static EpicHeads getInstance() {
        return getPlugin(EpicHeads.class);
    }

    @Override
    public void onEnable() {
        Settings.setupConfig(this);
        this.locale = new Locale(this);
        this.locale.reload(Settings.languageMode());

        this.economies = new Economies();
        this.economies.load();
        this.economies.setPreferred(Settings.economyPlugin());

        this.headManager = new HeadManager();
        this.playerManager = new PlayerManager();
        this.chatInput = new ChatInput(this);

        HeadsCommand command = new HeadsCommand(this);
        getCommand("EpicHeads").setExecutor(command);
        getCommand("EpicHeads").setTabCompleter(command);

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new MenuListener(), this);
        pluginManager.registerEvents(this.chatInput, this);
        pluginManager.registerEvents(new DeathListeners(this), this);
        pluginManager.registerEvents(new ItemListeners(this), this);
        pluginManager.registerEvents(new LoginListeners(this), this);

        this.database = new Database(this);
        this.database.open();
        this.getLogger().info("Data handler connected using SQLite.");
        DataHelper.init(this, this.database);

        Bukkit.getScheduler().runTaskAsynchronously(this, this::loadData);
    }

    @Override
    public void onDisable() {
        if (this.database != null) {
            DataHelper.saveAllPlayers();
            this.database.shutdown();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        this.locale.reload(Settings.languageMode());
        this.economies.setPreferred(Settings.economyPlugin());
        downloadHeads();
        loadHeads();
    }

    private void loadData() {
        downloadHeads();
        loadHeads();

        File dataFile = new File(getDataFolder(), "data.yml");
        boolean converted = false;
        if (dataFile.exists()) {
            converted = true;
            Storage storage = new StorageYaml(this);
            if (storage.containsGroup("players")) {
                Bukkit.getConsoleSender().sendMessage("[" + getPluginMeta().getName() + "] " + ChatColor.RED +
                        "Conversion process starting. Do NOT turn off your server. " +
                        "EpicHeads hasn't fully loaded yet, so make sure users don't" +
                        "interact with the plugin until the conversion process is complete.");

                List<EPlayer> players = new ArrayList<>();
                for (StorageRow row : storage.getRowsByGroup("players")) {
                    if (row.get("uuid").asObject() == null) {
                        continue;
                    }

                    players.add(new EPlayer(
                            UUID.fromString(row.get("uuid").asString()),
                            (List<String>) row.get("favorites").asObject()));
                }
                DataHelper.migratePlayers(players);
            }

            if (storage.containsGroup("local")) {
                for (StorageRow row : storage.getRowsByGroup("local")) {
                    String categoryName = row.get("category").asString();
                    Category category = this.headManager.getOrCreateCategoryByName(categoryName);

                    Head head = new Head(row.get("id").asInt(),
                            row.get("name").asString(),
                            row.get("url").asString(),
                            category,
                            true);

                    DataHelper.createLocalHead(head);
                }

                if (storage.containsGroup("disabled")) {
                    List<Integer> ids = new ArrayList<>();
                    for (StorageRow row : storage.getRowsByGroup("disabled")) {
                        ids.add(row.get("id").asInt());
                    }

                    DataHelper.migrateDisabledHead(ids);
                }
            }

            dataFile.delete();
        }

        final boolean finalConverted = converted;
        this.database.async().submit(() -> {
            if (finalConverted) {
                Bukkit.getConsoleSender().sendMessage("[" + getPluginMeta().getName() + "] " + ChatColor.GREEN + "Conversion complete :)");
            }

            DataHelper.getLocalHeads(heads -> {
                this.headManager.addLocalHeads(heads);
                getLogger().info("Loaded " + this.headManager.getHeads().size() + " heads");
                this.doneLoadingHeads = true;
            });

            DataHelper.getDisabledHeads(ids -> {
                for (int id : ids) {
                    this.headManager.disableHead(new Head(id, false));
                }
            });
        });
    }

    private void downloadHeads() {
        String[] categories = new String[]{
                "alphabet",
                "animals",
                "blocks",
                "decoration",
                "food-drinks",
                "humans",
                "humanoid",
                "miscellaneous",
                "monsters",
                "plants"
        };

        JsonArray jsonArray = new JsonArray();
        int idCounter = 1;

        try {
            for (String category : categories) {
                getLogger().info("Downloading data for " + category + "...");
                String apiUrl = "https://minecraft-heads.com/scripts/api.php?cat=" + category + "&tags=true";
                InputStream is = new URL(apiUrl).openStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String jsonText = readAll(rd);
                JsonArray jsonCategoryArray = JsonParser.parseString(jsonText).getAsJsonArray();

                for (JsonElement element : jsonCategoryArray) {
                    JsonObject entry = element.getAsJsonObject();
                    String name = entry.get("name").getAsString();
                    String value = entry.get("value").getAsString();

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("name", name);
                    jsonObject.addProperty("id", Integer.toString(idCounter++));
                    jsonObject.addProperty("url", value);
                    jsonObject.addProperty("category", category);
                    jsonArray.add(jsonObject);
                }
            }

            try (FileWriter file = new FileWriter(new File(getDataFolder(), "heads.json"))) {
                file.write(jsonArray.toString());
            }
        } catch (Exception ex) {
            getLogger().warning("Failed to download heads: " + ex.getMessage());
        }
    }

    private boolean loadHeads() {
        try {
            this.headManager.clear();

            JsonArray jsonArray = JsonParser.parseReader(new FileReader(getDataFolder() + "/heads.json")).getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();

                JsonElement nameElement = jsonObject.get("name");
                JsonElement packElement = jsonObject.get("pack");
                String headName = nameElement == null || nameElement.isJsonNull() ? null : nameElement.getAsString();
                String headPack = packElement == null || packElement.isJsonNull() ? null : packElement.getAsString();
                if (headName == null || headName.equals("null") || (headPack != null && headPack.equals("null"))) {
                    continue;
                }

                String categoryName = jsonObject.get("category").getAsString();
                Category category = this.headManager.getOrCreateCategoryByName(categoryName);

                Head head = new Head(
                        Integer.parseInt(jsonObject.get("id").getAsString()),
                        headName,
                        jsonObject.get("url").getAsString(),
                        category,
                        false
                );
                this.headManager.addHead(head);
            }
        } catch (IOException ex) {
            getLogger().warning("Disabling plugin, failed to load heads: " + ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            getLogger().warning("Disabling plugin, failed to parse heads: " + ex.getMessage());
            return false;
        }

        return true;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public Locale getLocale() {
        return this.locale;
    }

    public ChatInput getChatInput() {
        return this.chatInput;
    }

    public Economies getEconomies() {
        return this.economies;
    }

    public HeadManager getHeadManager() {
        return this.headManager;
    }

    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public boolean isDoneLoadingHeads() {
        return this.doneLoadingHeads;
    }
}
