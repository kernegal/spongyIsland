package io.github.kernegal.spongyisland.commands;

import io.github.kernegal.spongyisland.DataHolder;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;


import javax.annotation.Nonnull;


public class IsHome implements CommandExecutor {
    private DataHolder dataHolder;

    public IsHome(DataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull CommandSource source, @Nonnull CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(Text.of(TextColors.RED, "Player only."));
            return CommandResult.success();
        }
        Player player = (Player) source;

        dataHolder.teleportPlayerToHome(player);

        return CommandResult.success();
    }
}
