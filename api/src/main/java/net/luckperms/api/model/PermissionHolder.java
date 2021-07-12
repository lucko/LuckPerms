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

package net.luckperms.api.model;

import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generic superinterface for an object which holds permissions.
 */
public interface PermissionHolder {

    /**
     * Represents a way to identify distinct {@link PermissionHolder}s.
     */
    interface Identifier {

        /**
         * The {@link #getType() type} of {@link User} permission holders.
         */
        String USER_TYPE = "user";

        /**
         * The {@link #getType() type} of {@link Group} permission holders.
         */
        String GROUP_TYPE = "group";

        /**
         * Gets the {@link PermissionHolder}s generic name.
         *
         * <p>The result of this method is guaranteed to be a unique identifier for distinct instances
         * of the same type of object.</p>
         *
         * <p>For {@link User}s, this method returns a {@link UUID#toString() string} representation of
         * the users {@link User#getUniqueId() unique id}.</p>
         *
         * <p>For {@link Group}s, this method returns the {@link Group#getName() group name}.</p>
         *
         * <p>The {@link User#getUniqueId()}, {@link User#getUsername()} and {@link Group#getName()} methods
         * define a "tighter" specification for obtaining object identifiers.</p>
         *
         * @return the identifier for this object. Either a uuid string or name.
         */
        @NonNull String getName();

        /**
         * Gets the type of the {@link PermissionHolder}.
         *
         * @return the type
         */
        @NonNull String getType();
    }

    /**
     * Gets the identifier of the holder.
     *
     * @return the identifier
     */
    @NonNull Identifier getIdentifier();

    /**
     * Gets a friendly name for this holder, to be displayed in command output, etc.
     *
     * <p>This will <strong>always</strong> return a value, eventually falling back to
     * {@link Identifier#getName()} if no other "friendlier" identifiers are present.</p>
     *
     * <p>For {@link User}s, this method will attempt to return the {@link User#getUsername() username},
     * before falling back to {@link Identifier#getName()}.</p>
     *
     * <p>For {@link Group}s, this method will attempt to return the groups display name, before
     * falling back to {@link Identifier#getName()}.</p>
     *
     * @return a friendly identifier for this holder
     */
    @NonNull String getFriendlyName();

    /**
     * Gets the most appropriate query options available at the time for the
     * {@link PermissionHolder}.
     *
     * <p>For {@link User}s, the most appropriate query options will be their
     * {@link ContextManager#getQueryOptions(User) current active query options} if the
     * corresponding player is online, and otherwise, will fallback to
     * {@link ContextManager#getStaticQueryOptions() the current static query options}
     * if they are offline.</p>
     *
     * <p>For {@link Group}s, the most appropriate query options will always be
     * {@link ContextManager#getStaticQueryOptions()} the current static query options.</p>
     *
     * @return query options
     * @since 5.1
     */
    @NonNull QueryOptions getQueryOptions();

    /**
     * Gets the holders {@link CachedDataManager} cache.
     *
     * @return the holders cached data.
     */
    @NonNull CachedDataManager getCachedData();

    /**
     * Gets the {@link NodeMap} of a particular type.
     *
     * @param dataType the data type
     * @return the data
     */
    @NonNull NodeMap getData(@NonNull DataType dataType);

    /**
     * Gets the holders {@link DataType#NORMAL} data.
     *
     * @return the normal data
     */
    @NonNull NodeMap data();

    /**
     * Gets the holders {@link DataType#TRANSIENT} data.
     *
     * <p>Transient permissions only exist for the duration of the session.</p>
     *
     * <p>A transient node is a permission that does not persist.
     * Whenever a user logs out of the server, or the server restarts, this permission will
     * disappear. It is never saved to the datastore, and therefore will not apply on other
     * servers.</p>
     *
     * <p>This is useful if you want to temporarily set a permission for a user while they're
     * online, but don't want it to persist, and have to worry about removing it when they log
     * out.</p>
     *
     * @return the transient data
     */
    @NonNull NodeMap transientData();

    /**
     * Gets a flattened view of the holders own {@link Node}s.
     *
     * <p>This list is constructed using the values of both the {@link #data() normal}
     * and {@link #transientData() transient} backing node maps.</p>
     *
     * <p>It <b>may contain</b> duplicate entries if the same node is added to both the normal
     * and transient node maps. You can use {@link #getDistinctNodes()} for a view without
     * duplicates.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * @return a collection of the holders own nodes.
     */
    default @NonNull @Unmodifiable Collection<Node> getNodes() {
        /* This default method is overridden in the implementation, and is just here
           to demonstrate what this method does in the API sources. */
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(data().toCollection());
        nodes.addAll(transientData().toCollection());
        return nodes;
    }

