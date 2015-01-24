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
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * ユーザーのポイントデータクラス
 * @author ucchy
 */
public class BPUserData {

    private static final String DATA_FOLDER_NAME = "users";

    private static File saveFolder;

    private static HashMap<OfflinePlayer, BPUserData> cache
            = new HashMap<OfflinePlayer, BPUserData>();

    public static boolean needRefresh = true;

    private File file;

    /** プレイヤー */
    private OfflinePlayer player;

    /** ポイント */
    private int point;

    /** キル数 */
    private int kills;

    /** デス数 */
    private int deaths;

    /**
     * コンストラクタ。pointは初期値になる。
     * @param player プレイヤー名
     */
    private BPUserData(OfflinePlayer player) {
        this(player, -1, 0, 0);
    }

    /**
     * コンストラクタ
     * @param player プレイヤー名
     * @param point ポイント（-1を指定した場合、初期値に設定される）
     * @param kills キル数
     * @param deaths デス数
     */
    private BPUserData(OfflinePlayer player, int point, int kills, int deaths) {

        this.player = player;
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
    protected void save() {

        needRefresh = true;

        if ( saveFolder == null ) {
            saveFolder = new File(
                    BattlePoints.getConfigFolder(), DATA_FOLDER_NAME);
            if ( !saveFolder.exists() && !saveFolder.isDirectory() ) {
                saveFolder.mkdirs();
            }
        }

        if ( file == null ) {
            if ( Utility.isCB178orLater() ) {
                file = new File(saveFolder, player.getUniqueId().toString() + ".yml");
            } else {
                file = new File(saveFolder, player.getName() + ".yml");
            }
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("point", point);
        config.set("kills", kills);
        config.set("deaths", deaths);
        if ( Utility.isCB178orLater() ) {
            config.set("name", player.getName());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 全データを再読み込みして、キャッシュを初期化する。
     */
    protected static void initCache() {

        cache = new HashMap<OfflinePlayer, BPUserData>();
        for ( BPUserData data : getAllUserData() ) {
            cache.put(data.player, data);
        }

        // 1位を調べて更新する
        if ( cache.size() > 0 ) {
            ArrayList<BPUserData> datas = new ArrayList<BPUserData>(cache.values());
            sortUserData(datas);
            BattlePoints.instance.setChampion(datas.get(0).player);
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
                BPUserData data1 = getData(ent1);
                BPUserData data2 = getData(ent2);
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
                return ent2.getKills() - ent1.getKills();
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
                return ent2.getDeaths() - ent1.getDeaths();
            }
        });
    }

    /**
     * プレイヤーに対応したユーザーデータを取得する
     * @param player プレイヤー
     * @return BPUserData
     */
    public static BPUserData getData(OfflinePlayer player) {

        if ( player == null ) {
            return null;
        }

        if ( cache.containsKey(player) ) {
            return cache.get(player);
        }

        if ( saveFolder == null ) {
            saveFolder = new File(
                    BattlePoints.getConfigFolder(), DATA_FOLDER_NAME);
            if ( !saveFolder.exists() && !saveFolder.isDirectory() ) {
                saveFolder.mkdirs();
            }
        }

        String filename;
        if ( Utility.isCB178orLater() ) {
            filename = player.getUniqueId().toString() + ".yml";
        } else {
            filename = player.getName() + ".yml";
        }
        File file = new File(saveFolder, filename);
        if ( !file.exists() ) {
            cache.put(player, new BPUserData(player));
            return cache.get(player);
        }

        int initial = BattlePoints.instance.getBPConfig().getInitialPoint();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int point = config.getInt("point", initial);
        int kills = config.getInt("kills", 0);
        int deaths = config.getInt("deaths", 0);
        cache.put(player, new BPUserData(player, point, kills, deaths));
        return cache.get(player);
    }

    /**
     * プレイヤー名からBPUserDataを取得する
     * @param name プレイヤー名
     * @return BPUserData
     */
    @SuppressWarnings("deprecation")
    public static BPUserData getDataFromName(String name) {
        return getData(Bukkit.getOfflinePlayer(name));
    }

    /**
     * 指定したプレイヤーのポイントを返す
     * @param player プレイヤー
     * @return ポイント
     */
    public static int getPoint(OfflinePlayer player) {

        BPUserData data = getData(player);
        if ( data != null ) {
            return data.getPoint();
        } else {
            return 0;
        }
    }

    /**
     * 全てのユーザーデータをまとめて返す。
     * @return 全てのユーザーデータ。
     */
    public static ArrayList<BPUserData> getAllUserData() {

        if ( cache != null && cache.size() > 0 ) {
            return new ArrayList<BPUserData>(cache.values());
        }

        if ( saveFolder == null ) {
            saveFolder = new File(
                    BattlePoints.getConfigFolder(), DATA_FOLDER_NAME);
            if ( !saveFolder.exists() && !saveFolder.isDirectory() ) {
                saveFolder.mkdirs();
            }
        }

        String[] filelist = saveFolder.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        ArrayList<BPUserData> results = new ArrayList<BPUserData>();
        for ( String f : filelist ) {
            String name = f.substring(0, f.indexOf(".") );
            results.add(getData(getOfflinePlayer(name)));
        }

        return results;
    }

    /**
     * K/Dレートを取得する
     * @return K/Dレート
     */
    public double getKDRate() {

        if ( deaths == 0 && kills > 0 )
            return (double)kills;
        else if ( deaths == 0 && kills == 0 )
            return 0;
        else if ( deaths > 0 && kills == 0 )
            return -(double)deaths;
        else
            return (double)kills / (double)deaths;
    }

    /**
     * 現在のポイントに該当するクラスランクを取得する
     * @return クラスランク
     */
    public String getRankClass() {
        return BattlePoints.getInstance().getBPConfig().getRankFromPoint(point);
    }

    /**
     * 名前、または、UUIDの文字列から、OfflinePlayerを取得して返す
     * @param nameOrUuid CB175以前なら名前、CB178以降ならUUIDを指定する
     * @return OfflinePlayer
     */
    @SuppressWarnings("deprecation")
    private static OfflinePlayer getOfflinePlayer(String nameOrUuid) {
        if ( Utility.isCB178orLater() ) {
            UUID id = UUID.fromString(nameOrUuid);
            return Bukkit.getOfflinePlayer(id);
        } else {
            return Bukkit.getOfflinePlayer(nameOrUuid);
        }
    }

    /**
     * データのバージョンアップが必要かチェックを行い、
     * 必要ならバージョンアップを行う。
     * @return バージョンアップを行ったかどうか
     */
    protected static boolean upgrade() {

        if ( !Utility.isCB178orLater() ) {
            return false;
        }

        if ( saveFolder == null ) {
            saveFolder = new File(
                    BattlePoints.getConfigFolder(), DATA_FOLDER_NAME);
            if ( !saveFolder.exists() && !saveFolder.isDirectory() ) {
                saveFolder.mkdirs();
            }
        }

        boolean upgraded = false;

        File[] files = saveFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        for ( File file : files ) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // nameというタグでデータが含まれているなら、
            // このファイルは既にアップグレード済みとみなす。
            if ( config.contains("name") ) continue;

            // アップグレード処理を行う。
            String filename = file.getName();
            String name = filename.substring(0, filename.indexOf(".") );
            @SuppressWarnings("deprecation")
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            int initial = BattlePoints.instance.getBPConfig().getInitialPoint();
            int point = config.getInt("point", initial);
            int kills = config.getInt("kills", 0);
            int deaths = config.getInt("deaths", 0);
            BPUserData data = new BPUserData(player, point, kills, deaths);

            // 元のファイルを削除、新しいデータを保存
            file.delete();
            data.save();
            upgraded = true;
        }

        return upgraded;
    }

    /**
     * プレイヤーを取得する
     * @return player
     */
    public OfflinePlayer getPlayer() {
        return player;
    }

    /**
     * キル数を取得する
     * @return kills
     */
    public int getKills() {
        return kills;
    }

    /**
     * キル数を設定する
     * @param kills キル数
     */
    public void setKills(int kills) {
        this.kills = kills;
    }

    /**
     * デス数を取得する
     * @return deaths
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * デス数を設定する
     * @param deaths デス数
     */
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    /**
     * プレイヤー名を取得する
     * @return プレイヤー名
     */
    public String getName() {
        // 必ず非nullを返すように、保護を行う。
        if ( player.getName() == null ) {
            if ( Utility.isCB178orLater() ) {
                return player.getUniqueId().toString();
            } else {
                return player.toString();
            }
        }
        return player.getName();
    }

    /**
     * ポイントを取得する
     * @return ポイント
     */
    public int getPoint() {
        return point;
    }

    /**
     * ポイントを設定する(saveを忘れずに！)
     * @param point ポイント
     */
    public void setPoint(int point) {
        this.point = point;
    }
}
