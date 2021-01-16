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

import me.lucko.luckperms.common.context.QueryOptionsSupplier;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPProxiedServiceObject;
import me.lucko.luckperms.sponge.service.model.LPProxiedSubject;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;

import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public final class SubjectProxy implements Subject, LPProxiedSubject, LPProxiedServiceObject {
    private final LPPermissionService service;
    private final LPSubjectReference ref;

    private QueryOptionsSupplier queryOptionsSupplier;

    public SubjectProxy(LPPermissionService service, LPSubjectReference ref) {
        this.service = service;
        this.ref = ref;
    }

    private CompletableFuture<LPSubject> handle() {
        return this.ref.resolveLp();
    }

    // lazy init
    private QueryOptionsSupplier queryOptionsCache() {
        if (this.queryOptionsSupplier == null) {
            this.queryOptionsSupplier = this.service.getContextManager().getCacheFor(this);
        }
        return this.queryOptionsSupplier;
    }

    @Override
    public @NonNull LPSubjectReference asSubjectReference() {
        return this.ref;
    }

    @Override
    public @NonNull QueryOptions getQueryOptions() {
        return queryOptionsCache().getQueryOptions();
    }

    @Override
    public @NonNull SubjectCollection getContainingCollection() {
        return this.service.getCollection(this.ref.getCollectionIdentifier()).sponge();
    }

    @Override
    public boolean isSubjectDataPersisted() {
        return true;
    }

    @Override
    public SubjectData getSubjectData() {
        return new SubjectDataProxy(this.service, this.ref, true);
    }

    @Override
    public SubjectData getTransientSubjectData() {
        return new SubjectDataProxy(this.service, this.ref, false);
    }

    @Override
    public boolean hasPermission(@NonNull Set<Context> contexts, @NonNull String permission) {
        return handle().thenApply(handle -> handle.getPermissionValue(CompatibilityUtil.convertContexts(contexts), permission).asBoolean()).join();
    }

    @Override
    public boolean hasPermission(@NonNull String permission) {
        return handle().thenApply(handle -> handle.getPermissionValue(queryOptionsCache().getContextSet(), permission).asBoolean()).join();
    }

    @Override
    public @NonNull Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String permission) {
        return handle().thenApply(handle -> CompatibilityUtil.convertTristate(handle.getPermissionValue(CompatibilityUtil.convertContexts(contexts), permission))).join();
    }

    @Override
    public boolean isChildOf(@NonNull SubjectReference parent) {
        return handle().thenApply(handle -> handle.isChildOf(queryOptionsCache().getContextSet(), this.service.getReferenceFactory().obtain(parent))).join();
    }

    @Override
    public boolean isChildOf(@NonNull Set<Context> contexts, @NonNull SubjectReference parent) {
        return handle().thenApply(handle -> handle.isChildOf(CompatibilityUtil.convertContexts(contexts), this.service.getReferenceFactory().obtain(parent))).join();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull List<SubjectReference> getParents() {
        return (List) handle().thenApply(handle -> handle.getParents(queryOptionsCache().getContextSet())).join();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull List<SubjectReference> getParents(@NonNull Set<Context> contexts) {
        return (List) handle().thenApply(handle -> handle.getParents(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public @NonNull Optional<String> getOption(@NonNull Set<Context> contexts, @NonNull String key) {
        return handle().thenApply(handle -> handle.getOption(CompatibilityUtil.convertContexts(contexts), key)).join();
    }

    @Override
    public @NonNull Optional<String> getOption(@NonNull String key) {
        return handle().thenApply(handle -> handle.getOption(queryOptionsCache().getContextSet(), key)).join();
    }

    @Override
    public String getIdentifier() {
        return this.ref.getSubjectIdentifier();
    }

    @Override
    public @NonNull Optional<String> getFriendlyIdentifier() {
        return handle().thenApply(LPSubject::getFriendlyIdentifier).join();
    }

    @Override
    public @NonNull Set<Context> getActiveContexts() {
        return CompatibilityUtil.convertContexts(queryOptionsCache().getContextSet());
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SubjectProxy && this.ref.equals(((SubjectProxy) o).ref);
    }

    @Override
    public int hashCode() {
        return this.ref.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api8.SubjectProxy(ref=" + this.ref + ")";
    }
}
