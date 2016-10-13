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

/**
 * Created by kernegal on 09/10/2016.
 */
public class IsCreate implements CommandExecutor {

    private SpongyIsland plugin;
    private IslandManager isManager;
    private DataHolder data;


    public IsCreate(SpongyIsland plugin,IslandManager isManager,DataHolder data) {
        this.isManager=isManager;
        this.plugin = plugin;
        this.data=data;
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
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
            plugin.getService().newPetition(source, new ConfirmationPetition() {

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
