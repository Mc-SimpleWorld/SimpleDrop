package org.nott.simpledrop.executor;

import lombok.Data;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.dbutils.DbUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.nott.simpledrop.SimpleDropPlugin;
import org.nott.simpledrop.manager.SqlLiteManager;
import org.nott.simpledrop.utils.MessageUtils;
import org.nott.simpledrop.utils.SwUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nott
 * @date 2024-10-12
 */

@Data
public class OfferExecutor implements TabExecutor {

    private SimpleDropPlugin plugin;

    public OfferExecutor(SimpleDropPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 0) {
            throw new CommandException("未知命令");
        }
        String commandType = args[0];

        switch (commandType){
            default -> offerPlayerCommand(commandSender,args);
            case "help" -> helpCommand(commandSender);
            case "page" -> pageCommand(commandSender,args);
            case "add" -> addCommand(commandSender,args);
        }

        return true;
    }

    private void addCommand(CommandSender commandSender, String[] args) {
        if(checkIfConsole(commandSender)) return;
        String name = args[1];
        String amount = args[2];
        SimpleDropPlugin simpleDropPlugin = getPlugin();
        Player playerExact = simpleDropPlugin.getServer().getPlayerExact(name);
        if (checkIfSelf(commandSender, name)) return;
        int amountInt = 0;
        try {
            amountInt = Integer.parseInt(amount);
        } catch (NumberFormatException e) {
            MessageUtils.errorMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.amount_invalid"));
        }
        if (SwUtil.isNull(playerExact)) {
            String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.player_not_found")), name);
            MessageUtils.errorMessage(true, commandSender, msg);
            return;
        }
        Player sender = (Player) commandSender;
        final Integer addAmount = amountInt;
        String offer = commandSender.getName();
        Player offerPlayer = simpleDropPlugin.getServer().getPlayerExact(offer);
        boolean support4Towny = SimpleDropPlugin.CONFIG_YML_FILE.getBoolean("offer.support.towny", false);
        if (support4Towny && SwUtil.checkSupport4Towny("offer", offerPlayer, playerExact)) {
            String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.same_group")), name);
            MessageUtils.errorMessage(true, commandSender, msg);
            return;
        }
        int tax = SimpleDropPlugin.CONFIG_YML_FILE.getInt("offer.tax",10);
        if (!withdrawCommandSender(commandSender, simpleDropPlugin, addAmount + tax)){
            return;
        }
        SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(simpleDropPlugin, () -> {
            Server server = Bukkit.getServer();
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
                    MessageUtils.errorMessage(true, commandSender, msg);
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
                    final Integer finalOfferAmount = total;
                    server.getOnlinePlayers().forEach(p -> {
                        String pName = p.getName();
                        if (name.equals(pName)) {
                            MessageUtils.warnMessage(true, p, String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.player_add_offered"), finalOfferAmount));
                        } else if (sender.getName().equals(pName)) {
                            MessageUtils.successMessage(true, commandSender, msg);
                            MessageUtils.successMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.offer_tax") + tax);
                        } else {
                            MessageUtils.successMessage(true, p, String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.add_offer")), name, finalOfferAmount));
                        }
                    });
                } else {
                    MessageUtils.errorMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.concurrence_offer"));
                    refund(sender,SimpleDropPlugin.ECONOMY,addAmount + tax);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                DbUtils.closeQuietly(con);
                DbUtils.closeQuietly(preparedStatement);
            }
        });
    }

    private void offerPlayerCommand(CommandSender commandSender, String[] args) {
        if(checkIfConsole(commandSender)) return;
        String name = args[0];
        if (checkIfSelf(commandSender, name)) return;
        String amount = args[1];
        int amountInt = 0;
        SimpleDropPlugin simpleDropPlugin = getPlugin();
        Player playerExact = simpleDropPlugin.getServer().getPlayerExact(name);
        try {
            amountInt = Integer.parseInt(amount);
        } catch (NumberFormatException e) {
            MessageUtils.errorMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.amount_invalid"));
        }
        if (SwUtil.isNull(playerExact)) {
            String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.player_not_found")), name);
            MessageUtils.errorMessage(true, commandSender, msg);
            return;
        }
        String offer = commandSender.getName();
        Player offerPlayer = simpleDropPlugin.getServer().getPlayerExact(offer);
        boolean support4Towny = SimpleDropPlugin.CONFIG_YML_FILE.getBoolean("offer.support.towny", false);
        if (support4Towny && SwUtil.checkSupport4Towny("offer", offerPlayer, playerExact)) {
            String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.same_group")), name);
            MessageUtils.errorMessage(true, commandSender, msg);
            return;
        }
        int tax = SimpleDropPlugin.CONFIG_YML_FILE.getInt("offer.tax",10);
        if (!withdrawCommandSender(commandSender, simpleDropPlugin, amountInt + tax)) {
            return;
        }
        final Integer finalAmount = amountInt;
        SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(simpleDropPlugin, () -> {
            Player sender = (Player) commandSender;
            Server server = Bukkit.getServer();
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
                    MessageUtils.errorMessage(true, commandSender, msg);
                    return;
                }
                pst = con.prepareStatement("insert into offer_info(id,amount) values (?,?)");
                pst.setString(1, name);
                pst.setInt(2, finalAmount);
                pst.execute();
                String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.create_offer")), name) + finalAmount;
                server.getOnlinePlayers().forEach(p -> {
                    String pName = p.getName();
                    if (name.equals(pName)) {
                        MessageUtils.warnMessage(true,p, String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.player_offered"), finalAmount));
                    } else if (sender.getName().equals(pName)) {
                        MessageUtils.successMessage(true, commandSender, msg);
                        MessageUtils.successMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.offer_tax") + tax);
                    } else {
                        MessageUtils.successMessage(true, p, String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.new_offer")), name, finalAmount));
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                DbUtils.closeQuietly(con);
                DbUtils.closeQuietly(ps);
                DbUtils.closeQuietly(pst);
            }
        });
    }

    private void pageCommand(CommandSender commandSender, String[] args) {
        Integer pageSize = 10;
        boolean hasPageIndex = args.length >= 2;
        Integer page = 1;
        if (hasPageIndex) {
            String pageStr = args[1];
            try {
                page = Integer.parseInt(pageStr);
            } catch (NumberFormatException e) {
                MessageUtils.errorMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.page_format_error"));
                return;
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
                    String msg = String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.list_info")), id, amount);
                    msg += ">>> ";
                    MessageUtils.sendMessage2Sender(commandSender,
                            msg,
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
                    MessageUtils.successMessage(true, commandSender,
                            String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.page_info")), queryPage, totalPage));
                    MessageUtils.successMessage(true, commandSender,
                            String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.page_next")), "/of page " + queryPage + 1));
                } else {
                    MessageUtils.errorMessage(true, commandSender,
                            SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.not_next"));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                DbUtils.closeQuietly(con);
                DbUtils.closeQuietly(ps);
            }
        });
    }

    private static void helpCommand(CommandSender commandSender) {
        List<String> list = SimpleDropPlugin.MESSAGE_YML_FILE.getStringList("help.offer");
        StringBuffer bf = new StringBuffer();
        for (String str : list) {
            bf.append(ChatColor.GOLD + str).append("\n");
        }
        commandSender.sendMessage(bf.toString());
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        if(args.length == 1){
            if(commandSender.hasPermission("offer.player")){
                List<String> suggestArg = new ArrayList<>();
                List<String> otherArg = Arrays.asList("page", "add");
                Collection<? extends Player> onlinePlayers = getPlugin().getServer().getOnlinePlayers();
                if(!onlinePlayers.isEmpty()){
                    otherArg.addAll(onlinePlayers.stream().map(Player::getName).toList());
                }
                suggestArg.addAll(otherArg);
                return suggestArg;
            }
        }
        if(args.length == 2){
            String arg = args[0];
            if(commandSender.hasPermission("offer.player")){
                return Collections.singletonList("add".equals(arg) ? "已被悬赏玩家名称" : "<金额>");
            }
        }
        return null;
    }

    private static boolean checkIfConsole(CommandSender commandSender) {
        if("console".equalsIgnoreCase(commandSender.getName())){
            MessageUtils.errorMessage(true,commandSender,SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.only_support_player"));
            return true;
        }
        return false;
    }

    private static boolean checkIfSelf(CommandSender commandSender, String name) {
        Player sender = (Player) commandSender;
        String senderName = sender.getName();
        if (name.equals(senderName)) {
            MessageUtils.errorMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.not_self"));
            return true;
        }
        return false;
    }

    private static boolean withdrawCommandSender(CommandSender commandSender, SimpleDropPlugin simpleDropPlugin, int amountInt) {
        String offer = commandSender.getName();
        Player offerPlayer = simpleDropPlugin.getServer().getPlayerExact(offer);
        EconomyResponse resp = SimpleDropPlugin.ECONOMY.withdrawPlayer(offerPlayer, amountInt);
        if (!resp.transactionSuccess()) {
            MessageUtils.errorMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.bal_not_enough"));
            return false;
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
           MessageUtils.errorMessage(true, commandSender, SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.bal_not_enough"));
            return true;
        }
        return false;
    }


}
