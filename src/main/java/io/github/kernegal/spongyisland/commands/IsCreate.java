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

public class IsCreate implements CommandExecutor {

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

        String schema = args.<String>getOne("schematic").orElse("");

        IslandPlayer playerData = data.getPlayerData(player.getUniqueId());
        if(playerData.getIsland()!=-1){
            player.sendMessage(Text.of(TextColors.RED, "You already have an island. If you continue, your actual island will be lost forever"));
            SpongyIsland.getPlugin().getService().newPetition(source, new ConfirmationPetition() {

                @Override
                public void confirm(CommandSource source) {
                    if(isManager.create(schema,player))
                        data.teleportPlayerToHome(player);
                }

                @Override
                public void deny(CommandSource source) {

                }
            });
            return CommandResult.success();
        }


        if(isManager.create(schema,player))
            data.teleportPlayerToHome(player);

        return CommandResult.success();
    }
}
