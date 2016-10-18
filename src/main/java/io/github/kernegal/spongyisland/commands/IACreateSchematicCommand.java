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

package io.github.kernegal.spongyisland.commands;

import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.SpongyIsland;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.ArchetypeVolume;
import org.spongepowered.api.world.schematic.BlockPaletteTypes;
import org.spongepowered.api.world.schematic.Schematic;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

public class IACreateSchematicCommand implements CommandExecutor {
    private File schematicsFolder;

    public IACreateSchematicCommand() {

        this.schematicsFolder = SpongyIsland.getPlugin().getSchematicsFolder();
    }


    @Override
    @Nonnull
    public CommandResult execute(@Nonnull CommandSource source, @Nonnull CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(Text.of(TextColors.RED, "Player only."));
            return CommandResult.success();
        }

        Player player = (Player) source;
        World world = player.getWorld();

        String name=args.<String>getOne("name").orElse("");
        String filename = name+".schematic";

        File schematicFile = new File(schematicsFolder,filename);
        if(schematicFile.exists()){
            source.sendMessage(Text.of(TextColors.RED,"file already exist"));
            return CommandResult.success();

        }
        Vector3i pos1 = new Vector3i(args.<Integer>getOne("x1").orElse(0),
                args.<Integer>getOne("y1").orElse(0),
                args.<Integer>getOne("z1").orElse(0));
        Vector3i pos2 = new Vector3i(args.<Integer>getOne("x2").orElse(0),
                args.<Integer>getOne("y2").orElse(0),
                args.<Integer>getOne("z2").orElse(0));
        Vector3i min = pos1.min(pos2);
        Vector3i max = pos1.max(pos2);

        //TODO make a configurable threshold and control size to not be bigger tan island
        if(min.distance(max)==0){
            source.sendMessage(Text.of(TextColors.RED,"Size too small"));
            return CommandResult.success();

        }

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
            return CommandResult.success();
        }

        SpongyIsland.getPlugin().getLogger().info("Creating schema");
        ArchetypeVolume volume = player.getWorld().createArchetypeVolume(min, max, bedRockPos);

        Schematic schematic = Schematic.builder()
                .volume(volume)
                .metaValue(Schematic.METADATA_AUTHOR, player.getName())
                .metaValue(Schematic.METADATA_NAME, name)
                .metaValue(SpongyIsland.SchematicBedrockPosition,bedRockPos)
                .paletteType(BlockPaletteTypes.LOCAL)
                .build();
        DataContainer schematicData=DataTranslators.SCHEMATIC.translate(schematic);

        try {
            DataFormats.NBT.writeTo(new GZIPOutputStream(new FileOutputStream(schematicFile)), schematicData);
            player.sendMessage(Text.of(TextColors.GREEN, "Saved schematic to " + schematicFile.getAbsolutePath()));
        } catch (Exception e) {
            SpongyIsland.getPlugin().getLogger().error(e.toString());
            player.sendMessage(Text.of(TextColors.DARK_RED, "Error saving schematic: " + e.getMessage()));
            return CommandResult.success();
        }

/*
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
*/
        return CommandResult.success();
    }


}
