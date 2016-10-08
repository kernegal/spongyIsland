package io.github.kernegal.spongyisland.commands;

import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.SpongyIsland;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;

/**
 * Created by somebody on 07/10/2016.
 */
public class IACreateSchematicCommand implements CommandExecutor {
    private SpongyIsland plugin;
    private File schematicsFolder;

    public IACreateSchematicCommand(SpongyIsland plugin) {
        this.plugin=plugin;
        schematicsFolder = new File(plugin.getConfigPath(), "schematics");
        if(!schematicsFolder.exists()){
            schematicsFolder.mkdir();
        }
    }


    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(Text.of(TextColors.RED, "Player only."));
            return CommandResult.success();
        }

        Player player = (Player) source;
        World world = player.getWorld();
        String filename = args.<String>getOne("name").orElse(".")+".schema";

        File schematicFile = new File(schematicsFolder,filename);
        if(schematicFile.exists()){
            source.sendMessage(Text.of(TextColors.RED,"file already exist"));
        }
        Vector3i pos1 = new Vector3i(args.<Integer>getOne("x1").orElse(0),
                args.<Integer>getOne("y1").orElse(0),
                args.<Integer>getOne("z1").orElse(0));
        Vector3i pos2 = new Vector3i(args.<Integer>getOne("x2").orElse(0),
                args.<Integer>getOne("y2").orElse(0),
                args.<Integer>getOne("z2").orElse(0));;
        Vector3i min = pos1.min(pos2);
        Vector3i max = pos1.max(pos2);
        //TODO make a configurable threshold and control size to not be bigger tan island
        if(min.distance(max)==0){
            source.sendMessage(Text.of(TextColors.RED,"Size too small"));
        }

        ConfigurationLoader<CommentedConfigurationNode> schema =
                HoconConfigurationLoader.builder().setFile(schematicFile).build();
        CommentedConfigurationNode schemaRootNode;
        try {
            schemaRootNode = schema.load();
        }catch(IOException e){
            plugin.getLogger().error("Error opening schematic file");
            plugin.getLogger().error(e.toString());
            return CommandResult.success();
        }
        schemaRootNode.getNode("schemaVersion").setValue("0");


        Vector3i bedRockPos=null;
        for (int i = min.getX(); i <= max.getX() && bedRockPos==null; i++) {
            for (int j = max.getY(); j >= min.getY() && bedRockPos==null; j--) {
                for (int k = min.getZ(); k <= max.getZ() && bedRockPos==null; k++) {
                    BlockSnapshot block = world.createSnapshot(new Vector3i(i, j, k));
                    if (block.getState().getType() == BlockTypes.BEDROCK){
                        bedRockPos=new Vector3i(i,j,k);
                    }

                }
            }
        }

        if(bedRockPos==null){
            source.sendMessage(Text.of(TextColors.RED,"A bedrock block needed"));
        }

        plugin.getLogger().info("Creating schema");
        int numBlock=0;
        for (int i = min.getX(); i <= max.getX(); i++) {
            for (int j = min.getY(); j <= max.getY(); j++) {
                for (int k = min.getZ(); k <= max.getZ(); k++) {
                    BlockSnapshot block = world.createSnapshot(new Vector3i(i, j, k));
                    if (block.getState().getType() == BlockTypes.AIR) {
                        continue;
                    }
                    DataContainer dataBlock = block.toContainer();
                    plugin.getLogger().info(dataBlock.toString());
                    ConfigurationNode node = DataTranslators.CONFIGURATION_NODE.translate(dataBlock);
                    node.removeChild("WorldUuid");
                    node.getNode("Position","X").setValue(i-bedRockPos.getX());
                    node.getNode("Position","Y").setValue(j-bedRockPos.getY());
                    node.getNode("Position","Z").setValue(k-bedRockPos.getZ());
                    schemaRootNode.getNode("block"+(numBlock++)).setValue(node);
                }
            }
        }
        try {
            schema.save(schemaRootNode);
        }catch(IOException e){
            plugin.getLogger().error("Error saving schematic file");
            plugin.getLogger().error(e.toString());
            return CommandResult.success();
        }

        return CommandResult.success();
    }


}
