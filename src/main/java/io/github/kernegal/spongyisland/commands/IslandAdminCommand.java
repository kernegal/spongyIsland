package io.github.kernegal.spongyisland.commands;

import io.github.kernegal.spongyisland.SpongyIsland;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

/**
 * Created by kernegal on 07/10/2016.
 */
public class IslandAdminCommand implements CommandExecutor {
    private SpongyIsland plugin;

    public IslandAdminCommand(SpongyIsland plugin) {
        this.plugin=plugin;
    }


    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {

        return CommandResult.success();
    }
}
