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

package compiler.compilercontrol.share.actions;

import compiler.compilercontrol.share.pool.PoolHelper;
import compiler.compilercontrol.share.scenario.State;
import compiler.testlibrary.CompilerUtils;
import jdk.test.lib.Asserts;
import jdk.test.lib.util.Pair;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.concurrent.Callable;

public class CompileAction {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int[] COMP_LEVELS;
    private static final List<Pair<Executable, Callable<?>>> METHODS
            = new PoolHelper().getAllMethods();
    private static final int EXEC_AMOUNT = 100;

    static {
        COMP_LEVELS = CompilerUtils.getAvailableCompilationLevels();
        if (COMP_LEVELS.length == 0) {
            throw new Error("TESTBUG: test requires JIT " +
                    "compiler to be available");
        }
    }

    /**
     * Checks executable if it could be compiled
     *
     * @param executable given executable to check
     * @param state method compilation state
     */
    public static void checkCompiled(Executable executable,
                                     State state) {
        { // Dumping the state being checked
            System.out.println("Checking expected compilation state: {");
            System.out.println("  method: " + executable);
            state.toString().lines()
                    .map(line -> "  " + line).forEach(System.out::println);
            System.out.println("}");
        }
        int first = COMP_LEVELS[0];
        if (first < 4) {
            checkCompilation(executable, first, state.isC1Compilable());
        }
        int last = COMP_LEVELS[COMP_LEVELS.length - 1];
        if (last == 4) {
            checkCompilation(executable, last, state.isC2Compilable());
        }
    }

    private static void checkCompilation(Executable executable,
                                         int level,
                                         boolean expectedCompiled) {
        execute(executable);
        WHITE_BOX.enqueueMethodForCompilation(executable, level);
        Utils.waitForCondition(
                () -> {
                    execute(executable);
                    return !WHITE_BOX.isMethodQueuedForCompilation(executable);
                }, 100L);
        execute(executable);
        boolean isCompilable = WHITE_BOX.isMethodCompilable(executable, level);
        Asserts.assertEQ(isCompilable, expectedCompiled,
                String.format("FAILED: method %s compilable: %b, but should: %b"
                        + " on required level: %d", executable, isCompilable,
                        expectedCompiled, level));
    }

    private static void execute(Executable executable) {
        Callable<?> callable = getCallableFor(executable);
        try {
            for (int i = 0; i < EXEC_AMOUNT; i++) {
                callable.call();
            }
        } catch (Exception e) {
            throw new Error("Got exception during execution", e);
        }
    }

    private static Callable<?> getCallableFor(Executable executable) {
        for (Pair<Executable, Callable<?>> pair : METHODS) {
            if (pair.first == executable) {
                return pair.second;
            }
        }
        throw new Error("TESTBUG: wrong executable: " + executable);
    }
}
