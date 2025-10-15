package com.thiagocgo.arcanemoney.economy.utils;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;

public class InvestmentManager {

    private final ArcaneMoney plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private static final long YIELD_INTERVAL = 24 * 60 * 60 * 1000; // 24 horas

    public InvestmentManager(ArcaneMoney plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.economyManager = plugin.getEconomyManager();
    }

    // Removido startYieldTask, yield agora só no login

    private void processPlayerYield(Player player, long currentTime, double yieldRate, double yieldLimit) {
        String uuid = player.getUniqueId().toString();
        FileConfiguration config = configManager.getConfig();
        long lastYield = config.getLong("investment." + uuid + ".last_yield", 0L);

        if (currentTime - lastYield >= YIELD_INTERVAL) {
            double balance = economyManager.getBalance(player);
            double yield = Math.min(balance * yieldRate, yieldLimit);
            yield = Math.floor(yield);

            if (yield > 0) {
                economyManager.processTransaction(player, yield, "Daily yield");
                player.sendMessage(configManager.getMessage("yield-processed")
                        .replace("%amount%", String.valueOf((long) yield))
                        .replace("%currency%", economyManager.getCurrencyName()));
            } else {
                player.sendMessage(configManager.getMessage("yield-no-gain"));
            }

            config.set("investment." + uuid + ".last_yield", currentTime);
            config.set("investment." + uuid + ".last_amount", yield);
            configManager.saveConfig();
        }
    }

    public void sendYieldStatus(Player player) {
        if (!isVipOrInvestor(player)) return;

        FileConfiguration config = configManager.getConfig();
        long currentTime = System.currentTimeMillis();
        String uuid = player.getUniqueId().toString();
        long lastYield = config.getLong("investment." + uuid + ".last_yield", 0L);
        double lastAmount = config.getDouble("investment." + uuid + ".last_amount", 0.0);

        if (currentTime - lastYield < YIELD_INTERVAL) {
            if (lastAmount > 0) {
                player.sendMessage(configManager.getMessage("yield-already-received")
                        .replace("%amount%", String.valueOf((long) lastAmount))
                        .replace("%currency%", economyManager.getCurrencyName()));
            } else {
                player.sendMessage(configManager.getMessage("yield-no-gain"));
            }
        } else {
            processPlayerYield(player, currentTime,
                    config.getDouble("investment.yield-rate", 0.005), // 0.5% para produção
                    config.getDouble("investment.yield-limit", 1000.0));
        }
    }

    public boolean isVipOrInvestor(Player player) {
        return player.hasPermission("arcanemoney.vip") || player.hasPermission("arcanemoney.staff");
    }
}