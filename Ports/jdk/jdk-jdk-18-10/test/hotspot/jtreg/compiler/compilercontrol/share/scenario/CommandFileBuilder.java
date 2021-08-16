/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share.scenario;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Creates CompileCommandFile from the given array of commands
 */
public class CommandFileBuilder extends AbstractCommandBuilder {
    private final String fileName;

    public CommandFileBuilder(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public List<String> getOptions() {
        Function<CompileCommand, String> mapper = cc -> {
            StringBuilder sb = new StringBuilder(cc.command.name);
            sb.append(" ");
            sb.append(cc.methodDescriptor.getString());
            if (cc.argument != null) {
                sb.append(" ");
                sb.append(cc.argument);
            }
            return sb.toString();
        };

        // Create CommandFile
        try (PrintWriter pw = new PrintWriter(fileName)) {
            compileCommands.stream()
                    .map(mapper)
                    .forEach(pw::println);
            if (pw.checkError()) {
                throw new Error("TESTBUG: write error");
            }
        } catch (IOException e) {
            throw new Error("TESTBUG: can't write a file", e);
        }
        List<String> opt = new ArrayList<>();
        opt.add("-XX:CompileCommandFile=" + fileName);
        return opt;
    }
}
