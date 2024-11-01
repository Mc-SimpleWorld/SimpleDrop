package org.nott.simpledrop.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nott.simpledrop.SimpleDropPlugin;

/**
 * @author Nott
 * @date 2024-11-1
 */
public class Permissions {

    public static boolean hasPermission(CommandSender sender, String node) {
        return hasPermission(sender, node, false);
    }

    public static boolean hasPermission(CommandSender sender, String node, boolean silent) {
        if (sender instanceof Player player) {
            boolean hasPermission = player.hasPermission(node);
            if (!hasPermission && !silent)
               MessageUtils.errorMessage(true, player, SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.not_per"));
            return hasPermission;
        }
        return true;
    }

}
