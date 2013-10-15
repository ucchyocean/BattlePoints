/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.github.ucchyocean.ct.ColorTeaming;
import com.github.ucchyocean.ct.ColorTeamingAPI;
import com.github.ucchyocean.ct.config.ColorTeamingConfig;

/**
 * BPCommandクラスから、ColorTeaming連携部分を取り出してカプセル化したクラス
 * @author ucchy
 */
public class BPCTCommand {

    private ColorTeaming colorteaming;

    protected BPCTCommand(Plugin colorteaming) {
        colorteaming = (ColorTeaming)colorteaming;
    }

    /**
     * BattlePointに応じてチーム分けをする
     * @param sender
     * @param numberOfGroups
     * @return
     */
    protected boolean doTeaming(CommandSender sender, int numberOfGroups) {

        ColorTeamingAPI api = colorteaming.getAPI();
        ColorTeamingConfig ctconf = colorteaming.getCTConfig();

        // ゲームモードがクリエイティブの人は除外する
        ArrayList<Player> tempPlayers =
                api.getAllPlayersOnWorld(ctconf.getWorldNames());
        ArrayList<Player> players = new ArrayList<Player>();
        for ( Player p : tempPlayers ) {
            if ( p.getGameMode() != GameMode.CREATIVE ) {
                players.add(p);
            }
        }
        if ( players.size() == 0 ) {
            sender.sendMessage(
                    ChatColor.RED + "対象のワールドに、誰も居ないようです。");
            return false;
        }

        // ポイント順でソートする
        BPUserData.sortPlayerByPoint(players);
        
        // チームわけを実行する
        api.makeColorTeamsWithOrderSelection(players, numberOfGroups);

        return true;
    }
}
