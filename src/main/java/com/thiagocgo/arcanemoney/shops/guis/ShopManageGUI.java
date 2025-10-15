package com.thiagocgo.arcanemoney.shops.guis;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.shops.utils.Shop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopManageGUI {
    public static void open(Player player, Shop shop, ArcaneMoney plugin) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.LIGHT_PURPLE + "Loja ArcaneMC - Gerenciar [" + shop.getId() + "]");

        ItemStack manageButton = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta manageMeta = manageButton.getItemMeta();
        manageMeta.setDisplayName(ChatColor.YELLOW + "Gerenciar Loja");
        manageButton.setItemMeta(manageMeta);
        inventory.setItem(13, manageButton);

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Fechar");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(26, closeButton);

        player.openInventory(inventory);
    }

    public static void handleClick(Player player, Shop shop, Inventory inventory, ItemStack clickedItem, int slot, ClickType clickType, ArcaneMoney plugin) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.getType() == Material.YELLOW_WOOL) {
            SelectItemGUI.open(player, shop, plugin);
        } else if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }
}