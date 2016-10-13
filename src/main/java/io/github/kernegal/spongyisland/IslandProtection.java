package io.github.kernegal.spongyisland;

import com.flowpowered.math.vector.Vector2i;
import io.github.kernegal.spongyisland.utils.IslandPlayer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

/**
 * Created by kernegal on 12/10/2016.
 */
public class IslandProtection {

    private DataHolder data;
    private int islandRadius, protectionRadius;

    public IslandProtection(DataHolder data, int islandRadius, int protectionRadius) {
        this.data = data;
        this.islandRadius = islandRadius;
        this.protectionRadius = protectionRadius;
    }

    @Listener
    @IsCancelled(Tristate.FALSE)
    public void blockPlaceEvent(ChangeBlockEvent.Place event, @Root Player player) {
        //logger.info("Only filtering when the root is a player and the event is a Place!");
        IslandPlayer playerData = data.getPlayerData(player.getUniqueId());

        Location<World> location = player.getLocation();
        /*Vector2i nearIsland=new Vector2i(location.getX(),location.getZ()).mul(islandRadius*2);
        */

        Vector2i islandCoordinates=playerData.getIsPosition().mul(islandRadius*2);
        Vector2i min = islandCoordinates.sub(protectionRadius,protectionRadius);
        Vector2i max = islandCoordinates.add(protectionRadius,protectionRadius);

        if (playerData.getIsland()==-1 ||
                location.getX()<min.getX() || location.getX()>=max.getX() ||
                location.getZ()<min.getY() || location.getZ()>=max.getY() ){
            event.setCancelled(true);
        }
    }

    @Listener
    @IsCancelled(Tristate.FALSE)
    public void blockBreakEvent(ChangeBlockEvent.Break event, @Root Player player) {
        //logger.info("Only filtering when the root is a player and the event is a Break!");
        // do stuff
        IslandPlayer playerData = data.getPlayerData(player.getUniqueId());
        Location<World> location = player.getLocation();

        Vector2i islandCoordinates=playerData.getIsPosition().mul(islandRadius*2);
        Vector2i min = islandCoordinates.sub(protectionRadius,protectionRadius);
        Vector2i max = islandCoordinates.add(protectionRadius,protectionRadius);

        if (playerData.getIsland()==-1 ||
                location.getX()<min.getX() || location.getX()>=max.getX() ||
                location.getZ()<min.getY() || location.getZ()>=max.getY() ){
            event.setCancelled(true);
        }
    }

    @Listener
    public void onEvent(InteractBlockEvent event, @First Player player) {
        //logger.info("Only filtering when the root is a player and the event is a Break!");
        // do stuff
        IslandPlayer playerData = data.getPlayerData(player.getUniqueId());
        Location<World> location = player.getLocation();

        Vector2i islandCoordinates=playerData.getIsPosition().mul(islandRadius*2);
        Vector2i min = islandCoordinates.sub(protectionRadius,protectionRadius);
        Vector2i max = islandCoordinates.add(protectionRadius,protectionRadius);

        if (playerData.getIsland()==-1 ||
                location.getX()<min.getX() || location.getX()>=max.getX() ||
                location.getZ()<min.getY() || location.getZ()>=max.getY() ){
            event.setCancelled(true);
        }
    }
}
