package com.thiagocgo.arcanemoney.shops.commands;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CreateShopCommand implements CommandExecutor {

    private final ArcaneMoney plugin;
    private final Map<UUID, Material> lastUsedColor = new HashMap<>(); // Armazena a última cor usada por jogador

    public CreateShopCommand(ArcaneMoney plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager configManager = plugin.getConfigManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Verificar permissão arcanemoney.vip
        if (!player.hasPermission("arcanemoney.vip")) {
            player.sendMessage(configManager.getMessage("no-vip-permission"));
            return true;
        }

        // Determinar a cor da shulker
        Material shulkerMaterial;
        if (player.hasPermission("arcanemoney.admin")) {
            shulkerMaterial = Material.RED_SHULKER_BOX; // Vermelha para admins
        } else {
            shulkerMaterial = getRandomShulkerColor(player.getUniqueId());
        }

        ItemStack shopShulker = new ItemStack(shulkerMaterial);
        ItemMeta meta = shopShulker.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Loja ArcaneMC");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Proprietário: " + player.getName(),
                ChatColor.GRAY + "UUID: " + player.getUniqueId().toString(),
                ChatColor.YELLOW + "Coloque para criar sua loja!"
        ));
        shopShulker.setItemMeta(meta);

        if (player.getInventory().addItem(shopShulker).isEmpty()) {
            player.sendMessage(configManager.getMessage("shop-create-success"));
        } else {
            player.sendMessage(configManager.getMessage("insufficient-inventory-space"));
        }
        return true;
    }

    private Material getRandomShulkerColor(UUID playerUUID) {
        Material[] shulkerColors = {
                Material.PURPLE_SHULKER_BOX,
                Material.BLUE_SHULKER_BOX,
                Material.GREEN_SHULKER_BOX,
                Material.CYAN_SHULKER_BOX,
                Material.LIGHT_BLUE_SHULKER_BOX,
                Material.YELLOW_SHULKER_BOX,
                Material.LIME_SHULKER_BOX,
                Material.PINK_SHULKER_BOX,
                Material.GRAY_SHULKER_BOX,
                Material.LIGHT_GRAY_SHULKER_BOX,
                Material.MAGENTA_SHULKER_BOX,
                Material.ORANGE_SHULKER_BOX,
                Material.WHITE_SHULKER_BOX,
                Material.BLACK_SHULKER_BOX,
                Material.BROWN_SHULKER_BOX
        };
        Material lastColor = lastUsedColor.get(playerUUID);
        Material newColor;
        Random random = new Random();
        do {
            newColor = shulkerColors[random.nextInt(shulkerColors.length)];
        } while (newColor == lastColor); // Evitar repetir a última cor
        lastUsedColor.put(playerUUID, newColor);
        return newColor;
    }
}