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

package me.lucko.luckperms.sponge.context;

import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.context.QueryOptionsCache;
import me.lucko.luckperms.common.context.QueryOptionsSupplier;
import me.lucko.luckperms.common.util.CaffeineFactory;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.model.LPProxiedSubject;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import me.lucko.luckperms.sponge.service.model.LPSubjectUser;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.QueryOptions;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SpongeContextManager extends ContextManager<Subject, ServerPlayer> {

    private final LoadingCache<Subject, QueryOptionsCache<Subject>> subjectCaches = CaffeineFactory.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(key -> new QueryOptionsCache<>(key, this));

    public SpongeContextManager(LPSpongePlugin plugin) {
        super(plugin, Subject.class, ServerPlayer.class);
    }

    @Override
    protected Subject unwrapProxy(Subject subject) {
        if (subject instanceof LPProxiedSubject) {
            LPSubjectReference ref = ((LPProxiedSubject) subject).asSubjectReference();
            if (ref.getCollectionIdentifier().equals(PermissionService.SUBJECTS_USER)) {
                LPSubject lpSubject = ref.resolveLp().join();
                if (lpSubject instanceof LPSubjectUser) {
                    ServerPlayer player = ((LPSubjectUser) lpSubject).resolvePlayer().orElse(null);
                    if (player != null) {
                        return player;
                    }
                }
            }
        }
        return subject;
    }

    @Override
    public UUID getUniqueId(ServerPlayer player) {
        return player.getUniqueId();
    }

    @Override
    public QueryOptionsSupplier getCacheFor(Subject subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        return this.subjectCaches.get(subject);
    }

    @Override
    protected void invalidateCache(Subject subject) {
        QueryOptionsCache<Subject> cache = this.subjectCaches.getIfPresent(subject);
        if (cache != null) {
            cache.invalidate();
        }
    }

    @Override
    public QueryOptions formQueryOptions(Subject subject, ImmutableContextSet contextSet) {
        return formQueryOptions(contextSet);
    }
}
