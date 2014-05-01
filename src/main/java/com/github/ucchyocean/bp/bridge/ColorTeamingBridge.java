/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp.bridge;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.github.ucchyocean.bp.BPUserData;
import com.github.ucchyocean.bp.BattlePoints;
import com.github.ucchyocean.bp.Messages;
import com.github.ucchyocean.bp.Utility;
import com.github.ucchyocean.ct.ColorTeaming;
import com.github.ucchyocean.ct.ColorTeamingAPI;
import com.github.ucchyocean.ct.config.ColorTeamingConfig;
import com.github.ucchyocean.ct.event.ColorTeamingPlayerAddEvent;
import com.github.ucchyocean.ct.event.ColorTeamingTrophyKillEvent;
import com.github.ucchyocean.ct.event.ColorTeamingWonLeaderEvent;
import com.github.ucchyocean.ct.event.ColorTeamingWonTeamEvent;

/**
 * ColorTeaming連携クラス
 * @author ucchy
 */
public class ColorTeamingBridge implements Listener {

    /** BattlePointsクラス */
    private BattlePoints battlepoints;

    /** ColroTeamingクラス */
    private ColorTeaming colorteaming;

    /** チームメンバーのキャッシュ */
    private HashMap<String, String> membersCache;

    /** コンストラクタは使用不可 */
    private ColorTeamingBridge() {
        membersCache = new HashMap<String, String>();
    }

    /**
     * ColroTeamingをロードする
     * @param battlepoints BattlePointsクラスのインスタンス
     * @param colorteaming 対象バージョン以降のColorTeaming
     * @return ロードされたColorTeamingBridge
     */
    public static ColorTeamingBridge load(BattlePoints battlepoints, Plugin colorteaming) {

        ColorTeamingBridge bridge = new ColorTeamingBridge();
        bridge.battlepoints = battlepoints;
        bridge.colorteaming = (ColorTeaming)colorteaming;
        return bridge;
    }

    /**
     * BattlePointに応じてチーム分けをする
     * @param sender
     * @param numberOfGroups
     * @return
     */
    public boolean doTeaming(CommandSender sender, int numberOfGroups) {

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

    /**
     * プレイヤーがチームに参加したときに発生するイベント
     * @param event
     */
    @EventHandler
    public void onTeamSet(ColorTeamingPlayerAddEvent event) {

        String player = event.getPlayer().getName();
        String team = event.getTeam().getName();
        membersCache.put(player, team);
    }

    /**
     * 殲滅戦の形式で、勝利チームが決定したときに発生するイベント
     * @param event
     */
    @EventHandler
    public void onTeamWin(ColorTeamingWonTeamEvent event) {

        String name = event.getWonTeamName().getID();
        String displayName =
                event.getWonTeamName().getColor() + event.getWonTeamName().getName();
        sendBonusPointToWonTeam(name, displayName);
    }

    /**
     * 大将戦の形式で、勝利チームが決定したときに発生するイベント
     * @param event
     */
    @EventHandler
    public void onTeamLeaderWin(ColorTeamingWonLeaderEvent event) {

        String name = event.getWonTeamName().getID();
        String displayName =
                event.getWonTeamName().getColor() + event.getWonTeamName().getName();
        sendBonusPointToWonTeam(name, displayName);
    }

    /**
     * 規定キル数を稼ぐ形式で、勝利チームが決定したときに発生するイベント
     * @param event
     */
    @EventHandler
    public void onKillTrophy(ColorTeamingTrophyKillEvent event) {

        String name = event.getTeam().getName();
        String displayName = event.getTeam().getDisplayName();
        sendBonusPointToWonTeam(name, displayName);
    }

    /**
     * 勝利チームのメンバーにボーナスポイントを与える
     * @param team 勝利チーム名
     * @param displayName 勝利チーム表示名
     */
    private void sendBonusPointToWonTeam(String team, String displayName) {

        // ボーナスポイント設定を取得。0なら何もしないで終わる
        int bonus = battlepoints.getBPConfig().getWinTeamBonusPoint();
        if ( bonus <= 0 ) {
            return;
        }

        // 勝利チームのメンバーを取得する
        ArrayList<String> playerNames = new ArrayList<String>();

        for ( String name : membersCache.keySet() ) {
            if ( membersCache.get(name).equals(team) ) {
                playerNames.add(name);
            }
        }

        // 勝利チームのメンバーが誰もいないならここで終わる
        if ( playerNames.size() == 0 ) {
            return;
        }

        // ボーナスポイントを与える
        for ( String name : playerNames ) {
            @SuppressWarnings("deprecation")
            Player player = Bukkit.getPlayerExact(name);
            if ( player == null ) {
                continue;
            }
            battlepoints.addPoint(name, bonus);
            BPUserData data = BPUserData.getData(name);
            sendMessage(player, "teamWonBonus", displayName, bonus, data.getPoint());
        }

        // メンバーのキャッシュをクリアする
        membersCache.clear();
    }

    /**
     * メッセージリソースを取得し、対象プレイヤーに送信する
     * @param player 送信先プレイヤー
     * @param key メッセージキー
     * @param args メッセージの引数
     */
    private void sendMessage(Player player, String key, Object... args) {

        String msg = Messages.get(key, args);
        if ( msg.equals("") ) {
            return;
        }
        String prefix = Messages.get("prefix");
        player.sendMessage(Utility.replaceColorCode(prefix + msg));
    }

}
