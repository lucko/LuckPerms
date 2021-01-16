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

package me.lucko.luckperms.sponge.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.model.manager.SpongeGroupManager;
import me.lucko.luckperms.sponge.model.manager.SpongeUserManager;
import me.lucko.luckperms.sponge.service.model.ContextCalculatorProxy;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import me.lucko.luckperms.sponge.service.model.SimplePermissionDescription;
import me.lucko.luckperms.sponge.service.model.SubjectDataUpdateEventImpl;
import me.lucko.luckperms.sponge.service.model.persisted.DefaultsCollection;
import me.lucko.luckperms.sponge.service.model.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.service.model.persisted.SubjectStorage;
import me.lucko.luckperms.sponge.service.reference.SubjectReferenceFactory;

import net.kyori.adventure.text.Component;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.permission.SubjectDataUpdateEvent;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.plugin.PluginContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * LuckPerms implementation of the Sponge Permission Service
 */
public class LuckPermsService implements LPPermissionService {

    /**
     * The plugin
     */
    private final LPSpongePlugin plugin;

    /**
     * A cached proxy of this instance
     */
    private final PermissionService spongeProxy;

    /**
     * Reference factory, used to obtain {@link LPSubjectReference}s.
     */
    private final SubjectReferenceFactory referenceFactory;

    /**
     * Subject storage, used to save PersistedSubjects to a file
     */
    private final SubjectStorage storage;

    /**
     * The defaults subject collection
     */
    private final DefaultsCollection defaultSubjects;

    /**
     * A set of registered permission description instances
     */
    private final Map<String, LPPermissionDescription> permissionDescriptions;

    /**
     * The loaded collections in this service
     */
    private final Map<String, LPSubjectCollection> collections = LoadingMap.of(s -> new PersistedCollection(this, s));

    public LuckPermsService(LPSpongePlugin plugin) {
        this.plugin = plugin;
        this.referenceFactory = new SubjectReferenceFactory(this);
        this.spongeProxy = ProxyFactory.toSponge(this);
        this.permissionDescriptions = new ConcurrentHashMap<>();

        // init subject storage
        this.storage = new SubjectStorage(this, plugin.getBootstrap().getDataDirectory().resolve("sponge-data"));

        // load defaults collection
        this.defaultSubjects = new DefaultsCollection(this);
        this.defaultSubjects.loadAll();

        // pre-populate collections map with the default types
        this.collections.put("user", plugin.getUserManager());
        this.collections.put("group", plugin.getGroupManager());
        this.collections.put("defaults", this.defaultSubjects);

        // load known collections
        for (String identifier : this.storage.getSavedCollections()) {
            if (this.collections.containsKey(identifier.toLowerCase())) {
                continue;
            }

            // load data
            PersistedCollection collection = new PersistedCollection(this, identifier.toLowerCase());
            collection.loadAll();

            // cache in this instance
            this.collections.put(collection.getIdentifier(), collection);
        }
    }

    @Override
    public PermissionService sponge() {
        return this.spongeProxy;
    }

    @Override
    public LPSpongePlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public ContextManager<Subject, ServerPlayer> getContextManager() {
        return this.plugin.getContextManager();
    }

    @Override
    public SubjectReferenceFactory getReferenceFactory() {
        return this.referenceFactory;
    }

    public SubjectStorage getStorage() {
        return this.storage;
    }

    @Override
    public SpongeUserManager getUserSubjects() {
        return this.plugin.getUserManager();
    }

    @Override
    public SpongeGroupManager getGroupSubjects() {
        return this.plugin.getGroupManager();
    }

    @Override
    public DefaultsCollection getDefaultSubjects() {
        return this.defaultSubjects;
    }

    @Override
    public LPSubject getRootDefaults() {
        return this.defaultSubjects.getRootSubject();
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return Predicates.alwaysTrue();
    }

    @Override
    public LPSubjectCollection getCollection(String s) {
        Objects.requireNonNull(s);
        return this.collections.get(s.toLowerCase());
    }

    @Override
    public ImmutableMap<String, LPSubjectCollection> getLoadedCollections() {
        return ImmutableMap.copyOf(this.collections);
    }

    @Override
    public LPPermissionDescription registerPermissionDescription(String id, Component description, PluginContainer owner) {
        Objects.requireNonNull(id, "id");
        SimplePermissionDescription desc = new SimplePermissionDescription(this, id, description, owner);
        this.permissionDescriptions.put(id, desc);
        this.plugin.getPermissionRegistry().insert(id);
        return desc;
    }

    @Override
    public Optional<LPPermissionDescription> getDescription(String s) {
        Objects.requireNonNull(s);
        return Optional.ofNullable(this.permissionDescriptions.get(s));
    }

    @Override
    public ImmutableSet<LPPermissionDescription> getDescriptions() {
        Map<String, LPPermissionDescription> descriptions = new HashMap<>(this.permissionDescriptions);

        // collect known values from the permission vault
        for (String perm : this.plugin.getPermissionRegistry().rootAsList()) {
            // don't override plugin defined values
            if (!descriptions.containsKey(perm)) {
                descriptions.put(perm, new SimplePermissionDescription(this, perm, null, null));
            }
        }

        return ImmutableSet.copyOf(descriptions.values());
    }

    @Override
    public void registerContextCalculator(ContextCalculator<Subject> calculator) {
        Objects.requireNonNull(calculator);
        this.plugin.getContextManager().registerCalculator(new ContextCalculatorProxy(calculator));
    }

    @Override
    public void fireUpdateEvent(LPSubjectData subjectData) {
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            SubjectDataUpdateEvent event = new SubjectDataUpdateEventImpl(this.plugin, subjectData);
            this.plugin.getBootstrap().getGame().getEventManager().post(event);
        });
    }

    @Override
    public void invalidateAllCaches() {
        for (LPSubjectCollection collection : this.collections.values()) {
            for (LPSubject subject : collection.getLoadedSubjects()) {
                subject.invalidateCaches();
            }
        }
    }

}
