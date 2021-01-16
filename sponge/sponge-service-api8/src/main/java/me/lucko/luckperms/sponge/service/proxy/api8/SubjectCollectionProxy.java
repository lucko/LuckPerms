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
import me.lucko.luckperms.sponge.service.model.LPProxiedServiceObject;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public final class SubjectCollectionProxy implements SubjectCollection, LPProxiedServiceObject {
    private final LPSubjectCollection handle;

    public SubjectCollectionProxy(LPSubjectCollection handle) {
        this.handle = handle;
    }

    @Override
    public @NonNull String getIdentifier() {
        return this.handle.getIdentifier();
    }

    @Override
    public @NonNull Predicate<String> getIdentifierValidityPredicate() {
        return this.handle.getIdentifierValidityPredicate();
    }

    @Override
    public @NonNull CompletableFuture<Subject> loadSubject(@NonNull String s) {
        return this.handle.loadSubject(s).thenApply(LPSubject::sponge);
    }

    @Override
    public @NonNull Optional<Subject> getSubject(@NonNull String s) {
        return this.handle.getSubject(s).map(LPSubject::sponge);
    }

    @Override
    public @NonNull CompletableFuture<Boolean> hasSubject(@NonNull String s) {
        return this.handle.hasRegistered(s);
    }

    @Override
    public @NonNull CompletableFuture<Map<String, Subject>> loadSubjects(@NonNull Set<String> set) {
        return this.handle.loadSubjects(set).thenApply(subs -> subs.stream().collect(ImmutableCollectors.toMap(LPSubject::getIdentifier, LPSubject::sponge)));
    }

    @Override
    public @NonNull Collection<Subject> getLoadedSubjects() {
        return this.handle.getLoadedSubjects().stream().map(LPSubject::sponge).collect(ImmutableCollectors.toSet());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull CompletableFuture<Set<String>> getAllIdentifiers() {
        return (CompletableFuture) this.handle.getAllIdentifiers();
    }

    @Override
    public @NonNull SubjectReference newSubjectReference(@NonNull String subjectIdentifier) {
        Objects.requireNonNull(subjectIdentifier, "identifier");
        if (!this.handle.getIdentifierValidityPredicate().test(subjectIdentifier)) {
            throw new IllegalArgumentException("Subject identifier '" + subjectIdentifier + "' does not pass the validity predicate");
        }

        return this.handle.getService().getReferenceFactory().obtain(getIdentifier(), subjectIdentifier);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(@NonNull String s) {
        return (CompletableFuture) this.handle.getAllWithPermission(s);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NonNull CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(@NonNull Set<Context> set, @NonNull String s) {
        return (CompletableFuture) this.handle.getAllWithPermission(CompatibilityUtil.convertContexts(set), s);
    }

    @Override
    public @NonNull Map<Subject, Boolean> getLoadedWithPermission(@NonNull String s) {
        return this.handle.getLoadedWithPermission(s).entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        sub -> sub.getKey().sponge(),
                        Map.Entry::getValue
                ));
    }

    @Override
    public @NonNull Map<Subject, Boolean> getLoadedWithPermission(@NonNull Set<Context> set, @NonNull String s) {
        return this.handle.getLoadedWithPermission(CompatibilityUtil.convertContexts(set), s).entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        sub -> sub.getKey().sponge(),
                        Map.Entry::getValue
                ));
    }

    @Override
    public @NonNull Subject getDefaults() {
        return this.handle.getDefaults().sponge();
    }

    @Override
    public void suggestUnload(@NonNull String s) {
        // unused by lp
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SubjectCollectionProxy && this.handle.equals(((SubjectCollectionProxy) o).handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api8.SubjectCollectionProxy(handle=" + this.handle + ")";
    }

}
