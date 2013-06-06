/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * ユーザーのポイントデータクラス
 * @author ucchy
 */
public class BPUserData {

    /** プレイヤー名 */
    public String name;

    /** ポイント */
    public int point;

    /**
     * コンストラクタ。pointは初期値になる。
     * @param name プレイヤー名
     */
    public BPUserData(String name) {
        this(name, BPConfig.initialPoint);
    }

    /**
     * コンストラクタ
     * @param name プレイヤー名
     * @param point ポイント
     */
    public BPUserData(String name, int point) {
        this.name = name;
        this.point = point;
    }

    /**
     * ArrayList&lt;BPUserData&gt; 型の配列を、降順にソートする。
     * @param data ソート対象の配列
     */
    protected static void sortUserData(ArrayList<BPUserData> data) {

        Collections.sort(data, new Comparator<BPUserData>(){
            public int compare(BPUserData ent1, BPUserData ent2){
                return ent2.point - ent1.point;
            }
        });
    }
}
