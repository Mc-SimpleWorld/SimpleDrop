package org.nott.simpledrop.executor;

import lombok.Data;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.dbutils.DbUtils;
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
import java.util.Objects;

/**
 * @author Nott
 * @date 2024-10-12
 */

@Data
public class OfferExecutor implements CommandExecutor {

    private SimpleDropPlugin plugin;

    public OfferExecutor(SimpleDropPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 1 && "help".equals(args[0])) {
            List<String> list = SimpleDropPlugin.MESSAGE_YML_FILE.getStringList("help.offer");
            StringBuffer bf = new StringBuffer();
            for (String str : list) {
                bf.append(ChatColor.GOLD + str).append("\n");
            }
            commandSender.sendMessage(bf.toString());
            return true;
        }
        if ("page".equals(args[0])) {
            Integer pageSize = 10;
            boolean hasPageIndex = args.length >= 2;
            Integer page = 1;
            if (hasPageIndex) {
                String pageStr = args[1];
                try {
                    page = Integer.parseInt(pageStr);
                } catch (NumberFormatException e) {
                    SwUtil.sendMessage2Sender(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.page_format_error"), ChatColor.RED);
                    return true;
                }
            }
            final Integer queryPage = page;
            SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(getPlugin(), () -> {
                Connection con = null;
                PreparedStatement ps = null;
                try {
                    con = SqlLiteManager.getConnect();
                    ps = con.prepareStatement("select * from offer_info order by amount desc limit ? offset ?");
                    ps.setInt(1, pageSize);
                    ps.setInt(2, (queryPage - 1) * pageSize);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        String id = rs.getString("id");
                        int amount = rs.getInt("amount");
                        SwUtil.sendMessage2Sender(commandSender,
                                String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.list_info")), id, amount),
                                ChatColor.RED);
                    }
                    ps = con.prepareStatement("select count(1) as num from offer_info");
                    ResultSet resultSet = ps.executeQuery();
                    Integer totalRecord = 0;
                    while (resultSet.next()) {
                        totalRecord = resultSet.getInt("num");
                    }
                    Integer totalPage = (totalRecord + pageSize - 1) / pageSize;
                    if (totalPage > queryPage) {
                        SwUtil.sendMessage2Sender(true, commandSender,
                                String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.page_info")), queryPage, totalPage),
                                ChatColor.GREEN);
                        SwUtil.sendMessage2Sender(true, commandSender,
                                String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.page_next")), "/of page " + queryPage + 1),
                                ChatColor.GREEN);
                    } else {
                        SwUtil.sendMessage2Sender(true, commandSender,
                                SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.not_next"),
                                ChatColor.GREEN);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(con);
                    DbUtils.closeQuietly(ps);
                }
            });
            return true;
        }
        if (args.length == 2 && !"page".equals(args[0])) {
            if(checkIfConsole(commandSender))return true;
            String name = args[0];
            if (checkIfSelf(commandSender, name)) return true;
            String amount = args[1];
            int amountInt = 0;
            SimpleDropPlugin simpleDropPlugin = getPlugin();
            Player playerExact = simpleDropPlugin.getServer().getPlayerExact(name);
            try {
                amountInt = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                SwUtil.sendMessage2Sender(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.amount_invalid"), ChatColor.RED);
            }
            if (SwUtil.isNull(playerExact)) {
                String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.player_not_found")), name);
                SwUtil.sendMessage2Sender(true, commandSender, msg, ChatColor.YELLOW);
                return true;
            }
            String offer = commandSender.getName();
            Player offerPlayer = simpleDropPlugin.getServer().getPlayerExact(offer);
            boolean support4Towny = SimpleDropPlugin.CONFIG_YML_FILE.getBoolean("offer.support.towny", false);
            if (support4Towny && SwUtil.checkSupport4Towny("offer", offerPlayer, playerExact)) {
                String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.same_group")), name);
                SwUtil.sendMessage2Sender(true, commandSender, msg, ChatColor.RED);
                return true;
            }
            if (!withdrawCommandSender(commandSender, simpleDropPlugin, amountInt)) {
                return true;
            }
            final Integer finalAmount = amountInt;
            SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(simpleDropPlugin, () -> {
                PreparedStatement ps = null;
                PreparedStatement pst = null;
                Connection con = null;
                try {
                    con = SqlLiteManager.getConnect();
                    ps = con.prepareStatement("select * from offer_info where id = ?");
                    ps.setString(1, name);
                    ResultSet resultSet = ps.executeQuery();
                    if (resultSet.next()) {
                        String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.another_offering")), name);
                        SwUtil.sendMessage2Sender(true, commandSender, msg, ChatColor.YELLOW);
                        return;
                    }
                    pst = con.prepareStatement("insert into offer_info(id,amount) values (?,?)");
                    pst.setString(1, name);
                    pst.setInt(2, finalAmount);
                    pst.execute();
                    String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.create_offer")), name) + finalAmount;
                    SwUtil.sendMessage2Sender(true, commandSender, msg, ChatColor.GREEN);
                    SwUtil.broadcast(true, String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.new_offer")), name, finalAmount), ChatColor.GOLD);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(con);
                    DbUtils.closeQuietly(ps);
                    DbUtils.closeQuietly(pst);
                }
            });
            return true;
        }
        if (args.length == 3 && "add".equals(args[0])) {
            if(checkIfConsole(commandSender))return true;
            String name = args[1];
            String amount = args[2];
            SimpleDropPlugin simpleDropPlugin = getPlugin();
            Player playerExact = simpleDropPlugin.getServer().getPlayerExact(name);
            if (checkIfSelf(commandSender, name)) return true;
            int amountInt = 0;
            try {
                amountInt = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                SwUtil.sendMessage2Sender(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.amount_invalid"), ChatColor.RED);
            }
            if (SwUtil.isNull(playerExact)) {
                String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.player_not_found")), name);
                SwUtil.sendMessage2Sender(true, commandSender, msg, ChatColor.YELLOW);
                return true;
            }
            Player sender = (Player) commandSender;
            final Integer addAmount = amountInt;
            String offer = commandSender.getName();
            Player offerPlayer = simpleDropPlugin.getServer().getPlayerExact(offer);
            boolean support4Towny = SimpleDropPlugin.CONFIG_YML_FILE.getBoolean("offer.support.towny", false);
            if (support4Towny && SwUtil.checkSupport4Towny("offer", offerPlayer, playerExact)) {
                String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.same_group")), name);
                SwUtil.sendMessage2Sender(true, commandSender, msg, ChatColor.RED);
                return true;
            }
            if (!withdrawCommandSender(commandSender, simpleDropPlugin, addAmount)) {
                return true;
            }
            SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(simpleDropPlugin, () -> {
                Connection con = null;
                PreparedStatement preparedStatement = null;
                int originAmount = 0;
                try {
                    con = SqlLiteManager.getConnect();
                    PreparedStatement ps = con.prepareStatement("select * from offer_info where id = ?");
                    ps.setString(1, name);
                    ResultSet resultSet = ps.executeQuery();
                    if (!resultSet.next()) {
                        String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.offer_end")), name);
                        SwUtil.sendMessage2Sender(true, commandSender, msg, ChatColor.YELLOW);
                        return;
                    }else {
                        originAmount = resultSet.getInt("amount");
                    }

                    int total = originAmount + addAmount;
                    while (resultSet.next()) {
                        total += resultSet.getInt("amount");
                    }
                    preparedStatement = con.prepareStatement("update offer_info set amount = ? where id = ? and amount = ?");
                    preparedStatement.setInt(1, total);
                    preparedStatement.setString(2, name);
                    preparedStatement.setInt(3, originAmount);
                    int effect = preparedStatement.executeUpdate();
                    if (effect > 0) {
                        String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.add_offer_success")), name);
                        SwUtil.sendMessage2Sender(true, commandSender, msg, ChatColor.GREEN);
                        SwUtil.broadcast(String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.add_offer")), name, total), ChatColor.GOLD);
                    } else {
                        SwUtil.sendMessage2Sender(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.concurrence_offer"), ChatColor.RED);
                        refund(sender,SimpleDropPlugin.ECONOMY,addAmount);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(con);
                    DbUtils.closeQuietly(preparedStatement);
                }
            });
            return true;
        }
        return false;
    }

    private static boolean checkIfConsole(CommandSender commandSender) {
        if("console".equalsIgnoreCase(commandSender.getName())){
            SwUtil.sendMessage2Sender(true,commandSender,SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.only_support_player"),ChatColor.RED);
            return true;
        }
        return false;
    }

    private static boolean checkIfSelf(CommandSender commandSender, String name) {
        Player sender = (Player) commandSender;
        String senderName = sender.getName();
        if (name.equals(senderName)) {
            SwUtil.sendMessage2Sender(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.not_self"), ChatColor.RED);
            return true;
        }
        return false;
    }

    private static boolean withdrawCommandSender(CommandSender commandSender, SimpleDropPlugin simpleDropPlugin, int amountInt) {
        String offer = commandSender.getName();
        Player offerPlayer = simpleDropPlugin.getServer().getPlayerExact(offer);
        EconomyResponse resp = SimpleDropPlugin.ECONOMY.withdrawPlayer(offerPlayer, amountInt);
        if (!resp.transactionSuccess()) {
            SwUtil.sendMessage2Sender(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.bal_not_enough"), ChatColor.RED);
        }
        return resp.transactionSuccess();
    }

    private static void refund(Player player, Economy economy, int money){
        EconomyResponse economyResponse = economy.depositPlayer(player, money);
        if(!economyResponse.transactionSuccess()){
            SwUtil.log(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.refund_error") + economyResponse.errorMessage);
        }
    }

    private static boolean checkEnoughMoney(CommandSender commandSender, Economy economy, int money){
        Player player = (Player) commandSender;
        double balance = economy.getBalance(player);
        if (balance >= money) {
            SwUtil.sendMessage2Sender(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.bal_not_enough"), ChatColor.RED);
            return true;
        }
        return false;
    }
}
