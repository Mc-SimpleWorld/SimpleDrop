package org.nott.simpledrop.executor;

import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nott.simpledrop.SimpleDropPlugin;
import org.nott.simpledrop.manager.SqlLiteManager;
import org.nott.simpledrop.utils.SwUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * @author Nott
 * @date 2024-10-12
 */

@Data
public class OfferExecutor implements CommandExecutor {

    private SimpleDropPlugin plugin;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 1 && "help".equals(args[0])) {
            List<String> list = SimpleDropPlugin.CONFIG_YML_FILE.getStringList("help.offer");
            StringBuffer bf = new StringBuffer();
            for (String str : list) {
                bf.append(ChatColor.GOLD + str).append("\n");
            }
            commandSender.sendMessage(bf.toString());
            return true;
        }
        if (args.length == 2) {
            String name = args[0];
            String amount = args[1];
            int amountInt = 0;
            SimpleDropPlugin simpleDropPlugin = getPlugin();
            Player playerExact = simpleDropPlugin.getServer().getPlayerExact(name);
            try {
                amountInt = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                SwUtil.sendMessage2Sender(commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.amount_invalid"), ChatColor.RED);
            }
            if (SwUtil.isNull(playerExact)) {
                String msg = String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.player_not_found"), name);
                SwUtil.sendMessage2Sender(commandSender, msg, ChatColor.YELLOW);
                return true;
            }
            final Integer finalAmount = amountInt;
            SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(simpleDropPlugin, () -> {
                try {
                    Connection con = SqlLiteManager.getConnect();
                    PreparedStatement ps = con.prepareStatement("select * from offer_info where id = ?");
                    ps.setString(1, name);
                    ResultSet resultSet = ps.executeQuery();
                    if (resultSet.next()) {
                        String msg = String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.another_offering"), name);
                        SwUtil.sendMessage2Sender(commandSender, msg, ChatColor.YELLOW);
                        return;
                    }
                    PreparedStatement pst = con.prepareStatement("insert into offer_info(id,amount) values (?,?)");
                    pst.setString(1, name);
                    pst.setInt(2, finalAmount);
                    pst.execute();
                    String msg = String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.create_offer"), name) + finalAmount;
                    SwUtil.sendMessage2Sender(commandSender, msg, ChatColor.GREEN);
                    SwUtil.broadcast(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.new_offer"), ChatColor.GOLD);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        }
        if (args.length == 3 && "add".equals(args[0])) {
            String name = args[1];
            String amount = args[2];
            SimpleDropPlugin simpleDropPlugin = getPlugin();
            Player playerExact = simpleDropPlugin.getServer().getPlayerExact(name);
            int amountInt = 0;
            try {
                amountInt = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                SwUtil.sendMessage2Sender(commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.amount_invalid"), ChatColor.RED);
            }
            if (SwUtil.isNull(playerExact)) {
                String msg = String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.player_not_found"), name);
                SwUtil.sendMessage2Sender(commandSender, msg, ChatColor.YELLOW);
                return true;
            }
            final Integer originAmount = amountInt;
            SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(simpleDropPlugin, () -> {
                try {
                    Connection con = SqlLiteManager.getConnect();
                    PreparedStatement ps = con.prepareStatement("select * from offer_info where id = ?");
                    ps.setString(1, name);
                    ResultSet resultSet = ps.executeQuery();
                    if (!resultSet.next()) {
                        String msg = String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.offer_end"), name);
                        SwUtil.sendMessage2Sender(commandSender, msg, ChatColor.YELLOW);
                        return;
                    }
                    int total = originAmount;
                    while (resultSet.next()){
                        total += resultSet.getInt("amount");
                    }
                    PreparedStatement preparedStatement = con.prepareStatement("update offer_info set amount = ? where id = ? and amount = ?");
                    preparedStatement.setInt(1,total);
                    preparedStatement.setString(2,name);
                    preparedStatement.setInt(3,originAmount);
                    int effect = preparedStatement.executeUpdate();
                    if(effect > 0){
                        String msg = String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.add_offer_success"), name);
                        SwUtil.sendMessage2Sender(commandSender, msg, ChatColor.GREEN);
                        SwUtil.broadcast(String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.add_offer"), name,total), ChatColor.GOLD);
                    }else {
                        SwUtil.sendMessage2Sender(commandSender,SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.concurrence_offer"),ChatColor.RED);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        }
        return false;
    }
}
