/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package vm.share.process;

import java.io.IOException;
import java.util.Collection;

public class CmdExecutor extends ProcessExecutor {
    private final StringBuilder cmd = new StringBuilder();
    @Override
    public void clearArgs() {
        cmd.setLength(0);
    }

    @Override
    public void addArg(String arg) {
        cmd.append(" " + arg);
    }

    @Override
    public void addArgs(String[] args) {
        for (String arg : args) {
            addArg(arg);
        }
    }

    @Override
    public void addArgs(Collection<String> args) {
        for (String arg : args) {
            addArg(arg);
        }
    }

    @Override
    protected Process createProcess() throws IOException {
        return Runtime.getRuntime().exec(cmd.toString());
    }

    @Override
    public String toString() {
        return cmd.toString();
    }
}
