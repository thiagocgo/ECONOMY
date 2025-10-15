package com.thiagocgo.arcanemoney.shops.listeners;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.shops.guis.*;
import com.thiagocgo.arcanemoney.shops.utils.Shop;
import com.thiagocgo.arcanemoney.shops.utils.ShopManager;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ShopInventoryListener implements Listener {

    private final ArcaneMoney plugin;
    private final ConfigManager configManager;
    private final ShopManager shopManager;

    public ShopInventoryListener(ArcaneMoney plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.shopManager = plugin.getShopManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();
        String title = event.getView().getTitle();
        ClickType clickType = event.getClick();
        int rawSlot = event.getRawSlot();

        String normalizedTitle = ChatColor.stripColor(title).toLowerCase().replace("\u00e7", "c").replace("\u00e3", "a");

        plugin.getLogger().info("InventoryClickEvent: Player=" + player.getName() + ", Title=" + title + ", NormalizedTitle=" + normalizedTitle + ", Slot=" + rawSlot + ", ClickType=" + clickType + ", Item=" + (clickedItem != null ? clickedItem.getType().name() : "null"));

        if (!normalizedTitle.startsWith("loja arcanemc")) {
            return;
        }

        Shop shop = getShopFromTitle(title);
        if (shop == null) {
            plugin.getLogger().warning("Shop not found for title: " + title);
            player.sendMessage(configManager.getMessage("shop-not-found"));
            player.closeInventory();
            event.setCancelled(true);
            return;
        }

        // Check if the click is in the player's inventory (bottom inventory)
        boolean isPlayerInventory = rawSlot >= inventory.getSize();

        if (normalizedTitle.contains("loja arcanemc [" + shop.getId() + "]")) {
            // CostumerGUI handling
            if (isPlayerInventory) {
                // Allow interaction with player's inventory without triggering shop actions
                event.setCancelled(false); // Permit moving items in player's inventory
                plugin.getLogger().info("Click in player's inventory in CostumerGUI by " + player.getName() + ", slot: " + rawSlot);
                return;
            } else {
                // Cancel clicks in the shop GUI to prevent item pickup, but process purchase logic
                event.setCancelled(true);
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    plugin.getLogger().warning("Clicked item is null or air in CostumerGUI: " + title);
                    return;
                }
                CustomerGUI.handleClick(player, shop, inventory, clickedItem, event.getSlot(), plugin);
            }
        } else {
            // Other GUIs (ShopManageGUI, SelectItemGUI, QuantityGUI, PriceGUI, StockGUI)
            event.setCancelled(true); // Cancel clicks by default for other GUIs
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                plugin.getLogger().warning("Clicked item is null or air in inventory: " + title);
                return;
            }

            if (normalizedTitle.contains("loja arcanemc - gerenciar [" + shop.getId() + "]")) {
                if (inventory.getSize() == 27) {
                    ShopManageGUI.handleClick(player, shop, inventory, clickedItem, event.getSlot(), clickType, plugin);
                } else if (inventory.getSize() == 54) {
                    SelectItemGUI.handleClick(player, shop, inventory, clickedItem, event.getSlot(), clickType, plugin);
                }
            } else if (normalizedTitle.contains("loja arcanemc - quantidade [" + shop.getId() + "]")) {
                QuantityGUI.handleClick(player, shop, inventory, clickedItem, event.getSlot(), plugin);
            } else if (normalizedTitle.contains("loja arcanemc - preco [" + shop.getId() + "]")) {
                plugin.getLogger().info("Handling click in PriceGUI: Slot=" + event.getSlot() + ", Item=" + clickedItem.getType().name());
                PriceGUI.handleClick(player, shop, inventory, clickedItem, event.getSlot(), plugin);
            } else if (normalizedTitle.contains("loja arcanemc - estoque [" + shop.getId() + "]")) {
                StockGUI.handleClick(event, player, shop, inventory, clickedItem, event.getSlot(), clickType, plugin);
            } else {
                plugin.getLogger().warning("Unrecognized GUI title: " + title);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String normalizedTitle = ChatColor.stripColor(title).toLowerCase().replace("\u00e7", "c").replace("\u00e3", "a");

        if (normalizedTitle.startsWith("loja arcanemc")) {
            if (normalizedTitle.contains("loja arcanemc - estoque")) {
                Shop shop = getShopFromTitle(title);
                if (shop != null) {
                    StockGUI.handleDrag(event, player, shop, event.getInventory());
                } else {
                    event.setCancelled(true);
                    player.sendMessage(configManager.getMessage("shop-not-found"));
                }
            } else {
                event.setCancelled(true);
                player.sendMessage(configManager.getMessage("shop-use-click"));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        String normalizedTitle = ChatColor.stripColor(title).toLowerCase().replace("\u00e7", "c").replace("\u00e3", "a");

        plugin.getLogger().info("Inventory closed by " + player.getName() + ", title: " + title + ", normalized: " + normalizedTitle);

        try {
            if (normalizedTitle.contains("loja arcanemc - quantidade")) {
                QuantityGUI.clearState(player);
            } else if (normalizedTitle.contains("loja arcanemc - preco")) {
                PriceGUI.clearState(player);
            } else if (normalizedTitle.contains("loja arcanemc - estoque")) {
                StockGUI.handleInventoryClose(event);
            }

            Shop shop = getShopFromTitle(title);
            if (shop == null) {
                plugin.getLogger().info("No shop found for title: " + title);
                return;
            }

            ShulkerBox shulkerBox = shop.getShulkerBox();
            if (shulkerBox != null) {
                shulkerBox.close();
                shulkerBox.update(true, false);
                Location loc = shop.getLocation().add(0.5, 0.5, 0.5);
                shop.getWorld().playSound(loc, Sound.BLOCK_SHULKER_BOX_CLOSE, 0.5f, 1.0f);
                plugin.getLogger().info("Forced shulker box close for shop " + shop.getId());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling InventoryCloseEvent for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(configManager.getMessage("shop-error"));
        }
    }

    private Shop getShopFromTitle(String title) {
        String shopId = getShopIdFromTitle(title);
        if (shopId.isEmpty()) {
            plugin.getLogger().warning("Invalid shop ID in title: " + title);
            return null;
        }

        String[] parts = shopId.split("_");
        if (parts.length != 4) {
            plugin.getLogger().warning("Malformed shop ID: " + shopId);
            return null;
        }

        World world = plugin.getServer().getWorld(parts[0]);
        if (world == null) {
            plugin.getLogger().warning("World not found for shop ID: " + shopId);
            return null;
        }

        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            Location location = new Location(world, x, y, z);
            Shop shop = shopManager.getShop(location);
            if (shop == null) {
                plugin.getLogger().warning("No shop found at location: " + location);
            }
            return shop;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid coordinates in shop ID: " + shopId);
            return null;
        }
    }

    private String getShopIdFromTitle(String title) {
        String[] parts = title.split("\\[");
        if (parts.length > 1) {
            return parts[1].replace("]", "");
        }
        return "";
    }
}