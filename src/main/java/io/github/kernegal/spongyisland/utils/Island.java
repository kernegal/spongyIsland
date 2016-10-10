package io.github.kernegal.spongyisland.utils;

import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.SpongyIsland;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.ArchetypeVolume;
import org.spongepowered.api.world.schematic.Schematic;

import java.io.File;
import java.util.Optional;

/**
 * Created by kernegal on 09/10/2016.
 * Class for creating the islands that are going to be pasted as players islands
 */
public class Island {
    private ArchetypeVolume volume;
    private Vector3i bedrockPosition, signPosition;
    private SpongyIsland plugin;
    private String name, description;

    public Island(Schematic schematic, SpongyIsland plugin, String name, String description) {
        this.name=name;
        this.description=description;
        this.plugin=plugin;



        Vector3i min=schematic.getBlockMin(),max=schematic.getBlockMax();

        for (int i = min.getX(); i <= max.getX() && bedrockPosition==null; i++) {
            for (int j = max.getY(); j >= min.getY() && bedrockPosition==null; j--) {
                for (int k = min.getZ(); k <= max.getZ() && bedrockPosition==null; k++) {
                    BlockState block = schematic.getBlock(i, j, k);
                    if (block.getType() == BlockTypes.BEDROCK){
                        bedrockPosition=new Vector3i(i,j,k);
                    }
                    if(block.getType() == BlockTypes.STANDING_SIGN){
                        signPosition = new Vector3i(i,j,k);

                    }
                }
            }
        }
        volume = schematic;
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

        position = position.sub(bedrockPosition);
        volume.apply(new Location<>(world, position), BlockChangeFlag.ALL,
                Cause.of(NamedCause.of("plugin", plugin.getPlugin()), NamedCause.source(player)));

        if(signPosition!=null) {
            Location<World> signLocation = new Location<>(world, signPosition.add(position));
            Optional<TileEntity> signOptional = signLocation.getTileEntity();

            plugin.getLogger().info(signPosition.toString());
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
}
