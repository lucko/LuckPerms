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

package me.lucko.luckperms.sponge.commands;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;

import net.luckperms.api.context.ImmutableContextSet;

public class OptionSet extends ChildCommand<LPSubjectData> {
    public OptionSet() {
        super(CommandSpec.SPONGE_OPTION_SET, "set", CommandPermission.SPONGE_OPTION_SET, Predicates.inRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, LPSubjectData subjectData, ArgumentList args, String label) {
        String key = args.get(0);
        String value = args.get(1);
        ImmutableContextSet contextSet = args.getContextOrEmpty(2);

        if (subjectData.setOption(contextSet, key, value).join()) {
            SpongeCommandUtils.sendPrefixed(sender, "&aSet &f\"" + key + "&f\"&a to &f\"" + value + "&f\"&a in context " + SpongeCommandUtils.contextToString(contextSet));
        } else {
            SpongeCommandUtils.sendPrefixed(sender, "Unable to set option. Does the Subject already have it set?");
        }

        return CommandResult.SUCCESS;
    }
}
