/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.internal.tool;

import java.io.PrintStream;
import java.util.Deque;
import java.util.List;

final class Help extends Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public List<String> getOptionSyntax() {
        return List.of("[<command>]");
    }

    @Override
    protected List<String> getAliases() {
        return List.of("--help", "-h", "-?");
    }

    @Override
    public void displayOptionUsage(PrintStream stream) {
        println("  <command>   The name of the command to get help for");
    }

    @Override
    public String getDescription() {
        return "Display all available commands, or help about a specific command";
    }

    @Override
    public void execute(Deque<String> options) throws UserSyntaxException, UserDataException {
        if (options.isEmpty()) {
            Command.displayHelp();
            return;
        }
        ensureMaxArgumentCount(options, 1);
        String commandName = options.remove();
        Command c = Command.valueOf(commandName);
        if (c == null) {
            throw new UserDataException("unknown command '" + commandName + "'");
        }
        println(c.getTitle());
        println();
        c.displayUsage(System.out);
    }
}
