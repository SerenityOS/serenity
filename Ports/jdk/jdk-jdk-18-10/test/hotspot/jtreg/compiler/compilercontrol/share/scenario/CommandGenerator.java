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

import compiler.compilercontrol.share.method.MethodDescriptor;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates random commands
 */
public class CommandGenerator {
    private static final int MAX_COMMANDS = Integer.getInteger(
            "compiler.compilercontrol.share.scenario.CommandGenerator.commands",
            100);
    private static final Random RANDOM = Utils.getRandomInstance();

    /**
     * Generates random command
     *
     * @return command
     */
    public Command generateCommand() {
        return Utils.getRandomElement(Command.values());
    }

    /**
     * Generates random number of random command
     *
     * @return a list of random commands
     */
    public List<Command> generateCommands() {
        int amount = 1 + RANDOM.nextInt(MAX_COMMANDS - 1);
        return generateCommands(amount);
    }

    /**
     * Generates specified amount of random command
     *
     * @param amount amount of commands to generate
     * @return a list of random commands
     */
    public List<Command> generateCommands(int amount) {
        return Stream.generate(this::generateCommand)
                .limit(amount)
                .collect(Collectors.toList());
    }

    /**
     * Generates random compile command {@link CompileCommand} with specified
     * command and method descriptor
     *
     * @param command a command type
     * @param md      a method descriptor
     * @param type    a type of the command, or null to generate any
     * @return the generated compile command
     */
    public CompileCommand generateCompileCommand(Command command,
            MethodDescriptor md, Scenario.Type type) {
        if (type == null) {
            type = Utils.getRandomElement(Scenario.Type.values());
        }
        return type.createCompileCommand(command, md, generateCompiler());
    }

    public CompileCommand generateCompileCommand(Command command,
            MethodDescriptor md, Scenario.Type type, String argument) {
        if (type == null) {
            type = Utils.getRandomElement(Scenario.Type.values());
        }
        return type.createCompileCommand(command, md, generateCompiler(), argument);
    }


    /**
     * Generates type of compiler that should be used for the command, or null
     * if any or all compilers should be used
     *
     * @return Compiler value, or null
     */
    public Scenario.Compiler generateCompiler() {
        Scenario.Compiler[] compilers = Scenario.Compiler.values();
        int compiler = RANDOM.nextInt(compilers.length + 1) - 1;
        return (compiler != -1) ? compilers[compiler] : null;
    }

    /**
     * Generates random diagnostic command
     * {@link Scenario.Type}
     */
    public Scenario.JcmdType generateJcmdType() {
        return Utils.getRandomElement(Scenario.JcmdType.values());
    }
}
