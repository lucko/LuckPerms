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

import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.Subject;

import java.util.Locale;
import java.util.UUID;

public class SpongeSenderFactory extends SenderFactory<LPSpongePlugin, Audience> {
    public SpongeSenderFactory(LPSpongePlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getName(Audience source) {
        if (source instanceof Player) {
            return ((Player) source).getName();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(Audience source) {
        if (source instanceof Player) {
            return ((Player) source).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(Audience source, Component message) {
        Locale locale = null;
        if (source instanceof Player) {
            locale = ((Player) source).getLocale();
        }
        Component rendered = TranslationManager.render(message, locale);

        source.sendMessage(rendered);
    }

    @Override
    protected Tristate getPermissionValue(Audience source, String node) {
        if (!(source instanceof Subject)) {
            throw new IllegalStateException("Source is not a subject");
        }

        final Subject subject = (Subject) source;
        Tristate result = CompatibilityUtil.convertTristate(subject.getPermissionValue(subject.getActiveContexts(), node));

        // check the permdefault
        if (result == Tristate.UNDEFINED && subject.hasPermission(node)) {
            result = Tristate.TRUE;
        }

        return result;
    }

    @Override
    protected boolean hasPermission(Audience source, String node) {
        if (!(source instanceof Subject)) {
            throw new IllegalStateException("Source is not a subject");
        }

        final Subject subject = (Subject) source;
        return subject.hasPermission(node);
    }

    @Override
    protected void performCommand(Audience source, String command) {
        if (!(source instanceof Subject)) {
            throw new IllegalStateException("Source is not a subject");
        }

        try {
            getPlugin().getBootstrap().getGame().getCommandManager().process(((Subject) source), source, command);
        } catch (CommandException e) {
            // ignore
        }
    }
}
