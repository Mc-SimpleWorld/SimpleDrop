package org.nott.simpledrop.listener;

import lombok.Data;
import org.apache.commons.dbutils.DbUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.nott.simpledrop.SimpleDropPlugin;
import org.nott.simpledrop.global.KeyWord;
import org.nott.simpledrop.manager.SqlLiteManager;
import org.nott.simpledrop.utils.SwUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

/**
 * @author Nott
 * @date 2024-10-12
 */
@Data
public class OfferDeathListener implements Listener {

    private SimpleDropPlugin plugin;

    public OfferDeathListener(SimpleDropPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        EntityDamageEvent lastDamageCause = dead.getLastDamageCause();
        if (lastDamageCause instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageCause = (EntityDamageByEntityEvent) lastDamageCause;
            Entity damageCauseEntity = damageCause.getEntity();
            if (!(damageCauseEntity instanceof Player)) {
                return;
            }
            Entity damager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
            Player killer = (Player)damager;
            if (SwUtil.checkSupport4Towny("offer", dead, killer)) return;
            SimpleDropPlugin simpleDropPlugin = getPlugin();
            SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(simpleDropPlugin, () -> {
                Connection con = null;
                PreparedStatement ps = null;
                try {
                    con = SqlLiteManager.getConnect();
                    ps = con.prepareStatement("select * from offer_info where id = ?");
                    ps.setString(1, dead.getName());
                    ResultSet resultSet = ps.executeQuery();
                    int totalAmount = 0;
                    if(!resultSet.next())return;
                    while (resultSet.next()) {
                        int amount = resultSet.getInt("amount");
                        totalAmount += amount;
                    }
                    int tax = SimpleDropPlugin.CONFIG_YML_FILE.getInt("offer.tax");
                    int reward = totalAmount - tax;
                    if (SimpleDropPlugin.ECONOMY.depositPlayer(killer, reward).transactionSuccess()) {
                        ps = con.prepareStatement("delete from offer_info where id = ?");
                        ps.setString(1,dead.getName());
                        ps.execute();
                        SwUtil.spigotTextMessage(killer.spigot()
                                , SimpleDropPlugin.MESSAGE_YML_FILE.getString("offer.offer_tax") + tax
                                , ChatColor.GOLD);
                        SwUtil.spigotTextMessage(killer.spigot()
                                , String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString(KeyWord.MSG.OFFER_GET_REWARD)), dead.getName()) + reward
                                , ChatColor.GOLD);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(ps);
                    DbUtils.closeQuietly(con);
                }
            });
        }
    }
}
