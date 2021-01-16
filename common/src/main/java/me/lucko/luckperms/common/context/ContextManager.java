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

package me.lucko.luckperms.common.context;

import me.lucko.luckperms.common.cache.ExpiringCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.StaticContextCalculator;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation of {@link ContextManager} which caches content lookups.
 *
 * @param <S> the calculator type
 */
public abstract class ContextManager<S, P extends S> {

    protected final LuckPermsPlugin plugin;
    private final Class<S> subjectClass;
    private final Class<P> playerClass;

    private final List<ContextCalculator<? super S>> calculators = new CopyOnWriteArrayList<>();
    private final List<StaticContextCalculator> staticCalculators = new CopyOnWriteArrayList<>();

    // caches static context lookups
    private final StaticLookupCache staticLookupCache = new StaticLookupCache();

    protected ContextManager(LuckPermsPlugin plugin, Class<S> subjectClass, Class<P> playerClass) {
        this.plugin = plugin;
        this.subjectClass = subjectClass;
        this.playerClass = playerClass;
    }

    public Class<S> getSubjectClass() {
        return this.subjectClass;
    }

    public Class<P> getPlayerClass() {
        return this.playerClass;
    }

    public abstract UUID getUniqueId(P player);

    public abstract QueryOptionsSupplier getCacheFor(S subject);

    public QueryOptions getQueryOptions(S subject) {
        return getCacheFor(subject).getQueryOptions();
    }

    public ImmutableContextSet getContext(S subject) {
        return getCacheFor(subject).getContextSet();
    }

    public QueryOptions getStaticQueryOptions() {
        return this.staticLookupCache.get();
    }

    public ImmutableContextSet getStaticContext() {
        return getStaticQueryOptions().context();
    }

    public QueryOptions formQueryOptions(ImmutableContextSet contextSet) {
        return this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_QUERY_OPTIONS).toBuilder().context(contextSet).build();
    }

    public abstract QueryOptions formQueryOptions(S subject, ImmutableContextSet contextSet);

    public void signalContextUpdate(S subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        // invalidate their cache
        invalidateCache(subject);

        // call event
        this.plugin.getEventDispatcher().dispatchContextUpdate(subject);
    }

    protected abstract void invalidateCache(S subject);

    public void registerCalculator(ContextCalculator<? super S> calculator) {
        // calculators registered first should have priority (and be checked last.)
        this.calculators.add(0, calculator);

        if (calculator instanceof StaticContextCalculator) {
            StaticContextCalculator staticCalculator = (StaticContextCalculator) calculator;
            this.staticCalculators.add(0, staticCalculator);
        }
    }

    public void unregisterCalculator(ContextCalculator<? super S> calculator) {
        this.calculators.remove(calculator);
        if (calculator instanceof StaticContextCalculator) {
            this.staticCalculators.remove(calculator);
        }
    }

    protected S unwrapProxy(S subject) {
        return subject;
    }

    protected QueryOptions calculate(S subject) {
        subject = unwrapProxy(subject);

        ImmutableContextSet.Builder accumulator = new ImmutableContextSetImpl.BuilderImpl();
        for (ContextCalculator<? super S> calculator : this.calculators) {
            try {
                calculator.calculate(subject, accumulator::add);
            } catch (Throwable e) {
                this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst calculating the context of subject " + subject, e);
            }
        }
        return formQueryOptions(subject, accumulator.build());
    }

    private QueryOptions calculateStatic() {
        ImmutableContextSet.Builder accumulator = new ImmutableContextSetImpl.BuilderImpl();
        for (StaticContextCalculator calculator : this.staticCalculators) {
            try {
                calculator.calculate(accumulator::add);
            } catch (Throwable e) {
                this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst calculating static contexts", e);
            }
        }
        return formQueryOptions(accumulator.build());
    }

    public ImmutableContextSet getPotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        for (ContextCalculator<? super S> calculator : this.calculators) {
            ContextSet potentialContexts;
            try {
                potentialContexts = calculator.estimatePotentialContexts();
            } catch (Throwable e) {
                this.plugin.getLogger().warn("An exception was thrown by " + getCalculatorClass(calculator) + " whilst estimating potential contexts", e);
                continue;
            }
            builder.addAll(potentialContexts);
        }
        return builder.build();
    }

    private final class StaticLookupCache extends ExpiringCache<QueryOptions> {
        StaticLookupCache() {
            super(50L, TimeUnit.MILLISECONDS);
        }

        @Override
        public @NonNull QueryOptions supply() {
            return calculateStatic();
        }
    }

    private static String getCalculatorClass(ContextCalculator<?> calculator) {
        Class<?> calculatorClass;
        if (calculator instanceof ForwardingContextCalculator) {
            calculatorClass = ((ForwardingContextCalculator<?>) calculator).delegate().getClass();
        } else {
            calculatorClass = calculator.getClass();
        }
        return calculatorClass.getName();
    }

}
