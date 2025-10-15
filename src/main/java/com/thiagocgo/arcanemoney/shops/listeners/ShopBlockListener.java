package com.thiagocgo.arcanemoney.shops.listeners;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.shops.guis.CustomerGUI;
import com.thiagocgo.arcanemoney.shops.guis.ShopManageGUI;
import com.thiagocgo.arcanemoney.shops.utils.Shop;
import com.thiagocgo.arcanemoney.shops.utils.ShopManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ShopBlockListener implements Listener {

    private final ArcaneMoney plugin;
    private final ConfigManager configManager;
    private final ShopManager shopManager;

    public ShopBlockListener(ArcaneMoney plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.shopManager = plugin.getShopManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if (!item.getType().name().endsWith("SHULKER_BOX") || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getDisplayName().equals(ChatColor.LIGHT_PURPLE + "Loja ArcaneMC")) return;

        UUID ownerUUID = null;
        for (String lore : meta.getLore()) {
            if (lore.startsWith(ChatColor.GRAY + "UUID: ")) {
                ownerUUID = UUID.fromString(lore.replace(ChatColor.GRAY + "UUID: ", ""));
                break;
            }
        }
        if (ownerUUID == null || !ownerUUID.equals(player.getUniqueId())) {
            player.sendMessage(configManager.getMessage("no-permission"));
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof ShulkerBox)) return;
        ShulkerBox shulkerBox = (ShulkerBox) block.getState();
        shopManager.createShop(player, block.getLocation(), shulkerBox);

        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.PORTAL, loc, 50, 0.5, 0.5, 0.5, 0);
        block.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof ShulkerBox)) return;

        Shop shop = shopManager.getShop(block.getLocation());
        if (shop == null) return;

        Player player = event.getPlayer();
        if (!shop.getOwner().equals(player.getUniqueId()) && !player.hasPermission("arcanemoney.admin")) {
            player.sendMessage(configManager.getMessage("shop-break-no-permission")
                    .replace("%owner%", shop.getOwnerName()));
            event.setCancelled(true);
            return;
        }

        if (!block.getType().name().endsWith("SHULKER_BOX")) {
            plugin.getLogger().warning("Mismatch in block type at " + block.getLocation() + ": expected shulker box, found " + block.getType());
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("shop-break-error"));
            return;
        }

        shopManager.removeShop(block.getLocation());
        player.sendMessage(configManager.getMessage("shop-break-success"));
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType()));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT")) return;
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof ShulkerBox)) return;

        Shop shop = shopManager.getShop(block.getLocation());
        if (shop == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.getLogger().info("Player " + player.getName() + " interacted with shop " + shop.getId());
        ShulkerBox shulkerBox = (ShulkerBox) block.getState();
        shulkerBox.setCustomName(ChatColor.LIGHT_PURPLE + "Loja ArcaneMC [" + shop.getId() + "]");

        // Play open animation and sound
        shulkerBox.open();
        shulkerBox.update(true);
        block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);

        // Check if player is the owner to open ShopManageGUI
        if (shop.getOwner().equals(player.getUniqueId())) {
            plugin.getLogger().info("Opening ShopManageGUI for owner " + player.getName());
            ShopManageGUI.open(player, shop, plugin);
        } else {
            plugin.getLogger().info("Opening CostumerGUI for " + player.getName());
            CustomerGUI.open(player, shop, plugin);
        }

        // Schedule shulker close animation to prevent it from staying open
        new BukkitRunnable() {
            @Override
            public void run() {
                if (shulkerBox.getInventory().getViewers().isEmpty()) {
                    shulkerBox.close();
                    shulkerBox.update(true);
                    block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_SHULKER_BOX_CLOSE, 0.5f, 1.0f);
                    plugin.getLogger().info("Closed shulker box for shop " + shop.getId());
                }
            }
        }.runTaskLater(plugin, 20L); // Close after 1 second if no viewers
    }
}