/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean.bp.webstat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.github.ucchyocean.bp.BattlePoints;

/**
 * WebStatサーバーのログファイル書き込みクラス
 * @author ucchy
 */
public class WebServerLogger {

    private static final String FORMAT_FOR_FILENAME = "'accesslog-'yyyyMMdd'.txt'";
    private static final String FORMAT_FOR_LINE = "yyyy-MM-dd','HH:mm:ss";
    
    private static WebServerLogger instance;
    
    private SimpleDateFormat formatForFileName;
    private SimpleDateFormat formatForLine;
    
    private WebServerLogger() {
        formatForFileName = new SimpleDateFormat(FORMAT_FOR_FILENAME);
        formatForLine = new SimpleDateFormat(FORMAT_FOR_LINE);
    }
    
    private void writeLog(String message) {

        Date date = new Date();
        String now = formatForLine.format(date);
        File file = new File(
                BattlePoints.getInstance().getWebstatLogFolder(), 
                formatForFileName.format(date));

        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(
                    new FileOutputStream(file, true)));
            writer.write(now + "," + message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( writer != null ) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }
    }
    
    /**
     * ログをログファイルに書き込む。
     * @param log ログ
     */
    public static void write(String log) {
        if ( instance == null ) {
            instance = new WebServerLogger();
        }
        instance.writeLog(log);
    }
}
