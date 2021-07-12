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

package me.lucko.luckperms.common.bulkupdate;

import me.lucko.luckperms.common.bulkupdate.action.Action;
import me.lucko.luckperms.common.bulkupdate.query.Query;
import me.lucko.luckperms.common.model.HolderType;

import net.luckperms.api.node.Node;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a query to be applied to a set of data.
 * Queries can either be applied to im-memory sets of data, or converted to SQL syntax to be executed remotely.
 */
public final class BulkUpdate {

    // the data types which this query should apply to
    private final DataType dataType;

    // the action to apply to the data which matches the constraints
    private final Action action;

    // a set of constraints which data must match to be acted upon
    private final List<Query> queries;

    // update statistics of the operation (number of nodes, users and groups affected)
    private final BulkUpdateStatistics statistics = new BulkUpdateStatistics();
    private final boolean trackStatistics;

    public BulkUpdate(DataType dataType, Action action, List<Query> queries, boolean trackStatistics) {
        this.dataType = dataType;
        this.action = action;
        this.queries = queries;
        this.trackStatistics = trackStatistics;
    }

    /**
     * Check to see if a Node instance satisfies the constrints of this query
     *
     * @param node the node to check
     * @return true if satisfied
     */
    public boolean satisfiesConstraints(Node node) {
        for (Query query : this.queries) {
            if (!query.isSatisfiedBy(node)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies this query to the given node, and returns the result.
     *
     * @param node the node to apply changes to
     * @return the transformed node, or null if the node should be deleted
     */
    private Node apply(Node node) {
        if (!satisfiesConstraints(node)) {
            return node; // make no change
        }

        Node result = this.action.apply(node).orElse(null);

        if (this.trackStatistics && result != node) {
            this.statistics.incrementAffectedNodes();
        }

        return result;
    }

    /**
     * Applies this query to the given set of nodes, and returns the result.
     *
     * @param nodes the input nodes
     * @param holderType the holder type the nodes are from
     * @return the transformed nodes, or null if no change was made
     */
    public @Nullable Set<Node> apply(Set<Node> nodes, HolderType holderType) {
        Set<Node> results = new HashSet<>();
        boolean change = false;

        for (Node node : nodes) {
            Node result = apply(node);
            if (result != node) {
                change = true;
            }
            if (result != null) {
                results.add(result);
            }
        }

        if (!change) {
            return null;
        }

        if (this.trackStatistics) {
            this.statistics.incrementAffected(holderType);
        }

        return results;
    }

    /**
     * Converts this {@link BulkUpdate} to SQL syntax
     *
     * @return this query in SQL form
     */
    public PreparedStatementBuilder buildAsSql() {
        // DELETE FROM {table} WHERE ...
        // UPDATE {table} SET ... WHERE ...

        PreparedStatementBuilder builder = new PreparedStatementBuilder();

        // add the action
        // (DELETE FROM or UPDATE)
        this.action.appendSql(builder);

        return appendConstraintsAsSql(builder);
    }

    /**
     * Appends the constraints of this {@link BulkUpdate} to the provided statement builder in SQL syntax
     *
     * @param builder the statement builder to append the constraints to
     * @return the same statement builder provided as input
     */
    public PreparedStatementBuilder appendConstraintsAsSql(PreparedStatementBuilder builder) {

        // if there are no constraints, just return without a WHERE clause
        if (this.queries.isEmpty()) {
            return builder;
        }

        // append constraints
        builder.append(" WHERE");
        for (int i = 0; i < this.queries.size(); i++) {
            Query query = this.queries.get(i);

            builder.append(" ");
            if (i != 0) {
                builder.append("AND ");
            }

            query.appendSql(builder);
        }

        return builder;
    }

    public DataType getDataType() {
        return this.dataType;
    }

    public Action getAction() {
        return this.action;
    }

    public List<Query> getQueries() {
        return this.queries;
    }

    public boolean isTrackingStatistics() {
        return this.trackStatistics;
    }

    public BulkUpdateStatistics getStatistics() {
        return this.statistics;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BulkUpdate)) return false;
        final BulkUpdate that = (BulkUpdate) o;

        return this.getDataType() == that.getDataType() &&
                Objects.equals(this.getAction(), that.getAction()) &&
                Objects.equals(this.getQueries(), that.getQueries());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataType(), getAction(), getQueries(), isTrackingStatistics());
    }

    @Override
    public String toString() {
        return "BulkUpdate(" +
                "dataType=" + this.getDataType() + ", " +
                "action=" + this.getAction() + ", " +
                "constraints=" + this.getQueries() + ", " +
                "trackStatistics=" + this.isTrackingStatistics() + ")";
    }
}
