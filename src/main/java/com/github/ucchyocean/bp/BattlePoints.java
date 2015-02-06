/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

    /** 現在1位のプレイヤー */
    private UUID championID;

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

        // データのアップグレード（必要に応じて）
        if ( BPUserData.upgrade() ) {
            getLogger().info("BattlePoints data files have been upgraded!!");
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

        BPUserData winnerData = BPUserData.getData(winner.getUniqueId());
        BPUserData loserData = BPUserData.getData(loser.getUniqueId());
        int lastWinnerPoint = winnerData.getPoint();
        int lastLoserPoint = loserData.getPoint();
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
        setPoint(winner, newWinnerPoint);
        addKill(winner, 1);
        setPoint(loser, newLoserPoint);
        addDeath(loser, 1);
        winnerData = BPUserData.getData(winner.getUniqueId());
        loserData = BPUserData.getData(loser.getUniqueId());

        // ポイント移動の通知を行う
        String wColor = config.getColorFromPoint(winnerData.getPoint());
        String lColor = config.getColorFromPoint(loserData.getPoint());
        broadcastMessage("battleResult",
                wColor, winner.getName(),  winnerData.getPoint(), winnerRate,
                lColor, loser.getName(), loserData.getPoint(), loserRate);
    }

    /**
     * 指定したプレイヤーのポイントを再設定する
     * @param player プレイヤー
     * @param point ポイント
     */
    public void setPoint(OfflinePlayer player, int point) {

        BPUserData data = BPUserData.getData(player.getUniqueId());

        // 変動前のランクを取得しておく
        String oldRank = config.getRankFromPoint(data.getPoint());
        boolean isPlus = data.getPoint() < point;

        // ポイント更新と保存
        data.setPoint(point);
        data.save();

        // スコアボード更新
        Score score = getScore(objective, player);
        score.setScore(data.getPoint());

        // ランクが変動するなら、メッセージを送信する
        String rank = config.getRankFromPoint(data.getPoint());
        if ( !oldRank.equals(rank) ) {
            if ( isPlus )
                broadcastMessage("rankup", player, rank);
            else
                broadcastMessage("rankdown", player, rank);
        }

        // Vault連携の場合は、ここでSuffixを設定する
        if ( config.isDisplayPointOnChat() && config.isUseVault() && vcbridge != null ) {
            String symbol = config.getSymbolFromRank(rank);
            String color = config.getColorFromRank(rank);
            String suffix = String.format("&f[%s%s%d&f]", color, symbol, point);

            for ( String world : config.getDisplayPointOnChatWorlds() ) {
                BattlePoints.vcbridge.setPlayerSuffix(world, player, suffix);
            }
        }

        // チャンピオンのポイントが減算されたか、
        // チャンピオン以外の人のポイントがチャンピオンのポイントを超えたなら、
        // チャンピオンの更新を確認する
        int championPoint = BPUserData.getPoint(championID);
        if ( (player.getUniqueId().equals(championID) && !isPlus) ||
                (!player.getUniqueId().equals(championID) && (championPoint < point) ) ) {
            ArrayList<BPUserData> datas = BPUserData.getAllUserData();
            BPUserData.sortUserData(datas);
            setChampion(datas.get(0).getID());
        }
    }

    /**
     * 指定したプレイヤーのポイントを追加する
     * @param player プレイヤー
     * @param point ポイント
     * @return 加算後のポイント
     */
    public int addPoint(OfflinePlayer player, int point) {
        int newpoint = BPUserData.getData(player.getUniqueId()).getPoint() + point;
        setPoint(player, newpoint);
        return newpoint;
    }

    /**
     * 指定したプレイヤーのキル数を追加する
     * @param player プレイヤー
     * @param amount キル数
     * @return 加算後のキル数
     */
    public int addKill(OfflinePlayer player, int amount) {
        BPUserData data = BPUserData.getData(player.getUniqueId());
        int newpoint = data.getKills() + amount;
        if ( newpoint < 0 ) {
            newpoint = 0;
        }
        data.setKills(newpoint);
        data.save();
        return newpoint;
    }

    /**
     * 指定したプレイヤー名のデス数を追加する
     * @param player プレイヤー
     * @param amount デス数
     * @return 加算後のデス数
     */
    public int addDeath(OfflinePlayer player, int amount) {
        BPUserData data = BPUserData.getData(player.getUniqueId());
        int newpoint = data.getDeaths() + amount;
        if ( newpoint < 0 ) {
            newpoint = 0;
        }
        data.setDeaths(newpoint);
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
     * チャンピオンのIDを返す
     * @return チャンピオン
     */
    protected UUID getChampion() {
        return championID;
    }

    /**
     * チャンピオンの更新を行う
     * @param id チャンピオンのID
     */
    protected void setChampion(UUID id) {
        setChampion(Bukkit.getOfflinePlayer(id));
    }

    /**
     * チャンピオンの更新を行う
     * @param player チャンピオン
     */
    protected void setChampion(OfflinePlayer player) {

        if ( championID == null ) {
            championID = player.getUniqueId();

            // Vault連携の場合は、ここでPrefixを設定する
            if ( config.isDisplayPointOnChat() && config.isUseVault()  && vcbridge != null ) {
                String pre = config.getChampionPrefix();

                for ( String world : config.getDisplayPointOnChatWorlds() ) {
                    String worldpre = vcbridge.getPlayerPrefix(world, player);
                    if ( !worldpre.startsWith(pre) ) {
                        vcbridge.setPlayerPrefix(world, player, pre + worldpre);
                    }
                }
            }

        } else {
            if ( championID.equals(player.getUniqueId()) ) {
                return; // チャンピオンに変化が無いので何もしない
            }

            UUID prevChampID = championID;
            OfflinePlayer prevChamp = Bukkit.getOfflinePlayer(prevChampID);
            championID = player.getUniqueId();

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
                    worldpre = vcbridge.getPlayerPrefix(world, player);
                    if ( !worldpre.startsWith(pre) ) {
                        vcbridge.setPlayerPrefix(world, player, pre + worldpre);
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

    /**
     * スコアボードのスコア項目を取得する
     * @param objective オブジェクティブ
     * @param playerName プレイヤー名
     * @return スコア
     */
    private static Score getScore(Objective objective, OfflinePlayer player) {

        if ( Utility.isCB178orLater() ) {
            return objective.getScore(player.getName());
        } else {
            @SuppressWarnings("deprecation")
            Score score = objective.getScore(player);
            return score;
        }
    }
}
