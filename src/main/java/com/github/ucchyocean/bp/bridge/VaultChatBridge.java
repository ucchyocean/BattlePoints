/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.bp.bridge;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
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
     * プレイヤーのprefixを取得します。
     * @param world ワールド
     * @param player プレイヤー
     * @return プレイヤーのprefix
     */
    public String getPlayerPrefix(String world, String player) {
        return chatPlugin.getPlayerPrefix(world, player);
    }

    /**
     * プレイヤーのprefixを設定します。
     * @param world ワールド
     * @param player プレイヤー
     * @param prefix プレイヤーのprefix
     */
    public void setPlayerPrefix(String world, String player, String prefix) {
        chatPlugin.setPlayerPrefix(world, player, prefix);
    }

    /**
     * プレイヤーのsuffixを設定します。
     * @param world ワールド
     * @param player プレイヤー
     * @param suffix プレイヤーのsuffix
     */
    public void setPlayerSuffix(String world, String player, String suffix) {
        chatPlugin.setPlayerSuffix(world, player, suffix);
    }
}
