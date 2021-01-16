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

package me.lucko.luckperms.sponge;

import com.google.inject.Inject;

import me.lucko.luckperms.common.dependencies.classloader.PluginClassLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.util.MoreFiles;
import me.lucko.luckperms.sponge.util.SpongeClassLoader;

import net.luckperms.api.platform.Platform;

import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform.Component;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;
import org.spongepowered.plugin.metadata.PluginMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Sponge.
 */
@Plugin("luckperms")
public class LPSpongeBootstrap implements LuckPermsBootstrap {

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final SpongeSchedulerAdapter schedulerAdapter;

    /**
     * The plugin classloader
     */
    private final PluginClassLoader classLoader;

    /**
     * The plugin instance
     */
    private final LPSpongePlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    /**
     * Reference to the central {@link Game} instance in the API
     */
    private final Game game;

    /**
     * Injected plugin container for the plugin
     */
    private final PluginContainer pluginContainer;

    /**
     * Injected configuration directory for the plugin
     */
    private final Path configDirectory;

    @Inject
    public LPSpongeBootstrap(Logger logger, Game game, PluginContainer pluginContainer, @ConfigDir(sharedRoot = false) Path configDirectory) {
        this.logger = new Log4jPluginLogger(logger);
        this.game = game;
        this.pluginContainer = pluginContainer;
        this.configDirectory = configDirectory;

        this.schedulerAdapter = new SpongeSchedulerAdapter(this.game, this.pluginContainer);
        this.classLoader = new SpongeClassLoader(this);
        this.plugin = new LPSpongePlugin(this);
    }

    // provide adapters

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SpongeSchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public PluginClassLoader getPluginClassLoader() {
        return this.classLoader;
    }

    // lifecycle
    @Listener(order = Order.FIRST)
    public void onEnable(ConstructPluginEvent event) {
        this.startTime = Instant.now();
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Listener
    public void onDisable(StoppingEngineEvent<Server> event) {
        this.plugin.disable();
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    // getters for the injected sponge instances

    public Game getGame() {
        return this.game;
    }

    public Optional<Server> getServer() {
        return this.game.isServerAvailable() ? Optional.of(this.game.getServer()) : Optional.empty();
    }

    public PluginContainer getPluginContainer() {
        return this.pluginContainer;
    }

    public void registerListeners(Object obj) {
        this.game.getEventManager().registerListeners(this.pluginContainer, obj);
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return this.pluginContainer.getMetadata().getVersion();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.SPONGE;
    }

    @Override
    public String getServerBrand() {
        PluginMetadata brandMetadata = this.game.getPlatform().getContainer(Component.IMPLEMENTATION).getMetadata();
        return brandMetadata.getName().orElseGet(brandMetadata::getId);
    }

    @Override
    public String getServerVersion() {
        PluginMetadata api = this.game.getPlatform().getContainer(Component.API).getMetadata();
        PluginMetadata impl = this.game.getPlatform().getContainer(Component.IMPLEMENTATION).getMetadata();
        return api.getName() + ": " + api.getVersion() + " - " + impl.getName() + ": " + impl.getVersion();
    }
    
    @Override
    public Path getDataDirectory() {
        Path dataDirectory = this.game.getGameDirectory().toAbsolutePath().resolve("luckperms");
        try {
            MoreFiles.createDirectoriesIfNotExists(dataDirectory);
        } catch (IOException e) {
            this.logger.warn("Unable to create LuckPerms directory", e);
        }
        return dataDirectory;
    }

    @Override
    public Path getConfigDirectory() {
        return this.configDirectory.toAbsolutePath();
    }

    @Override
    public InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public Optional<ServerPlayer> getPlayer(UUID uniqueId) {
        return getServer().flatMap(s -> s.getPlayer(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return getServer().flatMap(server -> server.getGameProfileManager().getBasicProfile(username)
                .thenApply(p -> Optional.of(p.getUniqueId()))
                .exceptionally(x -> Optional.empty())
                .join()
        );
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return getServer().flatMap(server -> server.getGameProfileManager().getBasicProfile(uniqueId)
                .thenApply(GameProfile::getName)
                .exceptionally(x -> Optional.empty())
                .join()
        );
    }

    @Override
    public int getPlayerCount() {
        return getServer().map(server -> server.getOnlinePlayers().size()).orElse(0);
    }

    @Override
    public Collection<String> getPlayerList() {
        return getServer().map(server -> {
            Collection<ServerPlayer> players = server.getOnlinePlayers();
            List<String> list = new ArrayList<>(players.size());
            for (Player player : players) {
                list.add(player.getName());
            }
            return list;
        }).orElse(Collections.emptyList());
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return getServer().map(server -> {
            Collection<ServerPlayer> players = server.getOnlinePlayers();
            List<UUID> list = new ArrayList<>(players.size());
            for (Player player : players) {
                list.add(player.getUniqueId());
            }
            return list;
        }).orElse(Collections.emptyList());
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        return getServer().map(server -> server.getPlayer(uniqueId).isPresent()).orElse(false);
    }
    
}
