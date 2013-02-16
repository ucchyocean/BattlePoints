/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * @author ucchy
 * BattlePointsのデータファイル(points.yml)を管理するクラス
 */
public class BPData {

    private static final String CONFIG_FILE_NAME = "points.yml";

    private File file;
    private YamlConfiguration config;

    /**
     * コンストラクタ
     */
    public BPData() {
        reload();
    }

    /**
     * プレイヤーのポイントを取得する。
     * @param name プレイヤー名
     * @return プレイヤーのポイント
     */
    public int getPoint(String name) {

        return config.getInt(name, BPConfig.initialPoint);
    }

    /**
     * プレイヤーポイントを設定してコンフィグの保存を行う。
     * @param name プレイヤー名
     * @param point プレイヤーのポイント
     */
    public void setPoint(String name, int points) {

        config.set(name, points);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * データファイルを再読み込みする
     */
    public void reload() {

        File dir = new File(
                BattlePoints.instance.getDataFolder().getAbsolutePath());
        if ( !dir.exists() ) {
            dir.mkdirs();
        }

        file = new File(
                BattlePoints.instance.getDataFolder() +
                File.separator + CONFIG_FILE_NAME);

        if ( !file.exists() ) {
            YamlConfiguration conf = new YamlConfiguration();
            try {
                conf.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * 全てのユーザーデータをまとめて返す。
     * @return 全てのユーザーデータ。
     */
    public ArrayList<BPUserData> getAllUserData() {

        ArrayList<BPUserData> users = new ArrayList<BPUserData>();

        Iterator<String> i = config.getValues(false).keySet().iterator();
        while (i.hasNext()) {
            String name = i.next();
            int point = config.getInt(name, BPConfig.initialPoint);
            users.add(new BPUserData(name, point));
        }

        ArrayList<Player> players = BattlePoints.getAllPlayers();
        for ( Player p : players ) {
            if ( !config.contains(p.getName()) ) {
                users.add(new BPUserData(p.getName()));
            }
        }

        return users;
    }
}
