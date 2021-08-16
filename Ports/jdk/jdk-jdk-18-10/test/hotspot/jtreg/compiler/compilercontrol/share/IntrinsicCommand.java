/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share;

import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.scenario.Command;
import compiler.compilercontrol.share.scenario.CommandGenerator;
import compiler.compilercontrol.share.scenario.CompileCommand;
import compiler.compilercontrol.share.scenario.Scenario;
import jdk.test.lib.Utils;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.stream.Collectors;

public class IntrinsicCommand extends AbstractTestBase {
    public static String[] VALID_INTRINSIC_SAMPLES = {"+_fabs", "-_maxF", "+_newArray", "-_isDigit", "+_putInt"};
    public static String[] INVALID_INTRINSIC_SAMPLES = {"+fabs", "-maxF"};

    public static class IntrinsicId {
        private String id;
        private boolean enable;

        public IntrinsicId(String id, boolean enable) {
            this.id = id;
            this.enable = enable;
        }

        @Override
        public String toString() {
            return (enable ? "+" : "-")  + id;
        }
    }

    private final Command command;
    private final Scenario.Type type;
    private String intrinsic_ids;

    public IntrinsicCommand(Scenario.Type type, IntrinsicId[] intrinsic_ids) {
        this.command = Command.INTRINSIC;
        this.type = type;
        this.intrinsic_ids = Arrays.stream(intrinsic_ids).map(id -> id.toString())
                                                         .collect(Collectors.joining(","));
    }

    @Override
    public void test() {
        Scenario.Builder builder = Scenario.getBuilder();
        Executable exec = Utils.getRandomElement(METHODS).first;
        MethodDescriptor md = getValidMethodDescriptor(exec);
        CommandGenerator cmdGen = new CommandGenerator();

        CompileCommand compileCommand = cmdGen.generateCompileCommand(command,
                md, type, intrinsic_ids);
        builder.add(compileCommand);
        Scenario scenario = builder.build();
        scenario.execute();
    }
}
