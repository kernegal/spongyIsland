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

package io.github.kernegal.spongyisland.utils;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.DataHolder;
import io.github.kernegal.spongyisland.SpongyIsland;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.FoodData;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.schematic.Schematic;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by kernegal on 09/10/2016.
 * Class for managing island creation
 */
public class IslandManager {
    private Map<String,Island> islandsPresets;
    private DataHolder dataHolder;
    private Vector2i lastIslandCreated, preLastIslandCreated;
    private int islandHeight;
    private int islandRadius;
    private int secondsBetweenIslands;

    public IslandManager( DataHolder dataHolder, ConfigurationNode configuration) {
        this.dataHolder=dataHolder;

        ConfigurationNode islandNode = configuration.getNode("island");
        ConfigurationNode schematicsNode = configuration.getNode("schematics");
        ConfigurationNode generalNode = configuration.getNode("general");


        this.islandRadius = islandNode.getNode("radius").getInt();
        this.islandHeight = islandNode.getNode("island_height").getInt();

        this.secondsBetweenIslands = generalNode.getNode("reset_wait").getInt();

        this.islandsPresets = new HashMap<>();
        Map<Object, ? extends ConfigurationNode> childrenMap = schematicsNode.getChildrenMap();

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : childrenMap.entrySet())
        {
            //System.out.println(entry.getKey() + "/" + entry.getValue());
            //for(int i=0; i<childrenList.size(); i++){
            ConfigurationNode schematicNode = entry.getValue();
            File schematicFile = new File(SpongyIsland.getPlugin().getSchematicsFolder(),schematicNode.getNode("filename").getString());
            if(!schematicFile.exists()){
                SpongyIsland.getPlugin().getLogger().error("Schematic "+schematicFile.getPath()+" does not exist. Ignoring");
                continue;
            }

            DataContainer schematicData;
            try {
                schematicData = DataFormats.NBT.readFrom(new GZIPInputStream(new FileInputStream(schematicFile)));
            } catch (Exception e) {
                e.printStackTrace();
                SpongyIsland.getPlugin().getLogger().error("Error loading schematic: " + e.getMessage());
                continue;
            }

            Schematic schematic = DataTranslators.SCHEMATIC.translate(schematicData);

            Island is= new Island(schematic,
                    schematicNode.getNode("name").getString(),
                    schematicNode.getNode("description").getString());
            this.islandsPresets.put(schematicNode.getNode("schematic_name").getString(),is);
            SpongyIsland.getPlugin().getLogger().info("Loaded schematic "+schematicNode.getNode("schematic_name").getString()+".");

        }

        Vector2i[] lastIslandsPosition = dataHolder.getLastIslandsPosition(2);
        this.lastIslandCreated = lastIslandsPosition[0];
        this.preLastIslandCreated = lastIslandsPosition[1];
    }

    public boolean create(String schematic, Player player){


        Island is = islandsPresets.get(schematic);
        if(is == null){
            player.sendMessage(Text.of(TextColors.RED, "The schematic selected does not exist. Available schematics:"));
            islandsPresets.forEach((k,v) -> player.sendMessage(Text.of(TextColors.RED, k)));
            return false;
        }

        IslandPlayer playerData = dataHolder.getPlayerData(player.getUniqueId());
        if(!playerData.canHaveNewIsland( secondsBetweenIslands)){
            player.sendMessage(Text.of(TextColors.RED, "You have to wait until you can create/join another island"));
            return false;
        }

        //next position free
        Vector2i newIslandPos;
        if(lastIslandCreated==null){
            newIslandPos=new Vector2i(0,0);
        }
        else if(preLastIslandCreated==null){
            newIslandPos=new Vector2i(1,1);
        }
        else{
            Vector2i increment;
            if(lastIslandCreated.getX()!=preLastIslandCreated.getX() &&
                    lastIslandCreated.getY()!=preLastIslandCreated.getY()){
                increment = new Vector2i(0,-1);
            }
            else {
                increment = lastIslandCreated.sub(preLastIslandCreated);
                Vector2i abs = lastIslandCreated.abs();
                if(abs.getX()==abs.getY()){
                    increment= new Vector2i(increment.getY(),-increment.getX());
                }
            }
            newIslandPos=lastIslandCreated.add(increment);
            if(newIslandPos.getX()>0 && newIslandPos.getX()==newIslandPos.getY()){
                newIslandPos = newIslandPos.add(1,1);
            }


        }
        //TODO make the world configurable
        World world = Sponge.getServer().getWorld("world").get();
        Vector3i position = new Vector3i(newIslandPos.getX()*islandRadius*2,islandHeight,newIslandPos.getY()*islandRadius*2);
        if(is.place(world,position,player)==-1) return false;
        preLastIslandCreated=lastIslandCreated;
        lastIslandCreated=newIslandPos;

        //Tell to save the data
        dataHolder.newIsland(newIslandPos,position,player.getUniqueId());
        //dataHolder.uptdateIslandHome(player.getUniqueId(),position);

        player.getInventory().clear();
        player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).get());
        player.offer(Keys.FOOD_LEVEL,player.foodLevel().getDefault());
        player.offer(Keys.SATURATION,player.saturation().getDefault());

        //player.offer(Keys.SATURATION,player.get()
        SpongyIsland.getPlugin().getLogger().info("pasting schematic "+schematic+" into position ("+newIslandPos.getX()*islandRadius*2+"["+newIslandPos.getX()
                +"],"+newIslandPos.getY()*islandRadius*2+"["+newIslandPos.getY()+"])");
        return true;
    }
}
