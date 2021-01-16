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

package me.lucko.luckperms.sponge.service.reference;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Splitter;

import me.lucko.luckperms.common.util.CaffeineFactory;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPProxiedSubject;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;

import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Caches the creation of {@link LPSubjectReference}s.
 */
public final class SubjectReferenceFactory {

    /**
     * The permission service to obtain real subject instances from
     */
    private final LPPermissionService service;

    /**
     * Cache based factory for SubjectReferences.
     *
     * Using a factory and caching here makes the Subject cache in SubjectReference
     * more effective. This reduces the no. of times i/o is executed due to resolve calls
     * within the SubjectReference.
     *
     * It's perfectly ok if two instances of the same SubjectReference exist. (hence the 1 hour expiry)
     */
    private final LoadingCache<SubjectReferenceAttributes, CachedSubjectReference> referenceCache;

    public SubjectReferenceFactory(LPPermissionService service) {
        this.service = service;
        this.referenceCache = CaffeineFactory.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build(a -> new CachedSubjectReference(this.service, a.collectionId, a.id));
    }

    @Deprecated
    public LPSubjectReference deserialize(String serializedReference) {
        Objects.requireNonNull(serializedReference, "serializedReference");
        List<String> parts = Splitter.on('/').limit(2).splitToList(serializedReference);
        return obtain(parts.get(0), parts.get(1));
    }

    public LPSubjectReference obtain(LPSubject subject) {
        Objects.requireNonNull(subject, "subject");
        LPSubjectReference reference = obtain(subject.getParentCollection().getIdentifier(), subject.getIdentifier());
        ((CachedSubjectReference) reference).fillCache(subject);
        return reference;
    }

    public LPSubjectReference obtain(Subject subject) {
        Objects.requireNonNull(subject, "subject");
        if (subject instanceof LPProxiedSubject) {
            return ((LPProxiedSubject) subject).asSubjectReference();
        }

        return obtain(subject.getContainingCollection().getIdentifier(), subject.getIdentifier());
    }

    public LPSubjectReference obtain(SubjectReference reference) {
        Objects.requireNonNull(reference, "reference");
        if (reference instanceof LPSubjectReference) {
            return ((LPSubjectReference) reference);
        } else {
            return obtain(reference.getCollectionIdentifier(), reference.getSubjectIdentifier());
        }
    }

    public LPSubjectReference obtain(String collectionIdentifier, String subjectIdentifier) {
        Objects.requireNonNull(collectionIdentifier, "collectionIdentifier");
        Objects.requireNonNull(subjectIdentifier, "subjectIdentifier");
        return this.referenceCache.get(new SubjectReferenceAttributes(collectionIdentifier, subjectIdentifier));
    }

    /**
     * Used as a cache key.
     */
    private static final class SubjectReferenceAttributes {
        private final String collectionId;
        private final String id;
        private final int hashCode;

        private SubjectReferenceAttributes(String collectionId, String id) {
            this.collectionId = collectionId.toLowerCase();
            this.id = id.toLowerCase();
            this.hashCode = calculateHashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof SubjectReferenceAttributes)) return false;
            final SubjectReferenceAttributes other = (SubjectReferenceAttributes) o;
            return this.collectionId.equals(other.collectionId) && this.id.equals(other.id);
        }

        private int calculateHashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.collectionId.hashCode();
            result = result * PRIME + this.id.hashCode();
            return result;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

}
