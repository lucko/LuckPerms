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

package me.lucko.luckperms.common.backup;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.utils.NodeJsonSerializer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Uuids;

import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles import operations
 */
public class Importer implements Runnable {

    private final LuckPermsPlugin plugin;
    private final Set<Sender> notify;
    private final JsonObject data;
    private final boolean merge;

    public Importer(LuckPermsPlugin plugin, Sender executor, JsonObject data, boolean merge) {
        this.plugin = plugin;

        if (executor.isConsole()) {
            this.notify = ImmutableSet.of(executor);
        } else {
            this.notify = ImmutableSet.of(executor, plugin.getConsoleSender());
        }
        this.data = data;
        this.merge = merge;
    }

    private static final class UserData {
        private final String username;
        private final String primaryGroup;
        private final Set<Node> nodes;

        UserData(String username, String primaryGroup, Set<Node> nodes) {
            this.username = username;
            this.primaryGroup = primaryGroup;
            this.nodes = nodes;
        }
    }

    private void processGroup(String groupName, Set<Node> nodes) {
        Group group = this.plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();
        if (this.merge) {
            group.mergeNodes(DataType.NORMAL, nodes, false);
        } else {
            group.setNodes(DataType.NORMAL, nodes, false);
        }
        this.plugin.getStorage().saveGroup(group);
    }

    private void processTrack(String trackName, List<String> groups) {
        Track track = this.plugin.getStorage().createAndLoadTrack(trackName, CreationCause.INTERNAL).join();
        track.setGroups(groups);
        this.plugin.getStorage().saveTrack(track).join();
    }

    private void processUser(UUID uuid, UserData userData) {
        User user = this.plugin.getStorage().loadUser(uuid, userData.username).join();
        if (userData.primaryGroup != null) {
            user.getPrimaryGroup().setStoredValue(userData.primaryGroup);
        }
        if (this.merge) {
            user.mergeNodes(DataType.NORMAL, userData.nodes, false);
        } else {
            user.setNodes(DataType.NORMAL, userData.nodes, false);
        }
        this.plugin.getStorage().saveUser(user).join();
        this.plugin.getUserManager().getHouseKeeper().cleanup(user.getUniqueId());
    }

    private Set<Map.Entry<String, JsonElement>> getDataSection(String id) {
        if (this.data.has(id)) {
            return this.data.get(id).getAsJsonObject().entrySet();
        } else {
            return Collections.emptySet();
        }
    }

    private void parseExportData(Map<String, Set<Node>> groups, Map<String, List<String>> tracks, Map<UUID, UserData> users) {
        for (Map.Entry<String, JsonElement> group : getDataSection("groups")) {
            groups.put(group.getKey(), NodeJsonSerializer.deserializeNodes(group.getValue().getAsJsonObject().get("nodes").getAsJsonArray()));
        }
        for (Map.Entry<String, JsonElement> track : getDataSection("tracks")) {
            JsonArray trackGroups = track.getValue().getAsJsonObject().get("groups").getAsJsonArray();
            List<String> trackGroupsList = new ArrayList<>();
            trackGroups.forEach(g -> trackGroupsList.add(g.getAsString()));
            tracks.put(track.getKey(), trackGroupsList);
        }
        for (Map.Entry<String, JsonElement> user : getDataSection("users")) {
            JsonObject jsonData = user.getValue().getAsJsonObject();

            UUID uuid = UUID.fromString(user.getKey());
            String username = null;
            String primaryGroup = null;
            Set<Node> nodes = NodeJsonSerializer.deserializeNodes(jsonData.get("nodes").getAsJsonArray());

            if (jsonData.has("username")) {
                username = jsonData.get("username").getAsString();
            }
            if (jsonData.has("primaryGroup")) {
                primaryGroup = jsonData.get("primaryGroup").getAsString();
            }

            users.put(uuid, new UserData(username, primaryGroup, nodes));
        }
    }

