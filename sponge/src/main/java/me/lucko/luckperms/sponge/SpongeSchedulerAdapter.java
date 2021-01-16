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

package me.lucko.luckperms.sponge;

import com.google.common.base.Suppliers;

import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.util.Iterators;

import org.spongepowered.api.Game;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.plugin.PluginContainer;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SpongeSchedulerAdapter implements SchedulerAdapter {

    private final Game game;
    private final PluginContainer pluginContainer;

    private final Scheduler asyncScheduler;
    private final Supplier<TaskExecutorService> sync;
    private final TaskExecutorService async;
    
    private final Set<ScheduledTask> tasks = Collections.newSetFromMap(new WeakHashMap<>());

    public SpongeSchedulerAdapter(Game game, PluginContainer pluginContainer) {
        this.game = game;
        this.pluginContainer = pluginContainer;

        this.asyncScheduler = game.getAsyncScheduler();
        this.async = this.asyncScheduler.createExecutor(pluginContainer);
        this.sync = Suppliers.memoize(() -> getSyncScheduler().createExecutor(this.pluginContainer));
    }

    @Override
    public Executor async() {
        return this.async;
    }

    @Override
    public Executor sync() {
        return this.sync.get();
    }

    public Scheduler getSyncScheduler() {
        return this.game.getServer().getScheduler();
    }

    private SchedulerTask submitAsyncTask(Runnable runnable, Consumer<Task.Builder> config) {
        Task.Builder builder = Task.builder();
        config.accept(builder);

        Task task = builder
                .execute(runnable)
                .plugin(this.pluginContainer)
                .build();

        ScheduledTask scheduledTask = this.asyncScheduler.submit(task);
        this.tasks.add(scheduledTask);
        return scheduledTask::cancel;
    }

    @Override
    public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
        return submitAsyncTask(task, builder -> builder.delay(delay, unit));
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        return submitAsyncTask(task, builder -> builder.delay(interval, unit).interval(interval, unit));
    }

    @Override
    public void shutdownScheduler() {
        Iterators.tryIterate(this.tasks, ScheduledTask::cancel);
    }

    @Override
    public void shutdownExecutor() {
        // do nothing
    }
}
