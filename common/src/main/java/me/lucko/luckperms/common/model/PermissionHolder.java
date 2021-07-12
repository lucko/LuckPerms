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

package me.lucko.luckperms.common.model;

import com.google.common.collect.Iterables;

import me.lucko.luckperms.common.cacheddata.HolderCachedDataManager;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.inheritance.InheritanceComparator;
import me.lucko.luckperms.common.inheritance.InheritanceGraph;
import me.lucko.luckperms.common.model.nodemap.MutateResult;
import me.lucko.luckperms.common.model.nodemap.NodeMap;
import me.lucko.luckperms.common.model.nodemap.NodeMapMutable;
import me.lucko.luckperms.common.model.nodemap.RecordedNodeMap;
import me.lucko.luckperms.common.node.NodeEquality;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.query.DataSelector;

import net.kyori.adventure.text.Component;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * Represents an object that can hold permissions, (a user or group)
 *
 * <p>Data is stored in {@link NodeMap}s. A holder has two of these, one for
 * enduring nodes and one for transient nodes.</p>
 *
 * <p>This class provides a number of methods to perform inheritance lookups.
 * These lookup methods initially use Lists of nodes populated with the
 * inheritance tree. Nodes at the start of this list have priority over nodes at
 * the end. Nodes higher up the tree appear at the end of these lists. In order
 * to remove duplicate elements, the lists are flattened. This is significantly
 * faster than trying to prevent duplicates throughout the process of accumulation,
 * and reduces the need for too much caching.</p>
 *
 * <p>Cached state is avoided in these instances to cut down on memory
 * footprint. The nodes are stored indexed to the contexts they apply in, so
 * doing context specific querying should be fast. Caching would be ineffective
 * here, due to the potentially vast amount of contexts being used by nodes,
 * and the potential for very large inheritance trees.</p>
 */
public abstract class PermissionHolder {

    /**
     * Reference to the main plugin instance
     * @see #getPlugin()
     */
    private final LuckPermsPlugin plugin;

    /**
     * The holders identifier
     */
    private @MonotonicNonNull PermissionHolderIdentifier identifier;

    /**
     * The holders persistent nodes.
     *
     * <p>These (unlike transient nodes) are saved to the storage backing.</p>
     *
     * @see #normalData()
     */
    private final RecordedNodeMap normalNodes = new RecordedNodeMap(new NodeMapMutable(this));

    /**
     * The holders transient nodes.
     *
     * <p>These are nodes which are never stored or persisted to a file, and
     * only last until the end of the objects lifetime. (for a group, that's
     * when the server stops, and for a user, it's when they log out, or get
     * unloaded.)</p>
     *
     * @see #transientData()
     */
    private final NodeMap transientNodes = new NodeMapMutable(this);

    /**
     * Comparator used to ordering groups when calculating inheritance
     */
    private final Comparator<? super PermissionHolder> inheritanceComparator = InheritanceComparator.getFor(this);

