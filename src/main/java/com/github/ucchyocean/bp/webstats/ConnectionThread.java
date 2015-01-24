/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean.bp.webstats;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;

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
    private BPDataSorter sorter;

    /**
     * コンストラクタ
     * @param socket クライアントからの接続
     * @param cache ファイルキャッシュ
     * @param sorter BPUserDataのキャッシュ
     */
    public ConnectionThread(Socket socket, FileCache cache, BPDataSorter sorter) {
        this.socket = socket;
        this.cache = cache;
        this.sorter = sorter;
    }

    /**
     * 接続処理
     * @see java.lang.Thread#run()
     */
    public void run() {

        PrintStream outstream = null;
        BufferedReader reader = null;
        boolean isResponced = false;

        // クライアントIP
        String destIP = "";

        // クライアントポート
        int destport = -1;

        // リクエストファイル
        String requestFile = "";

        // UserAgent
        String userAgent = "";

        try {
            // クライアントIP
            destIP = socket.getInetAddress().toString();
            // クライアントポート
            destport = socket.getPort();

            outstream = new PrintStream(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // リクエストヘッダーの解析処理
            String inline = reader.readLine();
            HashMap<String, String> parameters = new HashMap<String, String>();
            if ( inline != null ) {
                requestFile = inline.split(" ")[1].trim();
                if ( requestFile.contains("?") ) {
                    int index = requestFile.indexOf("?");
                    String[] temp = requestFile.substring(index+1).split("&");
                    requestFile = requestFile.substring(0, index);
                    for ( String t : temp ) {
                        index = t.indexOf("=");
                        if ( index == -1 ) {
                            continue;
                        }
                        String key = t.substring(0, index);
                        String value = t.substring(index+1);
                        parameters.put(key, value);
                    }
                }
            } else {
                requestFile = "index.html";
            }
            if ( requestFile.endsWith("/") ) {
                requestFile += "index.html";
            }

            while (reader.ready() && inline != null) {
                inline = reader.readLine();
                if ( inline != null && inline.startsWith("User-Agent:") ) {
                    userAgent = inline.substring("User-Agent:".length()).trim();
                }
            }

            File file = new File(
                    BattlePoints.getInstance().getWebstatsContentFolder(),
                    requestFile);

            // 顔画像のリクエストなら、更新をまず行う
            if ( isFaceFile(requestFile) ) {
                refreshFaceFile(requestFile, file);
            }

            // データファイルなら、そのままレスポンス
            if ( requestFile.equals("/sort_data") ) {
                String type = parameters.get("type");
                String page = parameters.get("page");
                String size = parameters.get("size");
                String filter = parameters.get("filter");
                byte[] content = sorter.getDataContents(type, page, size, filter);
                int len = content.length;
                isResponced = true;
                outstream.println("HTTP/1.0 200 OK");
                outstream.println("MIME-version: 1.0");
                outstream.println("Content-Type: text/xml");
                outstream.println("Content-Length: " + len);
                outstream.println("");
                outstream.write(content, 0, len);
                WebServerLogger.write(destIP + "," + destport + ",200,"
                        + requestFile + "," + userAgent);
                return;
            }

            // レスポンス処理
            if ( !file.exists() ) {
                isResponced = true;
                outstream.println("HTTP/1.0 404 Not Found");
                outstream.println("");
                WebServerLogger.write(destIP + "," + destport + ",404,"
                        + requestFile + "," + userAgent);
            } else {
                int len = (int) file.length();
                isResponced = true;
                outstream.println("HTTP/1.0 200 OK");
                outstream.println("MIME-version: 1.0");
                outstream.println("Content-Type: " + getContentType(file.getName()));
                outstream.println("Content-Length: " + len);
                outstream.println("");

                // ファイル転送
                byte[] content = cache.readFile(requestFile, file);
                outstream.write(content, 0, len);

                WebServerLogger.write(destIP + "," + destport + ",200,"
                        + requestFile + "," + userAgent);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if ( outstream != null && !isResponced ) {
                outstream.println("HTTP/1.0 500 Internal Server Error");
                outstream.println("");
            }
            WebServerLogger.write(destIP + "," + destport + ",500,"
                    + requestFile + "," + userAgent);
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

    private static String getContentType(String name) {
        if ( name.endsWith(".txt") ) {
            return "text/plain";
        } else if ( name.endsWith(".htm") || name.endsWith(".html") ) {
            return "text/html";
        } else if ( name.endsWith(".xml") ) {
            return "text/xml";
        } else if ( name.endsWith(".js") ) {
            return "text/javascript";
        } else if ( name.endsWith(".vbs") ) {
            return "text/vbscript";
        } else if ( name.endsWith(".css") ) {
            return "text/css";
        } else if ( name.endsWith(".gif") ) {
            return "image/gif";
        } else if ( name.endsWith(".jpg") || name.endsWith(".jpeg") ) {
            return "image/jpeg";
        } else if ( name.endsWith(".png") ) {
            return "image/png";
        } else if ( name.endsWith(".cgi") ) {
            return "application/x-httpd-cgi";
        } else if ( name.endsWith(".pdf") ) {
            return "application/pdf";
        }
        return "text/plain";
    }
}
