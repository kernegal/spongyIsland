package io.github.kernegal.spongyisland;

import com.google.inject.Inject;
import io.github.kernegal.spongyisland.commands.IACreateSchematicCommand;
import io.github.kernegal.spongyisland.commands.IslandAdminCommand;
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
import org.spongepowered.api.text.Text;

import java.io.File;
import java.io.IOException;

/**
 * Created by kernegal on 02/10/2016.
 * Base class for SpongyIsland plugin
 */
@Plugin(id = SpongyIsland.pluginId, name = SpongyIsland.pluginName, version = SpongyIsland.version)
public class SpongyIsland {

    public static final String version="0.1.0";
    public static final String pluginId="spongyisland";
    public static final String pluginName="Spongy Island";

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    private DataHolder data;



    public File getConfigPath() { return this.configDir; }
    public Logger getLogger() {
        return logger;
    }


    @Listener
    public void onPreInitialization(GamePreInitializationEvent event) {

        if (!configDir.exists()) {
            configDir.mkdir();
        }



        File globalConfig = new File(configDir , "config.conf");
        ConfigurationLoader<CommentedConfigurationNode> globalConfigManager =
                HoconConfigurationLoader.builder().setFile(globalConfig).build();
        CommentedConfigurationNode globalConfigNode;

        File challengesConfig = new File(configDir + "/challenges.conf");
        ConfigurationLoader<CommentedConfigurationNode> challengesConfigManager =
                HoconConfigurationLoader.builder().setFile(challengesConfig).build();
        CommentedConfigurationNode challengesConfigNode;

        File valuesConfig = new File(configDir + "/blockvalues.conf");
        ConfigurationLoader<CommentedConfigurationNode> valuesConfigManager =
                HoconConfigurationLoader.builder().setFile(valuesConfig).build();
        CommentedConfigurationNode valuesConfigNode;
        try {
            globalConfigNode = globalConfigManager.load();
            globalConfigManager.save(globalConfigNode);

            challengesConfigNode = challengesConfigManager.load();
            challengesConfigManager.save(challengesConfigNode);

            valuesConfigNode = valuesConfigManager.load();
            valuesConfigManager.save(valuesConfigNode);

        } catch(IOException e) {
            // error
        }

    }

    @Listener
    public void init(GameInitializationEvent event) {
        getLogger().info("Preparing data");
        data = new DataHolder(this);

        prepareCommands();

        getLogger().info("sending some weird stuff to test my plugin");
        getLogger().info("SDASGdfHFDHGJSGJ SJ GJSGJ S JFJFGGSJGJGSJGJNSF \n" +
                "SGJGFJFJD GS GJGSJ GF JGJ F \n" +
                "SGJ S J GJ GSJ GJ   SJ SJ\n");

    }


    private void prepareCommands(){

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
