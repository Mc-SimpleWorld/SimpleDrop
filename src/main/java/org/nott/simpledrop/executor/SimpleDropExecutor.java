package org.nott.simpledrop.executor;

import lombok.Data;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.nott.simpledrop.SimpleDropPlugin;
import org.nott.simpledrop.global.GlobalFactory;
import org.nott.simpledrop.utils.SwUtil;

/**
 * @author Nott
 * @date 2024-10-12
 */
@Data
public class SimpleDropExecutor implements CommandExecutor {

    private SimpleDropPlugin plugin;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 1 && "reload".equals(args[0])) {
            if (commandSender.isOp()) {
                plugin.pluginInit();
                commandSender.spigot().sendMessage(TextComponent.fromLegacy(net.md_5.bungee.api.ChatColor.GREEN + SwUtil.retMessage(SimpleDropPlugin.MESSAGE_YML_FILE, GlobalFactory.COMMON_MSG_SUFFIX, "reloaded")));
                return true;
            } else {
                commandSender.sendMessage(ChatColor.RED + SwUtil.retMessage(SimpleDropPlugin.MESSAGE_YML_FILE, GlobalFactory.COMMON_MSG_SUFFIX, "not_per"));
                return true;
            }
        }
        return false;
    }
}
