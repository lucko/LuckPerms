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

package net.luckperms.api.model.data;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.util.Tristate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Encapsulates a store of data ({@link Node}s) within a {@link PermissionHolder}.
 *
 * <p>The effect of any mutate operation will not persist in storage unless changes are
 * explicitly saved. If changes are not saved, the effect will only be observed until the next
 * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
 * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
 * using {@link GroupManager#saveGroup(Group)}.</p>
 *
 * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
 * the backing data from the storage if you haven't done so already, to avoid overwriting changes
 * made already. This can be done via {@link UserManager#loadUser(UUID)} or
 * {@link GroupManager#loadGroup(String)} respectively.</p>
 */
public interface NodeMap {

    /**
     * Gets a map of the {@link Node}s contained within this instance,
     * mapped to their defined {@link Node#getContexts() context}.
     *
     * @return a map of nodes
     */
    @NonNull @Unmodifiable Map<ImmutableContextSet, Collection<Node>> toMap();

    /**
     * Gets a flattened view of {@link Node}s contained within this instance.
     *
     * <p>Effectively combines the value collections of the map returned by
     * {@link #toMap()}.</p>
     *
     * @return a flattened collection of nodes
     */
    @NonNull @Unmodifiable Collection<Node> toCollection();

    /**
     * Gets if this instance contains a given {@link Node}.
     *
     * <p>Returns {@link Tristate#UNDEFINED} if the instance does not contain the node,
     * and the {@link Node#getValue() assigned value} of the node as a {@link Tristate}
     * if it is present.</p>
     *
     * @param node              the node to check for
     * @param equalityPredicate how to determine if a node matches
     * @return a Tristate relating to the assigned state of the node
     * @throws NullPointerException if the node is null
     */
    @NonNull Tristate contains(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate);

    /**
     * Adds a node.
     *
     * @param node the node to be add
     * @return the result of the operation
     */
    @NonNull DataMutateResult add(@NonNull Node node);

    /**
     * Adds a node.
     *
     * @param node the node to add
     * @param temporaryNodeMergeStrategy the strategy used to merge temporary permission entries
     * @return the result of the operation
     */
    DataMutateResult.@NonNull WithMergedNode add(@NonNull Node node, @NonNull TemporaryNodeMergeStrategy temporaryNodeMergeStrategy);

    /**
     * Adds a collection of nodes.
     *
     * @param nodes the collection of nodes to add
     * @since 5.4
     */
    void addAll(@NonNull Iterable<? extends Node> nodes);

    /**
     * Adds the nodes from another node map.
     *
     * <p>Effectively calls {@link #addAll(Iterable)} with the result from
     * {@link #toCollection()}.</p>
     *
     * @param nodeMap the node map to merge into this one
     * @since 5.4
     */
    void addAll(@NonNull NodeMap nodeMap);

    /**
     * Removes a node.
     *
     * @param node the node to remove
     * @return the result of the operation
     */
    @NonNull DataMutateResult remove(@NonNull Node node);

    /**
     * Clears all nodes.
     */
    void clear();

    /**
     * Clears any nodes which pass the predicate.
     *
     * @param test the predicate to test for nodes which should be removed
     */
    void clear(@NonNull Predicate<? super Node> test);

    /**
     * Clears all nodes in a specific context.
     *
     * @param contextSet the contexts to filter by
     */
    void clear(@NonNull ContextSet contextSet);

    /**
     * Clears all nodes in a specific context which pass the predicate.
     *
     * @param contextSet the contexts to filter by
     * @param test the predicate to test for nodes which should be removed
     */
    void clear(@NonNull ContextSet contextSet, @NonNull Predicate<? super Node> test);

}
