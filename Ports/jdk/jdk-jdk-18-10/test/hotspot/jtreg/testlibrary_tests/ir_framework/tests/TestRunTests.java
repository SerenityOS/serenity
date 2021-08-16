/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package ir_framework.tests;

import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.driver.IRViolationException;
import compiler.lib.ir_framework.shared.TestRunException;
import jdk.test.lib.Asserts;

import java.util.Arrays;

/*
 * @test
 * @requires vm.debug == true & vm.compMode != "Xint" & vm.compiler2.enabled & vm.flagless
 * @summary Test different custom run tests.
 * @library /test/lib /
 * @run driver ir_framework.tests.TestRunTests
 */

public class TestRunTests {

    public static void main(String[] args) {
        TestFramework.run();
        try {
            TestFramework.run(BadStandalone.class);
            throw new RuntimeException("Should not reach");
        } catch (IRViolationException e) {
            String[] matches = { "test(int)", "test2(int)", "Failed IR Rules (2)"};
            Arrays.stream(matches).forEach(m -> Asserts.assertTrue(e.getExceptionInfo().contains(m)));
            Asserts.assertEQ(e.getExceptionInfo().split("STANDALONE mode", -1).length - 1, 2);
        }
        new TestFramework(SkipCompilation.class).addFlags("-XX:-UseCompiler").start();
        new TestFramework(SkipCompilation.class).addFlags("-Xint").start();
        new TestFramework(SkipC2Compilation.class).addFlags("-XX:TieredStopAtLevel=1").start();
        new TestFramework(SkipC2Compilation.class).addFlags("-XX:TieredStopAtLevel=2").start();
        new TestFramework(SkipC2Compilation.class).addFlags("-XX:TieredStopAtLevel=3").start();
    }
    public int iFld;

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test1(int x) {
        iFld = x;
        return x;
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test2(int y) {
        iFld = y;
        return y;
    }

    @Run(test = {"test1", "test2"})
    public void run(RunInfo info) {
        test1(23);
        test2(42);
        Asserts.assertTrue(info.isC2CompilationEnabled());
        if (!info.isWarmUp()) {
            TestFramework.assertCompiledByC2(info.getTest("test1"));
            TestFramework.assertCompiledByC2(info.getTest("test2"));
        }
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test3(int x) {
        iFld = x;
        return x;
    }

    @Run(test = "test3")
    public void run2(RunInfo info) {
        Asserts.assertTrue(info.isC2CompilationEnabled());
        test3(42);
        if (!info.isWarmUp()) {
            TestFramework.assertCompiledByC2(info.getTest());
            try {
                info.getTest("test2");
                throw new RuntimeException("should not reach");
            } catch (TestRunException e) {
                // Excepted, do not call this method for single associated test.
            }
            try {
                info.isTestC1Compiled("test2");
                throw new RuntimeException("should not reach");
            } catch (TestRunException e) {
                // Excepted, do not call this method for single associated test.
            }
            try {
                info.isTestC2Compiled("test2");
                throw new RuntimeException("should not reach");
            } catch (TestRunException e) {
                // Excepted, do not call this method for single associated test.
            }
            try {
                info.isTestCompiledAtLevel("test2", CompLevel.C2);
                throw new RuntimeException("should not reach");
            } catch (TestRunException e) {
                // Excepted, do not call this method for single associated test.
            }
        }
    }


    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test4(int x) {
        iFld = x;
        return x;
    }

    @Run(test = "test4", mode = RunMode.STANDALONE)
    public void run3(RunInfo info) {
        for (int i = 0; i < 2000; i++) {
            test4(i);
        }
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test5(int x) {
        iFld = x;
        return x;
    }

    @Test(compLevel = CompLevel.WAIT_FOR_COMPILATION)
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test6(int y) {
        iFld = y;
        return y;
    }

    @Run(test = {"test5", "test6"})
    public void run4(RunInfo info) {
        test5(23);
        test6(42);
        if (!info.isWarmUp()) {
            TestFramework.assertCompiledByC2(info.getTest("test5"));
            TestFramework.assertCompiledByC2(info.getTest("test6"));
            try {
                info.getTest();
                throw new RuntimeException("should not reach");
            } catch (TestRunException e) {
                // Excepted, do not call this method for single associated test.
            }
            try {
                info.isTestC1Compiled();
                throw new RuntimeException("should not reach");
            } catch (TestRunException e) {
                // Excepted, do not call this method for single associated test.
            }
            try {
                info.isTestC2Compiled();
                throw new RuntimeException("should not reach");
            } catch (TestRunException e) {
                // Excepted, do not call this method for single associated test.
            }
            try {
                info.isTestCompiledAtLevel(CompLevel.C2);
                throw new RuntimeException("should not reach");
            } catch (TestRunException e) {
                // Excepted, do not call this method for single associated test.
            }
        }
    }


    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test7(int x) {
        for (int i = 0; i < 100; i++) {}
        iFld = x;
        return x;
    }


    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test8(int x) {
        for (int i = 0; i < 100; i++) {}
        iFld = x;
        return x;
    }

    @Run(test = {"test7", "test8"}, mode = RunMode.STANDALONE)
    public void run5() {
        for (int i = 0; i < 10000; i++) {
            test7(23);
            test8(42);
        }
    }

    @Test(compLevel = CompLevel.WAIT_FOR_COMPILATION)
    @Warmup(0)
    public void test9() {
        TestClass tmp = new TestClass();
        for (int i = 0; i < 100; ++i) {
            tmp.test();
        }
    }

    static class TestClass {
        public int test() {
            int res = 0;
            for (int i = 1; i < 20_000; ++i) {
                res -= i;
            }
            return res;
        }
    }
}

class BadStandalone {
    int iFld;

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test(int x) {
        iFld = x;
        return x;
    }

    @Run(test = "test", mode = RunMode.STANDALONE)
    public void run(RunInfo info) {
        test(42);
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test2(int x) {
        iFld = x;
        return x;
    }

    @Run(test = "test2", mode = RunMode.STANDALONE)
    public void run2(RunInfo info) {
    }
}

// Run with TieredStopAt=[1,3]. IR verification is skipped.
class SkipC2Compilation {

    int iFld;
    @Test(compLevel = CompLevel.C2)
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void testC2() {
        iFld = 34;
    }

    @Check(test = "testC2")
    public void checkC2(TestInfo info) {
        Asserts.assertFalse(info.isC2CompilationEnabled());
        Asserts.assertTrue(info.isCompilationSkipped());
    }

    @Test(compLevel = CompLevel.C2)
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void test2C2() {
        iFld = 34;
    }


    @Run(test = "test2C2")
    public void run2C2(RunInfo info) {
        Asserts.assertFalse(info.isC2CompilationEnabled());
        Asserts.assertTrue(info.isCompilationSkipped());
        test2C2();
        Asserts.assertTrue(info.isCompilationSkipped());
        try {
            info.isCompilationSkipped("test2C2");
            throw new RuntimeException("should not reach");
        } catch (TestRunException e) {
            // Excepted, do not call this method for single associated test.
        }
    }

    @Test(compLevel = CompLevel.C2)
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void test3C2() {
        iFld = 34;
    }

    @Test(compLevel = CompLevel.C2)
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void test4C2() {
        iFld = 34;
    }


    @Test // Level any
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void testAny() {
        iFld = 34;
    }

    @Run(test = {"test3C2", "test4C2", "testAny"})
    public void runMulti(RunInfo info) {
        Asserts.assertFalse(info.isC2CompilationEnabled());
        if (!info.isWarmUp()) {
            TestFramework.assertCompiledByC1(info.getTest("testAny"));
        }
        Asserts.assertTrue(info.isCompilationSkipped("test3C2"));
        Asserts.assertTrue(info.isCompilationSkipped("test4C2"));
        Asserts.assertFalse(info.isCompilationSkipped("testAny"));
        test2C2();
        Asserts.assertTrue(info.isCompilationSkipped("test3C2"));
        Asserts.assertTrue(info.isCompilationSkipped("test4C2"));
        Asserts.assertFalse(info.isCompilationSkipped("testAny"));
        try {
            info.isCompilationSkipped();
            throw new RuntimeException("should not reach");
        } catch (TestRunException e) {
            // Excepted, do not call this method for multiple associated tests.
        }
    }
}

// Run with -Xint and -XX:-Compiler. IR verification is skipped.
class SkipCompilation {
    int iFld;
    @Test(compLevel = CompLevel.C2)
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void testC2() {
        iFld = 34;
    }

    @Check(test = "testC2")
    public void checkC2(TestInfo info) {
        Asserts.assertTrue(info.isCompilationSkipped());
        Asserts.assertFalse(info.isC2CompilationEnabled());
    }

    @Test(compLevel = CompLevel.C2)
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void test2C2() {
        iFld = 34;
    }


    @Run(test = "test2C2")
    public void run2C2(RunInfo info) {
        Asserts.assertFalse(info.isC2CompilationEnabled());
        Asserts.assertTrue(info.isCompilationSkipped());
        test2C2();
        Asserts.assertTrue(info.isCompilationSkipped());
    }

    @Test(compLevel = CompLevel.C2)
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void test3C2() {
        iFld = 34;
    }

    @Test(compLevel = CompLevel.C2)
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void test4C2() {
        iFld = 34;
    }


    @Test // Level any
    @IR(failOn = IRNode.STORE) // Would fail but not evaluated.
    public void testAny() {
        iFld = 34;
    }

    @Run(test = {"test3C2", "test4C2", "testAny"})
    public void runMulti(RunInfo info) {
        Asserts.assertFalse(info.isC2CompilationEnabled());
        if (!info.isWarmUp()) {
            TestFramework.assertNotCompiled(info.getTest("testAny"));
            TestFramework.assertNotCompiled(info.getTest("test3C2"));
            TestFramework.assertNotCompiled(info.getTest("test4C2"));
        }
        Asserts.assertTrue(info.isCompilationSkipped("test3C2"));
        Asserts.assertTrue(info.isCompilationSkipped("test4C2"));
        Asserts.assertTrue(info.isCompilationSkipped("testAny"));
        test2C2();
        Asserts.assertTrue(info.isCompilationSkipped("test3C2"));
        Asserts.assertTrue(info.isCompilationSkipped("test4C2"));
        Asserts.assertTrue(info.isCompilationSkipped("testAny"));
    }
}
