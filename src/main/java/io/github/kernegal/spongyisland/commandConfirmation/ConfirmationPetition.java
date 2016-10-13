package io.github.kernegal.spongyisland.commandConfirmation;


import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

/**
 * Created by kernegal on 12/10/2016.
 */
public interface ConfirmationPetition {


    void confirm(CommandSource source);

    void deny(CommandSource source);
}
