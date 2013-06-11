/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * BattlePointのプレイヤー関連イベントの監視クラス
 * @author ucchy
 */
public class PlayerListener implements Listener {

    private static String prefix = Messages.get("prefix");

    /**
     * Player が死亡したときに発生するイベント
     * @param event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player loser = event.getEntity();

        // killer を取得。
        // 直接攻撃で倒された場合は、killerをそのまま使う
        // 間接攻撃で倒された場合は、shooterを取得して使う
        Player winner = loser.getKiller();
        if ( (winner != null) && (winner instanceof Projectile) ) {
            EntityDamageEvent cause = loser.getLastDamageCause();
            LivingEntity shooter = ((Projectile) winner).getShooter();
            if ( cause instanceof EntityDamageByEntityEvent && shooter instanceof Player ) {
                winner = (Player)shooter;
            }
        }

        // killer が取得できなかったら、ここで諦める
        if ( winner == null ) {
            //BattlePoints.sendBroadcast(String.format(
            //        ChatColor.LIGHT_PURPLE + "%s は自殺した！", loser.getName()));
            return;
        }

        // サーバーメッセージでアナウンス
        //BattlePoints.sendBroadcast(String.format(
        //        ChatColor.LIGHT_PURPLE + "%s は %s によって倒された！",
        //        loser.getName(), winner.getName()));

        // ポイント計算
        int lastWinnerPoint = BattlePoints.data.getPoint(winner.getName());
        int lastLoserPoint = BattlePoints.data.getPoint(loser.getName());
        int rate = BPConfig.getEloRating(lastWinnerPoint, lastLoserPoint);
        int winnerRate = rate + BPConfig.winOffsetPoint;
        int loserRate = rate;
        int winnerPoint = lastWinnerPoint + winnerRate;
        int loserPoint = lastLoserPoint - loserRate;

        // 勝者、敗者が上限、下限に達したら、補正を行う
        if ( winnerPoint > 9999 ) {
            winnerPoint = 9999;
        }
        if ( loserPoint < 0 ) {
            loserPoint = 0;
        }

        BattlePoints.data.setPoint(winner.getName(), winnerPoint);
        BattlePoints.data.setPoint(loser.getName(), loserPoint);
        String wRank = BPConfig.getRankFromPoint(winnerPoint);
        String lRank = BPConfig.getRankFromPoint(loserPoint);
        String wColor = BPConfig.rankColors.get(wRank).toString();
        String lColor = BPConfig.rankColors.get(lRank).toString();

        broadcastMessage("battleResult",
                wColor, winner.getName(),  winnerPoint, winnerRate,
                lColor, loser.getName(), loserPoint, loserRate);

        // 称号が変わったかどうかを確認する
        if ( !wRank.equals(BPConfig.getRankFromPoint(lastWinnerPoint)) ) {
            broadcastMessage("rankup", winner.getName(), wRank);
        }
        if ( !lRank.equals(BPConfig.getRankFromPoint(lastLoserPoint)) ) {
            broadcastMessage("rankdown", loser.getName(), lRank);
        }

        // Vault連携の場合は、ここでSuffixを設定する
        if ( BPConfig.displayPointOnChat && BPConfig.useVault && BattlePoints.vaultChat != null ) {
            String wSymbol = BPConfig.rankSymbols.get(wRank);
            String wSuf = String.format("&f[%s%s%d&f]", wColor, wSymbol, winnerPoint);
            BattlePoints.vaultChat.setPlayerSuffix(winner, wSuf);
            String lSymbol = BPConfig.rankSymbols.get(lRank);
            String lSuf = String.format("&f[%s%s%d&f]", lColor, lSymbol, loserPoint);
            BattlePoints.vaultChat.setPlayerSuffix(loser, lSuf);
        }
    }

    /**
     * Playerがチャットで発言したときに呼び出されるイベント
     * @param event
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        if ( BPConfig.displayPointOnChat && !BPConfig.useVault ) {
            Player player = event.getPlayer();
            int point = BattlePoints.data.getPoint(player.getName());
            String rank = BPConfig.getRankFromPoint(point);
            String symbol = BPConfig.rankSymbols.get(rank);
            ChatColor color = BPConfig.rankColors.get(rank);
            String format = String.format("<%s&f>[%s%s%d&f]&r %s", "%1$s", color.toString(), symbol, point, "%2$s");
            event.setFormat(Utility.replaceColorCode(format));
        }
    }

    /**
     * Playerがサーバーに参加したときに呼び出されるイベント
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        // Vault連携の場合は、ここでSuffixを設定する
        if ( BPConfig.displayPointOnChat && BPConfig.useVault && BattlePoints.vaultChat != null ) {
            Player player = event.getPlayer();
            int point = BattlePoints.data.getPoint(player.getName());
            String rank = BPConfig.getRankFromPoint(point);
            String symbol = BPConfig.rankSymbols.get(rank);
            ChatColor color = BPConfig.rankColors.get(rank);
            String suffix = String.format("&f[%s%s%d&f]", color.toString(), symbol, point);
            BattlePoints.vaultChat.setPlayerSuffix(player, suffix);
        }
    }

    /**
     * メッセージリソースを取得し、ブロードキャストする
     * @param key メッセージキー
     * @param args メッセージの引数
     * @return メッセージ
     */
    private void broadcastMessage(String key, Object... args) {

        String msg = Messages.get(key, args);
        if ( msg.equals("") ) {
            return;
        }
        Bukkit.broadcastMessage(Utility.replaceColorCode(prefix + msg));
    }
}
