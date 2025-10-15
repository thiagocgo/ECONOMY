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

import java.util.Arrays;

public class SelectItemGUI {
    public static void open(Player player, Shop shop, ArcaneMoney plugin) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.LIGHT_PURPLE + "Loja ArcaneMC - Gerenciar [" + shop.getId() + "]");

        int slot = 0;
        for (Shop.ShopItem item : shop.getItems().values()) {
            if (slot >= 45) break;
            ItemStack itemStack = new ItemStack(Material.matchMaterial(item.getItemTag().split(":")[1].toUpperCase()), item.getQuantity());
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + item.getItemTag().split(":")[1].toUpperCase());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Preço: " + (item.getBuyPrice() > 0 ? (long) item.getBuyPrice() : "Não definido"),
                    ChatColor.GRAY + "Quantidade por compra: " + item.getQuantity(),
                    ChatColor.GRAY + "Estoque: " + (shop.isAdminShop() ? "Ilimitado" : item.getStock()),
                    ChatColor.YELLOW + "Clique esquerdo para gerenciar",
                    ChatColor.RED + "Clique direito para remover"
            ));
            itemStack.setItemMeta(meta);
            inventory.setItem(slot++, itemStack);
        }

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Voltar");
        backButton.setItemMeta(backMeta);
        inventory.setItem(45, backButton);

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Fechar");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(53, closeButton);

        player.openInventory(inventory);
    }

    public static void handleClick(Player player, Shop shop, Inventory inventory, ItemStack clickedItem, int slot, ClickType clickType, ArcaneMoney plugin) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (slot == 45 && clickedItem.getType() == Material.ARROW) {
            if (shop.isAdminShop()) {
                ShopManageGUI.open(player, shop, plugin);
            } else {
                ShopManageGUI.open(player, shop, plugin);
            }
            return;
        } else if (slot == 53 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (slot >= 45) return;

        String itemTag = "minecraft:" + clickedItem.getType().name().toLowerCase();
        if (clickType == ClickType.RIGHT) {
            plugin.getShopManager().removeItemFromShop(shop, itemTag, player);
            open(player, shop, plugin); // Refresh GUI
        } else if (clickType == ClickType.LEFT) {
            QuantityGUI.open(player, shop, new ItemStack(clickedItem.getType()), plugin, "Gerenciar");
        }
    }
}