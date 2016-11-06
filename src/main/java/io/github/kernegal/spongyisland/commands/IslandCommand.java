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

import java.util.Map;


public class IslandCommand implements CommandExecutor {

    BookView bookView;

    public IslandCommand(ConfigurationNode conf) {


        BookView.Builder bookView = BookView.builder()
                .title(Text.of("Island Command"))
                .author(Text.of("SpongyIsland"));

        Text page=Text.EMPTY;
        final int charPerRow=20;
        final int linesPerPage=14;
        int actualLines=0;
        for(Map.Entry<Object, ? extends ConfigurationNode> entry : conf.getNode("commands").getChildrenMap().entrySet()) {
            String nameStr = entry.getValue().getNode("friendly_name").getString("");
            Text name = Text.builder(nameStr)
                    .color(TextColors.DARK_BLUE)
                    .style(TextStyles.UNDERLINE)
                    .onClick(TextActions.runCommand(entry.getValue().getNode("command").getString("")))
                    .build();
            String descriptionStr = entry.getValue().getNode("description").getString("");
            Text description = Text.builder(descriptionStr).build();

            int numLines = nameStr.length()/charPerRow+descriptionStr.length()/charPerRow+2;
            if(actualLines+numLines>linesPerPage && actualLines!=0){

                bookView.addPage(page);
                page=Text.EMPTY;
                actualLines=0;
            }

            page = Text.of(page, Text.NEW_LINE,
                    name, Text.NEW_LINE,
                    description, Text.NEW_LINE);
            actualLines+=numLines+1;

        }
        bookView.addPage(page);


        this.bookView=bookView.build();
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(Text.of(TextColors.RED, "Player only."));
            return CommandResult.success();
        }
        Player player = (Player) source;

        player.sendBookView(bookView);

        return CommandResult.success();
    }
}
