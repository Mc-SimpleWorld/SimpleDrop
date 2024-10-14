package org.nott.simpledrop.manager;

import com.google.common.io.Files;
import org.bukkit.plugin.Plugin;
import org.nott.simpledrop.SimpleDropPlugin;
import org.nott.simpledrop.global.GlobalFactory;
import org.nott.simpledrop.utils.SwUtil;

import java.io.File;
import java.io.IOException;
import java.sql.*;

/**
 * @author Nott
 * @date 2024-10-12
 */
public class SqlLiteManager {

    public static final String DB_PATH = "plugins/" + GlobalFactory.PLUGIN_NAME + "/database.db";

    public static Connection getConnect() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:plugins/" + GlobalFactory.PLUGIN_NAME + "/src/main/db/database.db");
    }

    public static void checkDbFileIsExist(Plugin plugin) {
        File file = new File(DB_PATH);
        if (file.exists()) {
            return;
        }
        String dbFilePath = plugin.getDataFolder() + "/org/nott/simpledrop/db/database.bd";
        File dbFile = new File(dbFilePath);
        if (!dbFile.exists()) {
            throw new RuntimeException(String.format(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.db_not_found"), dbFile));
        }
        try {
            Files.copy(dbFile, file);
        } catch (IOException e) {
            SwUtil.log(SimpleDropPlugin.MESSAGE_YML_FILE.getString("common.copy_db_error") + e.getMessage());
            throw new RuntimeException(e);
        }
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
