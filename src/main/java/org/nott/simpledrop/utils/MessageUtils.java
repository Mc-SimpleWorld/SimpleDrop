package org.nott.simpledrop.utils;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nott.simpledrop.SimpleDropPlugin;

import javax.annotation.Nullable;

/**
 * @author Nott
 * @date 2024-11-1
 */
public class MessageUtils {

    private final static ChatColor SUCCESS_COLOR = ChatColor.GREEN;
    private final static ChatColor FAILED_COLOR = ChatColor.RED;
    private final static ChatColor WARN_COLOR = ChatColor.GOLD;

    public static void successMessage(boolean withPrefix, CommandSender sender,String message){
        if(withPrefix){
            message = getPrefix() + message;
        }
        sendMessage2Sender(withPrefix,sender,message,SUCCESS_COLOR);
    }

    public static void errorMessage(boolean withPrefix, CommandSender sender,String message){
        if(withPrefix){
            message = getPrefix() + message;
        }
        sendMessage2Sender(withPrefix,sender,message,FAILED_COLOR);
    }

    public static void sendMessage2Sender(CommandSender sender, String message, @Nullable ChatColor color) {
        sendMessage2Sender(false,sender,message,color);
    }

    public static void sendMessage2Sender(boolean withPrefix, CommandSender sender, String message, @Nullable ChatColor color) {
        String prefix = getPrefix();
        if(withPrefix){
            message = prefix + message;
        }
        sender.spigot().sendMessage(
                TextComponent.fromLegacy(color + prefix + message)
        );
    }

    public static void sendMessage(Player player, String msg, @Nullable ChatColor color){
        sendMessage(false, player, msg, color);
    }

    public static void successMessage(boolean withPrefix,Player player,String msg){
        String prefix = getPrefix();
        if (withPrefix) {
            msg = prefix + msg;
        }
        player.spigot().sendMessage(
                TextComponent.fromLegacy(SUCCESS_COLOR + msg)
        );
    }

    public static void warnMessage(boolean withPrefix,Player player,String msg){
        String prefix = getPrefix();
        if (withPrefix) {
            msg = prefix + msg;
        }
        player.spigot().sendMessage(
                TextComponent.fromLegacy(WARN_COLOR + msg)
        );
    }

    public static void sendMessage(boolean withPrefix,Player player,String msg,@Nullable ChatColor color){
        String prefix = getPrefix();
        if (withPrefix) {
            msg = prefix + msg;
        }
        player.spigot().sendMessage(
                TextComponent.fromLegacy(color + msg)
        );
    }

    private static String getPrefix() {
        String prefix = "[" + SimpleDropPlugin.MESSAGE_YML_FILE.getString("prefix") + "] ";
        return prefix;
    }

    public static void broadcast(String msg, ChatColor color) {
        broadcast(false,getPrefix() + msg,color);
    }

    public static void broadcast(boolean withPrefix, String msg, ChatColor color) {
        if(withPrefix){
            msg = getPrefix() + msg;
        }
        Bukkit.spigot().broadcast(
                TextComponent.fromLegacy(msg, color.asBungee())
        );
    }
}
