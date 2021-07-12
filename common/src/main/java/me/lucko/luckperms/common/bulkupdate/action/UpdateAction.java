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

package me.lucko.luckperms.common.bulkupdate.action;

import me.lucko.luckperms.common.bulkupdate.PreparedStatementBuilder;
import me.lucko.luckperms.common.bulkupdate.query.QueryField;
import me.lucko.luckperms.common.node.factory.NodeBuilders;

import net.luckperms.api.bulkupdate.action.UpdatingAction;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.node.Node;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

public class UpdateAction implements Action, UpdatingAction {

    public static UpdateAction of(QueryField field, String value) {
        return new UpdateAction(field, value);
    }

    // the field we're updating
    private final QueryField field;

    // the new value of the field
    private final String value;

    private UpdateAction(QueryField field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public @NonNull String getName() {
        return "update";
    }

    @Override
    public @NonNull Optional<Node> apply(@NonNull Node from) {
        switch (this.field) {
            case PERMISSION:
                return Optional.of(NodeBuilders.determineMostApplicable(this.value)
                                    .value(from.getValue())
                                    .expiry(from.getExpiry())
                                    .context(from.getContexts())
                                    .build());
            case SERVER: {
                MutableContextSet contexts = from.getContexts().mutableCopy();
                contexts.removeAll(DefaultContextKeys.SERVER_KEY);
                if (!this.value.equals("global")) {
                    contexts.add(DefaultContextKeys.SERVER_KEY, this.value);
                }

                return Optional.of(from.toBuilder()
                                    .context(contexts)
                                    .build());
            }
            case WORLD: {
                MutableContextSet contexts = from.getContexts().mutableCopy();
                contexts.removeAll(DefaultContextKeys.WORLD_KEY);
                if (!this.value.equals("global")) {
                    contexts.add(DefaultContextKeys.WORLD_KEY, this.value);
                }

                return Optional.of(from.toBuilder()
                                    .context(contexts)
                                    .build());
            }
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void appendSql(PreparedStatementBuilder builder) {
        builder.append("UPDATE {table} SET " + this.field.getSqlName() + "=");
        builder.variable(this.value);
    }
}
