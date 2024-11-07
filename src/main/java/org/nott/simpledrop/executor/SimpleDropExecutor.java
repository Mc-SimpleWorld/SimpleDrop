package org.nott.simpledrop.executor;

import lombok.Data;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.nott.simpledrop.SimpleDropPlugin;
import org.nott.simpledrop.global.GlobalFactory;
import org.nott.simpledrop.utils.Permissions;
import org.nott.simpledrop.utils.SwUtil;

import java.util.Collections;
import java.util.List;

/**
 * @author Nott
 * @date 2024-10-12
 */
@Data
public class SimpleDropExecutor implements TabExecutor {

    private SimpleDropPlugin plugin;

    public SimpleDropExecutor(SimpleDropPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 0) {
            throw new CommandException("未知命令");
        }

        String arg = args[0];
        switch (arg){
            default -> {
                throw new CommandException("未知命令");
            }
            case "reload" -> reloadCommand(commandSender);
        }

        return true;
    }

    private void reloadCommand(CommandSender commandSender){
        if(Permissions.hasPermission(commandSender,"simpledrop.admin")){
            plugin.pluginInit();
            boolean isConsole = "console".equalsIgnoreCase(commandSender.getName());
            if (isConsole) {
                SwUtil.log(SimpleDropPlugin.MESSAGE_YML_FILE.getString(GlobalFactory.COMMON_MSG_SUFFIX + "reloaded"));
            } else {
                commandSender.spigot().sendMessage(TextComponent.fromLegacy(net.md_5.bungee.api.ChatColor.GREEN + SwUtil.retMessage(SimpleDropPlugin.MESSAGE_YML_FILE, GlobalFactory.COMMON_MSG_SUFFIX, "reloaded")));
            }
        }else {
            commandSender.sendMessage(ChatColor.RED + SwUtil.retMessage(SimpleDropPlugin.MESSAGE_YML_FILE, GlobalFactory.COMMON_MSG_SUFFIX, "not_per"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if(args.length == 1 && Permissions.hasPermission(commandSender,"simpledrop.admin")){
            return Collections.singletonList("reload");
        }
        return null;
    }
}
