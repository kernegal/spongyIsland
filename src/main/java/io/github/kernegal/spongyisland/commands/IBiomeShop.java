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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.DataHolder;
import io.github.kernegal.spongyisland.SpongyIsland;
import io.github.kernegal.spongyisland.utils.IslandPlayer;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.api.world.extent.worker.procedure.BiomeVolumeFiller;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class IBiomeShop implements CommandExecutor {
    public static final String commandName="biomeshop";
    private DataHolder data;
    private int islandRadius;
    private ConfigurationNode biomesValues;
    private boolean useEconomy;

    public IBiomeShop(DataHolder data, int islandRadius, ConfigurationNode biomesValues, boolean useEconomy) {
        this.data = data;
        this.islandRadius = islandRadius;
        this.biomesValues = biomesValues;
        this.useEconomy = useEconomy;
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

        if(playerData.getIsland()==-1){
            player.sendMessage(Text.of(TextColors.DARK_RED,"You need an island"));
            return CommandResult.success();
        }

        Vector2i islandCoordinates=playerData.getIsPosition().mul(islandRadius*2);

        Vector2i min2 = islandCoordinates.sub(islandRadius,islandRadius);
        Vector2i max2 = islandCoordinates.add(islandRadius,islandRadius);

        Vector3i min = new Vector3i(min2.getX(),0,min2.getY());
        Vector3i max = new Vector3i(max2.getX(),255,max2.getY());

        Optional<World> world = Sponge.getServer().getWorld("world");
        if(!world.isPresent()){
            SpongyIsland.getPlugin().getLogger().error("World not found when changing biome");
            return CommandResult.success();
        }
        Extent view = world.get().getExtentView(min, max);
        Optional<String> opBiome = args.<String>getOne("biome");
        if(!opBiome.isPresent()){

            player.sendBookView(getBiomeBookView());
            //printBiomes(player);
            return CommandResult.success();
        }
        String biomeStr=opBiome.get();
        BiomeType biome=getBiomeFromText(biomeStr);
        if(biome==null){
            player.sendMessage(Text.of(TextColors.DARK_RED,"biome not valid"));
            return CommandResult.success();
        }
        ConfigurationNode selectedBiome = biomesValues.getNode(biomeStr);
        if(selectedBiome.getString()==null){
            player.sendMessage(Text.of(TextColors.DARK_RED,"biome not valid"));
            return CommandResult.success();
        }
        if(useEconomy){
            EconomyService economyService=SpongyIsland.getPlugin().getEconomyService();
            Optional<UniqueAccount> uOpt = economyService.getOrCreateAccount(player.getUniqueId());
            if (uOpt.isPresent()) {
                UniqueAccount account = uOpt.get();

                BigDecimal requiredAmount = BigDecimal.valueOf(selectedBiome.getNode("cost").getDouble());

                TransactionResult result = account.withdraw(economyService.getDefaultCurrency(),
                        requiredAmount, Cause.source(this).build());
                if(result.getResult() == ResultType.ACCOUNT_NO_FUNDS){
                    player.sendMessage(Text.of(TextColors.DARK_RED,"Insufficient funds"));
                    return CommandResult.success();
                }
                else if (result.getResult() == ResultType.FAILED ) {
                    player.sendMessage(Text.of(TextColors.DARK_RED,"Some problem happened"));
                    return CommandResult.success();
                }
            }

        }

        view.getBiomeWorker().fill(new BiomeVolumeFiller() {
            @Override
            @Nonnull
            public BiomeType produce(int x, int y, int z) {
                return biome;
            }
        });
        return CommandResult.success();
    }

    private void printBiomes(Player player){
        player.sendMessage(Text.of("Possible biomes are:"));

        for(Map.Entry<Object, ? extends ConfigurationNode> entry : biomesValues.getChildrenMap().entrySet()) {
            player.sendMessage(Text.of(entry.getKey()));
        }
    }
    private BookView getBiomeBookView(){
        BookView.Builder bookView = BookView.builder()
                .title(Text.of("Biome Shop"))
                .author(Text.of("SpongyIsland"));

        Text page=Text.EMPTY;
        final int charPerRow=20;
        final int linesPerPage=14;
        int actualLines=0;
        for(Map.Entry<Object, ? extends ConfigurationNode> entry : biomesValues.getChildrenMap().entrySet()) {
            String biomeNameStr = entry.getValue().getNode("friendly_name").getString("");
            Text biomeName = Text.builder(biomeNameStr)
                    .color(TextColors.DARK_BLUE)
                    .style(TextStyles.UNDERLINE)
                    .onClick(TextActions.runCommand("/is "+commandName+" "+entry.getKey()))
                    .build();
            String biomeDescriptionStr = entry.getValue().getNode("description").getString("");
            Text biomeDescription = Text.builder(biomeDescriptionStr).build();
            String biomeCostStr = entry.getValue().getNode("cost").getString("");
            Text biomeCost = Text.builder("Value: "+biomeCostStr).build();
            int numLines = biomeNameStr.length()/charPerRow+biomeDescriptionStr.length()/charPerRow+3;
            if(actualLines+numLines>linesPerPage && actualLines!=0){

                bookView.addPage(page);
                page=Text.EMPTY;
                actualLines=0;
            }

            page = Text.of(page, Text.NEW_LINE,
                    biomeName, Text.NEW_LINE,
                    biomeDescription, Text.NEW_LINE,
                    biomeCost, Text.NEW_LINE);
            actualLines+=numLines+1;

        }
        bookView.addPage(page);


        return bookView.build();

    }

    private BiomeType getBiomeFromText(String biome){
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

            default: return null;
        }
    }
}
