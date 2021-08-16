/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test TestMembarDependencies
 * @bug 8172850
 * @summary Tests correct scheduling of memory loads around MembarVolatile emitted by GC barriers.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver compiler.membars.TestMembarDependencies
 */

package compiler.membars;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestMembarDependencies {
    private static TestMembarDependencies f1;
    private static TestMembarDependencies f2;

    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            // For debugging, add "-XX:+TraceOptoPipelining"
            OutputAnalyzer oa = ProcessTools.executeTestJvm("-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:-TieredCompilation", "-XX:-BackgroundCompilation", "-XX:+PrintOpto",
                "-XX:CompileCommand=compileonly,compiler.membars.TestMembarDependencies::test*",
                "-XX:CompileCommand=dontinline,compiler.membars.TestMembarDependencies::test_m1",
                TestMembarDependencies.class.getName(), "run");
            // C2 should not crash or bail out from compilation
            oa.shouldHaveExitValue(0);
            oa.shouldNotMatch("Bailout: Recompile without subsuming loads");
            System.out.println(oa.getOutput());
        } else {
            f2 = new TestMembarDependencies();
            // Trigger compilation of test1 and test2
            for (int i = 0; i < 10_000; ++i) {
              f2.test1(f2);
              f2.test2(f2);
            }
        }
    }

    public void test_m1() { }
    public void test_m2() { }

    public void test1(TestMembarDependencies obj) {
        // Try/catch/finally is used to create a CFG block without a test + jmpCon
        // allowing GCM to schedule the testN_mem_reg0 instruction into that block.
        try {
            // Method call defines memory state that is then
            // used by subsequent instructions/blocks (see below).
            test_m1();
        } catch (Exception e) {

        } finally {
            // Oop write to field emits a GC post-barrier with a MembarVolatile
            // which has a wide memory effect (kills all memory). This creates an
            // anti-dependency on all surrounding memory loads.
            f1 = obj;
        }
        // The empty method m2 is inlined but the null check of f2 remains. It is encoded
        // as CmpN(LoadN(MEM), NULL) where MEM is the memory after the call to test_m1().
        // This is matched to testN_mem_reg0 on x86 which is scheduled before the barrier
        // in the try/catch block due to the anti-dependency on the MembarVolatile.
        // C2 crashes in the register allocator when trying to spill the flag register
        // to keep the result of the testN instruction live from the try/catch block
        // until it is here.
        f2.test_m2();
    }

    public void test2(TestMembarDependencies obj) {
        // Same as test1 but without try/catch/finally.
        // This causes C2 to bail out in block local scheduling because testN_mem_reg0 is
        // scheduled into a block that already contains another test + jmpCon instruction.
        test_m1();
        f1 = obj;
        f2.test_m2();
    }
}
