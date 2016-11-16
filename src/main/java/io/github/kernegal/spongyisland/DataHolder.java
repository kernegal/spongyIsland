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
import com.google.common.reflect.TypeToken;
import io.github.kernegal.spongyisland.utils.CompletedChallenges;
import io.github.kernegal.spongyisland.utils.IslandPlayer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;
import sun.nio.cs.ArrayEncoder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import static javax.xml.bind.JAXBIntrospector.getValue;


public class DataHolder {
    private SqlService sql;
    private static final String dbPath="./world/data/spongyisland";
    private String jdbcUrl;
    private Map<UUID, IslandPlayer> players;
    private Map<UUID,CompletedChallenges> playerChallenges;
    private ConfigurationNode challenges,config;
    private Random rg;

    private javax.sql.DataSource getDataSource() throws SQLException {
        return sql.getDataSource(jdbcUrl);
    }

    public DataHolder(ConfigurationNode challenges, ConfigurationNode config) {
        Optional<SqlService> sqlOpt = Sponge.getServiceManager().provide(SqlService.class);
        if(!sqlOpt.isPresent()){
            SpongyIsland.getPlugin().getLogger().error("An error occurred when  getting the sql service ");
            return;//TODO exit properly
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
                            "   special  BOOLEAN ,\n" +
                            "   creator_id INT (36),       \n" +
                            "   PRIMARY KEY (id)\n" +
                            ");").execute();

                    conn.prepareStatement("CREATE TABLE player(\n" +
                            "   id   INT              NOT NULL AUTO_INCREMENT,\n" +
                            "   uuid CHAR (36)        NOT NULL,\n" +
                            "   name  CHAR (32)       NOT NULL,\n" +
                            "   is_home_x  INT,\n" +
                            "   is_home_y  INT,\n" +
                            "   is_home_z  INT,\n" +
                            "   lt_create  SMALLDATETIME,\n" +
                            "   island INT            DEFAULT '-1',       \n" +
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
        playerChallenges = new HashMap<>();

        this.challenges = challenges;
        this.config=config;

        rg= new Random();
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
                p.setLocationSafely(Sponge.getServer().getWorld("world").get().getSpawnLocation());
                //playerInfo=players.get(uuid);
            }
            else{
                players.put(uuid,playerInfo);
            }
        }
        //SpongyIsland.getPlugin().getLogger().info("new player! uuid: "+playerInfo.getUuid()+" Name: "+playerInfo.getName());

