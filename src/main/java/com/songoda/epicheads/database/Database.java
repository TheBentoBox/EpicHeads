package com.songoda.epicheads.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class Database {
    public static final String TABLE_PREFIX = "epicheads_";

    private final JavaPlugin plugin;
    private final File file;
    private final ExecutorService async = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "EpicHeads-Database");
        thread.setDaemon(true);
        return thread;
    });

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "EpicHeads.db");
    }

    public void open() {
        this.plugin.getDataFolder().mkdirs();
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + "players ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "favorites TEXT NOT NULL"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + "local_heads ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "category VARCHAR(48) NOT NULL, "
                    + "name VARCHAR(64) NOT NULL, "
                    + "url VARCHAR(256)"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + "disabled_heads ("
                    + "id INTEGER PRIMARY KEY"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + "head_ratings ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "head_id INTEGER NOT NULL, "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "rating INTEGER NOT NULL, "
                    + "rated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE(head_id, player_uuid)"
                    + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_head_ratings_head_id ON "
                    + TABLE_PREFIX + "head_ratings (head_id)");
        } catch (SQLException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", ex);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + this.file.getAbsolutePath());
    }

    public ExecutorService async() {
        return this.async;
    }

    public void shutdown() {
        this.async.shutdown();
    }
}
