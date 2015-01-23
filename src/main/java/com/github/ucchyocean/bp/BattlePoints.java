/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.github.ucchyocean.bp.bridge.ColorTeamingBridge;
import com.github.ucchyocean.bp.bridge.VaultChatBridge;
import com.github.ucchyocean.bp.webstats.BPWebServer;

/**
 * バトルポイントシステム プラグイン
 * @author ucchy
 */
public class BattlePoints extends JavaPlugin {

    private static String prefix;

    protected static BattlePoints instance;
    protected static VaultChatBridge vcbridge;
    protected static ColorTeamingBridge ctbridge;

    /** 現在1位のプレイヤー名 */
    private String championName;

    /** スコアボードのオブジェクティブ */
    private Objective objective;

    /** コンフィグデータ */
    private BPConfig config;

    /** Webstatsサーバー */
    private BPWebServer webserver;

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        instance = this;

        // 設定のロード
        reloadDatas();

        // ColorTeaming のロード
        Plugin colorteaming = null;
        if ( getServer().getPluginManager().isPluginEnabled("ColorTeaming") ) {
            colorteaming = getServer().getPluginManager().getPlugin("ColorTeaming");
            String ctversion = colorteaming.getDescription().getVersion();
            if ( Utility.isUpperVersion(ctversion, "2.2.5") ) {
                ctbridge = ColorTeamingBridge.load(this, colorteaming);
                getServer().getPluginManager().registerEvents(ctbridge, this);
                getLogger().info("ColorTeaming was loaded. BattlePoints was in cooperation with ColorTeaming.");
            } else {
                getLogger().warning("ColorTeaming was too old. The cooperation feature will be disabled.");
                getLogger().warning("NOTE: Please use ColorTeaming v2.2.5 or later version.");
            }
        }

        // コマンドをサーバーに登録
        getCommand("battlepoints").setExecutor(new BPCommand(this));

        // イベント購読をサーバーに登録
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Vault経由のチャット装飾プラグインのロード
        if ( getServer().getPluginManager().isPluginEnabled("Vault") ) {
            vcbridge = VaultChatBridge.load();
        }

        // ユーザーデータのキャッシュ生成
        BPUserData.initCache();

        // objectiveの取得
        Scoreboard sb = getServer().getScoreboardManager().getMainScoreboard();
        objective = sb.getObjective("battlepoints");
        if ( objective == null ) {
            objective = sb.registerNewObjective("battlepoints", "dummy");
            objective.setDisplayName("points");
        }

        // 全プレイヤーのスコア更新
        ArrayList<BPUserData> datas = BPUserData.getAllUserData();
        for ( BPUserData data : datas ) {
            Score s = getScore(objective, data.getName());
            s.setScore(data.getPoint());
        }

