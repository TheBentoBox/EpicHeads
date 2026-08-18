package com.songoda.epicheads.listeners;

import com.songoda.epicheads.utils.ItemEconomy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class ItemListeners implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (ItemEconomy.isItem(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }
}
