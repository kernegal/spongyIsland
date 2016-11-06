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
import io.github.kernegal.spongyisland.commands.IsCreate;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.FoodData;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.api.world.extent.worker.procedure.BiomeVolumeFiller;
import org.spongepowered.api.world.schematic.Schematic;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
                    schematicNode.getNode("description").getString(),
                    getBiomeFromText(schematicNode.getNode("biome").getString("ocean"))
            );
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
        Optional<World> worldOpt = Sponge.getServer().getWorld("world");
        if(!worldOpt.isPresent()){
            SpongyIsland.getPlugin().getLogger().error("World not found when changing biome");
            return false;
        }
        World world = worldOpt.get();
        Vector3i position = new Vector3i(newIslandPos.getX()*islandRadius*2,islandHeight,newIslandPos.getY()*islandRadius*2);
        if(is.place(world,position,player)==-1) return false;
        preLastIslandCreated=lastIslandCreated;
        lastIslandCreated=newIslandPos;

        //Tell to save the data
        dataHolder.newIsland(newIslandPos,position,player.getUniqueId());
        //dataHolder.uptdateIslandHome(player.getUniqueId(),position);

        Vector3i min2 = position.sub(islandRadius,0,islandRadius);
        Vector3i max2 = position.add(islandRadius,0,islandRadius);

        Vector3i min = new Vector3i(min2.getX(),0,min2.getZ());
        Vector3i max = new Vector3i(max2.getX(),255,max2.getZ());

        Extent view = world.getExtentView(min, max);
        BiomeType biome=is.getBiome();

        view.getBiomeWorker().fill(new BiomeVolumeFiller() {
            @Override
            @Nonnull
            public BiomeType produce(int x, int y, int z) {
                return biome;
            }
        });

        dataHolder.resetChallenges(player.getUniqueId());

        player.getInventory().clear();
        player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).orElse(1.0));
        player.offer(Keys.FOOD_LEVEL,player.foodLevel().getDefault());
        player.offer(Keys.SATURATION,player.saturation().getDefault());

        //player.offer(Keys.SATURATION,player.get()
        SpongyIsland.getPlugin().getLogger().info("pasting schematic "+schematic+" into position ("+newIslandPos.getX()*islandRadius*2+"["+newIslandPos.getX()
                +"],"+newIslandPos.getY()*islandRadius*2+"["+newIslandPos.getY()+"])");
        return true;
    }

    public static BiomeType getBiomeFromText(String biome){
        biome=biome.toLowerCase();

        switch (biome) {
            case "beach":
                return BiomeTypes.BEACH;

            case "birch_forest":
                return BiomeTypes.BIRCH_FOREST;

            case "cold_taiga":
                return BiomeTypes.COLD_TAIGA;

            case "deep_ocean":
                return BiomeTypes.DEEP_OCEAN;

            case "desert":
                return BiomeTypes.DESERT;

            case "extreme_hills":
                return BiomeTypes.EXTREME_HILLS;

            case "flower_forest":
                return BiomeTypes.FLOWER_FOREST;

            case "forest":
                return BiomeTypes.FOREST;

            case "frozen_ocean":
                return BiomeTypes.FROZEN_OCEAN;

            case "frozen_river":
                return BiomeTypes.FROZEN_RIVER;

            case "hell":
                return BiomeTypes.HELL;

            case "ice_mountains":
                return BiomeTypes.ICE_MOUNTAINS;

            case "ice_plains":
                return BiomeTypes.ICE_PLAINS;

            case "ice_plains_spikes":
                return BiomeTypes.ICE_PLAINS_SPIKES;

            case "jungle":
                return BiomeTypes.JUNGLE;

            case "mega_spruce_taiga":
                return BiomeTypes.MEGA_SPRUCE_TAIGA;

            case "mega_taiga":
                return BiomeTypes.MEGA_TAIGA;

            case "mesa":
                return BiomeTypes.MESA;

            case "mushroom_island":
                return BiomeTypes.MUSHROOM_ISLAND;

            case "ocean":
                return BiomeTypes.OCEAN;

            case "plains":
                return BiomeTypes.PLAINS;

            case "river":
                return BiomeTypes.RIVER;

            case "roofed_forest":
                return BiomeTypes.ROOFED_FOREST;

            case "savanna":
                return BiomeTypes.SAVANNA;

            case "sky":
                return BiomeTypes.SKY;

            case "stone_beach":
                return BiomeTypes.STONE_BEACH;

            case "sunflower_plains":
                return BiomeTypes.SUNFLOWER_PLAINS;

            case "swampland":
                return BiomeTypes.SWAMPLAND;

            case "taiga":
                return BiomeTypes.TAIGA;

            case "void":
                return BiomeTypes.VOID;

            default:
                return BiomeTypes.OCEAN;
        }
    }
    public BookView getIslandBook(){
        BookView.Builder bookView = BookView.builder()
                .title(Text.of("Biome Shop"))
                .author(Text.of("SpongyIsland"));

        Text page=Text.EMPTY;
        final int charPerRow=20;
        final int linesPerPage=14;
        int actualLines=0;
        for(Map.Entry<String, Island> entry : islandsPresets.entrySet()) {
            String islandNameStr = entry.getValue().getName();
            Text islandName = Text.builder(islandNameStr)
                    .color(TextColors.DARK_BLUE)
                    .style(TextStyles.UNDERLINE)
                    .onClick(TextActions.runCommand("/is "+ IsCreate.commandName+" "+entry.getKey()))
                    .build();
            String islandDescriptionStr = entry.getValue().getDescription();
            Text islandDescription = Text.builder(islandDescriptionStr).build();
            int numLines = islandNameStr.length()/charPerRow+islandDescriptionStr.length()/charPerRow+2;
            if(actualLines+numLines>linesPerPage && actualLines!=0){

                bookView.addPage(page);
                page=Text.EMPTY;
                actualLines=0;
            }

            page = Text.of(page, Text.NEW_LINE,
                    islandName, Text.NEW_LINE,
                    islandDescription, Text.NEW_LINE);
            actualLines+=numLines+1;

        }
        bookView.addPage(page);


        return bookView.build();
    }
}