        // Webstatsサーバーの起動
        if ( config.isUseWebstat() ) {
            webserver = new BPWebServer();
            webserver.runTaskAsynchronously(this);
        }
    }

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {

        // Webstatsサーバーの停止
        if ( webserver != null ) {
            webserver.stop();
            webserver.cancel();
        }
    }

    /**
     * BattlePointsのインスタンスを返す
     * @return インスタンス
     */
    public static BattlePoints getInstance() {
        return instance;
    }

    /**
     * プラグインのデータフォルダを返す
     * @return プラグインのデータフォルダ
     */
    protected static File getConfigFolder() {
        return instance.getDataFolder();
    }

    /**
     * このプラグインのJarファイル自身を示すFileクラスを返す。
     * @return
     */
    public static File getPluginJarFile() {
        return instance.getFile();
    }

    /**
     * Webstatsのコンテンツフォルダを取得する
     * @return コンテンツフォルダ
     */
    public File getWebstatsContentFolder() {
        File file = new File(instance.getDataFolder(), "webstats");
        if ( !file.exists() ) {
            file.mkdirs();
        }
        return file;
    }

    /**
     * Webstatsのログフォルダを取得する
     * @return ログフォルダ
     */
    public File getWebstatsLogFolder() {
        File file = new File(instance.getDataFolder(), "webstats-log");
        if ( !file.exists() ) {
            file.mkdirs();
        }
        return file;
    }

    /**
     * ポイントの移動を行う
     * @param winner 勝者
     * @param loser 敗者
     */
    public void changePoints(Player winner, Player loser) {

        BPUserData winnerData = BPUserData.getData(winner.getName());
        BPUserData loserData = BPUserData.getData(loser.getName());
        int lastWinnerPoint = winnerData.point;
        int lastLoserPoint = loserData.point;
        int rate = config.getEloRating(lastWinnerPoint, lastLoserPoint);
        int winnerRate = rate + config.getWinOffsetPoint();
        int loserRate = rate;
        int newWinnerPoint = lastWinnerPoint + winnerRate;
        int newLoserPoint = lastLoserPoint - loserRate;

        // 勝者、敗者が上限、下限に達したら、補正を行う
        if ( newWinnerPoint > 9999 ) {
            newWinnerPoint = 9999;
        }
        if ( newLoserPoint < 0 ) {
            newLoserPoint = 0;
        }

        // 更新を行い再取得する
        setPoint(winner.getName(), newWinnerPoint);
        addKill(winner.getName(), 1);
        setPoint(loser.getName(), newLoserPoint);
        addDeath(loser.getName(), 1);
        winnerData = BPUserData.getData(winner.getName());
        loserData = BPUserData.getData(loser.getName());

        // ポイント移動の通知を行う
        String wColor = config.getColorFromPoint(winnerData.point);
        String lColor = config.getColorFromPoint(loserData.point);
        broadcastMessage("battleResult",
                wColor, winner.getName(),  winnerData.point, winnerRate,
                lColor, loser.getName(), loserData.point, loserRate);
    }

    /**
     * 指定したプレイヤー名のポイントを再設定する
     * @param name プレイヤー名
     * @param point ポイント
     */
    public void setPoint(String name, int point) {

        BPUserData data = BPUserData.getData(name);

        // 変動前のランクを取得しておく
        String oldRank = config.getRankFromPoint(data.point);
        boolean isPlus = data.point < point;

        // ポイント更新と保存
        data.point = point;
        data.save();

        // スコアボード更新
        Score score = getScore(objective, name);
        score.setScore(data.point);

        // ランクが変動するなら、メッセージを送信する
        String rank = config.getRankFromPoint(data.point);
        if ( !oldRank.equals(rank) ) {
            if ( isPlus )
                broadcastMessage("rankup", name, rank);
            else
                broadcastMessage("rankdown", name, rank);
        }

        // Vault連携の場合は、ここでSuffixを設定する
        if ( config.isDisplayPointOnChat() && config.isUseVault() && vcbridge != null ) {
            String symbol = config.getSymbolFromRank(rank);
            String color = config.getColorFromRank(rank);
            String suffix = String.format("&f[%s%s%d&f]", color, symbol, point);

            for ( String world : config.getDisplayPointOnChatWorlds() ) {
                BattlePoints.vcbridge.setPlayerSuffix(world, name, suffix);
            }
        }

        // チャンピオンのポイントが減算されたか、
        // チャンピオン以外の人のポイントがチャンピオンのポイントを超えたなら、
        // チャンピオンの更新を確認する
        int championPoint = BPUserData.getPoint(championName);
        if ( (name.equals(championName) && !isPlus) ||
                (!name.equals(championName) && (championPoint < point) ) ) {
            ArrayList<BPUserData> datas = BPUserData.getAllUserData();
            BPUserData.sortUserData(datas);
            refreshChampionNameWith(datas.get(0).name);
        }
    }

    /**
     * 指定したプレイヤー名のポイントを追加する
     * @param name プレイヤー名
     * @param point ポイント
     * @return 加算後のポイント
     */
    public int addPoint(String name, int point) {
        int newpoint = BPUserData.getData(name).point + point;
        setPoint(name, newpoint);
        return newpoint;
    }

    /**
     * 指定したプレイヤー名のキル数を追加する
     * @param name プレイヤー名
     * @param amount キル数
     * @return 加算後のキル数
     */
    public int addKill(String name, int amount) {
        BPUserData data = BPUserData.getData(name);
        int newpoint = data.kills + amount;
        if ( newpoint < 0 ) {
            newpoint = 0;
        }
        data.kills = newpoint;
        data.save();
        return newpoint;
    }

    /**
     * 指定したプレイヤー名のデス数を追加する
     * @param name プレイヤー名
     * @param amount デス数
     * @return 加算後のデス数
     */
    public int addDeath(String name, int amount) {
        BPUserData data = BPUserData.getData(name);
        int newpoint = data.deaths + amount;
        if ( newpoint < 0 ) {
            newpoint = 0;
        }
        data.deaths = newpoint;
        data.save();
        return newpoint;
    }

    /**
     * BattlePointsの設定データを取得する
     * @return BattlePointsの設定データ
     */
    public BPConfig getBPConfig() {
        return config;
    }

    /**
     * チャンピオンの名前を返す
     * @return チャンピオン
     */
    protected String getChampionName() {
        return championName;
    }

    /**
     * チャンピオンの更新を行う
     * @param name
     */
    protected void refreshChampionNameWith(String name) {

        if ( championName == null ) {
            championName = name;

            // Vault連携の場合は、ここでPrefixを設定する
            if ( config.isDisplayPointOnChat() && config.isUseVault()  && vcbridge != null ) {
                String pre = config.getChampionPrefix();

                for ( String world : config.getDisplayPointOnChatWorlds() ) {
                    String worldpre = vcbridge.getPlayerPrefix(world, championName);
                    if ( !worldpre.startsWith(pre) ) {
                        vcbridge.setPlayerPrefix(world, championName, pre + worldpre);
                    }
                }
            }

        } else {
            if ( championName.equals(name) ) {
                return; // チャンピオンに変化が無いので何もしない
            }

            String prevChamp = championName;
            championName = name;

             // Vault連携の場合は、ここでPrefixを設定する
            if ( config.isDisplayPointOnChat() && config.isUseVault()  && vcbridge != null ) {
                String pre = config.getChampionPrefix();

                for ( String world : config.getDisplayPointOnChatWorlds() ) {

                    // 以前のチャンピオンからprefixを取り去る
                    String worldpre = vcbridge.getPlayerPrefix(world, prevChamp);
                    if ( worldpre.startsWith(pre) ) {
                        vcbridge.setPlayerPrefix(world, prevChamp, worldpre.substring(pre.length()));
                    }

                    // 新しいチャンピオンにprefixを与える
                    worldpre = vcbridge.getPlayerPrefix(world, championName);
                    if ( !worldpre.startsWith(pre) ) {
                        vcbridge.setPlayerPrefix(world, championName, pre + worldpre);
                    }
                }
            }
        }
    }

    /**
     * 設定ファイルなどのリロード処理
     */
    public void reloadDatas() {

        // 設定の読み込み処理
        config = BPConfig.load();

        // メッセージの初期化
        Messages.initialize();
        prefix = Messages.get("prefix");

        // Webstatsフォルダが存在しない場合は、jarファイルの中からコピーする
        File webstatsDir = new File(getDataFolder(), "webstats");
        if ( !webstatsDir.exists() || !webstatsDir.isDirectory() ) {
            webstatsDir.mkdirs();
            Utility.copyFolderFromJar(getFile(), webstatsDir, "webstats");
        }
    }

    /**
     * メッセージリソースを取得し、ブロードキャストする
     * @param key メッセージキー
     * @param args メッセージの引数
     */
    private void broadcastMessage(String key, Object... args) {

        String msg = Messages.get(key, args);
        if ( msg.equals("") ) {
            return;
        }
        Bukkit.broadcastMessage(Utility.replaceColorCode(prefix + msg));
    }

    /**
     * スコアボードのスコア項目を取得する
     * @param objective オブジェクティブ
     * @param playerName プレイヤー名
     * @return スコア
     */
    private static Score getScore(Objective objective, String playerName) {

        if ( Utility.isCB178orLater() ) {
            return objective.getScore(playerName);
        } else {
            @SuppressWarnings("deprecation")
            Score score = objective.getScore(Bukkit.getOfflinePlayer(playerName));
            return score;
        }
    }
}
