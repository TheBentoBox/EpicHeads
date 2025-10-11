package com.songoda.epicheads.listeners;

import com.songoda.epicheads.EpicHeads;
import com.songoda.epicheads.utils.SkullUtils;
import com.songoda.epicheads.database.DataHelper;
import com.songoda.epicheads.head.Category;
import com.songoda.epicheads.head.Head;
import com.songoda.epicheads.head.HeadManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.Optional;

public class LoginListeners implements Listener {
    private final EpicHeads plugin;

    public LoginListeners(EpicHeads plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreJoin(PlayerJoinEvent event) {
        if (!this.plugin.isDoneLoadingHeads()) {
            // This is a hotfix/workaround for when EpicHeads is not fully loaded yet (prevents duplicate heads due to race condition)
            return;
        }

        Player player = event.getPlayer();
        HeadManager headManager = this.plugin.getHeadManager();

        // Get the player's profile and extract texture value using Paper API
        PlayerProfile profile = player.getPlayerProfile();
        String encodedStr = profile.getProperties().stream()
                .filter(prop -> "textures".equals(prop.getName()))
                .map(ProfileProperty::getValue)
                .findFirst()
                .orElse(null);
        
        if (encodedStr == null) {
            return;
        }

        String url = SkullUtils.getDecodedTexture(encodedStr);

        Optional<Head> existingPlayerHead = headManager.getLocalHeads()
                .stream()
                .filter(h -> h.getName().equalsIgnoreCase(event.getPlayer().getName()))
                .findFirst();
        if (existingPlayerHead.isPresent()) {
            Head head = existingPlayerHead.get();
            head.setUrl(url);
            DataHelper.updateLocalHead(head);
            return;
        }

        String categoryName = this.plugin.getLocale().getMessage("general.word.playerheads").toText();
        Category category = headManager.getOrCreateCategoryByName(categoryName);

        Head head = new Head(headManager.getNextLocalId(), player.getName(), url, category, true);
        DataHelper.createLocalHead(head);
        headManager.addLocalHead(head);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Runnable task = () -> DataHelper.getPlayer(event.getPlayer(), ePlayer -> this.plugin.getPlayerManager().addPlayer(ePlayer));
        if (DataHelper.isInitialized()) {
            task.run();
            return;
        }

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, task, 20 * 3); // hotfix/workaround for another race condition \o/
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        DataHelper.updatePlayer(this.plugin.getPlayerManager().getPlayer(event.getPlayer()));
    }
}
