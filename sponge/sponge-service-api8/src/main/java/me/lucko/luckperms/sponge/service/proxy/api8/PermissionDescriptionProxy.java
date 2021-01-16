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

package me.lucko.luckperms.sponge.service.proxy.api8;

import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPProxiedServiceObject;

import net.kyori.adventure.text.Component;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.plugin.PluginContainer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class PermissionDescriptionProxy implements PermissionDescription, LPProxiedServiceObject {
    private final LPPermissionService service;
    private final LPPermissionDescription handle;

    public PermissionDescriptionProxy(LPPermissionService service, LPPermissionDescription handle) {
        this.service = service;
        this.handle = handle;
    }

    @Override
    public @NonNull String getId() {
        return this.handle.getId();
    }

    @Override
    public @NonNull Optional<Component> getDescription() {
        return this.handle.getDescription();
    }

    @Override
    public @NonNull Optional<PluginContainer> getOwner() {
        return this.handle.getOwner();
    }

    @Override
    public @NonNull Map<Subject, Boolean> getAssignedSubjects(@NonNull String s) {
        return this.handle.getAssignedSubjects(s).entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> new SubjectProxy(this.service, e.getKey().toReference()),
                        Map.Entry::getValue
                ));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NonNull CompletableFuture<Map<SubjectReference, Boolean>> findAssignedSubjects(@NonNull String s) {
        return (CompletableFuture) this.handle.findAssignedSubjects(s);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof PermissionDescriptionProxy && this.handle.equals(((PermissionDescriptionProxy) o).handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api8.PermissionDescriptionProxy(handle=" + this.handle + ")";
    }
}
