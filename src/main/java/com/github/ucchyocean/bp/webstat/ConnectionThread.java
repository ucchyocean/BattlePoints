/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean.bp.webstat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import com.github.ucchyocean.bp.BattlePoints;

/**
 * Webコネクションのスレッドクラス
 * @author ucchy
 */
public class ConnectionThread extends Thread {
    
    // 顔画像キャッシュの有効期限（1日）
    private static final long FACE_EXPIRE_LIMIT = 24 * 60 * 60 * 1000;

    private Socket socket;
    private FileCache cache;

    /**
     * コンストラクタ
     * @param socket クライアントからの接続
     * @param cache ファイルキャッシュ
     */
    public ConnectionThread(Socket socket, FileCache cache) {
        this.socket = socket;
        this.cache = cache;
    }

    /**
     * 接続処理
     * @see java.lang.Thread#run()
     */
    public void run() {
        
        PrintStream outstream = null;
        BufferedReader reader = null;
        
        try {
            // クライアントIP
            String destIP = socket.getInetAddress().toString();
            // クライアントポート
            int destport = socket.getPort();

            outstream = new PrintStream(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // リクエストヘッダーの解析処理
            String inline = reader.readLine();
            String responseFile;
            if ( inline != null ) {
                responseFile = inline.split(" ")[1].trim();
            } else {
                responseFile = "index.html";
            }
            if ( responseFile.endsWith("/") ) {
                responseFile += "index.html";
            }
//            if ( responseFile.startsWith("/") ) {
//                responseFile = responseFile.substring(1);
//            }

            String userAgent = "";
            while (reader.ready() && inline != null) {
                inline = reader.readLine();
                if ( inline != null && inline.startsWith("User-Agent:") ) {
                    userAgent = inline.substring("User-Agent:".length()).trim();
                }
            }

            File file = new File(
                    BattlePoints.getInstance().getWebstatContentFolder(), 
                    responseFile);
            
            // 顔画像のリクエストなら、更新をまず行う
            if ( isFaceFile(responseFile) ) {
                refreshFaceFile(responseFile, file);
            }
            
            // レスポンス処理
            if ( !file.exists() ) {
                outstream.println("HTTP/1.0 404 Not Found");
                outstream.println("");
                WebServerLogger.write(destIP + "," + destport + ",404," 
                        + responseFile + "," + userAgent);
            } else {
                int len = (int) file.length();
                outstream.println("HTTP/1.0 200 OK");
                outstream.println("MIME_version：1.0");
                outstream.println("Content_Type：text/htm1");
                outstream.println("Content_Length：" + len);
                outstream.println("");

                // ファイル転送
                byte[] content = cache.readFile(responseFile, file);
                outstream.write(content, 0, len);
                
                WebServerLogger.write(destIP + "," + destport + ",200," 
                        + responseFile + "," + userAgent);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( reader != null ) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( outstream != null ) {
                outstream.flush();
                outstream.close();
            }
        }
    }
    
    /**
     * 指定されたファイル名が、プレイヤーフェイスかどうか
     * @param name リクエストファイル名
     * @return プレイヤーフェイスかどうか
     */
    private boolean isFaceFile(String name) {
        return (name.startsWith("/faces/") && name.endsWith(".png"));
    }
    
    /**
     * プレイヤーフェイスファイルの更新を行う
     * @param name リクエストファイル名
     * @param file リクエストファイル
     */
    private void refreshFaceFile(String name, File file) {
        
        String playerName = name.substring("/faces/".length(), name.length() - ".png".length());
        
        if ( !file.exists() ) {
            // 顔画像が無いならダウンロード
            PlayerFaceDownloader.downloadSkin(playerName, file, true);
        } else {
            // 顔画像が古いなら再ダウンロード
            long modified = file.lastModified();
            if ( (modified + FACE_EXPIRE_LIMIT) < System.currentTimeMillis() ) {
                PlayerFaceDownloader.downloadSkin(playerName, file, false);
            }
        }
    }
}
