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

import me.lucko.luckperms.sponge.LPSpongePlugin;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.event.permission.SubjectDataUpdateEvent;
import org.spongepowered.api.service.permission.SubjectData;

public class SubjectDataUpdateEventImpl extends AbstractEvent implements SubjectDataUpdateEvent {
    private final LPSpongePlugin plugin;
    private final LPSubjectData subjectData;

    public SubjectDataUpdateEventImpl(LPSpongePlugin plugin, LPSubjectData subjectData) {
        this.plugin = plugin;
        this.subjectData = subjectData;
    }

    @Override
    public SubjectData getUpdatedData() {
        return this.subjectData.sponge();
    }

    public LPSubjectData getLuckPermsUpdatedData() {
        return this.subjectData;
    }

    @Override
    public @NonNull Cause getCause() {
        EventContext eventContext = EventContext.builder()
                .add(EventContextKeys.PLUGIN, this.plugin.getBootstrap().getPluginContainer())
                .build();

        return Cause.builder()
                .append(this.plugin.getService())
                .append(this.plugin.getService().sponge())
                .append(this.plugin.getBootstrap().getPluginContainer())
                .build(eventContext);
    }
}
