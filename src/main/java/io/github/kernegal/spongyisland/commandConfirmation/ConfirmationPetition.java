package io.github.kernegal.spongyisland.commandConfirmation;


import org.spongepowered.api.command.CommandSource;

public interface ConfirmationPetition {


    void confirm(CommandSource source);

    void deny(CommandSource source);
}
