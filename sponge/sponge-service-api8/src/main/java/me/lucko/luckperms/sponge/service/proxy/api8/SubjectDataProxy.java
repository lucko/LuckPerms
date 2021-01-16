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
import me.lucko.luckperms.sponge.service.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPProxiedServiceObject;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public final class SubjectDataProxy implements SubjectData, LPProxiedServiceObject {
    private final LPPermissionService service;
    private final LPSubjectReference ref;
    private final boolean enduring;

    public SubjectDataProxy(LPPermissionService service, LPSubjectReference ref, boolean enduring) {
        this.service = service;
        this.ref = ref;
        this.enduring = enduring;
    }

    private CompletableFuture<LPSubjectData> handle() {
        return this.enduring ?
                this.ref.resolveLp().thenApply(LPSubject::getSubjectData) :
                this.ref.resolveLp().thenApply(LPSubject::getTransientSubjectData);
    }

    @Override
    public Subject getSubject() {
        return handle().thenApply(handle -> handle.getParentSubject().sponge()).join();
    }

    @Override
    public boolean isTransient() {
        return !this.enduring;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return (Map) handle().thenApply(handle -> handle.getAllPermissions().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> CompatibilityUtil.convertContexts(e.getKey()),
                        Map.Entry::getValue
                ))).join();
    }

    @Override
    public @NonNull Map<String, Boolean> getPermissions(@NonNull Set<Context> contexts) {
        return handle().thenApply(handle -> handle.getPermissions(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public @NonNull CompletableFuture<Boolean> setPermission(@NonNull Set<Context> contexts, @NonNull String permission, @NonNull Tristate value) {
        return handle().thenCompose(handle -> handle.setPermission(
                CompatibilityUtil.convertContexts(contexts),
                permission,
                CompatibilityUtil.convertTristate(value)
        ));
    }

    @Override
    public @NonNull CompletableFuture<Boolean> clearPermissions() {
        return handle().thenCompose(LPSubjectData::clearPermissions);
    }

    @Override
    public @NonNull CompletableFuture<Boolean> clearPermissions(@NonNull Set<Context> contexts) {
        return handle().thenCompose(handle -> handle.clearPermissions(CompatibilityUtil.convertContexts(contexts)));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull Map<Set<Context>, List<org.spongepowered.api.service.permission.SubjectReference>> getAllParents() {
        return (Map) handle().thenApply(handle -> handle.getAllParents().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> CompatibilityUtil.convertContexts(e.getKey()),
                        Map.Entry::getValue
                ))).join();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull List<org.spongepowered.api.service.permission.SubjectReference> getParents(@NonNull Set<Context> contexts) {
        return (List) handle().thenApply(handle -> handle.getParents(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public @NonNull CompletableFuture<Boolean> addParent(@NonNull Set<Context> contexts, org.spongepowered.api.service.permission.@NonNull SubjectReference ref) {
        return handle().thenCompose(handle -> handle.addParent(CompatibilityUtil.convertContexts(contexts), this.service.getReferenceFactory().obtain(ref)));
    }

    @Override
    public @NonNull CompletableFuture<Boolean> removeParent(@NonNull Set<Context> contexts, org.spongepowered.api.service.permission.@NonNull SubjectReference ref) {
        return handle().thenCompose(handle -> handle.removeParent(CompatibilityUtil.convertContexts(contexts), this.service.getReferenceFactory().obtain(ref)));
    }

    @Override
    public @NonNull CompletableFuture<Boolean> clearParents() {
        return handle().thenCompose(LPSubjectData::clearParents);
    }

    @Override
    public @NonNull CompletableFuture<Boolean> clearParents(@NonNull Set<Context> contexts) {
        return handle().thenCompose(handle -> handle.clearParents(CompatibilityUtil.convertContexts(contexts)));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull Map<Set<Context>, Map<String, String>> getAllOptions() {
        return (Map) handle().thenApply(handle -> handle.getAllOptions().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> CompatibilityUtil.convertContexts(e.getKey()),
                        Map.Entry::getValue
                ))).join();
    }

    @Override
    public @NonNull Map<String, String> getOptions(@NonNull Set<Context> contexts) {
        return handle().thenApply(handle -> handle.getOptions(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public @NonNull CompletableFuture<Boolean> setOption(@NonNull Set<Context> contexts, @NonNull String key, @Nullable String value) {
        if (value == null) {
            return handle().thenCompose(handle -> handle.unsetOption(CompatibilityUtil.convertContexts(contexts), key));
        } else {
            return handle().thenCompose(handle -> handle.setOption(CompatibilityUtil.convertContexts(contexts), key, value));
        }
    }

    @Override
    public @NonNull CompletableFuture<Boolean> clearOptions() {
        return handle().thenCompose(LPSubjectData::clearOptions);
    }

    @Override
    public @NonNull CompletableFuture<Boolean> clearOptions(@NonNull Set<Context> contexts) {
        return handle().thenCompose(handle -> handle.clearOptions(CompatibilityUtil.convertContexts(contexts)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SubjectDataProxy)) return false;
        final SubjectDataProxy other = (SubjectDataProxy) o;
        return this.ref.equals(other.ref) && this.enduring == other.enduring;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.ref.hashCode();
        result = result * PRIME + (this.enduring ? 79 : 97);
        return result;
    }

    @Override
    public String toString() {
        return "luckperms.api8.SubjectDataProxy(ref=" + this.ref + ", enduring=" + this.enduring + ")";
    }
}
