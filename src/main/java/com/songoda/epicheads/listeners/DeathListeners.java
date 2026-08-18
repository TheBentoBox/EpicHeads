package com.songoda.epicheads.listeners;

import com.songoda.epicheads.head.Head;
import com.songoda.epicheads.head.MobHeadTextures;
import com.songoda.epicheads.settings.Settings;
import com.songoda.epicheads.utils.Text;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Optional;

public class DeathListeners implements Listener {

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        double dropChance = Double.parseDouble(Settings.dropChance().replace("%", ""));
        double rand = Math.random() * 100;
        if (rand - dropChance >= 0 && dropChance != 100) {
            return;
        }

        ItemStack itemNew = null;
        if (event.getEntity() instanceof Player player) {
            if (!Settings.dropPlayerHeads()) {
                return;
            }

            PlayerProfile profile = player.getPlayerProfile();
            String encodedStr = profile.getProperties().stream()
                    .filter(prop -> "textures".equals(prop.getName()))
                    .map(ProfileProperty::getValue)
                    .findFirst()
                    .orElse(null);

            if (encodedStr == null) {
                itemNew = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta meta = itemNew.getItemMeta();
                meta.setDisplayName(Text.color("&9" + player.getDisplayName()));
                itemNew.setItemMeta(meta);
            } else {
                ProfileProperty property = new ProfileProperty("textures", encodedStr, "");
                ResolvableProfile resolvableProfile = ResolvableProfile.resolvableProfile()
                        .addProperty(property)
                        .build();
                itemNew = new ItemStack(Material.PLAYER_HEAD);
                itemNew.setData(DataComponentTypes.PROFILE, resolvableProfile);
            }
        } else {
            if (!Settings.dropMobHeads() || event.getEntity() instanceof ArmorStand) {
                return;
            }

            Optional<MobHeadTextures> texture = MobHeadTextures.from(event.getEntity().getType());
            if (texture.isEmpty()) {
                return;
            }

            Head head = new Head(-1, Text.titleCase(event.getEntity().getType().name()),
                    texture.get().getUrlHash(),
                    null, true);
            itemNew = head.asItemStack();
            ItemMeta meta = itemNew.getItemMeta();
            meta.setLore(new ArrayList<>());
            itemNew.setItemMeta(meta);
        }

        if (itemNew == null) {
            return;
        }

        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), itemNew);
    }
}
