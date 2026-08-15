package com.songoda.epicheads.locale;

import com.songoda.epicheads.utils.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Locale {
    private final JavaPlugin plugin;
    private YamlConfiguration lang;

    public Locale(JavaPlugin plugin) {
        this.plugin = plugin;
        reload("en_US");
    }

    public void reload(String languageMode) {
        String fileName = languageMode + ".lang";
        File localesDir = new File(this.plugin.getDataFolder(), "locales");
        if (!localesDir.exists()) {
            localesDir.mkdirs();
        }

        File langFile = new File(localesDir, fileName);
        if (!langFile.exists()) {
            try (InputStream in = this.plugin.getResource(fileName)) {
                if (in != null) {
                    Files.copy(in, langFile.toPath());
                } else if (!fileName.equals("en_US.lang")) {
                    reload("en_US");
                    return;
                }
            } catch (IOException ex) {
                this.plugin.getLogger().warning("Failed to copy locale " + fileName + ": " + ex.getMessage());
            }
        }

        this.lang = YamlConfiguration.loadConfiguration(langFile);
        try (InputStream in = this.plugin.getResource("en_US.lang")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                this.lang.setDefaults(defaults);
            }
        } catch (IOException ignored) {
        }
    }

    public Message getMessage(String key) {
        String value = this.lang.getString(key, key);
        return new Message(this, value);
    }

    public Message newMessage(String raw) {
        return new Message(this, raw);
    }

    public String getPrefix() {
        return Text.color(this.lang.getString("general.nametag.prefix", "&7[&6EpicHeads&7]"));
    }

    public static final class Message {
        private final Locale locale;
        private String text;

        private Message(Locale locale, String text) {
            this.locale = locale;
            this.text = text == null ? "" : text;
        }

        public Message processPlaceholder(String key, Object value) {
            this.text = this.text.replace("%" + key + "%", String.valueOf(value));
            return this;
        }

        public String toText() {
            return Text.color(this.text);
        }

        public List<String> getMessageLines(char separator) {
            List<String> lines = new ArrayList<>();
            for (String part : this.text.split("\\" + separator)) {
                lines.add(Text.color(part));
            }
            return lines;
        }

        public void sendPrefixedMessage(CommandSender sender) {
            sender.sendMessage(this.locale.getPrefix() + " " + toText());
        }
    }
}
