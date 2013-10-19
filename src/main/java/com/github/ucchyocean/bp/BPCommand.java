/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * BattlePointsのコマンド実行クラス
 * @author ucchy
 */
public class BPCommand implements CommandExecutor {

    private static final String[] COMMANDS = {
        "rank", "kdrank", "set", "reload", "team",
    };
    
    private BattlePoints plugin;
    
    /**
     * コンストラクタ
     * @param plugin 
     */
    public BPCommand(BattlePoints plugin) {
        this.plugin = plugin;
    }

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

        } else if ( args[0].equalsIgnoreCase("kdrank") ) {
            int numberOfView = 10;
            if ( args.length >= 2 && Utility.tryIntParse(args[1]) ) {
                numberOfView = Integer.parseInt(args[1]);
            }
            viewKDRank(sender, numberOfView);
            return true;

        } else if ( args[0].equalsIgnoreCase("set") ) {
            if ( args.length >= 3 && Utility.tryIntParse(args[2]) ) {
                int point = Integer.parseInt(args[2]);
                plugin.setPoint(args[1], point);
                sender.sendMessage(ChatColor.GRAY +
                        "プレイヤー" + args[1] + "のポイントを" + point + "に設定しました。");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "パラメータの指定が正しくありません。");
                return false;
            }

        } else if ( args[0].equalsIgnoreCase("add") ) {
            if ( args.length >= 3 && Utility.tryIntParse(args[2]) ) {
                int point = Integer.parseInt(args[2]);
                plugin.addPoint(args[1], point);
                sender.sendMessage(ChatColor.GRAY +
                        "プレイヤー" + args[1] + "のポイントを" + point + "増やしました。");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "パラメータの指定が正しくありません。");
                return false;
            }

        } else if ( args[0].equalsIgnoreCase("reload") ) {
            plugin.reloadDatas();
            sender.sendMessage(ChatColor.GRAY + "設定を再読み込みしました。");
            return true;

        } else if ( args[0].equalsIgnoreCase("team") ) {
            if ( BattlePoints.ctbridge == null ) {
                sender.sendMessage(ChatColor.RED + "ColorTeaming連携機能が無効のため、このコマンドは使えません。");
                return false;
            }
            int numberOfGroups = 2;
            if ( args.length >= 2 && args[1].matches("[2-9]") ) {
                numberOfGroups = Integer.parseInt(args[1]);
            }

            return BattlePoints.ctbridge.doTeaming(sender, numberOfGroups);
        }

        return false;
    }

    /**
     * ランキングをsenderの画面に表示する
     * @param sender ランキング表示対象
     * @param numberOfView 表示する個数
     */
    private void viewRank(CommandSender sender, int numberOfView) {

        BPConfig config = plugin.getBPConfig();
        ArrayList<BPUserData> users = BPUserData.getAllUserData();
        BPUserData.sortUserData(users);

        if ( numberOfView > users.size() ) {
            numberOfView = users.size();
        }

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "===== Battle Points Ranking =====");
        for ( int i=0; i<numberOfView; i++ ) {
            BPUserData data = users.get(i);
            String rank = config.getRankFromPoint(users.get(i).point);
            String color = config.getColorFromRank(rank);
            double rate = data.getKDRate();
            sender.sendMessage(String.format(ChatColor.RED +
                    "%d. %s%s - %s - %dP, %dK, %dD, %.2f%%",
                    (i+1), color, users.get(i).name, rank,
                    data.point, data.kills, data.deaths, rate));
        }
    }

    /**
     * KDレートランキングをsenderの画面に表示する
     * @param sender ランキング表示対象
     * @param numberOfView 表示する個数
     */
    private void viewKDRank(CommandSender sender, int numberOfView) {

        BPConfig config = plugin.getBPConfig();
        ArrayList<BPUserData> users = BPUserData.getAllUserData();
        BPUserData.sortUserDataByKDRate(users);

        if ( numberOfView > users.size() ) {
            numberOfView = users.size();
        }

        sender.sendMessage(ChatColor.LIGHT_PURPLE + "===== K/D Rate Ranking =====");
        for ( int i=0; i<numberOfView; i++ ) {
            BPUserData data = users.get(i);
            String rank = config.getRankFromPoint(users.get(i).point);
            String color = config.getColorFromRank(rank);
            double rate = data.getKDRate();
            sender.sendMessage(String.format(ChatColor.RED +
                    "%d. %s%s - %s - %dP, %dK, %dD, %.2f%%",
                    (i+1), color, users.get(i).name, rank,
                    data.point, data.kills, data.deaths, rate));
        }
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
