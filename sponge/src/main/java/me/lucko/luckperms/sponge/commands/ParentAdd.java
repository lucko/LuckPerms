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
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;

import net.luckperms.api.context.ImmutableContextSet;

public class ParentAdd extends ChildCommand<LPSubjectData> {
    public ParentAdd() {
        super(CommandSpec.SPONGE_PARENT_ADD, "add", CommandPermission.SPONGE_PARENT_ADD, Predicates.inRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, LPSubjectData subjectData, ArgumentList args, String label) {
        String collection = args.get(0);
        String name = args.get(1);
        ImmutableContextSet contextSet = args.getContextOrEmpty(2);

        LPPermissionService service = subjectData.getParentSubject().getService();
        if (service.getLoadedCollections().keySet().stream().map(String::toLowerCase).noneMatch(s -> s.equalsIgnoreCase(collection))) {
            SpongeCommandUtils.sendPrefixed(sender, "Warning: SubjectCollection '&4" + collection + "&c' doesn't already exist.");
        }

        LPSubjectCollection c = service.getCollection(collection);
        if (!c.hasRegistered(name).join()) {
            SpongeCommandUtils.sendPrefixed(sender, "Warning: Subject '&4" + name + "&c' doesn't already exist.");
        }

        LPSubject subject = c.loadSubject(name).join();

        if (subjectData.addParent(contextSet, subject.toReference()).join()) {
            SpongeCommandUtils.sendPrefixed(sender, "&aAdded parent &b" + subject.getParentCollection().getIdentifier() +
                        "&a/&b" + subject.getIdentifier() + "&a in context " + SpongeCommandUtils.contextToString(contextSet));
        } else {
            SpongeCommandUtils.sendPrefixed(sender, "Unable to add parent. Does the Subject already have it added?");
        }
        return CommandResult.SUCCESS;
    }
}
