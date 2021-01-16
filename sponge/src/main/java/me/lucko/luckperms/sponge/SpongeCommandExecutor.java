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

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.sender.Sender;

import net.kyori.adventure.text.Component;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.selector.Selector;
import org.spongepowered.api.entity.living.player.Player;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpongeCommandExecutor extends CommandManager implements Command.Raw {
    private final LPSpongePlugin plugin;

    public SpongeCommandExecutor(LPSpongePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public @NonNull CommandResult process(@NonNull CommandCause source, @NonNull String args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(source.getAudience());
        List<String> arguments = resolveSelectors(source, ArgumentTokenizer.EXECUTE.tokenizeInput(args));
        executeCommand(wrapped, "lp", arguments);
        return CommandResult.success();
    }

    @Override
    public @NonNull List<String> getSuggestions(@NonNull CommandCause source, @NonNull String args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(source.getAudience());
        List<String> arguments = resolveSelectors(source, ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args));
        return tabCompleteCommand(wrapped, arguments);
    }

    @Override
    public boolean canExecute(CommandCause cause) {
        return true; // we run permission checks internally
    }

    @Override
    public Optional<Component> getShortDescription(CommandCause cause) {
        return Optional.of(Component.text("Manage permissions"));
    }

    @Override
    public Optional<Component> getExtendedDescription(CommandCause cause) {
        return Optional.empty();
    }

    @Override
    public Component getUsage(CommandCause cause) {
        return Component.text("/luckperms");
    }

    private List<String> resolveSelectors(CommandCause source, List<String> args) {
        if (!this.plugin.getConfiguration().get(ConfigKeys.RESOLVE_COMMAND_SELECTORS)) {
            return args;
        }

        for (ListIterator<String> it = args.listIterator(); it.hasNext(); ) {
            String arg = it.next();
            if (arg.isEmpty() || arg.charAt(0) != '@') {
                continue;
            }

            List<Player> matchedPlayers;
            try {
                matchedPlayers = Selector.parse(arg).select(source).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> ((Player) e))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                this.plugin.getLogger().warn("Error parsing selector '" + arg + "' for " + source + " executing " + args, e);
                continue;
            }

            if (matchedPlayers.isEmpty()) {
                continue;
            }

            if (matchedPlayers.size() > 1) {
                this.plugin.getLogger().warn("Error parsing selector '" + arg + "' for " + source + " executing " + args +
                        ": ambiguous result (more than one player matched) - " + matchedPlayers);
                continue;
            }

            Player player = matchedPlayers.get(0);
            it.set(player.getUniqueId().toString());
        }

        return args;
    }

}
