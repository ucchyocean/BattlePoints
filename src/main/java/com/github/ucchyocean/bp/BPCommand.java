/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.ucchyocean.ct.ColorTeaming;
import com.github.ucchyocean.ct.Utility;

/**
 * @author ucchy
 * BattlePointsのコマンド実行クラス
 */
public class BPCommand implements CommandExecutor {

    private static final String[] COMMANDS = {
        "rank", "set", "reload", "team",
    };
    private static final String[] GROUP_COLORS = {
        "red", "blue", "yellow", "green", "aqua", "gray", "dark_red", "dark_green", "dark_aqua"
    };

    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(
            CommandSender sender, Command command, String label, String[] args) {

        if ( args.length <= 0 ) {
            return false;
        }

        if ( !isValidCommand(args[0]) ) {
            return false;
        }

        if ( !sender.hasPermission("battlepoints." + args[0]) ) {
            sender.sendMessage(ChatColor.RED + "You don't have permission \"battlepoints." + args[0] + "\".");
            return true;
        }

        if ( args[0].equalsIgnoreCase("rank") ) {
            int numberOfView = 10;
            if ( args.length >= 2 && Utility.tryIntParse(args[1]) ) {
                numberOfView = Integer.parseInt(args[1]);
            }
            viewRank(sender, numberOfView);
            return true;

        } else if ( args[0].equalsIgnoreCase("set") ) {
            if ( args.length >= 3 && Utility.tryIntParse(args[2]) ) {
                int point = Integer.parseInt(args[2]);
                BattlePoints.data.setPoint(args[1], point);
                sender.sendMessage(ChatColor.GRAY +
                        "プレイヤー" + args[1] + "のポイントを" + point + "に設定しました。");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "パラメータの指定が正しくありません。");
                return false;
            }

        } else if ( args[0].equalsIgnoreCase("reload") ) {
            BPConfig.reloadConfig();
            BattlePoints.data.reload();
            sender.sendMessage(ChatColor.GRAY + "config.ymlを再読み込みしました。");
            return true;

        } else if ( args[0].equalsIgnoreCase("team") ) {
            if ( BattlePoints.colorteaming == null ) {
                sender.sendMessage(ChatColor.RED + "ColorTeaming連携機能が無効のため、このコマンドは使えません。");
                return false;
            }
            int numberOfGroups = 2;
            if ( args.length >= 2 && args[1].matches("[2-9]") ) {
                numberOfGroups = Integer.parseInt(args[1]);
            }

            doTeaming(sender, numberOfGroups);
            return true;
        }

        return false;
    }

    /**
     * ランキングをsenderの画面に表示する
     * @param sender ランキング表示対象
     * @param numberOfView 表示する個数
     */
    private void viewRank(CommandSender sender, int numberOfView) {

        ArrayList<BPUserData> users = BattlePoints.data.getAllUserData();
        BPUserData.sortUserData(users);

        if ( numberOfView > users.size() ) {
            numberOfView = users.size();
        }

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "===== Battle Points Ranking =====");
        for ( int i=0; i<numberOfView; i++ ) {
            String rank = BPConfig.getRankFromPoint(users.get(i).point);
            ChatColor color = BPConfig.rankColors.get(rank);
            sender.sendMessage(String.format(ChatColor.RED + "%d. %s%s - %s - %d",
                    (i+1), color.toString(), users.get(i).name, rank, users.get(i).point));
        }
    }

    /**
     * BattlePointに応じてチーム分けをする
     * @param sender
     * @param numberOfGroups
     * @return
     */
    private boolean doTeaming(CommandSender sender, int numberOfGroups) {

        ColorTeaming ct = BattlePoints.colorteaming;

        // ゲームモードがクリエイティブの人は除外する
        ArrayList<Player> tempPlayers =
                ct.getAllPlayersOnWorld(ct.getCTConfig().getWorldNames());
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
        ct.removeAllTeam();

        // ランキングデータを作成する
        ArrayList<BPUserData> users = new ArrayList<BPUserData>();
        for ( Player p : players ) {
            String name = p.getName();
            users.add(new BPUserData(name, BattlePoints.data.getPoint(name)));
        }
        BPUserData.sortUserData(users);

        // グループを設定していく
        for ( int i=0; i<users.size(); i++ ) {
            int group = i % numberOfGroups;
            String color = GROUP_COLORS[group];
            Player player = BattlePoints.getPlayerExact(users.get(i).name);
            ct.addPlayerTeam(player, color);
        }

        // 各グループに、通知メッセージを出す
        for ( int i=0; i<numberOfGroups; i++ ) {
            ct.sendInfoToTeamChat(GROUP_COLORS[i],
                    "あなたは " +
                    Utility.replaceColors(GROUP_COLORS[i]) +
                    GROUP_COLORS[i] +
                    ChatColor.GREEN +
                    " グループになりました。");
        }

        // キルデス情報のクリア
        ct.clearKillDeathPoints();

        // スコアボードの作成
        ct.makeSidebar();
        ct.makeTabkeyListScore();
        ct.makeBelowNameScore();

        // メンバー情報をlastdataに保存する
        ct.getCTSaveDataHandler().save("lastdata");

        return true;
    }

    private boolean isValidCommand(String command) {

        for ( String c : COMMANDS ) {
            if ( c.equalsIgnoreCase(command) ) {
                return true;
            }
        }
        return false;
    }
}
