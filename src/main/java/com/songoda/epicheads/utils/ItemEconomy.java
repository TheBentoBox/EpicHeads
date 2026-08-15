package com.songoda.epicheads.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemEconomy {
    public static boolean isItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }

        if (itemStack.getType() == Material.PLAYER_HEAD) {
            return SkullUtils.haveSameProfile(itemStack, Methods.createToken(1));
        }
        return itemStack.isSimilar(Methods.createToken(1));
    }

    private int convertAmount(double amount) {
        return (int) Math.ceil(amount);
    }

    public boolean hasBalance(Player player, double cost) {
        int amount = convertAmount(cost);
        for (ItemStack item : player.getInventory().getContents()) {
            if (!isItem(item)) {
                continue;
            }
            if (amount <= item.getAmount()) {
                return true;
            }
            amount -= item.getAmount();
        }
        return false;
    }

    public boolean withdrawBalance(Player player, double cost) {
        int amount = convertAmount(cost);
        ItemStack[] contents = player.getInventory().getContents();
        for (int index = 0; index < contents.length; ++index) {
            ItemStack item = contents[index];
            if (!isItem(item)) {
                continue;
            }
            if (amount >= item.getAmount()) {
                amount -= item.getAmount();
                contents[index] = null;
            } else {
                item.setAmount(item.getAmount() - amount);
                amount = 0;
            }
            if (amount == 0) {
                break;
            }
        }
        if (amount != 0) {
            return false;
        }
        player.getInventory().setContents(contents);
        return true;
    }
}
