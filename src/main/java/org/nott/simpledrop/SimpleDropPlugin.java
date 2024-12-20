package org.nott.simpledrop;


import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.nott.simpledrop.executor.OfferExecutor;
import org.nott.simpledrop.executor.SimpleDropExecutor;
import org.nott.simpledrop.global.GlobalFactory;
import org.nott.simpledrop.global.KeyWord;
import org.nott.simpledrop.listener.DropDeathListener;
import org.nott.simpledrop.listener.OfferDeathListener;
import org.nott.simpledrop.manager.SqlLiteManager;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public final class SimpleDropPlugin extends JavaPlugin {

    public final Logger logger = super.getLogger();

    public static YamlConfiguration MESSAGE_YML_FILE;

    public static YamlConfiguration CONFIG_YML_FILE;

    public static Economy ECONOMY;

    public static BukkitScheduler SCHEDULER;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        this.pluginInit();
        PluginManager pluginManager = this.getServer().getPluginManager();
        SCHEDULER = this.getServer().getScheduler();
        if (CONFIG_YML_FILE.getBoolean(KeyWord.CONFIG.DROP_ENABLE)) {
            pluginManager.registerEvents(new DropDeathListener(this), this);
            this.getCommand(KeyWord.COMMAND.SD).setExecutor(new SimpleDropExecutor(this));
            logger.info(MESSAGE_YML_FILE.getString(KeyWord.CONFIG.REG_DEATH));
        }
        if (CONFIG_YML_FILE.getBoolean(KeyWord.CONFIG.OFFER_ENABLE)) {
            SqlLiteManager.checkDbFileIsExist(this);
            SqlLiteManager.createTableIfNotExist(KeyWord.TABLE.OFFER,KeyWord.TABLE.OFFER_CREATE_SQL);
            pluginManager.registerEvents(new OfferDeathListener(this), this);
            this.getCommand(KeyWord.COMMAND.OFFER).setExecutor(new OfferExecutor(this));
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            ECONOMY = rsp.getProvider();
            logger.info(MESSAGE_YML_FILE.getString(KeyWord.CONFIG.REG_OFFER));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logger.info(MESSAGE_YML_FILE.getString(KeyWord.CONFIG.DIS_REG_DEATH));
        logger.info(MESSAGE_YML_FILE.getString(KeyWord.CONFIG.DIS_REG_OFFER));
    }

    public void pluginInit(){
        YamlConfiguration message = new YamlConfiguration();
        YamlConfiguration config = new YamlConfiguration();
        String path = this.getDataFolder() + File.separator + GlobalFactory.MESSAGE_YML;
        String configPath = this.getDataFolder() + File.separator + GlobalFactory.CONFIG_YML;
        File file = new File(path);
        File configFile = new File(configPath);
        if (!file.exists()) {
            this.saveResource(GlobalFactory.MESSAGE_YML, false);
            try {
                message.load(Objects.requireNonNull(this.getTextResource(GlobalFactory.MESSAGE_YML)));
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                message.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        if (!configFile.exists()) {
            this.saveResource(GlobalFactory.MESSAGE_YML, false);
            try {
                config.load(Objects.requireNonNull(this.getTextResource(GlobalFactory.MESSAGE_YML)));
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                config.load(configFile);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        MESSAGE_YML_FILE = message;
        CONFIG_YML_FILE = config;
    }
}
