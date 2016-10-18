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

import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nonnull;
import java.util.Optional;

public class IAShowBlockVariantsCommand implements CommandExecutor {

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull CommandSource source, @Nonnull CommandContext context) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(Text.of(TextColors.RED, "Player only."));
            return CommandResult.success();
        }
        Player player = (Player) source;

        Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        if(!itemInHand.isPresent()){
            player.sendMessage(Text.of("You need to have an object in your hands"));
            return CommandResult.success();
        }
        Optional<BlockType> block = itemInHand.get().getItem().getBlock();
        if(!block.isPresent()){
            player.sendMessage(Text.of("You need to have a block in your hands"));
            return CommandResult.success();
        }

        player.sendMessage(Text.of(block.get().getName()));
        for(BlockTrait<?> entry : block.get().getTraits()){
            player.sendMessage(Text.of(entry.getName()," ["+entry.getPossibleValues().toString()+"]"));
        }
        //player.sendMessage(Text.of(block.get().getTraits()));

        return CommandResult.success();
    }
}
