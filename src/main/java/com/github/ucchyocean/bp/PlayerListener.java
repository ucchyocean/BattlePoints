/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

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
            return;
        }

        // ポイント計算
        BattlePoints.instance.changePoints(winner, loser);
    }

    /**
     * Playerがチャットで発言したときに呼び出されるイベント
     * @param event
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        if ( BPConfig.displayPointOnChat && !BPConfig.useVault ) {
            Player player = event.getPlayer();
            BPUserData data = BPUserData.getData(player.getName());
            int point = data.point;
            String rank = BPConfig.getRankFromPoint(point);
            String symbol = BPConfig.rankSymbols.get(rank);
            ChatColor color = BPConfig.rankColors.get(rank);
            String format = String.format(
                    "<%s&f>[%s%s%d&f]&r %s",
                    "%1$s", color.toString(), symbol, point, "%2$s");
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
            BPUserData data = BPUserData.getData(player.getName());
            int point = data.point;
            String rank = BPConfig.getRankFromPoint(point);
            String symbol = BPConfig.rankSymbols.get(rank);
            ChatColor color = BPConfig.rankColors.get(rank);
            String suffix = String.format(
                    "&f[%s%s%d&f]", color.toString(), symbol, point);
            BattlePoints.vaultChat.setPlayerSuffix(player, suffix);
        }
    }
}
