package com.thiagocgo.arcanemoney.economy.utils;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvestmentManager {

    private final ArcaneMoney plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private static final long YIELD_INTERVAL = 24 * 60 * 60 * 1000; // 24 horas em milissegundos

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
        }.runTaskTimerAsynchronously(plugin, 0L, 20 * 60 * 5); // Executa a cada 5 minutos
    }

    private void processYields() {
        FileConfiguration vipsStaff = configManager.getVipsStaff();
        long currentTime = System.currentTimeMillis();
        double yieldRate = vipsStaff.getDouble("investment.yield-rate", 0.005); // 0,5% por padrão
        double yieldLimit = vipsStaff.getDouble("investment.yield-limit", 1000.0); // 1000 por padrão

        // Processar VIPs
        processGroupYields("vips", currentTime, yieldRate, yieldLimit);
        // Processar Staff
        processGroupYields("staff", currentTime, yieldRate, yieldLimit);

        configManager.saveVipsStaff();
    }

    private void processGroupYields(String group, long currentTime, double yieldRate, double yieldLimit) {
        FileConfiguration vipsStaff = configManager.getVipsStaff();
        List<Map<String, Object>> members = configManager.getSafeMapList(vipsStaff, group);
        List<Map<String, Object>> updatedMembers = new ArrayList<>();

        for (Map<String, Object> entry : members) {
            String uuid = (String) entry.get("uuid");
            if (uuid == null || uuid.trim().isEmpty()) {
                plugin.getLogger().warning("Invalid or empty UUID in " + group + " list. Skipping entry.");
                continue; // Ignorar UUIDs inválidos
            }

            long lastYield = entry.containsKey("last_yield") ? ((Number) entry.get("last_yield")).longValue() : 0L;
            double lastAmount = entry.containsKey("last_amount") ? ((Number) entry.get("last_amount")).doubleValue() : 0.0;

            if (currentTime - lastYield >= YIELD_INTERVAL) {
                try {
                    Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                    if (player != null && player.isOnline()) {
                        double balance = economyManager.getBalance(player);
                        double yield = Math.min(balance * yieldRate, yieldLimit); // Aplica o limite
                        yield = Math.floor(yield); // Arredonda para baixo para evitar decimais
                        Map<String, Object> updatedEntry = new HashMap<>(entry);
                        if (yield > 0) {
                            economyManager.processTransaction(player, yield, "Daily yield for " + group);
                            player.sendMessage(configManager.getMessage("yield-processed")
                                    .replace("%amount%", String.valueOf((long) yield))
                                    .replace("%currency%", economyManager.getCurrencyName()));
                            updatedEntry.put("last_amount", yield);
                        } else {
                            player.sendMessage(configManager.getMessage("yield-no-gain"));
                            updatedEntry.put("last_amount", 0.0);
                        }
                        updatedEntry.put("last_yield", currentTime);
                        updatedMembers.add(updatedEntry);
                    } else {
                        updatedMembers.add(entry); // Manter entrada inalterada se jogador offline
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in " + group + ": " + uuid);
                    updatedMembers.add(entry); // Manter entrada inalterada
                }
            } else {
                updatedMembers.add(entry); // Manter entrada inalterada se não é hora de processar
            }
        }

        vipsStaff.set(group, updatedMembers);
    }

    public void sendYieldStatus(Player player) {
        FileConfiguration vipsStaff = configManager.getVipsStaff();
        long currentTime = System.currentTimeMillis();
        String uuid = player.getUniqueId().toString();
        boolean isEligible = false;
        Map<String, Object> entry = null;

        // Verificar se é VIP
        for (Map<String, Object> vip : configManager.getSafeMapList(vipsStaff, "vips")) {
            if (uuid.equals(vip.get("uuid"))) {
                isEligible = true;
                entry = vip;
                break;
            }
        }

        // Verificar se é Staff
        if (!isEligible) {
            for (Map<String, Object> staff : configManager.getSafeMapList(vipsStaff, "staff")) {
                if (uuid.equals(staff.get("uuid"))) {
                    isEligible = true;
                    entry = staff;
                    break;
                }
            }
        }

        // Se não é elegível, não enviar mensagem
        if (!isEligible) {
            return;
        }

        // Verificar status do rendimento
        long lastYield = entry.containsKey("last_yield") ? ((Number) entry.get("last_yield")).longValue() : 0L;
        double lastAmount = entry.containsKey("last_amount") ? ((Number) entry.get("last_amount")).doubleValue() : 0.0;

        List<Map<String, Object>> groupList = configManager.getSafeMapList(vipsStaff, isEligible && entry.get("uuid").equals(configManager.getSafeMapList(vipsStaff, "vips").stream()
                .filter(e -> uuid.equals(e.get("uuid"))).findFirst().orElse(null)) ? "vips" : "staff");
        Map<String, Object> updatedEntry = new HashMap<>(entry);

        if (currentTime - lastYield < YIELD_INTERVAL) {
            // Já recebeu hoje
            if (lastAmount > 0) {
                player.sendMessage(configManager.getMessage("yield-already-received")
                        .replace("%amount%", String.valueOf((long) lastAmount))
                        .replace("%currency%", economyManager.getCurrencyName()));
            } else {
                player.sendMessage(configManager.getMessage("yield-no-gain"));
            }
        } else {
            // Processar rendimento imediatamente se elegível
            double balance = economyManager.getBalance(player);
            double yieldRate = vipsStaff.getDouble("investment.yield-rate", 0.005);
            double yieldLimit = vipsStaff.getDouble("investment.yield-limit", 1000.0);
            double yield = Math.min(balance * yieldRate, yieldLimit);
            yield = Math.floor(yield); // Arredonda para baixo
            if (yield > 0) {
                economyManager.processTransaction(player, yield, "Daily yield on join");
                player.sendMessage(configManager.getMessage("yield-processed")
                        .replace("%amount%", String.valueOf((long) yield))
                        .replace("%currency%", economyManager.getCurrencyName()));
                updatedEntry.put("last_amount", yield);
            } else {
                player.sendMessage(configManager.getMessage("yield-no-gain"));
                updatedEntry.put("last_amount", 0.0);
            }
            updatedEntry.put("last_yield", currentTime);

            // Atualizar a entrada na lista
            for (int i = 0; i < groupList.size(); i++) {
                if (groupList.get(i).get("uuid").equals(uuid)) {
                    groupList.set(i, updatedEntry);
                    break;
                }
            }
            vipsStaff.set(isEligible && entry.get("uuid").equals(configManager.getSafeMapList(vipsStaff, "vips").stream()
                    .filter(e -> uuid.equals(e.get("uuid"))).findFirst().orElse(null)) ? "vips" : "staff", groupList);
            configManager.saveVipsStaff();
        }
    }

    public boolean isVipOrInvestor(Player player) {
        FileConfiguration vipsStaff = configManager.getVipsStaff();
        String uuid = player.getUniqueId().toString();
        for (Map<String, Object> vip : configManager.getSafeMapList(vipsStaff, "vips")) {
            if (uuid.equals(vip.get("uuid"))) {
                return true;
            }
        }
        for (Map<String, Object> staff : configManager.getSafeMapList(vipsStaff, "staff")) {
            if (uuid.equals(staff.get("uuid"))) {
                return true;
            }
        }
        return false;
    }
}