    /**
     * Gets a flattened view of the holders own {@link Node}s of the given {@code type}.
     *
     * @param type the type of node to filter by
     * @param <T> the node type
     * @return a filtered collection of the holders own nodes
     * @see #getNodes()
     * @since 5.1
     */
    default <T extends Node> @NonNull @Unmodifiable Collection<T> getNodes(@NonNull NodeType<T> type) {
        /* This default method is overridden in the implementation, and is just here
           to demonstrate what this method does in the API sources. */
        return getNodes().stream()
                .filter(type::matches)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets a flattened and sorted view of the holders own distinct {@link Node}s.
     *
     * <p>Effectively a sorted version of {@link #getNodes()}, without duplicates. Use the
     * aforementioned method if you don't require either of these attributes.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * @return a sorted set of the holders own distinct nodes
     */
    @NonNull @Unmodifiable SortedSet<Node> getDistinctNodes();

    /**
     * Gets a resolved view of the holders own and inherited {@link Node}s.
     *
     * <p>The returned list will contain every inherited
     * node the holder has, in the order that they were inherited in.</p>
     *
     * <p>This means the list will contain duplicates.</p>
     *
     * <p>Inheritance is performed according to the platforms rules, and the order will vary
     * depending on the accumulation order. By default, the holders own nodes are first in the list,
     * with the entries from the end of the inheritance tree appearing last.</p>
     *
     * @param queryOptions the query options
     * @return a list of the holders inherited nodes
     */
    @NonNull @Unmodifiable Collection<Node> resolveInheritedNodes(@NonNull QueryOptions queryOptions);

    /**
     * Gets a resolved view of the holders own and inherited {@link Node}s of a given {@code type}.
     *
     * @param type the type of node to filter by
     * @param queryOptions the query options
     * @param <T> the node type
     * @return a filtered list of the holders inherited nodes
     * @see #resolveInheritedNodes(QueryOptions)
     * @since 5.1
     */
    default <T extends Node> @NonNull @Unmodifiable Collection<T> resolveInheritedNodes(@NonNull NodeType<T> type, @NonNull QueryOptions queryOptions) {
        /* This default method is overridden in the implementation, and is just here
           to demonstrate what this method does in the API sources. */
        return resolveInheritedNodes(queryOptions).stream()
                .filter(type::matches)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets a resolved and sorted view of the holders own and inherited distinct {@link Node}s.
     *
     * <p>Effectively a sorted version of {@link #resolveInheritedNodes(QueryOptions)},
     * without duplicates. Use the aforementioned method if you don't require either of these
     * attributes.</p>
     *
     * <p>Inheritance is performed according to the platforms rules, and the order will vary
     * depending on the accumulation order. By default, the holders own nodes are first in the list,
     * with the entries from the end of the inheritance tree appearing last.</p>
     *
     * @param queryOptions the query options
     * @return a sorted set of the holders distinct inherited nodes
     */
    @NonNull @Unmodifiable SortedSet<Node> resolveDistinctInheritedNodes(@NonNull QueryOptions queryOptions);

    /**
     * Gets a collection of the {@link Group}s this holder inherits nodes from.
     *
     * <p>If {@link Flag#RESOLVE_INHERITANCE} is set, this will include holders inherited from both
     * directly and indirectly (through directly inherited groups). It will effectively resolve the
     * whole "inheritance tree".</p>
     *
     * <p>If {@link Flag#RESOLVE_INHERITANCE} is not set, then the traversal will only go one
     * level up the inheritance tree, and return only directly inherited groups.</p>
     *
     * <p>The collection will be ordered according to the platforms inheritance rules. The groups
     * which are inherited from first will appear earlier in the list.</p>
     *
     * <p>The list will not contain the holder.</p>
     *
     * @param queryOptions the query options
     * @return a collection of the groups the holder inherits from
     * @since 5.1
     */
    @NonNull @Unmodifiable Collection<Group> getInheritedGroups(@NonNull QueryOptions queryOptions);

    /**
     * Gets a collection of the {@link Group}s this holder inherits nodes from.
     *
     * <p>This method exists to avoid the verbosity of converting an existing {@link QueryOptions}
     * to its builder form and back solely to change the {@link Flag#RESOLVE_INHERITANCE} flag.</p>
     *
     * <p>This calls {@link #getInheritedGroups(QueryOptions)} with the modified query options.
     * See the description for it for the specific behavior of this method.</p>
     *
     * @param queryOptions       the query options
     * @param resolveInheritance whether the resulting collection should include inherited groups
     *                           beyond the holder's direct parent groups
     * @return a collection of the groups the holder inherits from
     * @since 5.4
     * @see #getInheritedGroups(QueryOptions)
     */
    default @NonNull @Unmodifiable Collection<Group> getInheritedGroups(@NonNull QueryOptions queryOptions, boolean resolveInheritance) {
        return getInheritedGroups(queryOptions.toBuilder().flag(Flag.RESOLVE_INHERITANCE, resolveInheritance).build());
    }

    /**
     * Removes any temporary permissions that have expired.
     *
     * <p>This method is called periodically by the platform, so it is only necessary to run
     * if you want to guarantee that the current data is totally up-to-date.</p>
     */
    void auditTemporaryNodes();

}