        try(Connection conn = getDataSource().getConnection()) {
            try {
                ResultSet rs = conn.prepareStatement("SELECT cid,ntimes\n" +
                        "        FROM completed\n" +
                        "        WHERE pid='"+players.get(uuid).getId()+"';").executeQuery();
                CompletedChallenges c = new CompletedChallenges(challenges.getNode("root_level").getString());
                playerChallenges.put(uuid,c);

                while(rs.next()) {
                    String challenge = rs.getString("cid");
                    int ntimes = rs.getInt("ntimes");
                    c.setChallengeCompleted(challenge,challenges.getNode("challenge_list",challenge,"level").getString(),ntimes);
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
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

    public void markIslandAsSpecial(UUID uuid){
        try(Connection conn = getDataSource().getConnection()) {
            try {

                conn.prepareStatement("UPDATE island\n" +
                        "        SET " +
                        "        special=TRUE " +
                        "        WHERE id="+players.get(uuid).getIsland()+";").execute();
                conn.prepareStatement("UPDATE player\n" +
                        "        SET " +
                        "        island=-1 " +
                        "        WHERE id="+players.get(uuid).getId()+";").execute();
                players.get(uuid).setIsland(-1,null);
                //players.get(player).setIsPosition(gridPos);

            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }

    }

    public boolean hasAccessToLevel(UUID player, String level){
        String root=challenges.getNode("root_level").getString();
        String requiredLevel=challenges.getNode("level_list",level,"required_level").getString("");
        int challengesRequired = challenges.getNode("level_list",requiredLevel,"required_challenges").getInt(0);

        return level.equals(root) || requiredLevel.isEmpty()
                || playerChallenges.get(player).challengesCompletedInLevel(requiredLevel)>=challengesRequired;

    }

    public boolean challengeIsCompleted(UUID player, String challenge){
        return playerChallenges.get(player).timesCompleted(challenge)!=0;
    }

    public boolean canCompleteChallenge(UUID player, String challenge){
        ConfigurationNode challengeNode=challenges.getNode("challenge_list",challenge);
        int timesCompleted = playerChallenges.get(player).timesCompleted(challenge);
        int maxTimes = challengeNode.getNode("max_times").getInt(0);
        return timesCompleted !=0 &&
                (!challengeNode.getNode("repeatable").getBoolean() ||
                maxTimes!=0 && timesCompleted>=maxTimes);

    }

    public void completeChallenge(String challenge, int times ,Player player){

        ConfigurationNode challengeNode=challenges.getNode("challenge_list",challenge);
        if(challengeNode.getNode("friendly_name").getString()==null){
            player.sendMessage(Text.of("That challenge don't exist"));
            return;
        }
        if(!hasAccessToLevel(player.getUniqueId(),challengeNode.getNode("level").getString(""))){
            player.sendMessage(Text.of("You don't have access to that level"));
            return;
        }

        if(canCompleteChallenge(player.getUniqueId(),challenge)){
            player.sendMessage(Text.of("You can't complete that challenge any more times"));
            return;
        }

        String type = challengeNode.getNode("type").getString("");
        String requiredItems = challengeNode.getNode("required_items").getString("");
        String itemReward;
        String textReward;
        int moneyReward,expReward;

        int timesCompleted = playerChallenges.get(player.getUniqueId()).timesCompleted(challenge);

        if(timesCompleted==0) {
            itemReward = challengeNode.getNode("item_reward").getString("");
            textReward = challengeNode.getNode("reward_text").getString("");
            expReward = challengeNode.getNode("exp_eward").getInt(0);
            moneyReward = challengeNode.getNode("money_reward").getInt(0);

        }
        else{
            itemReward = challengeNode.getNode("repeat_item_reward").getString("");
            textReward = challengeNode.getNode("repeat_reward_text").getString("");
            expReward = challengeNode.getNode("repeat_exp_reward").getInt(0);
            moneyReward = challengeNode.getNode("money_reward").getInt(0);

        }



        if(type.equals("inventory") ){
            String[] requiredItemsArray = requiredItems.split(" ");
            ArrayList< ArrayList<Slot> > inventorySlots = new ArrayList<>(requiredItemsArray.length);
            for(int i=0;i<requiredItemsArray.length;i++){
                String item= requiredItemsArray[i];
                inventorySlots.add(new ArrayList<>());
                String[] itemElements = item.split(",");
                long itemUnsafeDamage;
                int quantity;

                if(itemElements.length>2){
                    itemUnsafeDamage=Integer.parseInt(itemElements[1]);
                    quantity=Integer.parseInt(itemElements[2]);
                }
                else{
                    quantity=Integer.parseInt(itemElements[1]);
                    itemUnsafeDamage=-1;
                }
                Optional<ItemType> itemType = Sponge.getGame().getRegistry().getType(ItemType.class, itemElements[0]);
                if(itemType.isPresent()){
                    Iterable<Slot> slotIter = player.getInventory().slots();

                    int needed=quantity;
                    for (Slot slot: slotIter){
                        Optional<ItemStack> slotItemStack = slot.peek();
                        if(slotItemStack.isPresent()){
                            ItemStack itemStack= slotItemStack.get();
                            Long unsafeDamage = itemStack.toContainer().getLong(DataQuery.of("UnsafeDamage")).orElse(0L);

                            if(itemType.get().matches(itemStack) && !(itemUnsafeDamage!=-1 && itemUnsafeDamage!=unsafeDamage)){
                                inventorySlots.get(i).add(slot);
                                needed-=itemStack.getQuantity();
                                if(needed<=0) break;
                            }
                        }

                    }
                    if(needed>0){
                        player.sendMessage(Text.of("You don't have all required items"));
                        return;
                    }

                }
                else{
                    SpongyIsland.getPlugin().getLogger().warn("item type incorrect: "+itemElements[0]);
                }


            }
            if(challengeNode.getNode("take_items").getBoolean(true)) {
                for (int i = 0; i < requiredItemsArray.length; i++) {

                    String item = requiredItemsArray[i];
                    String[] split = item.split(",");
                    int quantity = split.length > 2 ?
                            Integer.parseInt(split[2]) :
                            Integer.parseInt(split[1]);

                    ArrayList<Slot> slotArray = inventorySlots.get(i);
                    for (Slot slot : slotArray) {
                        if (slot.getStackSize() < quantity) {
                            quantity -= slot.getStackSize();
                            slot.clear();
                        } else {
                            slot.poll(quantity);
                        }
                    }
                }
            }


        }
        else if( type.equals("level") ){
            try(Connection conn = getDataSource().getConnection()) {
                try {
                    ResultSet rs  = conn.prepareStatement("SELECT level\n" +
                            "        FROM island\n" +
                            "        WHERE id="+players.get(player.getUniqueId()).getIsland()+";").executeQuery();
                    if (rs.next()) {
                        if(rs.getInt("level")<challengeNode.getNode("required_items").getInt(0)){
                            player.sendMessage(Text.of(TextColors.DARK_RED,"You don't have the required level"));
                            return;
                        }

                    }
                    else{
                        player.sendMessage(Text.of(TextColors.DARK_RED,"You don't have the required level"));
                        return;
                    }
                } finally {
                    conn.close();
                }
            } catch (SQLException e) {
                SpongyIsland.getPlugin().getLogger().error(e.toString());
                return;
            }
        }
        else if( type.equals("island") ){
            IslandPlayer playerData = players.get(player.getUniqueId());
            int islandRadius = config.getNode("island","radius").getInt(), protectionRadius=config.getNode("island","protectionRadius").getInt();
            Vector2i islandCoordinates=playerData.getIsPosition().mul(islandRadius*2);

            Location<World> location = player.getLocation();

            Vector2i min = islandCoordinates.sub(protectionRadius,protectionRadius);
            Vector2i max = islandCoordinates.add(protectionRadius,protectionRadius);

            if(!location.getExtent().getName().equals("world") ||
                    location.getX()<min.getX() || location.getX()>=max.getX() ||
                    location.getZ()<min.getY() || location.getZ()>=max.getY()){
                player.sendMessage(Text.of(TextColors.DARK_RED,"You need to be inside of your island"));
                return;
            }

            String[] requiredItemsArray = requiredItems.split(" ");
            for(int i=0;i<requiredItemsArray.length;i++) {

                String item= requiredItemsArray[i];
                String[] itemElements = item.split(",");
                int itemUnsafeDamage;
                int quantity;

                if(itemElements.length>2){
                    itemUnsafeDamage=Integer.parseInt(itemElements[1]);
                    quantity=Integer.parseInt(itemElements[2]);
                }
                else{
                    quantity=Integer.parseInt(itemElements[1]);
                    itemUnsafeDamage=0;
                }

                Optional<BlockType> blockType = Sponge.getGame().getRegistry().getType(BlockType.class, itemElements[0]);

                if(blockType.isPresent()) {
                    Vector3i playerPosition = player.getLocation().getBlockPosition();

                    World world = Sponge.getServer().getWorld("world").get();
                    Extent view = world.getExtentView(playerPosition.sub(10, 10, 10),
                            playerPosition.add(10, 10, 10));

                    GsonConfigurationLoader loader = GsonConfigurationLoader.builder().build();
                    ConfigurationNode node = loader.createEmptyNode();

                    node.getNode("ContentVersion").setValue(1);
                    node.getNode("ItemType").setValue(itemElements[0]);
                    node.getNode("Count").setValue(quantity);
                    node.getNode("UnsafeDamage").setValue(itemUnsafeDamage);


                    int sum = view.getBlockWorker(Cause.of(NamedCause.of("plugin", SpongyIsland.getPlugin().getPluginContainer()))).reduce(
                            (vol, x, y, z, red) -> vol.getBlockType(x, y, z).equals(BlockTypes.AIR) ?
                                    red :
                                    red + isSameBlockType(vol.getBlock(x, y, z),blockType.get(),itemUnsafeDamage)
                            ,
                            (a, b) -> a + b,
                            0);
                    if(sum<quantity){
                        player.sendMessage(Text.of(TextColors.DARK_RED,"All the required items needs to be 10 blocks nar you"));
                        return;
                    }

                }

            }
        }
        else{
            player.sendMessage(Text.of("Not implemented"));
            return;
        }


        player.sendMessage(Text.of(textReward));
        String[] itemsStr = itemReward.split(" ");
        giveItem(player,itemsStr);

        String randomReward = challengeNode.getNode("random_reward").getString();
        if(randomReward!=null) {
            String[] rr = randomReward.split(",");
            ConfigurationNode randomRewards = challenges.getNode("random_rewards", rr[0]);
            int randomRewardTimes = 1;
            if(rr.length>1){
                randomRewardTimes=Integer.parseInt(rr[1]);
            }
            for(int i=0;i<randomRewardTimes;i++) {
                double cumulative = 0;
                double random = rg.nextDouble();

                for (Map.Entry<Object, ? extends ConfigurationNode> entry : randomRewards.getChildrenMap().entrySet()) {
                    double prob = entry.getValue().getNode("probability").getDouble();
                    cumulative += prob;
                    if (random < cumulative) {
                        giveItem(player, entry.getValue().getNode("reward").getString("").split(" "));
                        break;
                    }
                }
            }
        }


        if(config.getNode("general","economy").getBoolean(false)) {
            Optional<UniqueAccount> orCreateAccount = SpongyIsland.getPlugin().getEconomyService().getOrCreateAccount(player.getUniqueId());
            if(orCreateAccount.isPresent()){
                orCreateAccount.get().deposit(
                        SpongyIsland.getPlugin().getEconomyService().getDefaultCurrency(),
                        BigDecimal.valueOf((long)moneyReward),
                        Cause.source(this).build()
                );

            }
        }

        player.offer(Keys.TOTAL_EXPERIENCE,player.get(Keys.TOTAL_EXPERIENCE).orElse(0)+expReward);


        try(Connection conn = getDataSource().getConnection()) {
            try {
                int id= players.get(player.getUniqueId()).getId();
                //int timesCompleted = playerChallenges.get(player.getUniqueId()).timesCompleted(challenge);
                timesCompleted++;
                conn.prepareStatement("REPLACE INTO completed (cid,pid,ntimes) " +
                        "VALUES('"+ challenge +"','"+ id +"','"+timesCompleted +"');").execute();

                playerChallenges.get(player.getUniqueId()).setChallengeCompleted(challenge,challengeNode.getNode("level").getString(""));
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }
    }

    private void giveItem(Player player, String[] itemsStr){
        for(String itemStr :itemsStr){
            String[] itemElements = itemStr.split(",");
            Optional<ItemType> itemType = Sponge.getGame().getRegistry().getType(ItemType.class, itemElements[0]);
            if(itemType.isPresent()) {
                int itemUnsafeDamage;
                int quantity;

                if (itemElements.length > 2) {
                    itemUnsafeDamage = Integer.parseInt(itemElements[1]);
                    quantity = Integer.parseInt(itemElements[2]);
                } else {
                    quantity = Integer.parseInt(itemElements[1]);
                    itemUnsafeDamage = 0;
                }

                //StringWriter sink = new StringWriter();
                //GsonConfigurationLoader loader = GsonConfigurationLoader.builder().setSink(() -> new BufferedWriter(sink)).build();
                GsonConfigurationLoader loader = GsonConfigurationLoader.builder().build();
                ConfigurationNode node = loader.createEmptyNode();

                node.getNode("ContentVersion").setValue(1);
                node.getNode("ItemType").setValue(itemElements[0]);
                node.getNode("Count").setValue(quantity);
                node.getNode("UnsafeDamage").setValue(itemUnsafeDamage);
                if(itemElements.length>3){
                    ConfigurationNode subNode = node.getNode("UnsafeData");
                    String[] data = itemElements[3].split("\\.");
                    for(int i=0;i<data.length-1;i++){
                        subNode=subNode.getNode(data[i]);
                    }
                    SpongyIsland.getPlugin().getLogger().info("data["+(data.length - 1)+"]"+data.length+" "+itemElements[3]);
                    String[] split = data[data.length - 1].split(":");
                    subNode.getNode(split[0]).setValue(split[1]);
                }

                /*try {
                    loader.save(node);
                    String json = sink.toString();
                    player.sendMessage(Text.of(json));
                }  catch (IOException e) {
                    e.printStackTrace();
                }*/

                ItemStack itemStack;
                try {
                    itemStack= node.getValue(TypeToken.of(ItemStack.class));
                } catch (ObjectMappingException e) {
                    SpongyIsland.getPlugin().getLogger().warn(e.toString());
                    return;
                }


                InventoryTransactionResult transactionResult = player.getInventory().offer(itemStack);
                for (ItemStackSnapshot itemStackSnapshot : transactionResult.getRejectedItems() ){
                    Entity entity = player.getLocation().getExtent().createEntity(EntityTypes.ITEM, player.getLocation().getPosition());
                    entity.offer(Keys.REPRESENTED_ITEM, itemStackSnapshot);
                    player.getLocation().getExtent().spawnEntity(entity, Cause.of(NamedCause.of("player",player)));
                }


            }
            else{
                SpongyIsland.getPlugin().getLogger().warn("item type incorrect: "+itemElements[0]);
            }

        }

    }

    private int isSameBlockType(BlockState bs, BlockType type, int damage){
        //ItemStack is=ItemStack.builder().fromBlockState(bs).build();
        if(bs.getType().equals(type)){
            /*if(damage!=0) {
                Optional<Long> unsafeDamage = is.toContainer().getLong(DataQuery.of("UnsafeDamage"));
                if(unsafeDamage.isPresent() && unsafeDamage.get()==damage){
                    return 1;
                }

            }
            else{
                return 1;
            }*/
            return 1;
        }
        return 0;
    }

    public String getLastLevelIssued(UUID uuid){
        return playerChallenges.get(uuid).getLastLevel();
    }

    public int getCompletedChallengesInLevel(UUID uuid,String level){
        return playerChallenges.get(uuid).challengesCompletedInLevel(level);
    }

    public void resetChallenges(UUID uuid){
        try(Connection conn = getDataSource().getConnection()) {
            try {
                conn.prepareStatement("DELETE FROM completed\n" +
                        "        WHERE pid='"+players.get(uuid).getId()+"';").execute();
                playerChallenges.put(uuid,new CompletedChallenges(challenges.getNode("root_level").getString()));
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
        }



    }
}
