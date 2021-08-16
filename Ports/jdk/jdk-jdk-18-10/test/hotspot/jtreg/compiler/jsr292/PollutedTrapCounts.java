/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8074551
 *
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @run driver compiler.jsr292.PollutedTrapCounts
 */

package compiler.jsr292;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class PollutedTrapCounts {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:-TieredCompilation", "-Xbatch",
                "-XX:PerBytecodeRecompilationCutoff=10", "-XX:PerMethodRecompilationCutoff=10",
                "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining",
                Test.class.getName());

        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());

        analyzer.shouldHaveExitValue(0);

        analyzer.shouldNotContain("not compilable (disabled)");
    }

    static class Test {
        public static final MethodHandle test1;
        public static final MethodHandle test2;
        public static final MethodHandle empty;

        static {
            try {
                Class<?> THIS_CLASS = Test.class;
                MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
                test1 = LOOKUP.findStatic(THIS_CLASS, "test1", MethodType.methodType(boolean.class, boolean.class));
                test2 = LOOKUP.findStatic(THIS_CLASS, "test2", MethodType.methodType(boolean.class, boolean.class));
                empty = LOOKUP.findStatic(THIS_CLASS, "empty", MethodType.methodType(void.class, boolean.class));
            } catch(Throwable e) {
                throw new Error(e);
            }
        }

        static boolean test1(boolean b) {
            return b;
        }
        static boolean test2(boolean b) {
            return true;
        }
        static void    empty(boolean b) {}

        static void test(boolean freqValue, boolean removeInlineBlocker) throws Throwable {
            MethodHandle innerGWT = MethodHandles.guardWithTest(test1, empty, empty);
            MethodHandle outerGWT = MethodHandles.guardWithTest(test2, innerGWT, innerGWT);

            // Trigger compilation
            for (int i = 0; i < 20_000; i++) {
                outerGWT.invokeExact(freqValue);
            }

            // Trigger deopt & nmethod invalidation
            outerGWT.invokeExact(!freqValue);

            // Force inline blocker removal on rare-taken path
            if (removeInlineBlocker) {
                for (int i = 0; i < 100; i++) {
                    outerGWT.invokeExact(!freqValue);
                }
            }

            // Trigger recompilation
            for (int i = 0; i < 20_000; i++) {
                outerGWT.invokeExact(freqValue);
            }
        }

        public static void main(String[] args) throws Throwable {
            boolean freqValue = true;
            boolean removeInlineBlocker = false;
            for (int i = 0; i < 20; i++) {
                test(freqValue, removeInlineBlocker);
                freqValue = !freqValue;
                removeInlineBlocker = !removeInlineBlocker;
            }
        }
    }
}
