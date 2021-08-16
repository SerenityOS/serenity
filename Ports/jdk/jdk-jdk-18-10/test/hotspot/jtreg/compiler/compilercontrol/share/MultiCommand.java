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
import java.util.ArrayList;
import java.util.List;

import static compiler.compilercontrol.share.IntrinsicCommand.VALID_INTRINSIC_SAMPLES;
import static compiler.compilercontrol.share.IntrinsicCommand.INVALID_INTRINSIC_SAMPLES;

public class MultiCommand extends AbstractTestBase {
    private final List<CompileCommand> testCases;

    public MultiCommand(List<CompileCommand> testCases) {
        this.testCases = testCases;
    }

    /**
     * Generates a test containing multiple random commands
     *
     * @param validOnly shows that all commands should be valid
     * @return test instance to run
     */
    public static AbstractTestBase generateRandomTest(boolean validOnly) {
        CommandGenerator cmdGen = new CommandGenerator();
        List<Command> commands = cmdGen.generateCommands();
        List<CompileCommand> testCases = new ArrayList<>();

        for (Command cmd : commands) {
            String argument = null;

            if (validOnly && cmd == Command.NONEXISTENT) {
                // replace with a valid command
                cmd = Command.EXCLUDE;
            }
            if (cmd == Command.INTRINSIC) {
                if (validOnly) {
                    argument = Utils.getRandomElement(VALID_INTRINSIC_SAMPLES);
                } else {
                    argument = Utils.getRandomElement(INVALID_INTRINSIC_SAMPLES);
                }
            }

            Executable exec = Utils.getRandomElement(METHODS).first;
            MethodDescriptor md;

            // Command.quiet discards the method descriptor - can never fail on the method descriptor
            if (validOnly || cmd == Command.QUIET) {
                md = AbstractTestBase.getValidMethodDescriptor(exec);
            } else {
                md = AbstractTestBase.METHOD_GEN.generateRandomDescriptor(exec);
            }
            CompileCommand cc;
            if (cmd == Command.INTRINSIC) {
                cc = cmdGen.generateCompileCommand(cmd, md, null, argument);
            } else {
                cc = cmdGen.generateCompileCommand(cmd, md, null);
            }
            testCases.add(cc);
        }
        return new MultiCommand(testCases);
    }

    @Override
    public void test() {
        Scenario.Builder builder = Scenario.getBuilder();
        builder.addFlag("-Xmixed");
        builder.addFlag("-XX:+UnlockDiagnosticVMOptions");
        builder.addFlag("-XX:CompilerDirectivesLimit=101");
        for (CompileCommand cc : testCases) {
            builder.add(cc);
        }
        Scenario scenario = builder.build();
        scenario.execute();
    }
}
