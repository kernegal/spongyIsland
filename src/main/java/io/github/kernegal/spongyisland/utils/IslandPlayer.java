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

package io.github.kernegal.spongyisland.utils;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import java.util.UUID;


public class IslandPlayer {
    private int id;
    private UUID uuid;
    private String name;
    private Vector2i isPosition;
    private Vector3i isHome;
    private int island;
    private long newIslandTime,newLevelTime;

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

    public void setNewLevelTime(){
        this.newLevelTime = System.currentTimeMillis();
    }
    /*public void setIsPosition(Vector2i isPosition) {
        this.isPosition = isPosition;
    }*/

    public boolean canHaveNewIsland(int waitTime){

        return newIslandTime==0 || (System.currentTimeMillis()-newIslandTime)/1000>waitTime;
    }

    public boolean canCalculateIslanLevel(int waitTime){
        return newLevelTime==0 || (System.currentTimeMillis()-newLevelTime)/1000>waitTime;
    }
}
