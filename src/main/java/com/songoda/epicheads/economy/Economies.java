package com.songoda.epicheads.economy;

import com.songoda.epicheads.utils.ItemEconomy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class Economies {
    private final ItemEconomy itemEconomy = new ItemEconomy();
    private Economy vault;
    private String preferred = "Vault";

    public void load() {
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            this.vault = registration.getProvider();
        }
    }

    public void setPreferred(String preferred) {
        this.preferred = preferred == null ? "Vault" : preferred;
    }

    public boolean isEnabled() {
        if (this.preferred.equalsIgnoreCase("item")) {
            return true;
        }
        return this.vault != null;
    }

    public boolean hasBalance(Player player, double cost) {
        if (cost <= 0) {
            return true;
        }
        if (this.preferred.equalsIgnoreCase("item")) {
            return this.itemEconomy.hasBalance(player, cost);
        }
        return this.vault != null && this.vault.has(player, cost);
    }

    public boolean withdraw(Player player, double cost) {
        if (cost <= 0) {
            return true;
        }
        if (this.preferred.equalsIgnoreCase("item")) {
            return this.itemEconomy.withdrawBalance(player, cost);
        }
        return this.vault != null && this.vault.withdrawPlayer(player, cost).transactionSuccess();
    }
}
