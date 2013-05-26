/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author ucchy
 * BattlePointsのコマンド実行クラス
 */
public class BPCommand implements CommandExecutor {

    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(
            CommandSender sender, Command command, String label, String[] args) {

        if ( args.length <= 0 ) {
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
            sender.sendMessage(String.format(ChatColor.RED + "%d. %s%s - %s - %d",
                    (i+1), users.get(i).color.toString(), users.get(i).name,
                    users.get(i).rank, users.get(i).point));
        }
    }
}
