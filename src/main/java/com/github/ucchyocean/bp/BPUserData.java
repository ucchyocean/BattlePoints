/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * ユーザーのポイントデータクラス
 * @author ucchy
 */
public class BPUserData {

    private static final String DATA_FOLDER_NAME = "users";

    private static File saveFolder;

    private File file;

    /** プレイヤー名 */
    public String name;

    /** ポイント */
    public int point;

    /** キル数 */
    public int kills;

    /** デス数 */
    public int deaths;

    /**
     * コンストラクタ。pointは初期値になる。
     * @param name プレイヤー名
     */
    private BPUserData(String name) {
        this(name, -1, 0, 0);
    }

    /**
     * コンストラクタ
     * @param name プレイヤー名
     * @param point ポイント（-1を指定した場合、初期値に設定される）
     * @param kills キル数
     * @param deaths デス数
     */
    private BPUserData(String name, int point, int kills, int deaths) {

        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        
        if ( point == -1 ) {
            BPConfig config = BattlePoints.instance.getBPConfig();
            this.point = config.getInitialPoint();
        } else {
            this.point = point;
        }
    }

    /**
     * このBPUserDataオブジェクトを保存する
     */
    public void save() {

        if ( saveFolder == null ) {
            saveFolder = new File(
                    BattlePoints.getConfigFolder(), DATA_FOLDER_NAME);
            if ( !saveFolder.exists() && !saveFolder.isDirectory() ) {
                saveFolder.mkdirs();
            }
        }

        if ( file == null ) {
            file = new File(saveFolder, name + ".yml");
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("point", point);
        config.set("kills", kills);
        config.set("deaths", deaths);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ArrayList&lt;BPUserData&gt; 型の配列を、降順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortUserData(ArrayList<BPUserData> data) {

        Collections.sort(data, new Comparator<BPUserData>() {
            public int compare(BPUserData ent1, BPUserData ent2) {
                return ent2.point - ent1.point;
            }
        });
    }

    /**
     * ArrayList&lt;Player&gt; 型の配列を、ポイント降順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortPlayerByPoint(ArrayList<Player> data) {

        Collections.sort(data, new Comparator<Player>() {
            public int compare(Player ent1, Player ent2) {
                BPUserData data1 = getData(ent1.getName());
                BPUserData data2 = getData(ent2.getName());
                return data2.point - data1.point;
            }
        });
    }

    /**
     * ArrayList&lt;BPUserData&gt; 型の配列を、KD降順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortUserDataByKDRate(ArrayList<BPUserData> data) {

        Collections.sort(data, new Comparator<BPUserData>() {
            public int compare(BPUserData ent1, BPUserData ent2) {
                double kd1 = ent1.getKDRate();
                double kd2 = ent2.getKDRate();
                if ( kd1 < kd2 ) {
                    return 1;
                } else if ( kd1 > kd2 ) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

    /**
     * ArrayList&lt;BPUserData&gt; 型の配列を、kill順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortUserDataByKillCount(ArrayList<BPUserData> data) {
        Collections.sort(data, new Comparator<BPUserData>() {
            public int compare(BPUserData ent1, BPUserData ent2) {
                return ent2.getKillCount() - ent1.getKillCount();
            }
        });
    }

    /**
     * ArrayList&lt;BPUserData&gt; 型の配列を、death順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortUserDataByDeathCount(ArrayList<BPUserData> data) {
        Collections.sort(data, new Comparator<BPUserData>() {
            public int compare(BPUserData ent1, BPUserData ent2) {
                return ent2.getDeathCount() - ent1.getDeathCount();
            }
        });
    }

    /**
     * プレイヤー名に対応したユーザーデータを取得する
     * @param name プレイヤー名
     * @return BPUserData
     */
    public static BPUserData getData(String name) {

        if ( saveFolder == null ) {
            saveFolder = new File(
                    BattlePoints.getConfigFolder(), DATA_FOLDER_NAME);
            if ( !saveFolder.exists() && !saveFolder.isDirectory() ) {
                saveFolder.mkdirs();
            }
        }

        File file = new File(saveFolder, name + ".yml");
        if ( !file.exists() ) {
            return new BPUserData(name);
        }

        int initial = BattlePoints.instance.getBPConfig().getInitialPoint();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int point = config.getInt("point", initial);
        int kills = config.getInt("kills", 0);
        int deaths = config.getInt("deaths", 0);
        return new BPUserData(name, point, kills, deaths);
    }

    /**
     * 全てのユーザーデータをまとめて返す。
     * @return 全てのユーザーデータ。
     */
    public static ArrayList<BPUserData> getAllUserData() {

        if ( saveFolder == null ) {
            saveFolder = new File(
                    BattlePoints.getConfigFolder(), DATA_FOLDER_NAME);
            if ( !saveFolder.exists() && !saveFolder.isDirectory() ) {
                saveFolder.mkdirs();
            }
        }

        String[] filelist = saveFolder.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if ( name.endsWith(".yml") )
                    return true;
                return false;
            }
        });

        ArrayList<BPUserData> results = new ArrayList<BPUserData>();
        for ( String f : filelist ) {
            String name = f.substring(0, f.indexOf(".") );
            results.add(getData(name));
        }

        return results;
    }
    
    /**
     * K/Dレートを取得する
     * @return K/Dレート
     */
    public double getKDRate() {
        
        if ( deaths == 0 && kills > 0 ) 
            return 999.0;
        else if ( deaths == 0 && kills == 0 ) 
            return 0;
        else 
            return (double)kills / (double)deaths;
    }
    
    /**
     * キル数を取得する
     * @return キル数
     */
    public int getKillCount() {
        return kills;
    }
    
    /**
     * デス数を取得する
     * @return デス数
     */
    public int getDeathCount() {
        return deaths;
    }
}
