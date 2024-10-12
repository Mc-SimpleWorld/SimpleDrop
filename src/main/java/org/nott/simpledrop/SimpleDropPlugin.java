package org.nott.simpledrop;


import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.nott.simpledrop.global.GlobalFactory;
import org.nott.simpledrop.global.KeyWord;
import org.nott.simpledrop.listener.SwDeathListener;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public final class SimpleDropPlugin extends JavaPlugin {

    public final Logger logger = super.getLogger();

    public static YamlConfiguration MESSAGE_YML_FILE;

    public static YamlConfiguration CONFIG_YML_FILE;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        MESSAGE_YML_FILE = (YamlConfiguration) config;
        PluginManager pluginManager = this.getServer().getPluginManager();
        YamlConfiguration message = new YamlConfiguration();
        try {
            message.load(Objects.requireNonNull(this.getTextResource(GlobalFactory.MESSAGE_YML)));
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        if (config.getBoolean(KeyWord.CONFIG.DROP_ENABLE)) {
            pluginManager.registerEvents(new SwDeathListener(), this);
            logger.info(message.getString(KeyWord.CONFIG.REG_DEATH));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logger.info(MESSAGE_YML_FILE.getString(KeyWord.CONFIG.DIS_REG_DEATH));
    }
}
