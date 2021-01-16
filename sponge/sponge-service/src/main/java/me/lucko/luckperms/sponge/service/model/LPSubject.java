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

import com.google.common.collect.ImmutableList;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;

import org.spongepowered.api.service.permission.Subject;

import java.util.Optional;

/**
 * LuckPerms model for the Sponge {@link Subject}
 */
public interface LPSubject {

    LPProxiedSubject sponge();

    LPPermissionService getService();

    String getIdentifier();

    default LPSubjectReference toReference() {
        return getService().getReferenceFactory().obtain(this);
    }

    LPSubject getDefaults();

    default Optional<String> getFriendlyIdentifier() {
        return Optional.empty();
    }

    LPSubjectCollection getParentCollection();

    LPSubjectData getSubjectData();

    LPSubjectData getTransientSubjectData();

    Tristate getPermissionValue(QueryOptions options, String permission);

    Tristate getPermissionValue(ImmutableContextSet contexts, String permission);

    boolean isChildOf(ImmutableContextSet contexts, LPSubjectReference parent);

    ImmutableList<LPSubjectReference> getParents(ImmutableContextSet contexts);

    Optional<String> getOption(ImmutableContextSet contexts, String key);

    void performCacheCleanup();

    void invalidateCaches();

}
