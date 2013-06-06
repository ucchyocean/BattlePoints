/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;

import com.github.ucchyocean.ct.scoreboard.CustomScoreCriteria;

/**
 * ColorTeaming連携用、カスタムスコアクライテリア
 * @author ucchy
 */
public class BPCustomScore implements CustomScoreCriteria {

    /**
     * @see com.github.ucchyocean.ct.scoreboard.CustomScoreCriteria#refreshScore(org.bukkit.scoreboard.Objective)
     */
    public void refreshScore(Objective objective) {
        Player[] temp = BattlePoints.instance.getServer().getOnlinePlayers();
        for ( Player p : temp ) {
            int point = BattlePoints.data.getPoint(p.getName());
            objective.getScore(p).setScore(point);
        }
    }

    /**
     * @see com.github.ucchyocean.ct.scoreboard.CustomScoreCriteria#getUnit()
     */
    public String getUnit() {
        return "P";
    }
}
