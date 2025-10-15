package com.thiagocgo.arcanemoney;

import com.thiagocgo.arcanemoney.economy.commands.ArcaneMoneyCommand;
import com.thiagocgo.arcanemoney.economy.commands.BalanceCommand;
import com.thiagocgo.arcanemoney.economy.commands.TradeCommands;
import com.thiagocgo.arcanemoney.economy.commands.TransferCommand;
import com.thiagocgo.arcanemoney.economy.listeners.PlayerListener;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.economy.utils.EconomyManager;
import com.thiagocgo.arcanemoney.economy.utils.InvestmentManager;
import com.thiagocgo.arcanemoney.economy.utils.BossBarManager;
import com.thiagocgo.arcanemoney.shops.commands.CreateShopCommand;
import com.thiagocgo.arcanemoney.shops.commands.ListShopsCommand;
import com.thiagocgo.arcanemoney.shops.guis.PriceGUI;
import com.thiagocgo.arcanemoney.shops.listeners.ShopBlockListener;
import com.thiagocgo.arcanemoney.shops.listeners.ShopInventoryListener;
import com.thiagocgo.arcanemoney.shops.utils.ShopManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ArcaneMoney extends JavaPlugin {

    private EconomyManager economyManager;
    private ConfigManager configManager;
    private InvestmentManager investmentManager;
    private BossBarManager bossBarManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        // Set default configuration values
        saveDefaultConfig();
        getConfig().addDefault("shops.stock-limit", 640);
        getConfig().addDefault("investment.yield-rate", 0.05); // Adicionado para testes
        getConfig().addDefault("investment.yield-limit", 1000.0);
        getConfig().options().copyDefaults(true);
        saveConfig();

        configManager = new ConfigManager(this);
        configManager.setupMessages();
        configManager.setupEconomy();
        configManager.setupBossBar();
        configManager.setupTrades();
        configManager.setupShops();
        economyManager = new EconomyManager(configManager);
        investmentManager = new InvestmentManager(this);
        bossBarManager = new BossBarManager(this);
        shopManager = new ShopManager(this);
        PriceGUI.setPlugin(this);

        // Register commands and tab completers
        getCommand("arcanemoney").setExecutor(new ArcaneMoneyCommand(this));
        getCommand("arcanemoney").setTabCompleter(new ArcaneMoneyCommand(this));
        getCommand("saldo").setExecutor(new BalanceCommand(this));
        getCommand("saldo").setTabCompleter(new BalanceCommand(this));
        getCommand("transferir").setExecutor(new TransferCommand(this));
        getCommand("transferir").setTabCompleter(new TransferCommand(this));
        getCommand("sell").setExecutor(new TradeCommands(this));
        getCommand("sell").setTabCompleter(new TradeCommands(this));
        getCommand("trade").setExecutor(new TradeCommands(this));
        getCommand("trade").setTabCompleter(new TradeCommands(this));
        getCommand("criarloja").setExecutor(new CreateShopCommand(this));
        getCommand("listlojas").setExecutor(new ListShopsCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopInventoryListener(this), this);


        bossBarManager.initializeBossBars();
        getLogger().info("ArcaneMoney enabled!");
    }

    @Override
    public void onDisable() {
        configManager.saveConfig();
        configManager.saveEconomy();
        configManager.saveBossBar();
        configManager.saveTrades();
        configManager.saveShops();
        bossBarManager.removeAllBossBars();
        getLogger().info("ArcaneMoney disabled!");
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public InvestmentManager getInvestmentManager() {
        return investmentManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}