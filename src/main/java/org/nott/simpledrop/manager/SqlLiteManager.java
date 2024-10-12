package org.nott.simpledrop.manager;

import org.nott.simpledrop.global.GlobalFactory;
import org.nott.simpledrop.utils.SwUtil;

import java.sql.*;

/**
 * @author Nott
 * @date 2024-10-12
 */
public class SqlLiteManager {

    public static Connection getConnect() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:plugins/" + GlobalFactory.PLUGIN_NAME + "/database.db");
    }

    public static void createTableIfNotExist(String tableName, String sql) {
        try {
            Connection connect = getConnect();
            PreparedStatement statement = connect.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name= ?");
            statement.setString(1, tableName);
            ResultSet set = statement.executeQuery();
            if (set.wasNull()) {
                Statement cs = connect.createStatement();
                cs.execute(sql);
            }
        } catch (Exception e) {
            SwUtil.log("Create table error:" + e.getMessage());
        }
    }

}
