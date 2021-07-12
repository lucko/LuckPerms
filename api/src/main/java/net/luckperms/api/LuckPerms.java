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

package net.luckperms.api;

import net.luckperms.api.actionlog.ActionLogger;
import net.luckperms.api.bulkupdate.BulkOperationFactory;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.metastacking.MetaStackElement;
import net.luckperms.api.metastacking.MetaStackFactory;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.NodeBuilderRegistry;
import net.luckperms.api.node.matcher.NodeMatcherFactory;
import net.luckperms.api.platform.Platform;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.platform.PluginMetadata;
import net.luckperms.api.query.QueryOptionsRegistry;
import net.luckperms.api.track.Track;
import net.luckperms.api.track.TrackManager;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The LuckPerms API.
 *
 * <p>The API allows other plugins on the server to read and modify LuckPerms
 * data, change behaviour of the plugin, listen to certain events, and integrate
 * LuckPerms into other plugins and systems.</p>
 *
 * <p>This interface represents the base of the API package. All functions are
 * accessed via this interface.</p>
 *
 * <p>To start using the API, you need to obtain an instance of this interface.
 * These are registered by the LuckPerms plugin to the platforms Services
 * Manager. This is the preferred method for obtaining an instance.</p>
 *
 * <p>For ease of use, and for platforms without a Service Manager, an instance
 * can also be obtained from the static singleton accessor in
 * {@link LuckPermsProvider}.</p>
 */
public interface LuckPerms {

    /**
     * Gets the name of this server.
     *
     * <p>This is defined in the LuckPerms configuration file, and is used for
     * server specific permission handling.</p>
     *
     * <p>The default server name is "global".</p>
     *
     * @return the server name
     */
    @NonNull String getServerName();

    /**
     * Gets the {@link UserManager}, responsible for managing
     * {@link User} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link User} by uuid
     * or name, or query all loaded users.</p>
     *
     * @return the user manager
     */
    @NonNull UserManager getUserManager();

    /**
     * Gets the {@link GroupManager}, responsible for managing
     * {@link Group} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link Group} by
     * name, or query all loaded groups.</p>
     *
     * @return the group manager
     */
    @NonNull GroupManager getGroupManager();

    /**
     * Gets the {@link TrackManager}, responsible for managing
     * {@link Track} instances.
     *
     * <p>This manager can be used to retrieve instances of {@link Track} by
     * name, or query all loaded tracks.</p>
     *
     * @return the track manager
     */
    @NonNull TrackManager getTrackManager();

    /**
     * Gets the {@link PlayerAdapter} instance, a utility class for adapting platform Player
     * instances to {@link User}s.
     *
     * <p>The {@code playerClass} parameter must be equal to the class or interface used by the
     * server platform to represent players.</p>
     *
     * <p>Specifically:</p>
     *
     * <p></p>
     * <ul>
     * <li>{@code org.bukkit.entity.Player}</li>
     * <li>{@code net.md_5.bungee.api.connection.ProxiedPlayer}</li>
     * <li>{@code org.spongepowered.api/entity.living.player.Player}</li>
     * <li>{@code net.minecraft.server.network.ServerPlayerEntity} (Fabric)</li>
     * <li>{@code cn.nukkit.Player}</li>
     * <li>{@code com.velocitypowered.api.proxy.Player}</li>
     * </ul>
     *
     * @param playerClass the class used by the platform to represent players
     * @param <T> the player class type
     * @return the player adapter
     * @throws IllegalArgumentException if the player class is not correct
     * @since 5.1
     */
    <T> @NonNull PlayerAdapter<T> getPlayerAdapter(@NonNull Class<T> playerClass);

    /**
     * Gets the {@link Platform}, which represents the server platform the
     * plugin is running on.
     *
     * @return the platform
     */
    @NonNull Platform getPlatform();

    /**
     * Gets the {@link PluginMetadata}, responsible for providing metadata about
     * the LuckPerms plugin currently running.
     *
     * @return the plugin metadata
     */
    @NonNull PluginMetadata getPluginMetadata();

    /**
     * Gets the {@link EventBus}, used for subscribing to internal LuckPerms
     * events.
     *
     * @return the event bus
     */
    @NonNull EventBus getEventBus();

    /**
     * Gets the {@link MessagingService}, used to dispatch updates throughout a
     * network of servers running the plugin.
     *
     * <p>Not all instances of LuckPerms will have a messaging service setup and
     * configured.</p>
     *
     * @return the messaging service instance, if present.
     */
    @NonNull Optional<MessagingService> getMessagingService();

    /**
     * Gets the {@link ActionLogger}, responsible for saving and broadcasting
     * defined actions occurring on the platform.
     *
     * @return the action logger
     */
    @NonNull ActionLogger getActionLogger();

    /**
     * Gets the {@link ContextManager}, responsible for managing
     * {@link ContextCalculator}s, and calculating applicable contexts.
     *
     * @return the context manager
     */
    @NonNull ContextManager getContextManager();

    /**
     * Gets the {@link MetaStackFactory}.
     *
     * <p>The metastack factory provides methods for retrieving
     * {@link MetaStackElement}s and constructing
     * {@link MetaStackDefinition}s.</p>
     *
     * @return the meta stack factory
     */
    @NonNull MetaStackFactory getMetaStackFactory();

    /**
     * Schedules the execution of an update task, and returns an encapsulation
     * of the task as a {@link CompletableFuture}.
     *
     * <p>The exact actions performed in an update task remains an
     * implementation detail of the plugin, however, as a minimum, it is
     * expected to perform a full reload of user, group and track data, and
     * ensure that any changes are fully applied and propagated.</p>
     *
     * @return a future
     */
    @NonNull CompletableFuture<Void> runUpdateTask();

    /**
     * Registers a {@link MessengerProvider} for use by the platform.
     *
     * <p>Note that the mere action of registering a provider doesn't
     * necessarily mean that it will be used.</p>
     *
     * @param messengerProvider the messenger provider.
     */
    void registerMessengerProvider(@NonNull MessengerProvider messengerProvider);

    /**
     * Gets the {@link NodeBuilderRegistry}.
     *
     * @return the node builder registry
     */
    @Internal
    @NonNull NodeBuilderRegistry getNodeBuilderRegistry();

    /**
     * Gets the {@link QueryOptionsRegistry}.
     *
     * @return the query options registry
     * @since 5.1
     */
    @Internal
    @NonNull QueryOptionsRegistry getQueryOptionsRegistry();

    /**
     * Gets the {@link NodeMatcherFactory}.
     *
     * @return the node matcher factory
     * @since 5.1
     */
    @Internal
    @NonNull NodeMatcherFactory getNodeMatcherFactory();

    /**
     * Gets the {@link BulkOperationFactory}.
     *
     * @return the bulk update factory
     * @since 5.4
     */
    @Internal
    @NonNull BulkOperationFactory getBulkUpdateFactory();
}
