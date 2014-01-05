/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean.bp.webstat;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * ファイルキャッシュクラス
 * @author ucchy
 */
public class FileCache {
    
    // キャッシュの有効期限（1時間）
    private static final long EXPIRE_LIMIT = 1 * 60 * 60 * 1000;
    
    // ファイルの更新日時
    private HashMap<String, Long> fileModifiedDate;
    // キャッシュした日時
    private HashMap<String, Long> fileCachedDate;
    // ファイルのコンテンツ
    private HashMap<String, byte[]> fileContent;
    
    /**
     * コンストラクタ
     */
    protected FileCache() {
        fileModifiedDate = new HashMap<String, Long>();
        fileCachedDate = new HashMap<String, Long>();
        fileContent = new HashMap<String, byte[]>();
    }
    
    /**
     * ファイルのコンテンツを取得する
     * @param name 名前
     * @param file ファイル
     * @return ファイルのコンテンツ
     * @throws IOException その他のIOエラー
     */
    protected byte[] readFile(String name, File file) throws IOException {
        
        long date = file.lastModified();
        
        if ( isCached(name, date) && !isExpired(name) ) {
            return fileContent.get(name);
        }
        
        byte[] content = read(file);
        fileModifiedDate.put(name, date);
        fileCachedDate.put(name, System.currentTimeMillis());
        fileContent.put(name, content);
        return content;
    }
    
    /**
     * ファイルの読み込みをする
     * @param file ファイル
     * @return ファイルのコンテンツ
     * @throws IOException その他のIOエラー
     */
    private byte[] read(File file) throws IOException {
        
        if ( file.length() > Integer.MAX_VALUE ) {
            throw new IOException("file " + file.getAbsolutePath() + " is too large!");
        }
        
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(file));
            int len = (int)file.length();
            byte[] buf = new byte[len];
            in.readFully(buf);
            return buf;
        } catch (IOException e) {
            throw e;
        } finally {
            if ( in != null ) {
                try {
                    in.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }
    }
    
    /**
     * 指定されたファイルがキャッシュされているかどうか
     * @param name 名前
     * @param date 更新日時
     * @return キャッシュされているかどうか
     */
    private boolean isCached(String name, long date) {
        if ( !fileModifiedDate.containsKey(name) ) {
            return false;
        }
        return fileModifiedDate.get(name).equals(date);
    }
    
    /**
     * 指定されたファイルがキャッシュの有効期限を過ぎているかどうか
     * @param name 名前
     * @return 有効期限を過ぎているかどうか
     */
    private boolean isExpired(String name) {
        if ( !fileCachedDate.containsKey(name) ) {
            return false;
        }
        long limit = fileCachedDate.get(name) + EXPIRE_LIMIT;
        return (limit < System.currentTimeMillis());
    }
}
