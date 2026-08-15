package com.songoda.epicheads.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.songoda.epicheads.EpicHeads;
import com.songoda.epicheads.head.Head;
import com.songoda.epicheads.players.EPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DataHelper {
    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST = new TypeToken<List<String>>() {
    }.getType();

    private static Database database;
    private static EpicHeads plugin;

    public static void init(EpicHeads plugin, Database database) {
        DataHelper.plugin = plugin;
        DataHelper.database = database;
    }

    public static boolean isInitialized() {
        return database != null;
    }

    private static void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void updatePlayer(EPlayer ePlayer) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE " + Database.TABLE_PREFIX + "players SET favorites = ? WHERE uuid = ?")) {
                statement.setString(1, GSON.toJson(ePlayer.getFavorites()));
                statement.setString(2, ePlayer.getUuid().toString());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to update player", ex);
            }
        });
    }

    public static void getPlayer(Player player, Consumer<EPlayer> callback) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection()) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT OR IGNORE INTO " + Database.TABLE_PREFIX + "players (uuid, favorites) VALUES (?, ?)")) {
                    insert.setString(1, player.getUniqueId().toString());
                    insert.setString(2, GSON.toJson(new ArrayList<>()));
                    insert.executeUpdate();
                }

                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT * FROM " + Database.TABLE_PREFIX + "players WHERE uuid = ?")) {
                    select.setString(1, player.getUniqueId().toString());
                    ResultSet result = select.executeQuery();
                    if (result.next()) {
                        UUID uuid = UUID.fromString(result.getString("uuid"));
                        List<String> favorites = GSON.fromJson(result.getString("favorites"), STRING_LIST);
                        EPlayer ePlayer = new EPlayer(uuid, favorites);
                        runSync(() -> callback.accept(ePlayer));
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load player", ex);
            }
        });
    }

    public static void migratePlayers(List<EPlayer> players) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement insert = connection.prepareStatement(
                         "INSERT OR REPLACE INTO " + Database.TABLE_PREFIX + "players (uuid, favorites) VALUES (?, ?)")) {
                for (EPlayer player : players) {
                    insert.setString(1, player.getUuid().toString());
                    insert.setString(2, GSON.toJson(player.getFavorites()));
                    insert.addBatch();
                }
                insert.executeBatch();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to migrate players", ex);
            }
        });
    }

    public static void createLocalHead(Head head) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO " + Database.TABLE_PREFIX + "local_heads (category, name, url) VALUES (?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, head.getCategory().getName());
                statement.setString(2, head.getName());
                statement.setString(3, head.getUrl());
                statement.executeUpdate();
                ResultSet keys = statement.getGeneratedKeys();
                if (keys.next()) {
                    head.setId(keys.getInt(1));
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to create local head", ex);
            }
        });
    }

    public static void getLocalHeads(Consumer<List<Head>> callback) {
        database.async().submit(() -> {
            List<Head> heads = new ArrayList<>();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM " + Database.TABLE_PREFIX + "local_heads ORDER BY id ASC");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int id = result.getInt("id");
                    String categoryString = result.getString("category");
                    String name = result.getString("name");
                    String url = result.getString("url");
                    Head head = new Head(id, name, url,
                            plugin.getHeadManager().getOrCreateCategoryByName(categoryString), true);
                    heads.add(head);
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load local heads", ex);
            }
            runSync(() -> callback.accept(heads));
        });
    }

    public static void updateLocalHead(Head head) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE " + Database.TABLE_PREFIX + "local_heads SET name = ?, url = ? WHERE id = ?")) {
                statement.setString(1, head.getName());
                statement.setString(2, head.getUrl());
                statement.setInt(3, head.getId());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to update local head", ex);
            }
        });
    }

    public static void disableHead(Head head) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT OR IGNORE INTO " + Database.TABLE_PREFIX + "disabled_heads (id) VALUES (?)")) {
                statement.setInt(1, head.getId());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to disable head", ex);
            }
        });
    }

    public static void migrateDisabledHead(List<Integer> heads) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT OR IGNORE INTO " + Database.TABLE_PREFIX + "disabled_heads (id) VALUES (?)")) {
                for (int id : heads) {
                    statement.setInt(1, id);
                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to migrate disabled heads", ex);
            }
        });
    }

    public static void getDisabledHeads(Consumer<List<Integer>> callback) {
        database.async().submit(() -> {
            List<Integer> heads = new ArrayList<>();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT id FROM " + Database.TABLE_PREFIX + "disabled_heads");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    heads.add(result.getInt("id"));
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load disabled heads", ex);
            }
            runSync(() -> callback.accept(heads));
        });
    }

    public static void saveAllPlayers() {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE " + Database.TABLE_PREFIX + "players SET favorites = ? WHERE uuid = ?")) {
                for (EPlayer player : plugin.getPlayerManager().getPlayers()) {
                    statement.setString(1, GSON.toJson(player.getFavorites()));
                    statement.setString(2, player.getUuid().toString());
                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to save players", ex);
            }
        });
    }

    public static void addHeadRating(int headId, UUID playerUuid, int rating) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO " + Database.TABLE_PREFIX + "head_ratings (head_id, player_uuid, rating) "
                                 + "VALUES (?, ?, ?) ON CONFLICT(head_id, player_uuid) DO UPDATE SET rating = excluded.rating, "
                                 + "rated_at = CURRENT_TIMESTAMP")) {
                statement.setInt(1, headId);
                statement.setString(2, playerUuid.toString());
                statement.setInt(3, rating);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to add head rating", ex);
            }
        });
    }

    public static void getHeadRatings(int headId, Consumer<Double> averageCallback, Consumer<Integer> totalCallback) {
        database.async().submit(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT AVG(rating) as avg_rating, COUNT(*) as total_ratings "
                                 + "FROM " + Database.TABLE_PREFIX + "head_ratings WHERE head_id = ?")) {
                statement.setInt(1, headId);
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    double avgRating = result.getDouble("avg_rating");
                    int totalRatings = result.getInt("total_ratings");
                    runSync(() -> {
                        averageCallback.accept(avgRating);
                        totalCallback.accept(totalRatings);
                    });
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load head ratings", ex);
            }
        });
    }

    public static void updateHeadRatingStats(Head head) {
        getHeadRatings(head.getId(),
                head::setAverageRating,
                head::setTotalRatings);
    }
}
