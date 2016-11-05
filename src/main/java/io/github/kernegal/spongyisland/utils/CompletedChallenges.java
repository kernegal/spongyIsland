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


import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class CompletedChallenges {
    Map<String,Integer> completed;
    Map<String,Integer> challengesInLevelCompleted;
    String lastLevel;

    public CompletedChallenges(String defaultLevel){
        completed=new HashMap<>();
        challengesInLevelCompleted=new HashMap<>();
        lastLevel=defaultLevel;
    }

    public int timesCompleted(String challenge){
        return completed.getOrDefault(challenge,0);
    }

    public int challengesCompletedInLevel(String level){
        return challengesInLevelCompleted.getOrDefault(level,0);
    }

    public void setChallengeCompleted(String challenge,String level) {
        setChallengeCompleted(challenge,level,1);
    }

    public void setChallengeCompleted(String challenge,String level,int ntimes){
        int completed=this.completed.getOrDefault(challenge,0);
        this.completed.put(challenge,completed+ntimes);
        if(completed==0){
            this.challengesInLevelCompleted.put(level,this.challengesInLevelCompleted.getOrDefault(level,0)+1);
        }
        lastLevel=level;
    }

    public String getLastLevel(){
        return lastLevel;
    }
}
