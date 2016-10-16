package io.github.kernegal.spongyisland.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;


public class IslandCommand implements CommandExecutor {

    public IslandCommand() {

    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {

        return CommandResult.success();
    }
}
