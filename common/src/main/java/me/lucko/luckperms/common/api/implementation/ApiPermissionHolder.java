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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.nodemap.MutateResult;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;

public class ApiPermissionHolder implements net.luckperms.api.model.PermissionHolder {
    private final PermissionHolder handle;

    private final NodeMapImpl normalData;
    private final NodeMapImpl transientData;

    ApiPermissionHolder(PermissionHolder handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.normalData = new NodeMapImpl(DataType.NORMAL);
        this.transientData = new NodeMapImpl(DataType.TRANSIENT);
    }

    PermissionHolder getHandle() {
        return this.handle;
    }

    protected void onNodeChange() {
        // overridden by groups
        // when a node is changed on a group, it could potentially affect other groups/users,
        // so their caches need to be invalidated too. we handle this automatically for API users.
    }

    @Override
    public @NonNull Identifier getIdentifier() {
        return this.handle.getIdentifier();
    }

    @Override
    public @NonNull String getFriendlyName() {
        return this.handle.getPlainDisplayName();
    }

    @Override
    public @NonNull QueryOptions getQueryOptions() {
        return this.handle.getQueryOptions();
    }

    @Override
    public @NonNull CachedDataManager getCachedData() {
        return this.handle.getCachedData();
    }

    @Override
    public @NonNull NodeMap getData(@NonNull DataType dataType) {
        switch (dataType) {
            case NORMAL:
                return this.normalData;
            case TRANSIENT:
                return this.transientData;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public @NonNull NodeMap data() {
        return this.normalData;
    }

    @Override
    public @NonNull NodeMap transientData() {
        return this.transientData;
    }

    @Override
    public @NonNull List<Node> getNodes() {
        return this.handle.getOwnNodes(QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL);
    }

    @Override
    public <T extends Node> @NonNull Collection<T> getNodes(@NonNull NodeType<T> type) {
        Objects.requireNonNull(type, "type");
        return this.handle.getOwnNodes(type, QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL);
    }

    @Override
    public @NonNull SortedSet<Node> getDistinctNodes() {
        return this.handle.getOwnNodesSorted(QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL);
    }

    @Override
    public @NonNull List<Node> resolveInheritedNodes(@NonNull QueryOptions queryOptions) {
        Objects.requireNonNull(queryOptions, "queryOptions");
        return this.handle.resolveInheritedNodes(queryOptions);
    }

    @Override
    public <T extends Node> @NonNull Collection<T> resolveInheritedNodes(@NonNull NodeType<T> type, @NonNull QueryOptions queryOptions) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(queryOptions, "queryOptions");
        return this.handle.resolveInheritedNodes(type, queryOptions);
    }

    @Override
    public @NonNull SortedSet<Node> resolveDistinctInheritedNodes(@NonNull QueryOptions queryOptions) {
        Objects.requireNonNull(queryOptions, "queryOptions");
        return this.handle.resolveInheritedNodesSorted(queryOptions);
    }

    @Override
    public @NonNull Collection<Group> getInheritedGroups(@NonNull QueryOptions queryOptions) {
        Objects.requireNonNull(queryOptions, "queryOptions");
        return this.handle.resolveInheritanceTree(queryOptions).stream()
                .map(me.lucko.luckperms.common.model.Group::getApiProxy)
                .collect(ImmutableCollectors.toList());
    }

    @Override
    public void auditTemporaryNodes() {
        this.handle.auditTemporaryNodes();
    }

    private class NodeMapImpl implements NodeMap {
        private final DataType dataType;

        NodeMapImpl(DataType dataType) {
            this.dataType = dataType;
        }

        @Override
        public @NonNull Map<ImmutableContextSet, Collection<Node>> toMap() {
            return ApiPermissionHolder.this.handle.getData(this.dataType).asMap();
        }

        @Override
        public @NonNull Set<Node> toCollection() {
            return ApiPermissionHolder.this.handle.getData(this.dataType).asSet();
        }

        @Override
        public @NonNull Tristate contains(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate) {
            Objects.requireNonNull(node, "node");
            Objects.requireNonNull(equalityPredicate, "equalityPredicate");
            return ApiPermissionHolder.this.handle.hasNode(this.dataType, node, equalityPredicate);
        }

        @Override
        public @NonNull DataMutateResult add(@NonNull Node node) {
            Objects.requireNonNull(node, "node");
            DataMutateResult result = ApiPermissionHolder.this.handle.setNode(this.dataType, node, true);
            if (result.wasSuccessful()) {
                onNodeChange();
            }
            return result;
        }

        @Override
        public DataMutateResult.@NonNull WithMergedNode add(@NonNull Node node, @NonNull TemporaryNodeMergeStrategy temporaryNodeMergeStrategy) {
            Objects.requireNonNull(node, "node");
            Objects.requireNonNull(temporaryNodeMergeStrategy, "temporaryNodeMergeStrategy");
            DataMutateResult.WithMergedNode result = ApiPermissionHolder.this.handle.setNode(this.dataType, node, temporaryNodeMergeStrategy);
            if (result.getResult().wasSuccessful()) {
                onNodeChange();
            }
            return result;
        }

        @Override
        public void addAll(@NonNull Iterable<? extends Node> nodes) {
            Objects.requireNonNull(nodes, "nodes");
            MutateResult result = ApiPermissionHolder.this.handle.mergeNodes(this.dataType, nodes, true);
            if (!result.isEmpty()) {
                onNodeChange();
            }
        }

        @Override
        public void addAll(@NonNull NodeMap nodeMap) {
            addAll(Objects.requireNonNull(nodeMap, "nodeMap").toCollection());
        }

        @Override
        public @NonNull DataMutateResult remove(@NonNull Node node) {
            Objects.requireNonNull(node, "node");
            DataMutateResult result = ApiPermissionHolder.this.handle.unsetNode(this.dataType, node);
            if (result.wasSuccessful()) {
                onNodeChange();
            }
            return result;
        }

        @Override
        public void clear() {
            if (ApiPermissionHolder.this.handle.clearNodes(this.dataType, null, false)) {
                onNodeChange();
            }
        }

        @Override
        public void clear(@NonNull Predicate<? super Node> test) {
            Objects.requireNonNull(test, "test");
            if (ApiPermissionHolder.this.handle.removeIf(this.dataType, null, test, false)) {
                onNodeChange();
            }
        }


        @Override
        public void clear(@NonNull ContextSet contextSet) {
            Objects.requireNonNull(contextSet, "contextSet");
            if (ApiPermissionHolder.this.handle.clearNodes(this.dataType, contextSet, false)) {
                onNodeChange();
            }
        }

        @Override
        public void clear(@NonNull ContextSet contextSet, @NonNull Predicate<? super Node> test) {
            Objects.requireNonNull(contextSet, "contextSet");
            Objects.requireNonNull(test, "test");
            if (ApiPermissionHolder.this.handle.removeIf(this.dataType, contextSet, test, false)) {
                onNodeChange();
            }
        }
    }

}
