package com.thiagocgo.arcanemoney.economy.utils;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {

    private final ArcaneMoney plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final Map<UUID, BossBar> playerBossBars;

    public BossBarManager(ArcaneMoney plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.economyManager = plugin.getEconomyManager();
        this.playerBossBars = new HashMap<>();
    }

    public void initializeBossBars() {
        if (!isBossBarEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            createBossBar(player);
        }
    }

    public void createBossBar(Player player) {
        if (!isBossBarEnabled()) {
            return;
        }
        removeBossBar(player); // Remove qualquer BossBar existente
        String message = configManager.getBossBar().getString("message", "&6&lARCANE&5&lMC &e• &6&l%amount% Arcanas");
        message = message.replace("%amount%", String.valueOf((long) economyManager.getBalance(player)));
        message = colorize(message);
        String colorStr = configManager.getBossBar().getString("color", "GREEN").toUpperCase();
        String styleStr = configManager.getBossBar().getString("style", "SOLID").toUpperCase();
        BarColor color;
        BarStyle style;
        try {
            color = BarColor.valueOf(colorStr);
        } catch (IllegalArgumentException e) {
            color = BarColor.GREEN;
            plugin.getLogger().warning("Invalid BossBar color: " + colorStr + ". Using GREEN.");
        }
        try {
            style = BarStyle.valueOf(styleStr);
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
            plugin.getLogger().warning("Invalid BossBar style: " + styleStr + ". Using SOLID.");
        }
        BossBar bossBar = Bukkit.createBossBar(message, color, style);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0); // Barra cheia
        playerBossBars.put(player.getUniqueId(), bossBar);
    }

    public void updateBossBar(Player player) {
        if (!isBossBarEnabled()) {
            return;
        }
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            String message = configManager.getBossBar().getString("message", "&6&lARCANE&5&lMC &e• &6&l%amount% Arcanas");
            message = message.replace("%amount%", String.valueOf((long) economyManager.getBalance(player)));
            message = colorize(message);
            bossBar.setTitle(message);
        } else {
            createBossBar(player); // Recria se não existir
        }
    }

    public void removeBossBar(Player player) {
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void removeAllBossBars() {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        playerBossBars.clear();
    }

    private boolean isBossBarEnabled() {
        return configManager.getBossBar().getBoolean("enabled", true);
    }

    private String colorize(String message) {
        return message.replace("&", "§");
    }
}