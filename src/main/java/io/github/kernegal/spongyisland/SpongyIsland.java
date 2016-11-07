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

package io.github.kernegal.spongyisland;

import com.google.inject.Inject;
import io.github.kernegal.spongyisland.commandConfirmation.ConfirmationService;
import io.github.kernegal.spongyisland.commands.*;
import io.github.kernegal.spongyisland.utils.IslandManager;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;

import java.io.*;

/**
 * Created by kernegal on 02/10/2016.
 * Base class for SpongyIsland pluginContainer
 */
@Plugin(id = SpongyIsland.pluginId,
        name = SpongyIsland.pluginName,
        version = SpongyIsland.version,
        description = SpongyIsland.pluginDescription)
public class SpongyIsland {

    public static final String version="0.3.2";
    public static final String pluginId="spongyisland";
    public static final String pluginName="Spongy Island";
    public static final String pluginDescription="A skyblock plugin for sponge";
    public static final String SchematicBedrockPosition = "bedrock_position";


    @Inject private PluginContainer pluginContainer;
    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    @Inject
    private Logger logger;

    @Inject private Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    private File schematicsFolder;

    private IslandManager isManager;

    private DataHolder data;
    public DataHolder getDataHolder() { return data; }

    private ConfirmationService service;
    public ConfirmationService getService() { return service; }

    private CommentedConfigurationNode globalConfigNode;
    private CommentedConfigurationNode challengesConfigNode;
    private CommentedConfigurationNode valuesConfigNode;
    private CommentedConfigurationNode biomeShopConfigNode;
    private CommentedConfigurationNode islandCommandConfigNode;

    public File getConfigPath() { return this.configDir; }
    public File getSchematicsFolder() { return schematicsFolder; }
    public Logger getLogger() {
        return logger;
    }

    private static SpongyIsland plugin;
    public static SpongyIsland getPlugin(){
        return plugin;
    }

    private EconomyService economyService;

