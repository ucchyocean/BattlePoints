/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author ucchy
 * バトルポイントシステム プラグイン
 */
public class BattlePoints extends JavaPlugin {

    protected static BattlePoints instance;
    protected static BPData data;

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
