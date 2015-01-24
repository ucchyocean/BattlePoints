/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean.bp.webstats;

import java.util.ArrayList;
import java.util.HashMap;

import com.github.ucchyocean.bp.BPUserData;

/**
 * ポイントデータのキャッシュクラス
 * @author ucchy
 */
public class BPDataSorter {

    /** キャッシュの有効期限（1分） */
    private static final long CACHE_EXPIRE_LIMIT = 1 * 60 * 1000;

    private static final String XML_PREFIX =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><battlepoints size=\"%d\">";
    private static final String XML_SUFFIX = "</battlepoints>";
    private static final String XML_LINE =
            "<data name=\"%s\">"
            + "<rank>%d</rank><rrank>%d</rrank><krank>%d</krank><drank>%d</drank>"
            + "<point>%d</point><cls>%s</cls><rate>%.3f</rate>"
            + "<kill>%d</kill><death>%d</death>"
            + "</data>";

    private HashMap<String, int[]> rankData;
    private ArrayList<BPUserData> dataCache;
    private ArrayList<String> pointRankingCache;
    private ArrayList<String> rateRankingCache;
    private ArrayList<String> killRankingCache;
    private ArrayList<String> deathRankingCache;
    private long lastCachedTime;

    public BPDataSorter() {
        refreshData();
    }

    /**
     * データをキャッシュしランキングデータを作成する
     */
    private void refreshData() {

        // データをキャッシュする
        dataCache = BPUserData.getAllUserData();

        rankData = new HashMap<String, int[]>();

        // ポイントランキングを作成する
        pointRankingCache = new ArrayList<String>();
        BPUserData.sortUserData(dataCache);
        for ( int i=0; i<dataCache.size(); i++ ) {
            int[] rank = new int[4];
            rank[0] = (i+1);
            rankData.put(dataCache.get(i).getName(), rank);
            pointRankingCache.add(dataCache.get(i).getName());
        }

        // レートランキングを作成する
        rateRankingCache = new ArrayList<String>();
        BPUserData.sortUserDataByKDRate(dataCache);
        for ( int i=0; i<dataCache.size(); i++ ) {
            rankData.get(dataCache.get(i).getName())[1] = (i+1);
            rateRankingCache.add(dataCache.get(i).getName());
        }

        // キル数ランキングを作成する
        killRankingCache = new ArrayList<String>();
        BPUserData.sortUserDataByKillCount(dataCache);
        for ( int i=0; i<dataCache.size(); i++ ) {
            rankData.get(dataCache.get(i).getName())[2] = (i+1);
            killRankingCache.add(dataCache.get(i).getName());
        }

        // デス数ラインキングを作成する
        deathRankingCache = new ArrayList<String>();
        BPUserData.sortUserDataByDeathCount(dataCache);
        for ( int i=0; i<dataCache.size(); i++ ) {
            rankData.get(dataCache.get(i).getName())[3] = (i+1);
            deathRankingCache.add(dataCache.get(i).getName());
        }

        // 作成した時間を記錄する
        lastCachedTime = System.currentTimeMillis();

        // needRefreshフラグを下ろす
        BPUserData.needRefresh = false;
    }

    /**
     * @return キャッシュが有効期限を過ぎているかどうか
     */
    private boolean isCacheOld() {
        return BPUserData.needRefresh &&
                (lastCachedTime + CACHE_EXPIRE_LIMIT) < System.currentTimeMillis();
    }

    /**
     * データコンテンツを作成する
     * @param type
     * @param page_
     * @param size_
     * @param filter
     * @return
     */
    public byte[] getDataContents(String type, String page_, String size_, String filter) {

        // キャッシュが古いなら更新を行う
        if ( isCacheOld() ) {
            refreshData();
        }

        int page = 1;
        int size = 10;

        if ( page_ != null && page_.matches("[0-9]+") ) {
            page = Integer.parseInt(page_);
        }
        if ( size_ != null && size_.matches("[0-9]+") ) {
            size = Integer.parseInt(size_);
        }
        if ( type == null ) {
            type = "point";
        }

        int startIndex = (page - 1) * size;
        int endIndex = page * size;

        ArrayList<String> rank;
        if ( type.equals("rate") ) {
            rank = rateRankingCache;
        } else if ( type.equals("kills") ) {
            rank = killRankingCache;
        } else if ( type.equals("deaths") ) {
            rank = deathRankingCache;
        } else {
            rank = pointRankingCache;
        }

        // フィルタ処理
        if ( !filter.equals("") ) {
            ArrayList<String> filteredRank = new ArrayList<String>();
            for ( String name : rank ) {
                if ( name.contains(filter) ) {
                    filteredRank.add(name);
                }
            }
            rank = filteredRank;
        }

        // 書き出しデータの作成
        StringBuilder buffer = new StringBuilder();

        buffer.append(String.format(XML_PREFIX, rank.size()) + "\r\n");
        for ( int i=startIndex; i<endIndex; i++ ) {
            if ( rank.size() > i ) {
                String name = rank.get(i);
                BPUserData data = BPUserData.getDataFromName(name);
                int[] ranking = rankData.get(name);
                String line = String.format(XML_LINE, name,
                        ranking[0], ranking[1], ranking[2], ranking[3],
                        data.getPoint(), data.getRankClass(), data.getKDRate(),
                        data.getKills(), data.getDeaths());
                buffer.append(line + "\r\n");
            }
        }
        buffer.append(XML_SUFFIX + "\r\n");

        return buffer.toString().getBytes();
    }
}
