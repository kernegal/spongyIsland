package io.github.kernegal.spongyisland;

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
                            "   id   INT              NOT NULL,\n" +
                            "   positionx INT         NOT NULL,\n" +
                            "   positiony  INT        NOT NULL,\n" +
                            "   name  CHAR (32) ,\n" +
                            "   creatorid   INT,       \n" +
                            "   PRIMARY KEY (id)\n" +
                            ");").execute();

                    conn.prepareStatement("CREATE TABLE player(\n" +
                            "   id   INT              NOT NULL,\n" +
                            "   uuid CHAR (32)        NOT NULL,\n" +
                            "   name  CHAR (32)       NOT NULL,\n" +
                            "   ltcreate  SMALLDATETIME,\n" +
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
}
