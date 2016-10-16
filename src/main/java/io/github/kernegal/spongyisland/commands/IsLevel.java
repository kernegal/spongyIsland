package io.github.kernegal.spongyisland.commands;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.DataHolder;
import io.github.kernegal.spongyisland.SpongyIsland;
import io.github.kernegal.spongyisland.utils.IslandPlayer;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import javax.annotation.Nonnull;
import java.util.Map;


/**
 * Created by kernegal on 13/10/2016.
 */
public class IsLevel implements CommandExecutor {
    DataHolder data;
    ConfigurationNode values;
    int islandRadius, protectionRadius;

    public IsLevel(DataHolder data, ConfigurationNode values, int islandRadius, int protectionRadius) {
        this.data = data;
        this.values = values;
        this.islandRadius = islandRadius;
        this.protectionRadius = protectionRadius;
    }

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull CommandSource source, @Nonnull CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(Text.of(TextColors.RED, "Player only."));
            return CommandResult.success();
        }
        Player player = (Player) source;
        IslandPlayer playerData = data.getPlayerData(player.getUniqueId());
        Vector2i islandCoordinates=playerData.getIsPosition().mul(islandRadius*2);

        Vector2i min2 = islandCoordinates.sub(protectionRadius,protectionRadius);
        Vector2i max2 = islandCoordinates.add(protectionRadius,protectionRadius);

        Vector3i min = new Vector3i(min2.getX(),0,min2.getY());
        Vector3i max = new Vector3i(max2.getX(),255,max2.getY());


        World world = Sponge.getServer().getWorld("world").get();
        Extent view = world.getExtentView(min, max);
        int sum = view.getBlockWorker(Cause.of(NamedCause.of("plugin", SpongyIsland.getPlugin().getPluginContainer()))).reduce(
                (vol, x, y, z, red) -> vol.getBlockType(x, y, z).equals(BlockTypes.AIR) ?
                        red :
                        red+getValue(vol.getBlock(x,y,z))
                ,
                (a, b) -> a + b,
                0);
        player.sendMessage(Text.of("Level: " + sum));

        return CommandResult.success();
    }

    private int getValue(BlockState block){

        ConfigurationNode blockNode=values.getNode(block.getType().getId());
        for(Map.Entry<BlockTrait<?>, ?> entry : block.getTraitMap().entrySet()){
            int value=blockNode.getNode(entry.getKey().getName()+"."+entry.getValue().toString()).getInt(-1);
            if(value != -1) return value;
        }
        int defaultValue = blockNode.getNode("_default").getInt(-1);
        if(defaultValue!=-1) return defaultValue;


        return 1;
    }
}