    /**
     * Creates a new instance
     *
     * @param plugin the plugin instance
     */
    protected PermissionHolder(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    // getters

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    public Comparator<? super PermissionHolder> getInheritanceComparator() {
        return this.inheritanceComparator;
    }

    public NodeMap getData(DataType type) {
        switch (type) {
            case NORMAL:
                return this.normalNodes;
            case TRANSIENT:
                return this.transientNodes;
            default:
                throw new AssertionError();
        }
    }

    public RecordedNodeMap normalData() {
        return this.normalNodes;
    }

    public NodeMap transientData() {
        return this.transientNodes;
    }

    public PermissionHolderIdentifier getIdentifier() {
        if (this.identifier == null) {
            this.identifier = new PermissionHolderIdentifier(getType(), getObjectName());
        }
        return this.identifier;
    }

    /**
     * Gets the unique name of this holder object.
     *
     * <p>Used as a base for identifying permission holding objects. Also acts
     * as a method for preventing circular inheritance issues.</p>
     *
     * @return the object name
     */
    public abstract String getObjectName();

    /**
     * Gets the formatted display name of this permission holder
     * (for use in commands, etc)
     *
     * @return the holders formatted display name
     */
    public abstract Component getFormattedDisplayName();

    /**
     * Gets a display name for this permission holder, without any formatting.
     *
     * @return the display name
     */
    public abstract String getPlainDisplayName();

    /**
     * Gets the most appropriate query options available at the time for the holder.
     *
     * @return query options
     */
    public abstract QueryOptions getQueryOptions();

    /**
     * Gets the holders cached data
     *
     * @return the holders cached data
     */
    public abstract HolderCachedDataManager<?> getCachedData();

    /**
     * Returns the type of this PermissionHolder.
     *
     * @return this holders type
     */
    public abstract HolderType getType();

    protected void invalidateCache() {
        getCachedData().invalidate();
        getPlugin().getEventDispatcher().dispatchDataRecalculate(this);
    }

    public void loadNodesFromStorage(Iterable<? extends Node> set) {
        // TODO: should we attempt to "replay" existing changes on top of the new data?
        normalData().discardChanges();
        normalData().bypass().setContent(set);
        invalidateCache();
    }

    public MutateResult setNodes(DataType type, Iterable<? extends Node> set, boolean callEvent) {
        MutateResult res = getData(type).setContent(set);
        invalidateCache();
        if (callEvent) {
            getPlugin().getEventDispatcher().dispatchNodeChanges(this, type, res);
        }
        return res;
    }

    public MutateResult mergeNodes(DataType type, Iterable<? extends Node> set, boolean callEvent) {
        MutateResult result = getData(type).addAll(set);
        invalidateCache();
        if (callEvent && !result.isEmpty()) {
            getPlugin().getEventDispatcher().dispatchNodeChanges(this, type, result);
        }
        return result;
    }

    private DataType[] queryOrder(QueryOptions queryOptions) {
        return DataSelector.selectOrder(queryOptions, getIdentifier());
    }

    public List<Node> getOwnNodes(QueryOptions queryOptions) {
        List<Node> nodes = new ArrayList<>();
        for (DataType dataType : queryOrder(queryOptions)) {
            getData(dataType).copyTo(nodes, queryOptions);
        }
        return nodes;
    }

    public SortedSet<Node> getOwnNodesSorted(QueryOptions queryOptions) {
        SortedSet<Node> nodes = new TreeSet<>(NodeWithContextComparator.reverse());
        for (DataType dataType : queryOrder(queryOptions)) {
            getData(dataType).copyTo(nodes, queryOptions);
        }
        return nodes;
    }

    public <T extends Node> List<T> getOwnNodes(NodeType<T> type, QueryOptions queryOptions) {
        List<T> nodes = new ArrayList<>();
        for (DataType dataType : queryOrder(queryOptions)) {
            getData(dataType).copyTo(nodes, type, queryOptions);
        }
        return nodes;
    }

    public List<InheritanceNode> getOwnInheritanceNodes(QueryOptions queryOptions) {
        List<InheritanceNode> nodes = new ArrayList<>();
        for (DataType dataType : queryOrder(queryOptions)) {
            getData(dataType).copyInheritanceNodesTo(nodes, queryOptions);
        }
        return nodes;
    }

    public List<Node> resolveInheritedNodes(QueryOptions queryOptions) {
        if (!queryOptions.flag(Flag.RESOLVE_INHERITANCE)) {
            return getOwnNodes(queryOptions);
        }

        List<Node> nodes = new ArrayList<>();
        InheritanceGraph graph = this.plugin.getInheritanceGraphFactory().getGraph(queryOptions);
        for (PermissionHolder holder : graph.traverse(this)) {
            for (DataType dataType : holder.queryOrder(queryOptions)) {
                holder.getData(dataType).copyTo(nodes, queryOptions);
            }
        }
        return nodes;
    }

    public SortedSet<Node> resolveInheritedNodesSorted(QueryOptions queryOptions) {
        if (!queryOptions.flag(Flag.RESOLVE_INHERITANCE)) {
            return getOwnNodesSorted(queryOptions);
        }

        SortedSet<Node> nodes = new TreeSet<>(NodeWithContextComparator.reverse());
        InheritanceGraph graph = this.plugin.getInheritanceGraphFactory().getGraph(queryOptions);
        for (PermissionHolder holder : graph.traverse(this)) {
            for (DataType dataType : holder.queryOrder(queryOptions)) {
                holder.getData(dataType).copyTo(nodes, queryOptions);
            }
        }
        return nodes;
    }

    public <T extends Node> List<T> resolveInheritedNodes(NodeType<T> type, QueryOptions queryOptions) {
        if (!queryOptions.flag(Flag.RESOLVE_INHERITANCE)) {
            return getOwnNodes(type, queryOptions);
        }

        List<T> nodes = new ArrayList<>();
        InheritanceGraph graph = this.plugin.getInheritanceGraphFactory().getGraph(queryOptions);
        for (PermissionHolder holder : graph.traverse(this)) {
            for (DataType dataType : holder.queryOrder(queryOptions)) {
                holder.getData(dataType).copyTo(nodes, type, queryOptions);
            }
        }
        return nodes;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Group> resolveInheritanceTree(QueryOptions queryOptions) {
        InheritanceGraph graph = this.plugin.getInheritanceGraphFactory().getGraph(queryOptions);

        List<PermissionHolder> inheritanceTree = new ArrayList<>();

        if (queryOptions.flag(Flag.RESOLVE_INHERITANCE)) {
            Iterables.addAll(inheritanceTree, graph.traverse(this));
            inheritanceTree.remove(this);
        } else {
            // if RESOLVE_INHERITANCE is not set, only go up by one level
            Iterables.addAll(inheritanceTree, graph.successors(this));
        }

        // ensure our tree now only consists of groups
        for (PermissionHolder permissionHolder : inheritanceTree) {
            if (!(permissionHolder instanceof Group)) {
                throw new IllegalStateException("Non-group object in inheritance tree: " + permissionHolder);
            }
        }

        // cast List<PermissionHolder> to List<Group>
        // this feels a bit dirty but it works & avoids needless copying!
        return (List) inheritanceTree;
    }

    public <M extends Map<String, Boolean>> M exportPermissions(IntFunction<M> mapFactory, QueryOptions queryOptions, boolean convertToLowercase, boolean resolveShorthand) {
        List<Node> entries = resolveInheritedNodes(queryOptions);
        M map = mapFactory.apply(entries.size());
        processExportedPermissions(map, entries, convertToLowercase, resolveShorthand);
        return map;
    }

    private static void processExportedPermissions(Map<String, Boolean> accumulator, List<Node> entries, boolean convertToLowercase, boolean resolveShorthand) {
        for (Node node : entries) {
            if (convertToLowercase) {
                accumulator.putIfAbsent(node.getKey().toLowerCase(), node.getValue());
            } else {
                accumulator.putIfAbsent(node.getKey(), node.getValue());
            }
        }

        if (resolveShorthand) {
            for (Node node : entries) {
                Collection<String> shorthand = node.resolveShorthand();
                for (String s : shorthand) {
                    if (convertToLowercase) {
                        accumulator.putIfAbsent(s.toLowerCase(), node.getValue());
                    } else {
                        accumulator.putIfAbsent(s, node.getValue());
                    }
                }
            }
        }
    }

    public MetaAccumulator accumulateMeta(QueryOptions queryOptions) {
        return accumulateMeta(MetaAccumulator.makeFromConfig(this.plugin), queryOptions);
    }

    public MetaAccumulator accumulateMeta(MetaAccumulator accumulator, QueryOptions queryOptions) {
        InheritanceGraph graph = this.plugin.getInheritanceGraphFactory().getGraph(queryOptions);
        for (PermissionHolder holder : graph.traverse(this)) {
            // accumulate nodes
            for (DataType dataType : holder.queryOrder(queryOptions)) {
                holder.getData(dataType).forEach(queryOptions, node -> {
                    if (node.getValue() && NodeType.META_OR_CHAT_META.matches(node)) {
                        accumulator.accumulateNode(node);
                    }
                });
            }

            // accumulate weight
            OptionalInt w = holder.getWeight();
            if (w.isPresent()) {
                accumulator.accumulateWeight(w.getAsInt());
            }
        }

        // accumulate primary group
        if (this instanceof User) {
            String primaryGroup = ((User) this).getPrimaryGroup().calculateValue(queryOptions);
            accumulator.setPrimaryGroup(primaryGroup);
        }

        accumulator.complete();
        return accumulator;
    }

    /**
     * Removes temporary permissions that have expired
     *
     * @return true if permissions had expired and were removed
     */
    public boolean auditTemporaryNodes() {
        boolean transientWork = auditTemporaryNodes(DataType.TRANSIENT);
        boolean normalWork = auditTemporaryNodes(DataType.NORMAL);

        return transientWork || normalWork;
    }

    private boolean auditTemporaryNodes(DataType dataType) {
        MutateResult result = getData(dataType).removeIf(Node::hasExpired);
        if (!result.isEmpty()) {
            invalidateCache();
        }
        this.plugin.getEventDispatcher().dispatchNodeChanges(this, dataType, result);
        return !result.isEmpty();
    }

    public Tristate hasNode(DataType type, Node node, NodeEqualityPredicate equalityPredicate) {
        if (this.getType() == HolderType.GROUP && node instanceof InheritanceNode && ((InheritanceNode) node).getGroupName().equalsIgnoreCase(getObjectName())) {
            return Tristate.TRUE;
        }

        Collection<Node> nodes;
        if (NodeEquality.comparesContexts(equalityPredicate)) {
            nodes = getData(type).nodesInContext(node.getContexts());
        } else {
            nodes = getData(type).asList();
        }

        for (Node other : nodes) {
            if (equalityPredicate.areEqual(node, other)) {
                return Tristate.of(other.getValue());
            }
        }

        return Tristate.UNDEFINED;
    }

    public DataMutateResult setNode(DataType dataType, Node node, boolean callEvent) {
        if (hasNode(dataType, node, NodeEqualityPredicate.IGNORE_EXPIRY_TIME) != Tristate.UNDEFINED) {
            return DataMutateResult.FAIL_ALREADY_HAS;
        }

        MutateResult changes = getData(dataType).add(node);
        invalidateCache();
        if (callEvent) {
            this.plugin.getEventDispatcher().dispatchNodeChanges(this, dataType, changes);
        }

        return DataMutateResult.SUCCESS;
    }

    public DataMutateResult.WithMergedNode setNode(DataType dataType, Node node, TemporaryNodeMergeStrategy mergeStrategy) {
        if (node.getExpiry() != null && mergeStrategy != TemporaryNodeMergeStrategy.NONE) {
            Node otherMatch = getData(dataType).nodesInContext(node.getContexts()).stream()
                    .filter(NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE.equalTo(node))
                    .findFirst().orElse(null);

            if (otherMatch != null && otherMatch.getExpiry() != null) {
                NodeMap data = getData(dataType);

                Node newNode = null;
                switch (mergeStrategy) {
                    case ADD_NEW_DURATION_TO_EXISTING: {
                        // Create a new Node with the same properties, but add the expiry dates together
                        Instant newExpiry = otherMatch.getExpiry().plus(Duration.between(Instant.now(), node.getExpiry()));
                        newNode = node.toBuilder().expiry(newExpiry).build();
                        break;
                    }
                    case REPLACE_EXISTING_IF_DURATION_LONGER: {
                        // Only replace if the new expiry time is greater than the old one.
                        if (node.getExpiry().compareTo(otherMatch.getExpiry()) > 0) {
                            newNode = node;
                        }
                        break;
                    }
                }

                if (newNode != null) {
                    // Remove the old Node & add the new one.
                    MutateResult changes = data.removeThenAdd(otherMatch, newNode);
                    invalidateCache();
                    this.plugin.getEventDispatcher().dispatchNodeChanges(this, dataType, changes);

                    return new MergedNodeResult(DataMutateResult.SUCCESS, newNode);
                }
            }
        }

        // Fallback to the normal handling.
        return new MergedNodeResult(setNode(dataType, node, true), node);
    }

    public DataMutateResult unsetNode(DataType dataType, Node node) {
        if (hasNode(dataType, node, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE) == Tristate.UNDEFINED) {
            return DataMutateResult.FAIL_LACKS;
        }

        MutateResult changes = getData(dataType).remove(node);
        invalidateCache();
        this.plugin.getEventDispatcher().dispatchNodeChanges(this, dataType, changes);

        return DataMutateResult.SUCCESS;
    }

    public DataMutateResult.WithMergedNode unsetNode(DataType dataType, Node node, @Nullable Duration duration) {
        if (node.getExpiry() != null && duration != null) {
            Node otherMatch = getData(dataType).nodesInContext(node.getContexts()).stream()
                    .filter(NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE.equalTo(node))
                    .findFirst().orElse(null);

            if (otherMatch != null && otherMatch.getExpiry() != null) {
                NodeMap data = getData(dataType);

                Instant newExpiry = otherMatch.getExpiry().minus(duration);

                if (newExpiry.isAfter(Instant.now())) {
                    Node newNode = node.toBuilder().expiry(newExpiry).build();

                    // Remove the old Node & add the new one.
                    MutateResult changes = data.removeThenAdd(otherMatch, newNode);
                    invalidateCache();
                    this.plugin.getEventDispatcher().dispatchNodeChanges(this, dataType, changes);

                    return new MergedNodeResult(DataMutateResult.SUCCESS, newNode);
                }
            }
        }

        // Fallback to the normal handling.
        return new MergedNodeResult(unsetNode(dataType, node), null);
    }

    public boolean removeIf(DataType dataType, @Nullable ContextSet contextSet, Predicate<? super Node> predicate, boolean giveDefault) {
        MutateResult changes;
        if (contextSet == null) {
            changes = getData(dataType).removeIf(predicate);
        } else {
            changes = getData(dataType).removeIf(contextSet, predicate);
        }

        if (changes.isEmpty()) {
            return false;
        }

        if (getType() == HolderType.USER && giveDefault) {
            getPlugin().getUserManager().giveDefaultIfNeeded((User) this);
        }

        invalidateCache();
        this.plugin.getEventDispatcher().dispatchNodeClear(this, dataType, changes);
        return true;
    }

    public boolean clearNodes(DataType dataType, ContextSet contextSet, boolean giveDefault) {
        MutateResult changes;
        if (contextSet == null) {
            changes = getData(dataType).clear();
        } else {
            changes = getData(dataType).clear(contextSet);
        }

        if (getType() == HolderType.USER && giveDefault) {
            getPlugin().getUserManager().giveDefaultIfNeeded((User) this);
        }

        invalidateCache();
        this.plugin.getEventDispatcher().dispatchNodeClear(this, dataType, changes);
        return true;
    }

    public OptionalInt getWeight() {
        return OptionalInt.empty();
    }

    private static final class MergedNodeResult implements DataMutateResult.WithMergedNode {
        private final DataMutateResult result;
        private final Node mergedNode;

        private MergedNodeResult(DataMutateResult result, Node mergedNode) {
            this.result = result;
            this.mergedNode = mergedNode;
        }

        @Override
        public @NonNull DataMutateResult getResult() {
            return this.result;
        }

        @Override
        public @NonNull Node getMergedNode() {
            return this.mergedNode;
        }
    }
}
