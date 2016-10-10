package io.github.kernegal.spongyisland;

import com.google.inject.Inject;
import io.github.kernegal.spongyisland.commands.IACreateSchematicCommand;
import io.github.kernegal.spongyisland.commands.IsCreate;
import io.github.kernegal.spongyisland.commands.IslandAdminCommand;
import io.github.kernegal.spongyisland.commands.IslandCommand;
import io.github.kernegal.spongyisland.utils.IslandManager;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.io.*;
import java.net.URL;

/**
 * Created by kernegal on 02/10/2016.
 * Base class for SpongyIsland plugin
 */
@Plugin(id = SpongyIsland.pluginId, name = SpongyIsland.pluginName, version = SpongyIsland.version)
public class SpongyIsland {

    public static final String version="0.1.0";
    public static final String pluginId="spongyisland";
    public static final String pluginName="Spongy Island";
    public static final String SchematicBedrockPosition = "bedrock_position";


    @Inject private PluginContainer plugin;
    public PluginContainer getPlugin() {
        return plugin;
    }

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    private File schematicsFolder;

    private IslandManager isManager;

    private DataHolder data;

    public DataHolder getDataHolder() {
        return data;
    }


    private CommentedConfigurationNode globalConfigNode;
    private CommentedConfigurationNode challengesConfigNode;
    private CommentedConfigurationNode valuesConfigNode;



    public File getConfigPath() { return this.configDir; }
    public File getSchematicsFolder() { return schematicsFolder; }
    public Logger getLogger() {
        return logger;
    }


    @Listener
    public void onPreInitialization(GamePreInitializationEvent event) {

        if (!configDir.exists()) {
            configDir.mkdir();
        }

        schematicsFolder = new File(getConfigPath(), "schematics");
        if(!schematicsFolder.exists()){
            schematicsFolder.mkdir();

            //URL inputUrl=this.getClass().getResource("defaultSchematics/default.schematic");
            File defSchem= new File(schematicsFolder, "default.schematic");


            InputStream ddlStream = this.getClass().getResourceAsStream("defaultSchematics/default.schematic");

            try (FileOutputStream fos = new FileOutputStream(defSchem)){
                byte[] buf = new byte[2048];
                int r;
                while(-1 != (r = ddlStream.read(buf))) {
                    fos.write(buf, 0, r);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        File globalConfig = new File(configDir, "config.conf");
        ConfigurationLoader<CommentedConfigurationNode> globalConfigManager =
                HoconConfigurationLoader.builder().setFile(globalConfig).build();

        File challengesConfig = new File(configDir, "challenges.conf");
        ConfigurationLoader<CommentedConfigurationNode> challengesConfigManager =
                HoconConfigurationLoader.builder().setFile(challengesConfig).build();

        File valuesConfig = new File(configDir, "blockvalues.conf");
        ConfigurationLoader<CommentedConfigurationNode> valuesConfigManager =
                HoconConfigurationLoader.builder().setFile(valuesConfig).build();
        try {
            if(!globalConfig.exists()){
                globalConfigNode = HoconConfigurationLoader.builder().setURL(this.getClass().getResource("defaultConfigs/config.conf")).build().load();
                globalConfigManager.save(globalConfigNode);
            }
            else {
                globalConfigNode = globalConfigManager.load();
            }

            if(!challengesConfig.exists()){
                challengesConfigNode = HoconConfigurationLoader.builder().setURL(this.getClass().getResource("defaultConfigs/challenges.conf")).build().load();
                challengesConfigManager.save(challengesConfigNode);
            }
            else{
                challengesConfigNode = challengesConfigManager.load();
            }

            if(!valuesConfig.exists()) {
                valuesConfigNode = HoconConfigurationLoader.builder().setURL(this.getClass().getResource("defaultConfigs/blockvalues.conf")).build().load();
                valuesConfigManager.save(valuesConfigNode);
            }
            else{
                valuesConfigNode = valuesConfigManager.load();

            }

        } catch(IOException e) {
            getLogger().error(e.toString());
        }



    }

    @Listener
    public void init(GameInitializationEvent event) {

        getLogger().info("Preparing data");
        data = new DataHolder(this);

        isManager=new IslandManager(this,data,globalConfigNode);

        prepareCommands();

        getLogger().info("sending some weird stuff to test my plugin");
        getLogger().info("SDASGdfHFDHGJSGJ SJ GJSGJ S JFJFGGSJGJGSJGJNSF \n" +
                "SGJGFJFJD GS GJGSJ GF JGJ F \n" +
                "SGJ S J GJ GSJ GJ   SJ SJ\n");

    }


    private void prepareCommands(){

        //Island commands
        //is create
        CommandSpec newIsCreateCommand =  CommandSpec.builder()
                .description(Text.of("list admin commands"))
                .permission(SpongyIsland.pluginId+".command.admin")
                .arguments(GenericArguments.string(Text.of("schematic")))
                .executor(new IsCreate(this,isManager))
                .build();

        //is
        CommandSpec newIslandCommand =  CommandSpec.builder()
                .description(Text.of("list admin commands"))
                .permission(SpongyIsland.pluginId+".command.admin")
                .child(newIsCreateCommand,"create","c")
                .executor(new IslandCommand(this))
                .build();

        Sponge.getCommandManager().register(this, newIslandCommand, "island", "is");

        //Admin commands
        CommandSpec newSchematicCommand = CommandSpec.builder()
                .description(Text.of("Creates a new schematic"))
                .permission(SpongyIsland.pluginId+".command.schematics")
                .arguments(
                        GenericArguments.string(Text.of("name")),
                        GenericArguments.integer(Text.of("x1")),
                        GenericArguments.integer(Text.of("y1")),
                        GenericArguments.integer(Text.of("z1")),
                        GenericArguments.integer(Text.of("x2")),
                        GenericArguments.integer(Text.of("y2")),
                        GenericArguments.integer(Text.of("z2"))
                )
                .executor(new IACreateSchematicCommand(this))
                .build();

        CommandSpec adminCommand = CommandSpec.builder()
                .description(Text.of("list admin commands"))
                .permission(SpongyIsland.pluginId+".command.admin")
                .child(newSchematicCommand,"newSchematic","ns")
                .executor(new IslandAdminCommand(this))
                .build();

        Sponge.getCommandManager().register(this, adminCommand, "islandAdmin");
    }



}
