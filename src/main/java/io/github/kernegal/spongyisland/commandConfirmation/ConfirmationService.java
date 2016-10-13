package io.github.kernegal.spongyisland.commandConfirmation;

import org.slf4j.Logger;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kernegal on 12/10/2016.
 */
public class ConfirmationService implements CommandExecutor {
    //private Logger logger;
    private Map<CommandSource,ConfirmationPetition> requests;
    private String accept = "accept";
    private String cancel = "cancel";
    private String failString = "You don not have any pending requests";
    private Text newPetitionString = Text.of("write",TextColors.GREEN, "/isconfirm ",accept,TextColors.NONE," or ",
            TextColors.RED,"/isconfirm ",cancel,TextColors.NONE," to accept or cancel the request");
    public static final String argumentString = "response";

    public ConfirmationService() {
        requests = new HashMap<>();
    }

    public void newPetition(CommandSource source, ConfirmationPetition petition){
        source.sendMessage(newPetitionString);
        requests.put(source,petition);
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        ConfirmationPetition pet=requests.get(source);
        if(pet==null){
            source.sendMessage(Text.of(failString));
        }
        else{
            String s = args.<String>getOne(argumentString).get();

            if(s.equals(accept)){
                pet.confirm(source);
                requests.remove(source);
            }
            else if(s.equals(cancel)){
                pet.deny(source);
                requests.remove(source);
            }
            else{
                source.sendMessage(Text.of(TextColors.DARK_RED, "Incorrect action. Possibilities are "+accept+" or "+cancel+"."));
            }
        }
        return CommandResult.success();
    }
}