    private void parseWebEditorData(Map<String, Set<Node>> groups, Map<String, List<String>> tracks, Map<UUID, UserData> users) {
        JsonArray holdersArray = this.data.get("permissionHolders").getAsJsonArray();
        for (JsonElement holderElement : holdersArray) {
            JsonObject jsonData = holderElement.getAsJsonObject();

            HolderType type = HolderType.valueOf(jsonData.get("type").getAsString().toUpperCase());
            String id = jsonData.get("id").getAsString();

            if (type == HolderType.GROUP) {
                groups.put(id, NodeJsonSerializer.deserializeNodes(jsonData.get("nodes").getAsJsonArray()));
            } else {
                UUID uuid = UUID.fromString(id);
                String username = null;

                String displayName = jsonData.get("displayName").getAsString();
                if (!Uuids.PREDICATE.test(displayName)) {
                    username = displayName;
                }

                Set<Node> nodes = NodeJsonSerializer.deserializeNodes(jsonData.get("nodes").getAsJsonArray());
                users.put(uuid, new UserData(username, null, nodes));
            }
        }

        JsonArray tracksArray = this.data.get("tracks").getAsJsonArray();
        for (JsonElement trackElement : tracksArray) {
            JsonObject jsonData = trackElement.getAsJsonObject();

            String name = jsonData.get("id").getAsString();
            JsonArray trackGroups = jsonData.get("groups").getAsJsonArray();

            List<String> trackGroupsList = new ArrayList<>();
            trackGroups.forEach(g -> trackGroupsList.add(g.getAsString()));
            tracks.put(name, trackGroupsList);
        }
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        this.notify.forEach(Message.IMPORT_START::send);

        // start an update task in the background - we'll #join this later
        CompletableFuture<Void> updateTask = CompletableFuture.runAsync(() -> this.plugin.getSyncTaskBuffer().requestDirectly());

        this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "Reading data..."));

        Map<String, Set<Node>> groups = new HashMap<>();
        Map<String, List<String>> tracks = new HashMap<>();
        Map<UUID, UserData> users = new HashMap<>();

        if (this.data.has("knownPermissions")) {
            this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "The data appears to be from a web editor upload - attempting to recover from it"));
            parseWebEditorData(groups, tracks, users);
        } else {
            parseExportData(groups, tracks, users);
        }

        this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "Waiting for initial update task to complete..."));

        // join the update task future before scheduling command executions
        updateTask.join();

        this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "Setting up data processor..."));

        // create a threadpool for the processing
        ExecutorService executor = Executors.newFixedThreadPool(16, new ThreadFactoryBuilder().setNameFormat("luckperms-importer-%d").build());

        // A set of futures, which are really just the processes we need to wait for.
        Set<CompletableFuture<Void>> futures = new HashSet<>();

        int total = 0;
        AtomicInteger processedCount = new AtomicInteger(0);

        for (Map.Entry<String, Set<Node>> group : groups.entrySet()) {
            futures.add(CompletableFuture.completedFuture(group).thenAcceptAsync(ent -> {
                processGroup(ent.getKey(), ent.getValue());
                processedCount.incrementAndGet();
            }, executor));
            total++;
        }
        for (Map.Entry<String, List<String>> track : tracks.entrySet()) {
            futures.add(CompletableFuture.completedFuture(track).thenAcceptAsync(ent -> {
                processTrack(ent.getKey(), ent.getValue());
                processedCount.incrementAndGet();
            }, executor));
            total++;
        }
        for (Map.Entry<UUID, UserData> user : users.entrySet()) {
            futures.add(CompletableFuture.completedFuture(user).thenAcceptAsync(ent -> {
                processUser(ent.getKey(), ent.getValue());
                processedCount.incrementAndGet();
            }, executor));
            total++;
        }

        // all of the threads have been scheduled now and are running. we just need to wait for them all to complete
        CompletableFuture<Void> overallFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        this.notify.forEach(s -> Message.IMPORT_INFO.send(s, "All data entries have been processed and scheduled for import - now waiting for the execution to complete."));

        while (true) {
            try {
                overallFuture.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                // abnormal error - just break
                e.printStackTrace();
                break;
            } catch (TimeoutException e) {
                // still executing - send a progress report and continue waiting
                sendProgress(processedCount.get(), total);
                continue;
            }

            // process is complete
            break;
        }

        executor.shutdown();

        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;

        this.notify.forEach(s -> Message.IMPORT_END_COMPLETE.send(s, seconds));
    }

    private void sendProgress(int processedCount, int total) {
        int percent = processedCount * 100 / total;
        this.notify.forEach(s -> Message.IMPORT_PROGRESS.send(s, percent, processedCount, total));
    }

}
