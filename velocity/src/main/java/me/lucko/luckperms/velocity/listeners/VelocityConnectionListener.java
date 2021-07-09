/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.velocity.listeners;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.proxy.Player;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.velocity.LPVelocityPlugin;
import me.lucko.luckperms.velocity.service.PlayerPermissionProvider;
import me.lucko.luckperms.velocity.util.AdventureCompat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VelocityConnectionListener extends AbstractConnectionListener {
    private final LPVelocityPlugin plugin;

    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    public VelocityConnectionListener(LPVelocityPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerPermissionsSetup(PermissionsSetupEvent e, Continuation continuation) {
        /* Called when the player first attempts a connection with the server.
           The PermissionsSetupEvent is called for players just before the Login event

           We delay the login here, as we want to cache UUID data before the player is connected to a backend bukkit server.
           This means that a player will have the same UUID across the network, even if parts of the network are running in
           Offline mode. */

        if (!(e.getSubject() instanceof Player)) {
            return;
        }

        final Player p = (Player) e.getSubject();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + p.getUniqueId() + " - " + p.getUsername());
        }

        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            /* Actually process the login for the connection.
               We do this here to delay the login until the data is ready.
               If the login gets cancelled later on, then this will be cleaned up.

               This includes:
               - loading uuid data
               - loading permissions
               - creating a user instance in the UserManager for this connection.
               - setting up cached data. */
            try {
                User user = loadUser(p.getUniqueId(), p.getUsername());
                recordConnection(p.getUniqueId());
                e.setProvider(new PlayerPermissionProvider(p, user, this.plugin.getContextManager().getCacheFor(p)));
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(p.getUniqueId(), p.getUsername(), user);
            } catch (Exception ex) {
                this.plugin.getLogger().severe("Exception occurred whilst loading data for " + p.getUniqueId() + " - " + p.getUsername(), ex);

                // there was some error loading
                if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                    // cancel the login attempt
                    this.deniedLogin.add(p.getUniqueId());
                }
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(p.getUniqueId(), p.getUsername(), null);
            } finally {
                continuation.resume();
            }
        });
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLogin(LoginEvent e) {
        final Player player = e.getPlayer();
        if (this.deniedLogin.remove(player.getUniqueId())) {
            e.setResult(AdventureCompat.deniedResult(TranslationManager.render(Message.LOADING_DATABASE_ERROR.build(), player.getPlayerSettings().getLocale())));
        }
    }

    @Subscribe
    public void onPlayerPostLogin(LoginEvent e) {
        final Player player = e.getPlayer();
        final User user = this.plugin.getUserManager().getIfLoaded(e.getPlayer().getUniqueId());

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + player.getUniqueId() + " - " + player.getUsername());
        }

        if (!e.getResult().isAllowed()) {
            return;
        }

        if (user == null) {
            if (!getUniqueConnections().contains(player.getUniqueId())) {
                this.plugin.getLogger().warn("User " + player.getUniqueId() + " - " + player.getUsername() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session.");
            } else {
                this.plugin.getLogger().warn("User " + player.getUniqueId() + " - " + player.getUsername() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session.");
            }

            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                // disconnect the user
                e.setResult(AdventureCompat.deniedResult(TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.getPlayerSettings().getLocale())));
            } else {
                // just send a message
                this.plugin.getBootstrap().getScheduler().asyncLater(() -> {
                    if (!player.isActive()) {
                        return;
                    }

                    Message.LOADING_STATE_ERROR.send(this.plugin.getSenderFactory().wrap(player));
                }, 1, TimeUnit.SECONDS);
            }
        }
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    @Subscribe(order = PostOrder.LAST)
    public void onPlayerQuit(DisconnectEvent e) {
        handleDisconnect(e.getPlayer().getUniqueId());
    }

}
