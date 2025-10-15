package com.thiagocgo.arcanemoney.economy.utils;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final ArcaneMoney plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration economy;
    private FileConfiguration bossBar;
    private FileConfiguration trades;
    private FileConfiguration shops;
    private File messagesFile;
    private File economyFile;
    private File bossBarFile;
    private File tradesFile;
    private File shopsFile;

    public ConfigManager(ArcaneMoney plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public ArcaneMoney getPlugin() {
        return plugin;
    }

    public void setupMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.getLogger().info("Loaded messages.yml");
    }

    public void setupEconomy() {
        economyFile = new File(plugin.getDataFolder(), "economy.yml");
        if (!economyFile.exists()) {
            plugin.saveResource("economy.yml", false);
        }
        economy = YamlConfiguration.loadConfiguration(economyFile);
        plugin.getLogger().info("Loaded economy.yml");
    }

    public void setupBossBar() {
        bossBarFile = new File(plugin.getDataFolder(), "bossbar.yml");
        if (!bossBarFile.exists()) {
            plugin.saveResource("bossbar.yml", false);
        }
        bossBar = YamlConfiguration.loadConfiguration(bossBarFile);
        plugin.getLogger().info("Loaded bossbar.yml");
    }

    public void setupTrades() {
        tradesFile = new File(plugin.getDataFolder(), "trades.yml");
        if (!tradesFile.exists()) {
            plugin.saveResource("trades.yml", false);
        }
        trades = YamlConfiguration.loadConfiguration(tradesFile);
        if (!trades.contains("trades") || trades.getConfigurationSection("trades").getKeys(false).isEmpty()) {
            plugin.saveResource("trades.yml", true);
            trades = YamlConfiguration.loadConfiguration(tradesFile);
        }
        plugin.getLogger().info("Loaded trades.yml");
    }

    public void setupShops() {
        shopsFile = new File(plugin.getDataFolder(), "shops.yml");
        if (!shopsFile.exists()) {
            plugin.saveResource("shops.yml", false);
        }
        shops = YamlConfiguration.loadConfiguration(shopsFile);
        plugin.getLogger().info("Loaded shops.yml");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getEconomy() {
        return economy;
    }

    public FileConfiguration getBossBar() {
        return bossBar;
    }

    public FileConfiguration getTrades() {
        return trades;
    }

    public FileConfiguration getShops() {
        return shops;
    }

    public void saveConfig() {
        try {
            plugin.saveConfig();
            plugin.getLogger().info("Saved config.yml");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save config.yml: " + e.getMessage());
        }
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
            plugin.getLogger().info("Saved messages.yml");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save messages.yml: " + e.getMessage());
        }
    }

    public void saveEconomy() {
        try {
            economy.save(economyFile);
            plugin.getLogger().info("Saved economy.yml");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save economy.yml: " + e.getMessage());
        }
    }

    public void saveBossBar() {
        try {
            bossBar.save(bossBarFile);
            plugin.getLogger().info("Saved bossbar.yml");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save bossbar.yml: " + e.getMessage());
        }
    }

    public void saveTrades() {
        try {
            File tempFile = new File(plugin.getDataFolder(), "trades_default.yml");
            plugin.saveResource("trades.yml", true);
            FileConfiguration defaultTrades = YamlConfiguration.loadConfiguration(tempFile);

            FileConfiguration mergedTrades = new YamlConfiguration();
            if (defaultTrades.contains("trades")) {
                for (String key : defaultTrades.getConfigurationSection("trades").getKeys(false)) {
                    mergedTrades.set("trades." + key, defaultTrades.getString("trades." + key));
                }
            }
            if (trades.contains("trades")) {
                for (String key : trades.getConfigurationSection("trades").getKeys(false)) {
                    mergedTrades.set("trades." + key, trades.getString("trades." + key));
                }
            }

            mergedTrades.save(tradesFile);
            trades = YamlConfiguration.loadConfiguration(tradesFile);
            tempFile.delete();
            plugin.getLogger().info("Saved trades.yml");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save trades.yml: " + e.getMessage());
        }
    }

    public void saveShops() {
        try {
            shops.save(shopsFile);
            plugin.getLogger().info("Saved shops.yml");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save shops.yml: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        economy = YamlConfiguration.loadConfiguration(economyFile);
        trades = YamlConfiguration.loadConfiguration(tradesFile);
        shops = YamlConfiguration.loadConfiguration(shopsFile);
        if (!trades.contains("trades") || trades.getConfigurationSection("trades").getKeys(false).isEmpty()) {
            plugin.saveResource("trades.yml", true);
            trades = YamlConfiguration.loadConfiguration(tradesFile);
        }
        plugin.getLogger().info("Reloaded all configuration files");
    }

    public String getMessage(String path) {
        String message = messages.getString("messages." + path, "&cMessage not found: " + path);
        return applyPrefix(colorize(message));
    }

    public List<String> getMessageList(String path) {
        List<String> messagesList = messages.getStringList("messages." + path);
        messagesList.replaceAll(this::colorize);
        messagesList.replaceAll(this::applyPrefix);
        return messagesList;
    }

    private String applyPrefix(String message) {
        String prefix = colorize(messages.getString("prefix", ""));
        return message.replace("%prefix%", prefix);
    }

    private String colorize(String message) {
        return message.replace("&", "§");
    }

    public List<Map<String, Object>> getSafeMapList(FileConfiguration config, String path) {
        List<?> rawList = config.getList(path, new ArrayList<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map) {
                Map<?, ?> rawMap = (Map<?, ?>) item;
                Map<String, Object> safeMap = new HashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() instanceof String) {
                        safeMap.put((String) entry.getKey(), entry.getValue());
                    }
                }
                result.add(safeMap);
            }
        }
        return result;
    }

    public int getShopStockLimit() {
        return config.getInt("shops.stock-limit", 640);
    }
}