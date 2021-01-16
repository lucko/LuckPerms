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

import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPProxiedServiceObject;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;

import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.plugin.PluginContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DescriptionBuilder implements PermissionDescription.Builder, LPProxiedServiceObject {

    public static LPPermissionDescription registerDescription(LPPermissionService service, PermissionDescription description) {
        if (!description.getOwner().isPresent()) {
            return null;
        }
        return service.registerPermissionDescription(description.getId(), description.getDescription().orElse(null), description.getOwner().get());
    }

    private final @NonNull LPPermissionService service;
    private final @NonNull PluginContainer container;
    private final @NonNull Map<String, Tristate> roles = new HashMap<>();
    private @Nullable String id = null;
    private @Nullable Component description = null;

    public DescriptionBuilder(LPPermissionService service, PluginContainer container) {
        this.service = Objects.requireNonNull(service, "service");
        this.container = Objects.requireNonNull(container, "container");
    }

    @Override
    public PermissionDescription.@NonNull Builder id(@NonNull String id) {
        this.id = Objects.requireNonNull(id, "id");
        return this;
    }

    @Override
    public PermissionDescription.@NonNull Builder description(@Nullable Component description) {
        this.description = description;
        return this;
    }

    @Override
    public PermissionDescription.@NonNull Builder assign(@NonNull String role, boolean value) {
        Objects.requireNonNull(role, "role");
        this.roles.put(role, Tristate.of(value));
        return this;
    }

    @Override
    public @NonNull PermissionDescription register() throws IllegalStateException {
        if (this.id == null) {
            throw new IllegalStateException("id cannot be null");
        }

        LPPermissionDescription description = this.service.registerPermissionDescription(this.id, this.description, this.container);

        // Set role-templates
        LPSubjectCollection subjects = this.service.getCollection(PermissionService.SUBJECTS_ROLE_TEMPLATE);
        for (Map.Entry<String, Tristate> assignment : this.roles.entrySet()) {
            LPSubject roleSubject = subjects.loadSubject(assignment.getKey()).join();
            roleSubject.getTransientSubjectData().setPermission(ImmutableContextSetImpl.EMPTY, this.id, assignment.getValue());
        }

        // null stuff so this instance can be reused
        this.roles.clear();
        this.id = null;
        this.description = null;

        return description.sponge();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DescriptionBuilder)) return false;
        final DescriptionBuilder other = (DescriptionBuilder) o;

        return this.container.equals(other.container) &&
                this.roles.equals(other.roles) &&
                Objects.equals(this.id, other.id) &&
                Objects.equals(this.description, other.description);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.container.hashCode();
        result = result * PRIME + this.roles.hashCode();
        result = result * PRIME + (this.id == null ? 43 : this.id.hashCode());
        result = result * PRIME + (this.description == null ? 43 : this.description.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "SimpleDescriptionBuilder(" +
                "container=" + this.container + ", " +
                "roles=" + this.roles + ", " +
                "id=" + this.id + ", " +
                "description=" + this.description + ")";
    }
}
