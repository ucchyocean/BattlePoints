/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import de.dustplanet.colorme.Actions;
import de.dustplanet.colorme.ColorMe;

/**
 * @author ucchy
 * バトルポイントシステム プラグイン
 */
public class BattlePoints extends JavaPlugin {

    protected static BattlePoints instance;
    protected static LastAttackData lastAttackData;
    protected static BPData data;
    private static ColorMe colorme;
    private static Logger logger;

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        instance = this;
        logger = getLogger();

        // 設定の読み込み処理
        BPConfig.reloadConfig();

        // 前提プラグイン ColorMe の取得
        Plugin temp = getServer().getPluginManager().getPlugin("ColorMe");
        if ( temp != null && temp instanceof ColorMe ) {
            colorme = (ColorMe)temp;
        } else {
            logger.severe("ColorMe がロードされていません。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // コマンドをサーバーに登録
        getCommand("battlepoints").setExecutor(new BPCommand());

        // イベント購読をサーバーに登録
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // 変数の初期化
        lastAttackData = new LastAttackData();
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
     * Player に、ColorMe の色を設定する。
     * @param player プレイヤー
     * @param color ColorMeの色
     */
    public static void setPlayerColor(Player player, String color) {

        Actions actions = new Actions(colorme);
        actions.set(player.getName(), color, "default", "colors");
        actions.checkNames(player.getName(), "default");
    }

    /**
     * Player に、ColorMe の Suffixを設定する。
     * @param player プレイヤー
     * @param suffix ColorMeのsuffix
     */
    public static void setPlayerSuffix(Player player, String suffix) {

        Actions actions = new Actions(colorme);
        actions.set(player.getName(), suffix, "default", "suffix");
        actions.checkNames(player.getName(), "default");
    }

    /**
     * メッセージをブロードキャストに送信する。
     * @param message 送信するメッセージ
     */
    public static void sendBroadcast(String message) {
        instance.getServer().broadcastMessage(message);
    }

    /**
     * プレイヤー名からPlayerインスタンスを返す。
     * @param name プレイヤー名
     * @return
     */
    public static Player getPlayerExact(String name) {
        return instance.getServer().getPlayerExact(name);
    }

    /**
     * 全てのプレイヤーを取得する
     * @return 全てのプレイヤー
     */
    public static ArrayList<Player> getAllPlayers() {
        Player[] temp = instance.getServer().getOnlinePlayers();
        ArrayList<Player> result = new ArrayList<Player>();
        for ( Player p : temp ) {
            result.add(p);
        }
        return result;
    }
}
