package com.songoda.epicheads.listeners;

import com.songoda.epicheads.EpicHeads;
import com.songoda.epicheads.head.Head;
import com.songoda.epicheads.head.MobHeadTextures;
import com.songoda.epicheads.settings.Settings;
import com.songoda.epicheads.utils.SkullUtils;
import com.songoda.epicheads.utils.Text;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
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
    private final EpicHeads plugin;

    public DeathListeners(EpicHeads plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        double dropChance = Double.parseDouble(Settings.dropChance().replace("%", ""));
        double rand = Math.random() * 100;
        if (rand - dropChance < 0 || dropChance == 100) {

            ItemStack itemNew = null;
            if (event.getEntity() instanceof Player player) {
                if (!Settings.dropPlayerHeads()) {
                    return;
                }

                ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
                PlayerProfile profile = player.getPlayerProfile();

                String encodedStr = profile.getProperties().stream()
                        .filter(prop -> "textures".equals(prop.getName()))
                        .map(ProfileProperty::getValue)
                        .findFirst()
                        .orElse(null);

                if (encodedStr == null) {
                    itemNew = playerHead;

                    ItemMeta meta = itemNew.getItemMeta();
                    meta.setDisplayName(Text.color("&9" + player.getDisplayName()));
                    itemNew.setItemMeta(meta);
                } else {
                    String url = SkullUtils.getDecodedTexture(encodedStr);

                    Optional<Head> optional = this.plugin.getHeadManager().getHeads().stream()
                            .filter(head -> url.equals(head.getUrl())).findFirst();

                    if (optional.isPresent()) {
                        itemNew = optional.get().asItemStack();
                    }
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
            }
            if (itemNew == null) {
                return;
            }

            ItemMeta meta = itemNew.getItemMeta();
            meta.setLore(new ArrayList<>());
            itemNew.setItemMeta(meta);

            event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), itemNew);
        }
    }
}
