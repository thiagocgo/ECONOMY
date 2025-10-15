package com.thiagocgo.arcanemoney.shops.guis;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.shops.utils.Shop;
import com.thiagocgo.arcanemoney.shops.utils.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StockGUI {
    private static final int[] INCREMENT_SLOTS = {12, 13, 14}; // Centered in columns 4, 5, 6
    private static final int[] DECREMENT_SLOTS = {21, 22, 23};
    private static final String[] LABELS = {"Unidade", "Dezena", "Centena"};
    private static final int[] INCREMENTS = {1, 10, 100};
    private static final Map<String, Map<String, Object>> guiStates = new HashMap<>();
    private static ArcaneMoney plugin;

    public static void setPlugin(ArcaneMoney pluginInstance) {
        plugin = pluginInstance;
    }

    public static void open(Player player, Shop shop, ItemStack item, int quantity, double buyPrice, ArcaneMoney plugin) {
        if (StockGUI.plugin == null) {
            StockGUI.plugin = plugin;
        }

        clearState(player); // Clear any existing state
        String title = ChatColor.LIGHT_PURPLE + "Loja ArcaneMC - Estoque [" + shop.getId() + "]";
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        // Create a set of reserved slots
        Set<Integer> reservedSlots = new HashSet<>();
        reservedSlots.add(4); // Display item
        reservedSlots.add(16); // Confirm button
        reservedSlots.add(18); // Back button
        reservedSlots.add(26); // Close button
        for (int slot : INCREMENT_SLOTS) {
            reservedSlots.add(slot);
        }
        for (int slot : DECREMENT_SLOTS) {
            reservedSlots.add(slot);
        }

        // Fill non-interactive slots with gray stained glass panes
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(ChatColor.RESET + "");
        border.setItemMeta(borderMeta);
        for (int i = 0; i < 27; i++) {
            if (!reservedSlots.contains(i)) {
                inventory.setItem(i, border);
            }
        }

        // Add increment and decrement buttons
        for (int i = 0; i < INCREMENT_SLOTS.length; i++) {
            ItemStack button = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "+" + LABELS[i]);
            button.setItemMeta(meta);
            inventory.setItem(INCREMENT_SLOTS[i], button);

            button = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "-" + LABELS[i]);
            button.setItemMeta(meta);
            inventory.setItem(DECREMENT_SLOTS[i], button);
        }

        // Confirm button
        ItemStack confirm = new ItemStack(Material.EMERALD);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirmar");
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(16, confirm);

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Voltar");
        backButton.setItemMeta(backMeta);
        inventory.setItem(18, backButton);

        // Close button
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Fechar");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(26, closeButton);

        // Display item
        ItemStack itemDisplay = new ItemStack(item.getType(), quantity);
        ItemMeta displayMeta = itemDisplay.getItemMeta();
        displayMeta.setDisplayName(ChatColor.YELLOW + item.getType().name().toLowerCase());
        displayMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Quantidade por compra: " + quantity,
                ChatColor.GRAY + "Preço por " + quantity + ": " + (long) buyPrice,
                ChatColor.GRAY + "Estoque a adicionar: 0",
                ChatColor.YELLOW + "Ajuste o estoque e clique em Confirmar"
        ));
        itemDisplay.setItemMeta(displayMeta);
        inventory.setItem(4, itemDisplay);

        // Save GUI state
        String playerUUID = player.getUniqueId().toString();
        Map<String, Object> state = new HashMap<>();
        state.put("item", item.clone());
        state.put("quantity", quantity);
        state.put("buyPrice", buyPrice);
        state.put("shopId", shop.getId());
        state.put("stock", 0); // Initial stock to add
        guiStates.put(playerUUID, state);

        player.openInventory(inventory);
        plugin.getLogger().info("Opened StockGUI for " + player.getName() + " for item " + item.getType().name());
    }

    public static void handleClick(InventoryClickEvent event, Player player, Shop shop, Inventory inventory, ItemStack clickedItem, int slot, ClickType clickType, ArcaneMoney plugin) {
        if (StockGUI.plugin == null) {
            StockGUI.plugin = plugin;
        }

        String playerUUID = player.getUniqueId().toString();
        Map<String, Object> state = guiStates.get(playerUUID);
        if (state == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("shop-error"));
            player.closeInventory();
            event.setCancelled(true);
            return;
        }

        ItemStack expectedItem = (ItemStack) state.get("item");
        String expectedItemTag = "minecraft:" + expectedItem.getType().name().toLowerCase();
        int quantity = (int) state.get("quantity");
        double buyPrice = (double) state.get("buyPrice");
        int stock = (int) state.get("stock");
        String shopId = (String) state.get("shopId");

        // Validate shop
        if (!shop.getId().equals(shopId)) {
            plugin.getLogger().warning("Shop ID mismatch in StockGUI for player " + player.getName() + ": expected " + shop.getId() + ", found " + shopId);
            player.sendMessage(plugin.getConfigManager().getMessage("shop-error"));
            player.closeInventory();
            guiStates.remove(playerUUID);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true); // Cancel all clicks by default

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            plugin.getLogger().info("Clicked item is null or air in StockGUI for " + player.getName() + ", slot: " + slot);
            return;
        }

        // Handle button clicks
        boolean stockChanged = false;
        for (int i = 0; i < INCREMENT_SLOTS.length; i++) {
            if (slot == INCREMENT_SLOTS[i] && clickedItem.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                stock += INCREMENTS[i];
                stockChanged = true;
                plugin.getLogger().info("Incremented stock by " + INCREMENTS[i] + " to " + stock + " for " + player.getName());
                break;
            } else if (slot == DECREMENT_SLOTS[i] && clickedItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                stock = Math.max(0, stock - INCREMENTS[i]);
                stockChanged = true;
                plugin.getLogger().info("Decremented stock by " + INCREMENTS[i] + " to " + stock + " for " + player.getName());
                break;
            }
        }

        if (slot == 16 && clickedItem.getType() == Material.EMERALD) {
            if (stock <= 0) {
                player.sendMessage(plugin.getConfigManager().getMessage("shop-no-items-added"));
                plugin.getLogger().warning("Attempted to confirm with stock <= 0 for " + player.getName());
                return;
            }

            // Validate mod ID and item
            String[] itemTagParts = expectedItemTag.split(":");
            if (itemTagParts.length != 2 || !itemTagParts[0].equals("minecraft")) {
                player.sendMessage(plugin.getConfigManager().getMessage("shop-invalid-item")
                        .replace("%item%", expectedItemTag));
                plugin.getLogger().warning("Invalid item tag: " + expectedItemTag + " for " + player.getName());
                return;
            }

            // Check if player has enough items
            ItemStack toRemove = new ItemStack(expectedItem.getType(), stock);
            if (!player.getInventory().containsAtLeast(toRemove, stock)) {
                player.sendMessage(plugin.getConfigManager().getMessage("trade-insufficient-items")
                        .replace("%qtd%", String.valueOf(stock))
                        .replace("%item%", expectedItemTag));
                plugin.getLogger().warning("Player " + player.getName() + " does not have enough items: " + stock + " " + expectedItemTag);
                return;
            }

            // Add item to shop
            ShopManager shopManager = plugin.getShopManager();
            boolean success = shopManager.addItemToShop(shop, player, expectedItem, quantity, buyPrice, stock);
            if (success) {
                // Remove the line below to prevent double removal
                // player.getInventory().removeItem(toRemove); // Remove items from inventory
                guiStates.remove(playerUUID);
                SelectItemGUI.open(player, shop, plugin);
                plugin.getLogger().info("Successfully added " + stock + " of " + expectedItem.getType().name() + " to shop " + shop.getId());
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("shop-error"));
                plugin.getLogger().warning("Failed to add " + stock + " of " + expectedItem.getType().name() + " to shop " + shop.getId());
            }
            return;
        }
        else if (slot == 18 && clickedItem.getType() == Material.ARROW) {
            PriceGUI.open(player, shop, expectedItem, quantity, plugin);
            guiStates.remove(playerUUID);
            plugin.getLogger().info("Player " + player.getName() + " returned to PriceGUI from StockGUI");
            return;
        } else if (slot == 26 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            guiStates.remove(playerUUID);
            plugin.getLogger().info("Player " + player.getName() + " closed StockGUI");
            return;
        }

        if (stockChanged) {
            // Update display item
            ItemStack itemDisplay = new ItemStack(expectedItem.getType(), quantity);
            ItemMeta displayMeta = itemDisplay.getItemMeta();
            displayMeta.setDisplayName(ChatColor.YELLOW + expectedItem.getType().name().toLowerCase());
            displayMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Quantidade por compra: " + quantity,
                    ChatColor.GRAY + "Preço por " + quantity + ": " + (long) buyPrice,
                    ChatColor.GRAY + "Estoque a adicionar: " + stock,
                    ChatColor.YELLOW + "Ajuste o estoque e clique em Confirmar"
            ));
            itemDisplay.setItemMeta(displayMeta);
            inventory.setItem(4, itemDisplay);

            state.put("stock", stock);
            guiStates.put(playerUUID, state);
            plugin.getLogger().info("Stock updated to " + stock + " for item " + expectedItem.getType().name() + " by " + player.getName());
        } else {
            plugin.getLogger().info("No action taken for click in StockGUI by " + player.getName() + " at slot " + slot);
        }
    }

    public static void handleDrag(InventoryDragEvent event, Player player, Shop shop, Inventory inventory) {
        event.setCancelled(true); // Prevent dragging in StockGUI
        player.sendMessage(plugin.getConfigManager().getMessage("shop-use-click"));
        plugin.getLogger().info("Blocked drag in StockGUI for player " + player.getName());
    }

    public static void handleInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        Map<String, Object> state = guiStates.get(playerUUID);
        if (state == null) {
            plugin.getLogger().info("No state found for player " + player.getName() + " on StockGUI close");
            return;
        }

        String shopId = (String) state.get("shopId");
        Shop shop = plugin.getShopManager().getShopById(shopId);
        if (shop == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("shop-not-found"));
            guiStates.remove(playerUUID);
            plugin.getLogger().warning("Shop not found for ID " + shopId + " on StockGUI close for " + player.getName());
            return;
        }

        guiStates.remove(playerUUID);
        plugin.getLogger().info("Inventory closed for " + player.getName() + ", cleared state");
    }

    public static void clearState(Player player) {
        String playerUUID = player.getUniqueId().toString();
        guiStates.remove(playerUUID);
        if (plugin != null) {
            plugin.getLogger().info("Cleared StockGUI state for " + player.getName());
        } else {
            Bukkit.getLogger().info("StockGUI: Cleared state for " + player.getName() + " (plugin not initialized)");
        }
    }
}