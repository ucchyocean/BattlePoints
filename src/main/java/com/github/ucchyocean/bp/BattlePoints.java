/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.ucchyocean.ct.ColorTeaming;

/**
 * バトルポイントシステム プラグイン
 * @author ucchy
 */
public class BattlePoints extends JavaPlugin {

    protected static BattlePoints instance;
    protected static BPData data;
    protected static ColorTeaming colorteaming;

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        instance = this;

        // 設定の読み込み処理
        BPConfig.reloadConfig();

        // コマンドをサーバーに登録
        getCommand("battlepoints").setExecutor(new BPCommand());

        // イベント購読をサーバーに登録
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // 変数の初期化
        data = new BPData();

        // ColorTeaming のロード
        if ( getServer().getPluginManager().isPluginEnabled("ColorTeaming") ) {
            Plugin temp = getServer().getPluginManager().getPlugin("ColorTeaming");
            String ctversion = temp.getDescription().getVersion();
            if ( Utility.isUpperVersion(ctversion, "1.5.9") ) {
                colorteaming = (ColorTeaming)temp;
                getLogger().info("ColorTeaming がロードされました。連携機能を有効にします。");
                colorteaming.getAPI().setCustomScoreCriteria(new BPCustomScore());
            } else {
                colorteaming = null;
                getLogger().warning("ColorTeaming のバージョンが古いため、連携機能は無効になりました。");
                getLogger().warning("連携機能を使用するには、ColorTeaming v1.5.9 以上が必要です。");
            }
        } else {
            colorteaming = null;
            getLogger().warning("ColorTeaming がロードされていないため、連携機能は無効になりました。");
        }
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
}
