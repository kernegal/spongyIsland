/*
 * This file is part of the plugin SopngyIsland
 *
 * Copyright (c) 2016 kernegal
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.kernegal.spongyisland;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.utils.Island;
import io.github.kernegal.spongyisland.utils.IslandPlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.File;
import java.sql.*;
import java.util.*;

import static sun.audio.AudioPlayer.player;

/**
 * Created by kernegal on 04/10/2016.
 */
public class DataHolder {
    private SqlService sql;
    private static final String dbPath="./world/data/spongyisland";
    private String jdbcUrl;
    private Map<UUID, IslandPlayer> players;

    private javax.sql.DataSource getDataSource() throws SQLException {
        return sql.getDataSource(jdbcUrl);
    }

    /**
     * Constructor - initializes the state variables and create the db if necessary
     *
     */
    public DataHolder() {
        Optional<SqlService> sqlOpt = Sponge.getServiceManager().provide(SqlService.class);
        if(!sqlOpt.isPresent()){
            SpongyIsland.getPlugin().getLogger().error("An error occurred when  getting the sql service ");
            //TODO exit properly
        }
        sql = sqlOpt.get();
        jdbcUrl="jdbc:h2:"+dbPath;


        File f = new File(dbPath+".mv.db");

        if(!f.exists()) {
            try(Connection conn = getDataSource().getConnection()) {
                SpongyIsland.getPlugin().getLogger().info("The database seems to not exist. Creating...");
                try {
                    conn.prepareStatement("CREATE TABLE island(\n" +
                            "   id   INT              NOT NULL AUTO_INCREMENT,\n" +
                            "   position_x INT         NOT NULL,\n" +
                            "   position_y  INT        NOT NULL,\n" +
                            "   level  INT            NOT NULL  DEFAULT '0',\n" +
                            "   name  CHAR (32) ,\n" +
                            "   creator_id INT (36),       \n" +
                            "   PRIMARY KEY (id)\n" +
                            ");").execute();

                    conn.prepareStatement("CREATE TABLE player(\n" +
                            "   id   INT              NOT NULL AUTO_INCREMENT,\n" +
                            "   uuid CHAR (36)         NOT NULL,\n" +
                            "   name  CHAR (32)       NOT NULL,\n" +
                            "   is_home_x  INT,\n" +
                            "   is_home_y  INT,\n" +
                            "   is_home_z  INT,\n" +
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
                SpongyIsland.getPlugin().getLogger().error(e.toString());
            }
        }
        players = new HashMap<>();
    }

    private IslandPlayer getPlayerFromDB(UUID uuid){
        IslandPlayer returnValue=null;
        try(Connection conn = getDataSource().getConnection()) {
            try {
                ResultSet rs  = conn.prepareStatement("" +
                        "SELECT player.id,player.uuid,player.name,is_home_x,is_home_y,is_home_z,island,island.position_x,island.position_y" +
                        " FROM player" +
                        " INNER JOIN island" +
                        " ON player.island=island.id" +
                        " WHERE uuid='"+uuid+"';").executeQuery();
                if (rs.next()) {
                    SpongyIsland.getPlugin().getLogger().info("found player with name: "+rs.getString("name"));
                    returnValue = new IslandPlayer(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            new Vector2i(rs.getInt("position_x"),rs.getInt("position_y")),
                            new Vector3i(rs.getInt("is_home_x"),rs.getInt("is_home_y"),rs.getInt("is_home_z")),
                            rs.getInt("island")
                    );

                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }

        return returnValue;
    }

    private void addPlayerToDatabase(UUID uuid,String name){
        try(Connection conn = getDataSource().getConnection()) {
            PreparedStatement statement = conn.prepareStatement("INSERT INTO player (uuid,name)\n" +
                    "VALUES ('" + uuid + "','" + name + "');",
                    Statement.RETURN_GENERATED_KEYS);
            int affectedRows=statement.executeUpdate();
            if(affectedRows==0){
                return;
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    players.put(uuid, new IslandPlayer(generatedKeys.getInt(1),uuid,name));
                }
            }

        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }

    }

    public void playerLogin(Player p){
        //Player p = event.getTargetEntity();
        UUID uuid = p.getUniqueId();
        IslandPlayer playerInfo=players.get(uuid);
        if(playerInfo==null){
            playerInfo=getPlayerFromDB(uuid);
            if(playerInfo==null){
                addPlayerToDatabase(uuid,p.getName());
                playerInfo=players.get(uuid);
            }
            else{
                players.put(uuid,playerInfo);
            }
        }
        SpongyIsland.getPlugin().getLogger().info("new player! uuid: "+playerInfo.getUuid()+" Name: "+playerInfo.getName());

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
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }

        return res;
    }

    public void newIsland(Vector2i gridPos, Vector3i worldPos, UUID player){
        try(Connection conn = getDataSource().getConnection()) {
            try {
                PreparedStatement statement =conn.prepareStatement("INSERT INTO island (position_x,position_y,creator_id)\n" +
                        "VALUES ("+gridPos.getX()+","+gridPos.getY()+",'"+players.get(player).getId()+"');",
                        Statement.RETURN_GENERATED_KEYS);

                int affectedRows=statement.executeUpdate();
                if(affectedRows==0){
                    return;
                }
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        conn.prepareStatement("UPDATE player\n" +
                                "        SET is_home_x="+worldPos.getX()+",is_home_y="+worldPos.getY()+",is_home_z="+worldPos.getZ()+",island='"+generatedKeys.getInt(1)+"'\n" +
                                "        WHERE id="+players.get(player).getId()+";").execute();
                        players.get(player).setIsHome(worldPos);
                        players.get(player).setIsland(generatedKeys.getInt(1),gridPos);
                        //players.get(player).setIsPosition(gridPos);
                    }
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }
    }

    public void updateIslandHome(UUID player, Vector3i pos){
        try(Connection conn = getDataSource().getConnection()) {
            try {
                conn.prepareStatement("UPDATE player\n" +
                        "        SET is_home_x="+pos.getX()+",is_home_y="+pos.getY()+",is_home_z="+pos.getZ()+"\n" +
                        "        WHERE id="+players.get(player).getId()+";").execute();
                players.get(player).setIsHome(pos);

            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }

    }

    public void teleportPlayerToHome(Player player){
        IslandPlayer playerData = getPlayerData(player.getUniqueId());

        if(playerData.getIsland()==-1){
            player.sendMessage(Text.of(TextColors.DARK_RED,"You need an island"));
            return;
        }
        Vector3i playerHome = playerData.getIsHome();
        Location<World> worldLocation = Sponge.getServer().getWorld("world").get().getLocation(playerHome);

        SpongyIsland.getPlugin().getLogger().info("Teleporting player "+player.getName()+" to "+playerHome);
        Optional<Location<World>> safeLocation = Sponge.getGame().getTeleportHelper()
                .getSafeLocation(worldLocation, 10, 20);

        player.sendMessage(Text.of("Teleporting to your island"));
        if(safeLocation.isPresent())
            player.setLocation(safeLocation.get());
        else
            player.sendMessage(Text.of(TextColors.DARK_RED,"Island not secure"));

    }

    public final IslandPlayer getPlayerData(UUID uuid){
        return players.get(uuid);

    }

    public void updateIslandLevel(UUID uuid, int level){
        try(Connection conn = getDataSource().getConnection()) {
            try {
                conn.prepareStatement("UPDATE island\n" +
                        "        SET level="+level+"\n" +
                        "        WHERE id="+players.get(uuid).getIsland()+";").execute();

            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }

    }

    public void listTopIslands(int number, CommandSource destination){
        destination.sendMessage(Text.of("Top "+number+" islands:"));
        try(Connection conn = getDataSource().getConnection()) {
            try {
                ResultSet rs  = conn.prepareStatement(
                        "SELECT island.id,island.name as island_name,island.level,c.name as player_name,count(player.id) as num_players " +
                        "FROM island " +
                        "JOIN player ON player.island=island.id " +
                        "JOIN player AS c ON c.id=island.creator_id " +
                        "GROUP BY island.id " +
                        "ORDER BY level " +
                        "DESC LIMIT "+number+";"
                    ).executeQuery();
                while (rs.next()) {
                    String name = rs.getString("island_name");
                    if(name==name){

                        destination.sendMessage(Text.of("Island of "+rs.getString("player_name")+": ",TextColors.AQUA,rs.getInt("level"),TextColors.NONE," ["+rs.getInt("num_players")+" players]"));
                    }
                    else{
                        destination.sendMessage(Text.of(name+": "+rs.getInt("level")+"["+rs.getInt("num_players")+" players]"));
                    }
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }
    }
}
