package com.songoda.epicheads.utils;

import com.songoda.epicheads.EpicHeads;
import com.songoda.epicheads.settings.Settings;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Methods {
    public static ItemStack createToken(int amount) {
        Material material = Material.valueOf(Settings.itemTokenType().toUpperCase());
        ItemStack itemStack = new ItemStack(material);

        if (itemStack.getType() == Material.PLAYER_HEAD) {
            itemStack = EpicHeads.getInstance()
                    .getHeadManager()
                    .getHeads()
                    .stream()
                    .filter(head -> head.getId() == Settings.itemTokenId())
                    .findFirst()
                    .get()
                    .asItemStack();
        }
        itemStack.setAmount(amount);

        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(Text.color(Settings.itemTokenName()));
        List<String> lore = new ArrayList<>();
        for (String line : Settings.itemTokenLore()) {
            if (!line.isEmpty()) {
                lore.add(Text.color(line));
            }
        }
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
