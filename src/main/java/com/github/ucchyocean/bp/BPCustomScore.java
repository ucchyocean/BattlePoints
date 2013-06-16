/*
 * @author     ucchy
 * @license    GPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;

import com.github.ucchyocean.ct.ColorTeaming;
import com.github.ucchyocean.ct.scoreboard.CustomScoreInterface;

/**
 * ColorTeaming連携用、カスタムスコアクライテリア
 * @author ucchy
 */
public class BPCustomScore implements CustomScoreInterface {

    /**
     * コンストラクタ
     * @param server サーバー
     * @param ctPlugin ColorTeaming
     */
    public BPCustomScore(Server server, Plugin ctPlugin) {

        ColorTeaming colorteaming = (ColorTeaming)ctPlugin;
        colorteaming.getAPI().setCustomScoreCriteria("battlepoints", this);
    }

    /**
     * @see com.github.ucchyocean.ct.scoreboard.CustomScoreCriteria#refreshScore(org.bukkit.scoreboard.Objective)
     */
    public void refreshScore(Objective objective) {

        ArrayList<BPUserData> datas = BPUserData.getAllUserData();
        for ( BPUserData data : datas ) {
//            String rank = BPConfig.getRankFromPoint(data.point);
//            String color = BPConfig.rankColors.get(rank).toString();
//            OfflinePlayer player = Bukkit.getOfflinePlayer(
//                    color + data.name + ChatColor.RESET);
            OfflinePlayer player = Bukkit.getOfflinePlayer(data.name);
            objective.getScore(player).setScore(data.point);
        }
    }

    /**
     * @see com.github.ucchyocean.ct.scoreboard.CustomScoreInterface#getTitle()
     */
    public String getTitle() {
        return "ポイントランキング";
    }

    /**
     * @see com.github.ucchyocean.ct.scoreboard.CustomScoreInterface#getUnit()
     */
    public String getUnit() {
        return "P";
    }

    /**
     * @see com.github.ucchyocean.ct.scoreboard.CustomScoreInterface#displayStart(org.bukkit.scoreboard.Objective)
     */
    public void displayStart(Objective objective) {
        refreshScore(objective);
    }

    /**
     * @see com.github.ucchyocean.ct.scoreboard.CustomScoreInterface#displayEnd()
     */
    public void displayEnd() {
        // 使用しません。
    }
}
