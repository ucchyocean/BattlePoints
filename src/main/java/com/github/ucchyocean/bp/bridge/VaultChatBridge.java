/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp.bridge;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault-Chat連携クラス
 * @author ucchy
 */
public class VaultChatBridge {

    /** vault-chatクラス */
    private Chat chatPlugin;

    /** コンストラクタは使用不可 */
    private VaultChatBridge() {
    }

    /**
     * vault-chatをロードする
     * @return ロードされたVaultChatBridge。Vaultがロードされていないか、
     * Vaultと連携可能なチャットプラグインがロードされていない場合は、
     * nullがかえされる。
     */
    public static VaultChatBridge load() {

        RegisteredServiceProvider<Chat> chatProvider =
                Bukkit.getServicesManager().getRegistration(Chat.class);
        if ( chatProvider != null ) {
            VaultChatBridge bridge = new VaultChatBridge();
            bridge.chatPlugin = chatProvider.getProvider();
            return bridge;
        }

        return null;
    }

    /**
     * プレイヤーのsuffixを取得します。
     * @param player プレイヤー
     * @return プレイヤーのsuffix
     */
    public String getPlayerSuffix(Player player) {
        return chatPlugin.getPlayerSuffix(player);
    }

    /**
     * プレイヤーのsuffixを取得します。
     * @param player プレイヤー
     * @return プレイヤーのsuffix
     */
    public void setPlayerSuffix(Player player, String suffix) {
        chatPlugin.setPlayerSuffix(player, suffix);
    }
}
