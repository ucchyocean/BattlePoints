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
            if ( Utility.isUpperVersion(ctversion, "2.2.0") ) {
                ctbridge = ColorTeamingBridge.load(this, colorteaming);
                getServer().getPluginManager().registerEvents(ctbridge, this);
                getLogger().info("ColorTeaming がロードされました。連携機能を有効にします。");
            } else {
                getLogger().warning("ColorTeaming のバージョンが古いため、連携機能は無効になりました。");
                getLogger().warning("連携機能を使用するには、ColorTeaming v2.2.0 以上が必要です。");
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
     */
    public void addPoint(String name, int point) {
        int newpoint = BPUserData.getData(name).point + point;
        setPoint(name, newpoint);
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
