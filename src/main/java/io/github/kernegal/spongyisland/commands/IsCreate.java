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
import io.github.kernegal.spongyisland.commandConfirmation.ConfirmationPetition;
import io.github.kernegal.spongyisland.utils.IslandManager;
import io.github.kernegal.spongyisland.utils.IslandPlayer;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nonnull;
import java.util.Optional;

public class IsCreate implements CommandExecutor {

    public static final String commandName="create";

    private IslandManager isManager;
    private DataHolder data;


    public IsCreate(IslandManager isManager,DataHolder data) {
        this.isManager=isManager;
        this.data=data;
    }

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull CommandSource source, @Nonnull CommandContext args) throws CommandException {
        //TODO check for player having no island


        if (!(source instanceof Player)) {
            source.sendMessage(Text.of(TextColors.RED, "Player only."));
            return CommandResult.success();
        }
        Player player = (Player) source;

        Optional<String> schematicOpt = args.<String>getOne("schematic");


        if(schematicOpt.isPresent()) {
            String schema = schematicOpt.get();
            IslandPlayer playerData = data.getPlayerData(player.getUniqueId());
            if (playerData.getIsland() != -1) {
                player.sendMessage(Text.of(TextColors.RED, "You already have an island. If you continue, your actual island will be lost forever"));
                SpongyIsland.getPlugin().getService().newPetition(source, new ConfirmationPetition() {

                    @Override
                    public void confirm(CommandSource source) {
                        if (isManager.create(schema, player))
                            data.teleportPlayerToHome(player);
                    }

                    @Override
                    public void deny(CommandSource source) {

                    }
                });
                return CommandResult.success();
            }


            if (isManager.create(schema, player))
                data.teleportPlayerToHome(player);

        }
        else{
            player.sendBookView(isManager.getIslandBook());

        }
        return CommandResult.success();
    }
}
