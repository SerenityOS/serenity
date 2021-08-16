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

package compiler.compilercontrol.share.processors;

import compiler.compilercontrol.share.scenario.Command;
import compiler.compilercontrol.share.scenario.CompileCommand;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Checks that output contains a string with commands and full method pattern
 */
public class CommandProcessor implements Consumer<OutputAnalyzer> {
    private static final String INVALID_COMMAND_MSG = "CompileCommand: "
            + "\\b(unrecognized command|Bad pattern|"
            + "An error occurred during parsing)\\b";
    private static final String WARNING_COMMAND_MSG = "CompileCommand: An error occurred during parsing";

    private final Iterator<CompileCommand> nonQuietedIterator;
    private final Iterator<CompileCommand> quietedIterator;

    public CommandProcessor(List<CompileCommand> nonQuieted,
                            List<CompileCommand> quieted) {
        this.nonQuietedIterator = nonQuieted.iterator();
        this.quietedIterator = quieted.iterator();
    }

    @Override
    public void accept(OutputAnalyzer outputAnalyzer) {
        try {
            outputAnalyzer.asLines().stream()
                    .filter(s -> s.startsWith("CompileCommand:"))
                    .forEachOrdered(this::check);
        } catch (Exception e) {
            System.err.println(outputAnalyzer.getOutput());
            throw e;
        }
    }

    private void check(String input) {
        // -XX:CompileCommand(File) ignores invalid items
        if (input.equals(WARNING_COMMAND_MSG)) {
            return;
        }

        if (nonQuietedIterator.hasNext()) {
            CompileCommand command = nonQuietedIterator.next();
            if (command.isValid()) {
                Asserts.assertTrue(input.contains(getOutputString(command)),
                        getOutputString(command) + " missing in output");
            } else {
                Asserts.assertTrue(input.matches(INVALID_COMMAND_MSG),
                        "Error message missing for: " + getOutputString(
                                command));
            }
        } else if (quietedIterator.hasNext()) {
            CompileCommand command = quietedIterator.next();
            if (command.isValid()) {
                Asserts.assertFalse(input.contains(getOutputString(command)));
            } else {
                Asserts.assertTrue(input.matches(INVALID_COMMAND_MSG),
                        "Error message missing for: " + getOutputString(
                                command));
            }
        }
    }

    // the output here must match hotspot compilerOracle.cpp::register_command
    //    tty->print("CompileCommand: %s ", option2name(option));
    //    matcher->print();
    private String getOutputString(CompileCommand cc) {
        StringBuilder sb = new StringBuilder("CompileCommand: ");
        // ControlIntrinsic output example:
        // CompileCommand: ControlIntrinsic *Klass.-()V const char* ControlIntrinsic = '+_newArray -_minF +_copyOf'
        sb.append(cc.command.name);
        sb.append(" ");
        sb.append(cc.methodDescriptor.getCanonicalString());
        if (cc.command == Command.INTRINSIC) {
            sb.append(" const char* ");
            sb.append("ControlIntrinsic = '");

            if (cc.argument != null) {
                boolean initial = true;
                for (String id: cc.argument.split(",")) {
                    if(!initial) {
                        sb.append(" ");
                    }
                    else {
                        initial = false;
                    }
                    sb.append(id);
                }
            }
            sb.append("'");
        }

        return sb.toString();
    }
}