    public EconomyService getEconomyService(){
        return economyService;
    }

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getService().equals(EconomyService.class)) {
            economyService = (EconomyService) event.getNewProviderRegistration().getProvider();
        }
    }

    @Listener
    public void onPreInitialization(GamePreInitializationEvent event) {

        plugin=this;

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
                fos.close();
            }  catch (IOException e) {
                e.printStackTrace();
            }

            File hardSchem= new File(schematicsFolder, "harder.schematic");


            InputStream hardddlStream = this.getClass().getResourceAsStream("defaultSchematics/harder.schematic");

            try (FileOutputStream fos = new FileOutputStream(hardSchem)){
                byte[] buf = new byte[2048];
                int r;
                while(-1 != (r = hardddlStream.read(buf))) {
                    fos.write(buf, 0, r);
                }
                fos.close();
            }  catch (IOException e) {
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

        File biomeShopConfig = new File(configDir, "biomeshop.conf");
        ConfigurationLoader<CommentedConfigurationNode> biomeShopConfigManager =
                HoconConfigurationLoader.builder().setFile(biomeShopConfig).build();

        File islandCommandConfig = new File(configDir, "islandCommand.conf");
        ConfigurationLoader<CommentedConfigurationNode> islandCommandConfigManager =
                HoconConfigurationLoader.builder().setFile(islandCommandConfig).build();
        try {
            if(!globalConfig.exists()){
                globalConfigNode = HoconConfigurationLoader.builder().setURL(this.getClass().getResource("defaultConfigs/config.conf")).build().load();
                globalConfigManager.save(globalConfigNode);
            }
            else {
                globalConfigNode = globalConfigManager.load();
            }

            if(!challengesConfig.exists()){
                InputStream ddlStream = this.getClass().getResourceAsStream("defaultConfigs/challenges.conf");

                try (FileOutputStream fos = new FileOutputStream(challengesConfig)){
                    byte[] buf = new byte[2048];
                    int r;
                    while(-1 != (r = ddlStream.read(buf))) {
                        fos.write(buf, 0, r);
                    }
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                challengesConfigNode = challengesConfigManager.load();
                //challengesConfigNode = HoconConfigurationLoader.builder().setURL(this.getClass().getResource("defaultConfigs/challenges.conf")).build().load();
                //challengesConfigManager.save(challengesConfigNode);
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

            if(!biomeShopConfig.exists()) {
                biomeShopConfigNode = HoconConfigurationLoader.builder().setURL(this.getClass().getResource("defaultConfigs/biomeshop.conf")).build().load();
                biomeShopConfigManager.save(biomeShopConfigNode);
            }
            else{
                biomeShopConfigNode = biomeShopConfigManager.load();

            }

            if(!islandCommandConfig.exists()) {
                islandCommandConfigNode = HoconConfigurationLoader.builder().setURL(this.getClass().getResource("defaultConfigs/islandcommand.conf")).build().load();
                islandCommandConfigManager.save(islandCommandConfigNode);
            }
            else{
                islandCommandConfigNode = islandCommandConfigManager.load();

            }

        } catch(IOException e) {
            getLogger().error(e.toString());
        }


    }

    @Listener
    public void init(GameInitializationEvent event) {


        getLogger().info("Preparing data");
        data = new DataHolder(challengesConfigNode,globalConfigNode);
        //Sponge.getEventManager().registerListeners(this, data);

        isManager=new IslandManager(data,globalConfigNode);

        service = new ConfirmationService();

        Sponge.getEventManager().registerListeners(this, new IslandProtection(data,globalConfigNode.getNode("island","radius").getInt(),globalConfigNode.getNode("island","protectionRadius").getInt()) );

        prepareCommands();

        getLogger().info("Initialization complete");



    }

    @Listener
    public void playerLogin(ClientConnectionEvent.Join event){
        data.playerLogin(event.getTargetEntity());
    }


    private void prepareCommands(){

        //Island commands
        //is create
        CommandSpec newIsCreateCommand =  CommandSpec.builder()
                .description(Text.of("Create new island"))
                .arguments(GenericArguments.optional(GenericArguments.string(Text.of("schematic"))))
                .executor(new IsCreate(isManager,data))
                .build();

        //is home
        CommandSpec newIsHomeCommand =  CommandSpec.builder()
                .description(Text.of("teleport to island"))
                .executor(new IsHome(data))
                .build();

        //is sethome
        CommandSpec newIsSetHomeCommand =  CommandSpec.builder()
                .description(Text.of("set your island home position"))
                .executor(new IsSetHome(data,globalConfigNode.getNode("island","radius").getInt(),globalConfigNode.getNode("island","protectionRadius").getInt()))
                .build();

        //is level
        CommandSpec newIsLevelCommand =  CommandSpec.builder()
                .description(Text.of("Calcule your island level"))
                .executor(new IsLevel(data,
                        valuesConfigNode,
                        globalConfigNode.getNode("island","radius").getInt(),
                        globalConfigNode.getNode("island","protectionRadius").getInt(),
                        100,
                        globalConfigNode.getNode("general","level_wait").getInt()))
                .build();

        //is top
        CommandSpec topIslandCommand =  CommandSpec.builder()
                .description(Text.of("show top islands"))
                .executor(new TopCommand(data))
                .build();

        //is biomeShop
        CommandSpec isBiomeShopCommand =  CommandSpec.builder()
                .description(Text.of("show top islands"))
                .arguments(GenericArguments.optional(GenericArguments.string(Text.of("biome"))))
                .executor(new IBiomeShop(
                        data,
                        globalConfigNode.getNode("island","radius").getInt(),
                        biomeShopConfigNode,
                        globalConfigNode.getNode("general","economy").getBoolean(true)
                ))
                .build();

        //is
        CommandSpec newIslandCommand =  CommandSpec.builder()
                .description(Text.of("list island commands"))
                .child(newIsCreateCommand,IsCreate.commandName,"reset")
                .child(newIsHomeCommand,"home", "h")
                .child(newIsSetHomeCommand,"sethome", "sh")
                .child(newIsLevelCommand,"level", "l")
                .child(topIslandCommand,"top")
                .child(isBiomeShopCommand,IBiomeShop.commandName)
                .executor(new IslandCommand(islandCommandConfigNode))
                .build();

        Sponge.getCommandManager().register(this, newIslandCommand, "island", "is");

        //challenges
        CommandSpec completeChallengeCommand =  CommandSpec.builder()
                .description(Text.of("Complete a challenge"))
                .arguments(
                        GenericArguments.string(Text.of(CComplete.argChallenge)),
                        GenericArguments.optional(GenericArguments.integer(Text.of(CComplete.argTimes)))
                )
                .executor(new CComplete(data))
                .build();

        CommandSpec challengesCommand =  CommandSpec.builder()
                .description(Text.of("show challenges"))
                .arguments(GenericArguments.optional(GenericArguments.string(Text.of(ChallengesCommand.argsName))))
                .executor(new ChallengesCommand(
                        challengesConfigNode,
                        data
                ))
                .child(completeChallengeCommand,CComplete.commandName,""+CComplete.commandName.charAt(0))
                .build();

        Sponge.getCommandManager().register(this, challengesCommand, ChallengesCommand.commandName, ""+ChallengesCommand.commandName.charAt(0) );
        //Sponge.getCommandManager().register(this, completeChallengeCommand, "completeChallenge", "cc");

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
                .executor(new IACreateSchematicCommand())
                .build();

        CommandSpec newValueCommand = CommandSpec.builder()
                .description(Text.of("Creates a new schematic"))
                .permission(SpongyIsland.pluginId+".command.values")
                .executor(new IAShowBlockVariantsCommand())
                .build();

        CommandSpec unlinkIslandCommand = CommandSpec.builder()
                .description(Text.of("Makes the island to not have an owner and marks it to avoid being deleted"))
                .permission(SpongyIsland.pluginId+".command.unlink")
                .executor(new IAUnlinkIsland(data))
                .build();

        CommandSpec adminCommand = CommandSpec.builder()
                .description(Text.of("list admin commands"))
                .permission(SpongyIsland.pluginId+".command.admin")
                .child(newSchematicCommand,"newSchematic","ns")
                .child(newValueCommand,"newvalue", "nv")
                .child(unlinkIslandCommand,"unlink")
                .executor(new IslandAdminCommand())
                .build();

        Sponge.getCommandManager().register(this, adminCommand, "islandAdmin");

        //confirmation
        CommandSpec confirmationCommand = CommandSpec.builder()
                .description(Text.of("confirm commands"))
                .arguments(GenericArguments.string(Text.of(ConfirmationService.argumentString)))
                .executor(service)
                .build();
        Sponge.getCommandManager().register(this, confirmationCommand, "isconfirm");
    }



}
