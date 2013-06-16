/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.util.ArrayList;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * バトルポイントシステム プラグイン
 * @author ucchy
 */
public class BattlePoints extends JavaPlugin {

    private static String prefix;

    protected static BattlePoints instance;
    protected static Chat vaultChat;

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        instance = this;

        // 設定の読み込み処理
        BPConfig.reloadConfig();

        // メッセージの初期化
        Messages.initialize();
        prefix = Messages.get("prefix");

        // ColorTeaming のロード
        Plugin colorteaming = null;
        if ( getServer().getPluginManager().isPluginEnabled("ColorTeaming") ) {
            colorteaming = getServer().getPluginManager().getPlugin("ColorTeaming");
            String ctversion = colorteaming.getDescription().getVersion();
            if ( Utility.isUpperVersion(ctversion, "2.0.0") ) {
                getLogger().info("ColorTeaming がロードされました。連携機能を有効にします。");
                new BPCustomScore(getServer(), colorteaming);
            } else {
                getLogger().warning("ColorTeaming のバージョンが古いため、連携機能は無効になりました。");
                getLogger().warning("連携機能を使用するには、ColorTeaming v2.0.0 以上が必要です。");
            }
        }

        // コマンドをサーバーに登録
        getCommand("battlepoints").setExecutor(new BPCommand(colorteaming));

        // イベント購読をサーバーに登録
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // Vault経由のチャット装飾プラグインのロード
        if ( BPConfig.useVault && getServer().getPluginManager().isPluginEnabled("Vault") ) {
            RegisteredServiceProvider<Chat> chatProvider =
                    getServer().getServicesManager().getRegistration(Chat.class);
            if ( chatProvider != null ) {
                vaultChat = chatProvider.getProvider();
            }
        }

        // 未アップデートなら、アップデートする
        File oldFile = new File(getDataFolder(), "points.yml");
        if ( oldFile.exists() ) {
            BPUserData.updateDatas(oldFile);
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
     * メッセージをブロードキャストに送信する。
     * @param message 送信するメッセージ
     */
    protected static void sendBroadcast(String message) {
        instance.getServer().broadcastMessage(message);
    }

    /**
     * プレイヤー名からPlayerインスタンスを返す。
     * @param name プレイヤー名
     * @return
     */
    protected static Player getPlayerExact(String name) {
        return instance.getServer().getPlayerExact(name);
    }

    /**
     * 全てのプレイヤーを取得する
     * @return 全てのプレイヤー
     */
    protected static ArrayList<Player> getAllPlayers() {
        Player[] temp = instance.getServer().getOnlinePlayers();
        ArrayList<Player> result = new ArrayList<Player>();
        for ( Player p : temp ) {
            result.add(p);
        }
        return result;
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
        int rate = BPConfig.getEloRating(lastWinnerPoint, lastLoserPoint);
        int winnerRate = rate + BPConfig.winOffsetPoint;
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
        String wRank = BPConfig.getRankFromPoint(winnerData.point);
        String lRank = BPConfig.getRankFromPoint(loserData.point);
        String wColor = BPConfig.rankColors.get(wRank).toString();
        String lColor = BPConfig.rankColors.get(lRank).toString();

        broadcastMessage("battleResultWinner",
                wColor, winner.getName(),  winnerData.point,
                winnerRate, winnerData.kills, winnerData.deaths);
        broadcastMessage("battleResultLoser",
                lColor, loser.getName(), loserData.point,
                loserRate, loserData.kills, loserData.deaths);

        // 称号が変わったかどうかを確認する
        if ( !wRank.equals(BPConfig.getRankFromPoint(lastWinnerPoint)) ) {
            broadcastMessage("rankup", winner.getName(), wRank);
        }
        if ( !lRank.equals(BPConfig.getRankFromPoint(lastLoserPoint)) ) {
            broadcastMessage("rankdown", loser.getName(), lRank);
        }

        // Vault連携の場合は、ここでSuffixを設定する
        if ( BPConfig.displayPointOnChat && BPConfig.useVault && BattlePoints.vaultChat != null ) {
            String wSymbol = BPConfig.rankSymbols.get(wRank);
            String wSuf = String.format("&f[%s%s%d&f]", wColor, wSymbol, winnerData.point);
            BattlePoints.vaultChat.setPlayerSuffix(winner, wSuf);
            String lSymbol = BPConfig.rankSymbols.get(lRank);
            String lSuf = String.format("&f[%s%s%d&f]", lColor, lSymbol, loserData.point);
            BattlePoints.vaultChat.setPlayerSuffix(loser, lSuf);
        }
    }

    /**
     * メッセージリソースを取得し、ブロードキャストする
     * @param key メッセージキー
     * @param args メッセージの引数
     * @return メッセージ
     */
    private void broadcastMessage(String key, Object... args) {

        String msg = Messages.get(key, args);
        if ( msg.equals("") ) {
            return;
        }
        Bukkit.broadcastMessage(Utility.replaceColorCode(prefix + msg));
    }

    /**
     * ポイントから、ランクの色を取得する
     * @param point ポイント
     * @return ランクの色
     */
    public ChatColor getColorByPoint(int point) {

        String rank = BPConfig.getRankFromPoint(point);
        return BPConfig.rankColors.get(rank);
    }
}
