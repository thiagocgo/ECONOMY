package com.thiagocgo.arcanemoney.economy.utils;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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

    public void startYieldTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                processYields();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20 * 60 * 5); // 5 minutos
    }

    private void processYields() {
        FileConfiguration vipsStaff = configManager.getVipsStaff();
        long currentTime = System.currentTimeMillis();
        double yieldRate = vipsStaff.getDouble("investment.yield-rate", 0.005);
        double yieldLimit = vipsStaff.getDouble("investment.yield-limit", 1000.0);

        // ✅ PROCESSA SÓ ONLINE! Permissões checam VIP/Staff
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVipOrInvestor(player)) {
                processPlayerYield(player, currentTime, yieldRate, yieldLimit);
            }
        }
    }

    private void processPlayerYield(Player player, long currentTime, double yieldRate, double yieldLimit) {
        String uuid = player.getUniqueId().toString();
        FileConfiguration vipsStaff = configManager.getVipsStaff();
        long lastYield = vipsStaff.getLong("investment." + uuid + ".last_yield", 0L);

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

            // Salva no YAML (só timestamp e amount)
            vipsStaff.set("investment." + uuid + ".last_yield", currentTime);
            vipsStaff.set("investment." + uuid + ".last_amount", yield);
            configManager.saveVipsStaff();
        }
    }

    public void sendYieldStatus(Player player) {
        // ✅ SÓ VIP/STAFF RECEBE (via permissão)
        if (!isVipOrInvestor(player)) return;

        FileConfiguration vipsStaff = configManager.getVipsStaff();
        long currentTime = System.currentTimeMillis();
        String uuid = player.getUniqueId().toString();
        long lastYield = vipsStaff.getLong("investment." + uuid + ".last_yield", 0L);
        double lastAmount = vipsStaff.getDouble("investment." + uuid + ".last_amount", 0.0);

        if (currentTime - lastYield < YIELD_INTERVAL) {
            if (lastAmount > 0) {
                player.sendMessage(configManager.getMessage("yield-already-received")
                        .replace("%amount%", String.valueOf((long) lastAmount))
                        .replace("%currency%", economyManager.getCurrencyName()));
            } else {
                player.sendMessage(configManager.getMessage("yield-no-gain"));
            }
        } else {
            // Processa imediatamente
            processPlayerYield(player, currentTime,
                    vipsStaff.getDouble("investment.yield-rate", 0.005),
                    vipsStaff.getDouble("investment.yield-limit", 1000.0));
        }
    }

    // ✅ 1 LINHA! MÁGICA DAS PERMISSÕES
    public boolean isVipOrInvestor(Player player) {
        return player.hasPermission("arcanemoney.vip") || player.hasPermission("arcanemoney.staff");
    }
}