/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * BattlePointのプレイヤー関連イベントの監視クラス
 * @author ucchy
 */
public class PlayerListener implements Listener {

    private BattlePoints plugin;

    public PlayerListener(BattlePoints plugin) {
        this.plugin = plugin;
    }

    /**
     * Player が死亡したときに発生するイベント
     * @param event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        // 設定がオフなら何もしない
        if ( !BattlePoints.instance.getBPConfig().isListenPlayerDeathEvent() ) {
            return;
        }

        Player loser = event.getEntity();

        // killer を取得。
        Player winner = loser.getKiller();

        // killer が取得できなかったら、ここで諦める
        if ( winner == null ) {
            return;
        }

        // ポイント計算
        BattlePoints.instance.changePoints(winner, loser);

        // 死亡メッセージを消す
        event.setDeathMessage("");
    }

    /**
     * Playerがチャットで発言したときに呼び出されるイベント
     * @param event
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        BPConfig config = plugin.getBPConfig();

        if ( config.isDisplayPointOnChat() && !config.isUseVault() ) {

            Player player = event.getPlayer();

            if ( !config.getDisplayPointOnChatWorlds().contains(
                    player.getWorld().getName()) ) {
                return;
            }

            BPUserData data = BPUserData.getData(player);
            int point = data.getPoint();
            String rank = config.getRankFromPoint(point);
            String symbol = config.getSymbolFromRank(rank);
            String color = config.getColorFromRank(rank);

            OfflinePlayer champ = BattlePoints.instance.getChampion();
            String champPre = "";
            if ( player.getName().equals(champ.getName()) ) {
                champPre = BattlePoints.getInstance().getBPConfig().getChampionPrefix();
            }

            String format = String.format(
                    "%s<%s&f>[%s%s%d&f]&r %s",
                    champPre, "%1$s", color, symbol, point, "%2$s");
            event.setFormat(Utility.replaceColorCode(format));
        }
    }

    /**
     * Playerがサーバーに参加したときに呼び出されるイベント
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        BPConfig config = plugin.getBPConfig();

        // Vault連携の場合は、ここでSuffixを設定する
        if ( config.isDisplayPointOnChat() && config.isUseVault()
                && BattlePoints.vcbridge != null ) {
            Player player = event.getPlayer();
            BPUserData data = BPUserData.getData(player);
            int point = data.getPoint();
            String rank = config.getRankFromPoint(point);
            String symbol = config.getSymbolFromRank(rank);
            String color = config.getColorFromRank(rank);
            String suffix = String.format(
                    "&f[%s%s%d&f]", color, symbol, point);

            for ( String world : config.getDisplayPointOnChatWorlds() ) {
                BattlePoints.vcbridge.setPlayerSuffix(world, player, suffix);
            }
        }
    }
}
