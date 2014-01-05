/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * BattlePointsの設定ファイルを管理するクラス
 * @author ucchy
 */
public class BPConfig {

    private boolean useVault;
    private boolean displayPointOnChat;
    private List<String> displayPointOnChatWorlds;
    private boolean useWebstat;
    private int webstatPortNumber;
    private int initialPoint;
    private int winBasePoint;
    private int winOffsetPoint;
    private int winBonusPointPercent;
    private int winTeamBonusPoint;
    private Hashtable<String, String> rankSymbols;
    private Hashtable<String, ChatColor> rankColors;
    private ArrayList<Entry<String, Integer>> rankBorders;

    /**
     * config.ymlの読み出し処理。
     */
    public static BPConfig load() {

        // フォルダやファイルがない場合は、作成したりする
        File dir = new File(
                BattlePoints.instance.getDataFolder().getAbsolutePath());
        if ( !dir.exists() ) {
            dir.mkdirs();
        }

        File file = new File(
                BattlePoints.instance.getDataFolder() +
                File.separator + "config.yml");

        if ( !file.exists() ) {
            Utility.copyFileFromJar(BattlePoints.getPluginJarFile(),
                    file, "config.yml", false);
        }

        // 再読み込み処理
        BattlePoints.instance.reloadConfig();
        FileConfiguration config = BattlePoints.instance.getConfig();

        // 各コンフィグの取得
        BPConfig conf = new BPConfig();
        conf.useVault = config.getBoolean("useVault", false);
        conf.displayPointOnChat = config.getBoolean("displayPointOnChat", true);
        if ( config.contains("displayPointOnChatWorlds") ) {
            conf.displayPointOnChatWorlds 
                = config.getStringList("displayPointOnChatWorlds");
        } else {
            conf.displayPointOnChatWorlds = new ArrayList<String>();
            conf.displayPointOnChatWorlds.add("world");
        }
        conf.useWebstat = config.getBoolean("useWebstat", true);
        conf.webstatPortNumber = config.getInt("webstatPortNumber", 8106);
        conf.initialPoint = config.getInt("initialPoint", 1500);
        conf.winBasePoint = config.getInt("winBasePoint", 16);
        conf.winOffsetPoint = config.getInt("winOffsetPoint", 2);
        conf.winBonusPointPercent = config.getInt("winBonusPointPercent", 4);
        conf.winTeamBonusPoint = config.getInt("winTeamBonusPoint", 10);

        conf.rankSymbols = new Hashtable<String, String>();
        conf.rankColors = new Hashtable<String, ChatColor>();
        Hashtable<String, Integer> rankBorders_temp = new Hashtable<String, Integer>();
        ConfigurationSection section = config.getConfigurationSection("ranks");
        if ( section != null ) {
            Iterator<String> i = section.getValues(false).keySet().iterator();
            while (i.hasNext()) {
                String rankName = i.next();
                if ( config.contains("ranks." + rankName + ".symbol") ) {
                    conf.rankSymbols.put(rankName, config.getString("ranks." + rankName + ".symbol") );
                } else {
                    conf.rankSymbols.put(rankName, rankName.substring(0, 1));
                }
                String color = config.getString("ranks." + rankName + ".color", "white");
                conf.rankColors.put(rankName, Utility.toChatColor(color));
                if ( config.contains("ranks." + rankName + ".border") ) {
                    rankBorders_temp.put(rankName, config.getInt("ranks." + rankName + ".border") );
                }
            }
        }

        // もしrankが正しくロードされていなければ、ここで初期値を設定する
        if ( rankBorders_temp.size() <= 0 ) {
            conf.rankSymbols = new Hashtable<String, String>();
            conf.rankSymbols.put("novice", "N");
            conf.rankSymbols.put("bronze", "B");
            conf.rankSymbols.put("silver", "S");
            conf.rankSymbols.put("gold", "G");
            conf.rankSymbols.put("platinum", "P");
            conf.rankColors = new Hashtable<String, ChatColor>();
            conf.rankColors.put("novice", ChatColor.BLUE);
            conf.rankColors.put("bronze", ChatColor.AQUA);
            conf.rankColors.put("silver", ChatColor.GOLD);
            conf.rankColors.put("gold", ChatColor.RED);
            conf.rankColors.put("platinum", ChatColor.LIGHT_PURPLE);
            rankBorders_temp = new Hashtable<String, Integer>();
            rankBorders_temp.put("novice", 0);
            rankBorders_temp.put("bronze", 1700);
            rankBorders_temp.put("silver", 2000);
            rankBorders_temp.put("gold", 2400);
            rankBorders_temp.put("platinum", 2900);
        }

        // rankBorders は、ここでソートを実行しておく
        conf.rankBorders = new ArrayList<Entry<String,Integer>>(rankBorders_temp.entrySet());

        Collections.sort(conf.rankBorders, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> ent1, Entry<String, Integer> ent2){
                Integer val1 = ent1.getValue();
                Integer val2 = ent2.getValue();
                return val1.compareTo(val2);
            }
        });
        
        return conf;
    }

    /**
     * イロレーティングで、変動するポイント数を算出する。
     * @param winnerPoint 勝者の変動前ポイント
     * @param loserPoint 敗者の変動前ポイント
     * @return 移動するポイント
     */
    public int getEloRating(int winnerPoint, int loserPoint) {

        int rate = winBasePoint + (int)Math.round(
                (double)(loserPoint - winnerPoint) * winBonusPointPercent / 100 );

        if ( rate <= 0 ) {
            rate = 1;
        } else if ( rate >= winBasePoint * 2 ) {
            rate = winBasePoint * 2 - 1;
        }
        return rate;
    }

    /**
     * ポイント数から、称号名を取得する。
     * @param point ポイント
     * @return 称号名
     */
    public String getRankFromPoint(int point) {

        for ( int i = rankBorders.size()-1; i>=0; i-- ) {
            int border = rankBorders.get(i).getValue();
            if ( point >= border ) {
                return rankBorders.get(i).getKey();
            }
        }
        return "";
    }
    
    /**
     * 称号名から、ランクカラーを取得する
     * @param rank 称号名
     * @return ランクカラー（§n 形式の文字列で返します）
     */
    public String getColorFromRank(String rank) {
        
        if ( rankColors.containsKey(rank) ) {
            return rankColors.get(rank).toString();
        }
        return "";
    }
    
    /**
     * ポイント数から、ランクカラーを取得する
     * @param point ポイント
     * @return ランクカラー（§n 形式の文字列で返します）
     */
    public String getColorFromPoint(int point) {
        return getColorFromRank(getRankFromPoint(point));
    }
    
    /**
     * 称号名から、ランクシンボルを取得する
     * @param rank 称号名
     * @return ランクシンボル
     */
    public String getSymbolFromRank(String rank) {
        
        if ( rankSymbols.containsKey(rank) ) {
            return rankSymbols.get(rank);
        }
        return "";
    }

    public boolean isUseVault() {
        return useVault;
    }

    public boolean isDisplayPointOnChat() {
        return displayPointOnChat;
    }

    public List<String> getDisplayPointOnChatWorlds() {
        return displayPointOnChatWorlds;
    }

    public boolean isUseWebstat() {
        return useWebstat;
    }

    public int getWebstatPortNumber() {
        return webstatPortNumber;
    }

    public int getInitialPoint() {
        return initialPoint;
    }

    public int getWinBasePoint() {
        return winBasePoint;
    }

    public int getWinOffsetPoint() {
        return winOffsetPoint;
    }

    public int getWinBonusPointPercent() {
        return winBonusPointPercent;
    }

    public int getWinTeamBonusPoint() {
        return winTeamBonusPoint;
    }
}
