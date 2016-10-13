package io.github.kernegal.spongyisland.utils;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import java.util.Date;
import java.util.UUID;

/**
 * Created by kernegal on 10/10/2016.
 */
public class IslandPlayer {
    private int id;
    private UUID uuid;
    private String name;
    private Vector2i isPosition;
    private Vector3i isHome;
    private int island;
    private long newIslandTime;

    public IslandPlayer(int id, UUID uuid, String name, Vector2i isPosition, Vector3i isHome, int island) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.isPosition = isPosition;
        this.isHome = isHome;
        this.island = island;
    }

    public IslandPlayer(int id, UUID uuid, String name) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.isPosition = null;
        this.island = -1;
        this.isHome = null;
    }


    public int getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Vector3i getIsHome() {
        return isHome;
    }

    public int getIsland() {
        return island;
    }

    public Vector2i getIsPosition() {
        return isPosition;
    }

    public void setIsHome(Vector3i isHome) {
        this.isHome = isHome;
    }

    public void setIsland(int island,Vector2i isPosition) {
        this.island = island;
        this.isPosition = isPosition;
        this.newIslandTime = System.currentTimeMillis();
    }

    /*public void setIsPosition(Vector2i isPosition) {
        this.isPosition = isPosition;
    }*/

    public boolean canHaveNewIsland(int waitIime){

        return newIslandTime==0 || (System.currentTimeMillis()-newIslandTime)/1000>waitIime;
    }
}
