package io.github.kernegal.spongyisland.commands;

import io.github.kernegal.spongyisland.SpongyIsland;
import io.github.kernegal.spongyisland.utils.IslandManager;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * Created by somebody on 09/10/2016.
 */
public class IsCreate implements CommandExecutor {

    private SpongyIsland plugin;
    private IslandManager isManager;

    public IsCreate(SpongyIsland plugin,IslandManager isManager) {
        this.isManager=isManager;
        this.plugin = plugin;
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
        isManager.create(schema,player);
        return CommandResult.success();
    }
}
