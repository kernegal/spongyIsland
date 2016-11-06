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

import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.SpongyIsland;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.extent.ArchetypeVolume;
import org.spongepowered.api.world.schematic.Schematic;

import java.util.Optional;

/**
 * Created by kernegal on 09/10/2016.
 * Class for creating the islands that are going to be pasted as players islands
 */
public class Island {
    private ArchetypeVolume volume;
    //private Vector3i bedrockPosition;
    private Vector3i signPosition;
    private String name, description;
    private BiomeType biome;

    public Island(Schematic schematic, String name, String description, BiomeType biome) {
        this.name=name;
        this.description=description;

        Vector3i min=schematic.getBlockMin(),max=schematic.getBlockMax();

        for (int i = min.getX(); i <= max.getX(); i++) {
            for (int j = max.getY(); j >= min.getY(); j--) {
                for (int k = min.getZ(); k <= max.getZ(); k++) {
                    BlockState block = schematic.getBlock(i, j, k);
                    /*if (block.getType() == BlockTypes.BEDROCK){
                        bedrockPosition=new Vector3i(i,j,k);
                    }*/
                    if(block.getType() == BlockTypes.STANDING_SIGN){
                        signPosition = new Vector3i(i,j,k);

                    }
                }
            }
        }
        volume = schematic;
        this.biome=biome;
        //bedrockPosition = new Vector3i((Vector3i) schematic.getMetadata().get(DataQuery.of(SpongyIsland.SchematicBedrockPosition)).get());
    }


    /**
     * Paste this island into the specified world positions and return 0 if all went ok
     * @param world the world where the island will be pasted
     * @param position the location of the future island
     * @param player the player who issued the command
     * @return 0 if ok, error code otherwise
     */
    public int place (World world, Vector3i position, Player player){
        if (volume == null) {
            return -1;
        }

        //position = position.sub(bedrockPosition);
        volume.apply(new Location<>(world, position), BlockChangeFlag.ALL,
                Cause.of(NamedCause.of("plugin", SpongyIsland.getPlugin().getPluginContainer())/*, NamedCause.source(player)*/));

        if(signPosition!=null) {
            Location<World> signLocation = new Location<>(world, signPosition.add(position));
            Optional<TileEntity> signOptional = signLocation.getTileEntity();

            SpongyIsland.getPlugin().getLogger().info(signPosition.toString());
            if (signOptional.isPresent()) {
                TileEntity s = signOptional.get();
                Optional<SignData> signDataOpt = s.get(SignData.class);
                if (signDataOpt.isPresent()) {
                    SignData sign = signDataOpt.get();
                    sign = sign.set(sign.lines().set(0, Text.of(TextColors.BLUE, "[Skyblock]")));
                    sign = sign.set(sign.lines().set(1, Text.of(player.getName())));
                    sign = sign.set(sign.lines().set(2, Text.of("Do not fall!!!")));
                    sign = sign.set(sign.lines().set(3, Text.of("Beware")));
                    s.offer(sign);
                }

            }
        }
        return 0;


    }

    public BiomeType getBiome() {
        return biome;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
