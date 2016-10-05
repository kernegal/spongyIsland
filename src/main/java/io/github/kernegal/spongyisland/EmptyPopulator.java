package io.github.kernegal.spongyisland;

import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.ImmutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBlockVolume;
import org.spongepowered.api.world.gen.GenerationPopulator;

/**
 * Created by kernegal on 03/10/2016.
 * Planed for use in the future instead of superflat mode
 */
public class EmptyPopulator implements GenerationPopulator {

    public void populate(World world, MutableBlockVolume buffer, ImmutableBiomeArea biomes) {

    }
}
