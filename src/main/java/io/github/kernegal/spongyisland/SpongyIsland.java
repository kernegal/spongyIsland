package io.github.kernegal.spongyisland;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.slf4j.Logger;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Created by kernegal on 02/10/2016.
 * Base class for SpongyIsland plugin
 */
@Plugin(id = "spongyisland", name = "Spongy Island", version = SpongyIsland.version)
public class SpongyIsland {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    private DataHolder data;


    public static final String version="0.1.0";

    public File getConfigPath() { return this.configDir; }
    public Logger getLogger() {
        return logger;
    }


    @Listener
    public void onPreInitialization(GamePreInitializationEvent event) {

        if (!configDir.exists()) {
            configDir.mkdir();
        }

        File globalConfig = new File(configDir + "/config.conf");
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
        getLogger().info("sending some weird stuff to test my plugin");
        getLogger().info("SDASGdfHFDHGJSGJ SJ GJSGJ S JFJFGGSJGJGSJGJNSF \n" +
                "SGJGFJFJD GS GJGSJ GF JGJ F \n" +
                "SGJ S J GJ GSJ GJ   SJ SJ\n");

    }




}
