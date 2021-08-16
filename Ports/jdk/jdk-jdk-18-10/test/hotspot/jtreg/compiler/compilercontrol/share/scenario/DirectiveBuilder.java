/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import compiler.compilercontrol.share.JSONFile;
import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.method.MethodGenerator;
import compiler.compilercontrol.share.pool.PoolHelper;
import jdk.test.lib.util.Pair;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Directive file and state builder class
 */
public class DirectiveBuilder implements StateBuilder<CompileCommand> {
    private static final List<Pair<Executable, Callable<?>>> METHODS
            = new PoolHelper().getAllMethods();
    private final Map<Executable, State> stateMap = new HashMap<>();
    private final String fileName;
    private final Map<MethodDescriptor, List<CompileCommand>> matchBlocks
            = new LinkedHashMap<>();
    private final List<CompileCommand> inlines = new ArrayList<>();
    private boolean isFileValid = true;

    public DirectiveBuilder(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        if (!matchBlocks.isEmpty()) {
            // add option only if there are any directive available
            options.add("-XX:CompilerDirectivesFile=" + fileName);
        }
        return options;
    }

    @Override
    public List<CompileCommand> getCompileCommands() {
        return matchBlocks.keySet().stream()
                // only method descriptor is required to check print_directives
                .map(md -> new CompileCommand(null, md, null, null))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValid() {
        // Invalid directives file makes VM exit with error code
        return isFileValid;
    }

    @Override
    public Map<Executable, State> getStates() {
        writeDirectiveFile();
        if (isFileValid) {
            // Build states for each method according to match blocks
            for (Pair<Executable, Callable<?>> pair : METHODS) {
                State state = getState(pair);
                if (state != null) {
                    stateMap.put(pair.first, state);
                }
            }
            return stateMap;
        } else {
            // return empty map because invalid file doesn't change states
            return new HashMap<>();
        }
    }

    private void writeDirectiveFile() {
        try (DirectiveWriter dirFile = new DirectiveWriter(fileName)) {
            for (MethodDescriptor matchDescriptor : matchBlocks.keySet()) {
                // Write match block with all options converted from commands
                dirFile.match(matchDescriptor);
                for (CompileCommand compileCommand :
                        matchBlocks.get(matchDescriptor)) {
                    handleCommand(dirFile, compileCommand);
                }
                if ("Inlinee.caller()".matches(matchDescriptor.getRegexp())
                        && !inlines.isEmpty()) {
                    // Got a *.* match block, where inline would be written
                    writeInlines(dirFile);
                    inlines.clear();
                }
                dirFile.end(); // ends match block
            }

            /*
             * Write inline directive in the end to the latest match block
             * if we didn't do this before
             * Inlinee caller methods should match this block only
             */
            if (!inlines.isEmpty()) {
                Pair<Executable, Callable<?>> pair = METHODS.get(0);
                MethodDescriptor md = MethodGenerator.anyMatchDescriptor(
                        pair.first);
                CompileCommand cc = new CompileCommand(Command.QUIET, md,
                        null, Scenario.Type.DIRECTIVE);
                List<CompileCommand> commands = new ArrayList<>();

                // Add appropriate "*.*" match block
                commands.add(cc);
                matchBlocks.put(md, commands);
                // Add match block for this descriptor with inlines
                dirFile.match(md);
                writeInlines(dirFile);
                dirFile.end();
            }
            if (!matchBlocks.isEmpty()) {
                // terminates file
                dirFile.end();
            }

        }
    }

    private State getState(Pair<Executable, Callable<?>> pair) {
        State state = null;
        MethodDescriptor execDesc = MethodGenerator.commandDescriptor(
                pair.first);
        boolean isMatchFound = false;

        if (stateMap.containsKey(pair.first)) {
            state = stateMap.get(pair.first);
        }
        for (MethodDescriptor matchDesc : matchBlocks.keySet()) {
            if (execDesc.getCanonicalString().matches(matchDesc.getRegexp())) {
                /*
                 * if executable matches regex
                 * then apply commands from this match to the state
                 */
                for (CompileCommand cc : matchBlocks.get(matchDesc)) {
                    if (state == null) {
                        state = new State();
                    }
                    if (!isMatchFound) {
                        // this is a first found match, apply all commands
                        state.apply(cc);
                    } else {
                        // apply only inline directives
                        switch (cc.command) {
                            case INLINE:
                            case DONTINLINE:
                                state.apply(cc);
                                break;
                        }
                    }
                }
                isMatchFound = true;
            }
        }
        return state;
    }

    private void handleCommand(DirectiveWriter dirFile, CompileCommand cmd) {
        Command command = cmd.command;

        switch (command) {
            case COMPILEONLY:
                dirFile.excludeCompile(cmd.compiler, false);
                break;
            case EXCLUDE:
                dirFile.excludeCompile(cmd.compiler, true);
                break;
            case QUIET:
                /* there are no appropriate directive for this, just make
                   match be enabled */
            case INLINE:
            case DONTINLINE:
                /* Inline commands will be written later.
                   Just make this match be enabled */
                dirFile.emitCompiler(Scenario.Compiler.C1);
                dirFile.option(DirectiveWriter.Option.ENABLE, true);
                dirFile.end();
                dirFile.emitCompiler(Scenario.Compiler.C2);
                dirFile.option(DirectiveWriter.Option.ENABLE, true);
                dirFile.end();
                break;
            case LOG:
                dirFile.option(DirectiveWriter.Option.LOG, true);
                break;
            case PRINT:
                dirFile.option(DirectiveWriter.Option.PRINT_ASSEMBLY, true);
                break;
            case INTRINSIC:
                dirFile.option(DirectiveWriter.Option.INTRINSIC, "\"" + cmd.argument + "\"");
                break;
            case NONEXISTENT:
                dirFile.write(JSONFile.Element.PAIR, command.name);
                dirFile.write(JSONFile.Element.OBJECT);
                dirFile.write(JSONFile.Element.PAIR, command.name);
                dirFile.write(JSONFile.Element.VALUE,
                        cmd.methodDescriptor.getString());
                dirFile.end(); // ends object
                break;
            default:
                throw new Error("TESTBUG: wrong command: " + command);
        }
    }

    private void writeInlines(DirectiveWriter dirFile) {
        List<String> c1Block = new ArrayList<>();
        List<String> c2Block = new ArrayList<>();
        List<String> allBlock = new ArrayList<>();
        for (CompileCommand cc : inlines) {
            String inlineMethodPattern;
            switch (cc.command) {
                case INLINE:
                    inlineMethodPattern = "+" + cc.methodDescriptor.getString();
                    break;
                case DONTINLINE:
                    inlineMethodPattern = "-" + cc.methodDescriptor.getString();
                    break;
                default:
                    throw new Error("TESTBUG: incorrect command got in "
                            + "the list: " + cc.command);
            }
            if (cc.compiler == Scenario.Compiler.C1) {
                c1Block.add(inlineMethodPattern);
            } else if (cc.compiler == Scenario.Compiler.C2) {
                c2Block.add(inlineMethodPattern);
            } else {
                allBlock.add(inlineMethodPattern);
            }
        }
        dirFile.emitCompiler(Scenario.Compiler.C1);
        if (!c1Block.isEmpty()) {
            dirFile.inline(c1Block);
        } else {
            dirFile.option(DirectiveWriter.Option.ENABLE, true);
        }
        dirFile.end();
        dirFile.emitCompiler(Scenario.Compiler.C2);
        if (!c2Block.isEmpty()) {
            dirFile.inline(c2Block);
        } else {
            dirFile.option(DirectiveWriter.Option.ENABLE, true);
        }
        dirFile.end();
        if (!allBlock.isEmpty()) {
            dirFile.inline(allBlock);
        }
    }

    @Override
    public void add(CompileCommand compileCommand) {
        isFileValid &= compileCommand.isValid();
        MethodDescriptor methodDescriptor = compileCommand.methodDescriptor;

        switch (compileCommand.command) {
            case INLINE:
            case DONTINLINE:
                inlines.add(compileCommand);
                break;
        }
        for (MethodDescriptor md: matchBlocks.keySet()) {
            if (methodDescriptor.getCanonicalString().matches(md.getRegexp())) {
                matchBlocks.get(md).add(compileCommand);
            }
        }
        if (!matchBlocks.containsKey(compileCommand.methodDescriptor)) {
            List<CompileCommand> commands = new ArrayList<>();
            commands.add(compileCommand);
            matchBlocks.put(compileCommand.methodDescriptor, commands);
        }
    }
}
