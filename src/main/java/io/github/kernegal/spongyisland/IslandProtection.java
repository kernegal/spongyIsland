package io.github.kernegal.spongyisland;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import io.github.kernegal.spongyisland.utils.IslandPlayer;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
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

        if (playerData.getIsland() != -1 ) {
            if(player.hasPermission(SpongyIsland.pluginId+".islands.modify_blocks")){
                return;
            }
            for(Transaction<BlockSnapshot> trans : event.getTransactions()) {
                trans.getOriginal().getLocation().ifPresent(location -> {
                    Vector2i islandCoordinates = playerData.getIsPosition().mul(islandRadius * 2);
                    Vector2i min = islandCoordinates.sub(protectionRadius, protectionRadius);
                    Vector2i max = islandCoordinates.add(protectionRadius, protectionRadius);
                    if (location.getX() < min.getX() || location.getX() >= max.getX() ||
                        location.getZ() < min.getY() || location.getZ() >= max.getY()) {
                        trans.setValid(false);
                        //event.setCancelled(true);
                    }

                });



            }
        } else {
            event.setCancelled(true);
        }
    }

    @Listener
    @IsCancelled(Tristate.FALSE)
    public void blockBreakEvent(ChangeBlockEvent.Break event, @Root Player player) {
        //logger.info("Only filtering when the root is a player and the event is a Break!");
        // do stuff
        IslandPlayer playerData = data.getPlayerData(player.getUniqueId());
        if (playerData.getIsland() != -1 ) {
            if(player.hasPermission(SpongyIsland.pluginId+".islands.modify_blocks")){
                return;
            }
            for(Transaction<BlockSnapshot> trans : event.getTransactions()) {
                trans.getOriginal().getLocation().ifPresent(location -> {
                    Vector2i islandCoordinates = playerData.getIsPosition().mul(islandRadius * 2);
                    Vector2i min = islandCoordinates.sub(protectionRadius, protectionRadius);
                    Vector2i max = islandCoordinates.add(protectionRadius, protectionRadius);
                    if (location.getX() < min.getX() || location.getX() >= max.getX() ||
                            location.getZ() < min.getY() || location.getZ() >= max.getY()) {
                        trans.setValid(false);
                        //event.setCancelled(true);
                    }

                });



            }
        } else {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onInteract(InteractBlockEvent event, @First Player player) {
        //logger.info("Only filtering when the root is a player and the event is a Break!");
        // do stuff
        IslandPlayer playerData = data.getPlayerData(player.getUniqueId());
        if (playerData.getIsland() != -1 ) {
            if(player.hasPermission(SpongyIsland.pluginId+".islands.modify_blocks")){
                return;
            }
            Vector3i location =  event.getTargetBlock().getPosition().toInt();
            Vector2i islandCoordinates = playerData.getIsPosition().mul(islandRadius * 2);
            Vector2i min = islandCoordinates.sub(protectionRadius, protectionRadius);
            Vector2i max = islandCoordinates.add(protectionRadius, protectionRadius);
            if (location.getX() < min.getX() || location.getX() >= max.getX() ||
                    location.getZ() < min.getY() || location.getZ() >= max.getY()) {
                event.setCancelled(true);
            }


        } else {
            event.setCancelled(true);
        }
    }
    @Listener
    public void onEvent(MoveEntityEvent event, @Getter("getTargetEntity") Player player) {
        IslandPlayer playerData = data.getPlayerData(player.getUniqueId());

        if (playerData.getIsland()!=-1){
            Vector2i islandCoordinates=playerData.getIsPosition().mul(islandRadius*2);
            Vector2i min = islandCoordinates.sub(protectionRadius,protectionRadius);
            Vector2i max = islandCoordinates.add(protectionRadius,protectionRadius);


            Vector3i from = event.getFromTransform().getPosition().toInt();
            Vector3i to = event.getToTransform().getPosition().toInt();


            if((from.getX()<min.getX() || from.getX()>=max.getX() ||
                    from.getZ()<min.getY() || from.getZ()>=max.getY()) &&
                    !(to.getX()<min.getX() || to.getX()>=max.getX() ||
                            to.getZ()<min.getY() || to.getZ()>=max.getY())){
                player.sendMessage(Text.of(TextColors.GREEN, "Entering your island"));

            }
            else if(!(from.getX()<min.getX() || from.getX()>=max.getX() ||
                    from.getZ()<min.getY() || from.getZ()>=max.getY()) &&
                    (to.getX()<min.getX() || to.getX()>=max.getX() ||
                            to.getZ()<min.getY() || to.getZ()>=max.getY())){
                player.sendMessage(Text.of(TextColors.RED, "leaving your island"));

            }
        }
    }


}
