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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.github.ucchyocean.bp.bridge.ColorTeamingBridge;
import com.github.ucchyocean.bp.bridge.VaultChatBridge;
import com.github.ucchyocean.bp.webstat.BPWebServer;

/**
 * バトルポイントシステム プラグイン
 * @author ucchy
 */
public class BattlePoints extends JavaPlugin {

    private static String prefix;

    protected static BattlePoints instance;
    protected static VaultChatBridge vcbridge;
    protected static ColorTeamingBridge ctbridge;
    
    /** スコアボードのオブジェクティブ */
    private Objective objective;
    
    /** コンフィグデータ */
    private BPConfig config;
    
    /** Webstatサーバー */
    private BPWebServer webserver;
    
    /** Webstatサーバーの起動タスク */
    private BukkitTask webserverTask;

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
            Score s = objective.getScore(Bukkit.getOfflinePlayer(data.name));
            s.setScore(data.point);
        }
        
        // Webstatサーバーの起動
        if ( config.isUseWebstat() ) {
            webserver = new BPWebServer();
            webserverTask = 
                    getServer().getScheduler().runTaskAsynchronously(this, webserver);
        }
    }
    
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        if ( webserver != null ) {
            webserver.stop();
            getServer().getScheduler().cancelTask(webserverTask.getTaskId());
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
    protected static File getPluginJarFile() {
        return instance.getFile();
    }
    
    /**
     * Webstatのコンテンツフォルダを取得する
     * @return コンテンツフォルダ
     */
    public File getWebstatContentFolder() {
        File file = new File(instance.getDataFolder(), "webstat");
        if ( !file.exists() ) {
            file.mkdirs();
        }
        return file;
    }
    
    /**
     * Webstatのログフォルダを取得する
     * @return ログフォルダ
     */
    public File getWebstatLogFolder() {
        File file = new File(instance.getDataFolder(), "webstat-log");
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
        winnerData.point = lastWinnerPoint + winnerRate;
        winnerData.kills++;
        loserData.point = lastLoserPoint - loserRate;
        loserData.deaths++;

        // 勝者、敗者が上限、下限に達したら、補正を行う
        if ( winnerData.point > 9999 ) {
            winnerData.point = 9999;
        }
        if ( loserData.point < 0 ) {
            loserData.point = 0;
        }

        // データ保存
        winnerData.save();
        loserData.save();

        // ポイント表示更新
        String wRank = config.getRankFromPoint(winnerData.point);
        String lRank = config.getRankFromPoint(loserData.point);
        String wColor = config.getColorFromPoint(winnerData.point);
        String lColor = config.getColorFromPoint(loserData.point);

        broadcastMessage("battleResult",
                wColor, winner.getName(),  winnerData.point, winnerRate, 
                lColor, loser.getName(), loserData.point, loserRate);

        // スコアボード更新
        objective.getScore(winner).setScore(winnerData.point);
        objective.getScore(loser).setScore(loserData.point);
        
        // 称号が変わったかどうかを確認する
        if ( !wRank.equals(config.getRankFromPoint(lastWinnerPoint)) ) {
            broadcastMessage("rankup", winner.getName(), wRank);
        }
        if ( !lRank.equals(config.getRankFromPoint(lastLoserPoint)) ) {
            broadcastMessage("rankdown", loser.getName(), lRank);
        }

        // Vault連携の場合は、ここでSuffixを設定する
        if ( config.isDisplayPointOnChat() && config.isUseVault() 
                && BattlePoints.vcbridge != null ) {
            String wSymbol = config.getSymbolFromRank(wRank);
            String wSuf = String.format("&f[%s%s%d&f]", wColor, wSymbol, winnerData.point);
            BattlePoints.vcbridge.setPlayerSuffix(winner, wSuf);
            String lSymbol = config.getSymbolFromRank(lRank);
            String lSuf = String.format("&f[%s%s%d&f]", lColor, lSymbol, loserData.point);
            BattlePoints.vcbridge.setPlayerSuffix(loser, lSuf);
        }
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
        Score score = objective.getScore(Bukkit.getOfflinePlayer(name));
        score.setScore(data.point);
        
        // ランクが変動するなら、メッセージを送信する
        String rank = config.getRankFromPoint(data.point);
        if ( !oldRank.equals(rank) ) {
            if ( isPlus ) 
                broadcastMessage("rankup", name, rank);
            else 
                broadcastMessage("rankdown", name, rank);
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
     * 設定ファイルなどのリロード処理
     */
    public void reloadDatas() {
        
        // 設定の読み込み処理
        config = BPConfig.load();

        // メッセージの初期化
        Messages.initialize();
        prefix = Messages.get("prefix");
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
}
