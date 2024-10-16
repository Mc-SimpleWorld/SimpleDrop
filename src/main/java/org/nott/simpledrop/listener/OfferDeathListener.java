package org.nott.simpledrop.listener;

import lombok.Data;
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
            Player killer = (Player) damageCauseEntity;
            if (SwUtil.checkSupport4Towny("offer", dead, killer)) return;
            SimpleDropPlugin simpleDropPlugin = getPlugin();
            SimpleDropPlugin.SCHEDULER.runTaskAsynchronously(simpleDropPlugin, () -> {
                try {
                    Connection con = SqlLiteManager.getConnect();
                    PreparedStatement ps = con.prepareStatement("select * from offer_info where id = ?");
                    ps.setString(1, dead.getName());
                    ResultSet resultSet = ps.executeQuery();
                    int totalAmount = 0;
                    while (resultSet.next()) {
                        int amount = resultSet.getInt("amount");
                        totalAmount += amount;
                    }

                    SimpleDropPlugin.ECONOMY.depositPlayer(killer, totalAmount);
                    SwUtil.spigotTextMessage(killer.spigot()
                            , String.format(Objects.requireNonNull(SimpleDropPlugin.MESSAGE_YML_FILE.getString(KeyWord.MSG.OFFER_GET_REWARD)), dead.getName()) + totalAmount
                            , ChatColor.GOLD);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
