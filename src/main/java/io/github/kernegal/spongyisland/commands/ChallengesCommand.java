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

import io.github.kernegal.spongyisland.DataHolder;
import io.github.kernegal.spongyisland.SpongyIsland;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import javax.annotation.Nonnull;
import java.util.*;

public class ChallengesCommand implements CommandExecutor {

    public static final String argsName = "level";
    public static final String commandName = "challenges";

    private ConfigurationNode challenges;
    private DataHolder data;

    public ChallengesCommand(ConfigurationNode challenges, DataHolder data) {
        this.challenges = challenges;
        this.data = data;
    }

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull CommandSource source, @Nonnull CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(Text.of(TextColors.RED, "Player only."));
            return CommandResult.success();
        }
        Player player = (Player) source;

        String level;

        Optional<String> opLevel = args.getOne(argsName);
        if(opLevel.isPresent()){
            level=opLevel.get();
        }
        else{
            level=data.getLastLevelIssued(player.getUniqueId());
        }
        String levelName = challenges.getNode("level_list", level, "friendly_name").getString("");
        if(levelName.isEmpty()){
            player.sendMessage(Text.of(TextColors.RED,"The selected level don't exist"));
            return CommandResult.success();
        }

        BookView.Builder bookView = BookView.builder()
                .title(Text.of("Challenges"))
                .author(Text.of("SpongyIsland"));
        Text page=Text.of("Level ", TextStyles.BOLD,levelName);
        final int charPerRow=20;
        final int linesPerPage=14;
        int actualLines=1;

        ConfigurationNode challengeList= challenges.getNode("challenge_list");

        for(Map.Entry<Object, ? extends ConfigurationNode> entry :
                challengeList.getChildrenMap().entrySet()) {
            if(!entry.getValue().getNode("level").getString("").equals(level))
                continue;

            Text challengeDescription = Text.of(TextColors.AQUA,entry.getValue().getNode("description").getString());
            Text challengeReward = Text.of("Reward: ",TextColors.GREEN,entry.getValue().getNode("reward_text").getString());
            String challengeStr= entry.getValue().getNode("friendly_name").getString("NAME ERROR");
            Text.Builder challengeBuilder = Text.builder(challengeStr);

            if(!data.canCompleteChallenge(player.getUniqueId(),entry.getKey().toString())){
                if(data.challengeIsCompleted(player.getUniqueId(),entry.getKey().toString()))
                    challengeBuilder.color(TextColors.DARK_GREEN);
                else
                    challengeBuilder.color(TextColors.DARK_BLUE);
                challengeBuilder.style(TextStyles.UNDERLINE)
                        .onHover(TextActions.showText(Text.of(challengeDescription,
                                Text.NEW_LINE,
                                challengeReward
                        )))
                        .onClick(TextActions.runCommand("/"+commandName+" "+CComplete.commandName+" "+entry.getKey()));
            }
            else {
                challengeBuilder.color(TextColors.LIGHT_PURPLE)
                        .onHover(TextActions.showText(Text.of("You can't complete that challenge any more times")));
            }
            Text challenge = challengeBuilder.build();
            int lines=(challengeStr.length()+2)/charPerRow+1;
            if(actualLines+lines>linesPerPage && actualLines!=0){

                bookView.addPage(page);
                page=Text.EMPTY;
                actualLines=0;
            }

            page = Text.of(page, Text.NEW_LINE,
                    "* ",challenge);
            actualLines+=lines;

        }
        bookView.addPage(page);
        page=Text.EMPTY;
        actualLines=0;


        ConfigurationNode levelList= challenges.getNode("level_list");
        Map<String, ArrayList<String> > tree = new HashMap<>();
        for(Map.Entry<Object, ? extends ConfigurationNode> entry :
                levelList.getChildrenMap().entrySet()) {

            String requiredLevel = entry.getValue().getNode("required_level").getString("");
            ArrayList<String> arrayList = tree.get(requiredLevel);
            if(arrayList==null){
                arrayList=new ArrayList<>();
                tree.put(requiredLevel,arrayList);
            }
            arrayList.add(entry.getKey().toString());
            
        }


        LinkedList<String> openNodes = new LinkedList<>();
        openNodes.offerLast(challenges.getNode("root_level").getString());
        while(!openNodes.isEmpty()){
            String currentLevel = openNodes.pollLast();
            ConfigurationNode levelNode= levelList.getNode(currentLevel);

            //Text challengeDescription = Text.of(TextColors.AQUA,entry.getValue().getNode("description").getString());
            //Text challengeReward = Text.of("Reward: ",TextColors.GREEN,entry.getValue().getNode("reward_text").getString());
            String levelStr= levelNode.getNode("friendly_name").getString("NAME ERROR");
            Text.Builder textBuilder = Text.builder(levelStr);
            if(data.hasAccessToLevel(player.getUniqueId(),currentLevel)) {
                //Text challengeDescription = Text.of(TextColors.AQUA,entry.getValue().getNode("description").getString());
                int challengesCompleted = data.getCompletedChallengesInLevel(player.getUniqueId(),currentLevel);
                int requiredChallenges = levelNode.getNode("required_challenges").getInt(0);
                if(challengesCompleted<requiredChallenges){
                    textBuilder.color(TextColors.BLUE)
                            .style(TextStyles.UNDERLINE);
                }
                else{
                    textBuilder.color(TextColors.DARK_GREEN)
                            .style(TextStyles.UNDERLINE);

                }
                textBuilder.onHover(TextActions.showText(Text.of("Completed challenges: ",challengesCompleted,"/",requiredChallenges)))
                        .onClick(TextActions.runCommand("/"+commandName+" "+currentLevel));



            }
            else{
                String parentLevelStr= levelList.getNode(levelNode.getNode("required_level").getString("NAME ERROR"),"friendly_name").getString();

                textBuilder.color(TextColors.DARK_RED)
                        .onHover(TextActions.showText(Text.of("Complete the level ",parentLevelStr, " to access this level.")));
            }
            Text actualLevelName=textBuilder.build();
            int lines=(levelStr.length()+2)/charPerRow+1;
            if(actualLines+lines>linesPerPage && actualLines!=0){

                bookView.addPage(page);
                page=Text.EMPTY;
                actualLines=0;
            }

            page = Text.of(page, Text.NEW_LINE,
                    "* ",actualLevelName);
            actualLines+=lines;

            ArrayList<String> empty = new ArrayList<>();
            tree.getOrDefault(currentLevel, empty).forEach(openNodes::offerLast);


        }
        bookView.addPage(page);

        player.sendBookView(bookView.build());

        return CommandResult.success();
    }
}
