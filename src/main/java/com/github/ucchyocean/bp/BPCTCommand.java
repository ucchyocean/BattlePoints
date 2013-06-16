/*
 * @author     ucchy
 * @license    GPLv3
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
import com.github.ucchyocean.ct.ColorTeamingConfig;

/**
 * BPCommandクラスから、ColorTeaming連携部分を取り出してカプセル化したクラス
 * @author ucchy
 */
public class BPCTCommand {

    private static final String[] GROUP_COLORS = {
        "red", "blue", "yellow", "green", "aqua", "gray", "dark_red", "dark_green", "dark_aqua"
    };

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

        // 全てのグループをいったん削除する
        api.removeAllTeam();

        // ランキングデータを作成する
        ArrayList<BPUserData> users = new ArrayList<BPUserData>();
        for ( Player p : players ) {
            users.add(BPUserData.getData(p.getName()));
        }
        BPUserData.sortUserData(users);

        // グループを設定していく
        for ( int i=0; i<users.size(); i++ ) {
            int group = i % numberOfGroups;
            String color = GROUP_COLORS[group];
            Player player = BattlePoints.getPlayerExact(users.get(i).name);
            api.addPlayerTeam(player, color);
        }

        // 各グループに、通知メッセージを出す
        for ( int i=0; i<numberOfGroups; i++ ) {
            api.sendInfoToTeamChat(GROUP_COLORS[i],
                    "あなたは " +
                    Utility.replaceColors(GROUP_COLORS[i]) +
                    GROUP_COLORS[i] +
                    ChatColor.GREEN +
                    " グループになりました。");
        }

        // キルデス情報のクリア
        api.clearKillDeathPoints();

        // スコアボードの作成
        api.makeSidebarScore();
        api.makeTabkeyListScore();
        api.makeBelowNameScore();

        // メンバー情報をlastdataに保存する
        api.getCTSaveDataHandler().save("lastdata");

        return true;
    }
}
