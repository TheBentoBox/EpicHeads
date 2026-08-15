package com.songoda.epicheads.input;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInput implements Listener {
    private final Plugin plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public ChatInput(Plugin plugin) {
        this.plugin = plugin;
    }

    public void prompt(Player player, String message, Consumer<String> onInput, Runnable onCancel) {
        player.closeInventory();
        player.sendMessage(message);
        this.prompts.put(player.getUniqueId(), new Prompt(onInput, onCancel));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Prompt prompt = this.prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (message.equalsIgnoreCase("cancel")) {
                if (prompt.onCancel != null) {
                    prompt.onCancel.run();
                }
                return;
            }
            prompt.onInput.accept(message);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.prompts.remove(event.getPlayer().getUniqueId());
    }

    private record Prompt(Consumer<String> onInput, Runnable onCancel) {
    }
}
