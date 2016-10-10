package io.github.kernegal.spongyisland;

import com.flowpowered.math.vector.Vector2i;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Created by kernegal on 04/10/2016.
 */
public class DataHolder {
    private SqlService sql;
    private SpongyIsland plugin;
    private static final String dbPath="./world/data/spongyisland";
    private String jdbcUrl;

    private javax.sql.DataSource getDataSource() throws SQLException {
        return sql.getDataSource(jdbcUrl);
    }

    /**
     * Constructor - initializes the state variables and create the db if necessary
     *
     * @param plugin the plugin base object
     */
    public DataHolder(SpongyIsland plugin) {
        this.plugin=plugin;
        Optional<SqlService> sqlOpt = Sponge.getServiceManager().provide(SqlService.class);
        if(!sqlOpt.isPresent()){
            plugin.getLogger().error("An error occurred when  getting the sql service ");
            //TODO exit properly
        }
        sql = sqlOpt.get();
        jdbcUrl="jdbc:h2:"+dbPath;


        File f = new File(dbPath+".mv.db");

        if(!f.exists()) {
            try(Connection conn = getDataSource().getConnection()) {
                plugin.getLogger().info("The database seems to not exist. Creating...");
                try {
                    conn.prepareStatement("CREATE TABLE island(\n" +
                            "   id   INT              NOT NULL AUTO_INCREMENT,\n" +
                            "   position_x INT         NOT NULL,\n" +
                            "   position_y  INT        NOT NULL,\n" +
                            "   level  INT            NOT NULL  DEFAULT '0',\n" +
                            "   name  CHAR (32) ,\n" +
                            "   creator_id   INT,       \n" +
                            "   PRIMARY KEY (id)\n" +
                            ");").execute();

                    conn.prepareStatement("CREATE TABLE player(\n" +
                            "   id   INT              NOT NULL AUTO_INCREMENT,\n" +
                            "   uuid CHAR (32        NOT NULL,\n" +
                            "   name  CHAR (32)       NOT NULL,\n" +
                            "   is_home_x  INT        NOT NULL,\n" +
                            "   is_home_y  INT        NOT NULL,\n" +
                            "   lt_create  SMALLDATETIME,\n" +
                            "   island INT,       \n" +
                            "   PRIMARY KEY (id)\n" +
                            ");").execute();
                    conn.prepareStatement("CREATE TABLE completed(\n" +
                            "   cid CHAR (32)       NOT NULL,\n" +
                            "   pid INT             NOT NULL,\n" +
                            "   ntimes INT          NOT NULL,\n" +
                            "   PRIMARY KEY (cid,pid)\n" +
                            ");").execute();
                } finally {
                    conn.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().error(e.toString());
            }
        }
    }

    public Vector2i[] getLastIslandsPosition(int num){
        Vector2i[] res = new Vector2i[num];
        try(Connection conn = getDataSource().getConnection()) {
            try {
                ResultSet rs  = conn.prepareStatement("SELECT position_x, position_y FROM island ORDER BY id DESC LIMIT "+num+";").executeQuery();
                int i=0;
                while (rs.next()) {
                    res[i]=new Vector2i(rs.getInt("position_x"),rs.getInt("position_y"));
                    i++;
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
        }

        return res;
    }
}
