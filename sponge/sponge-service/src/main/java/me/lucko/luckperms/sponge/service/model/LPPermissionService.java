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

package me.lucko.luckperms.sponge.service.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.sponge.service.reference.SubjectReferenceFactory;

import net.kyori.adventure.text.Component;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.plugin.PluginContainer;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * LuckPerms model for the Sponge {@link PermissionService}
 */
public interface LPPermissionService {

    LuckPermsPlugin getPlugin();

    ContextManager<Subject, ServerPlayer> getContextManager();

    SubjectReferenceFactory getReferenceFactory();

    PermissionService sponge();

    LPSubjectCollection getUserSubjects();

    LPSubjectCollection getGroupSubjects();

    LPSubjectCollection getDefaultSubjects();

    LPSubject getRootDefaults();

    Predicate<String> getIdentifierValidityPredicate();

    LPSubjectCollection getCollection(String identifier);

    ImmutableMap<String, LPSubjectCollection> getLoadedCollections();

    LPPermissionDescription registerPermissionDescription(String id, Component description, PluginContainer owner);

    Optional<LPPermissionDescription> getDescription(String permission);

    ImmutableCollection<LPPermissionDescription> getDescriptions();

    void registerContextCalculator(ContextCalculator<Subject> calculator);

    void fireUpdateEvent(LPSubjectData subjectData);

    void invalidateAllCaches();
}
