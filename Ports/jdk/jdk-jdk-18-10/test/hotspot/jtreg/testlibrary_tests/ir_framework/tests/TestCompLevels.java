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
import compiler.lib.ir_framework.test.TestVM;

import java.lang.reflect.Method;

/*
 * @test
 * @requires vm.flagless
 * @summary Test if compilation levels are used correctly in the framework.
 *          This test partly runs directly the test VM which normally does and should not happen in user tests.
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -DSkipWhiteBoxInstall=true -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -Xbatch -XX:+WhiteBoxAPI ir_framework.tests.TestCompLevels
 */

public class TestCompLevels {
    static int[] testExecuted = new int[5];

    public static void main(String[] args) throws Exception {
        Method runTestsOnSameVM = TestVM.class.getDeclaredMethod("runTestsOnSameVM", Class.class);
        runTestsOnSameVM.setAccessible(true);
        runTestsOnSameVM.invoke(null, new Object[]{null});

        for (int i = 0; i < testExecuted.length; i++) {
            int value = testExecuted[i];
            if (value != TestVM.WARMUP_ITERATIONS + 1) {
                // Warmups + 1 compiled invocation
                throw new RuntimeException("Test " + i + "  was executed " + value + " times stead of "
                                           + TestVM.WARMUP_ITERATIONS + 1 + " times." );
            }
        }
        TestFramework framework = new TestFramework(TestNoTiered.class);
        framework.setDefaultWarmup(10).addFlags("-XX:-TieredCompilation").start();
        framework = new TestFramework(TestNoTiered.class);
        framework.setDefaultWarmup(10).addScenarios(new Scenario(0, "-XX:-TieredCompilation")).start();
        framework = new TestFramework(TestStopAtLevel1.class);
        framework.setDefaultWarmup(10).addFlags("-XX:TieredStopAtLevel=1").start();
        framework = new TestFramework(TestStopAtLevel1.class);
        framework.setDefaultWarmup(10).addScenarios(new Scenario(0, "-XX:TieredStopAtLevel=1")).start();
    }

    @Test(compLevel = CompLevel.C1_SIMPLE)
    public void testC1() {
        testExecuted[0]++;
    }

    @Check(test = "testC1", when = CheckAt.COMPILED)
    public void checkTestC1(TestInfo info) {
        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C1_SIMPLE);
    }

    @Test(compLevel = CompLevel.C1_LIMITED_PROFILE)
    public void testC1Limited() {
        testExecuted[1]++;
    }

    @Check(test = "testC1Limited", when = CheckAt.COMPILED)
    public void checkTestLimited(TestInfo info) {
        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C1_LIMITED_PROFILE);
    }

    @Test(compLevel = CompLevel.C1_FULL_PROFILE)
    public void testC1Full() {
        testExecuted[2]++;
    }

    @Check(test = "testC1Full", when = CheckAt.COMPILED)
    public void checkTestC1Full(TestInfo info) {
        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C1_FULL_PROFILE);
    }

    @Test(compLevel = CompLevel.C2)
    public void testC2() {
        testExecuted[3]++;
    }

    @Check(test = "testC2", when = CheckAt.COMPILED)
    public void checkTestC2(TestInfo info) {
        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C2);
    }

    @Test(compLevel = CompLevel.SKIP)
    public void testSkip() {
        testExecuted[4]++; // Executed by eventually not compiled by framework
    }
}

class TestNoTiered {
    @Test(compLevel = CompLevel.C1_SIMPLE)
    public void level1() {
    }

    @Check(test = "level1")
    public void check1(TestInfo info) {
        TestFramework.assertNotCompiled(info.getTest()); // Never compiled without C1
    }

    @Test(compLevel = CompLevel.C1_LIMITED_PROFILE)
    public void level2() {
    }

    @Check(test = "level2")
    public void check2(TestInfo info) {
        TestFramework.assertNotCompiled(info.getTest()); // Never compiled without C1
    }

    @Test(compLevel = CompLevel.C1_FULL_PROFILE)
    public void level3() {
    }

    @Check(test = "level3")
    public void check3(TestInfo info) {
        TestFramework.assertNotCompiled(info.getTest()); // Never compiled without C1
    }

    @Test(compLevel = CompLevel.C2)
    public void level4() {
    }

    @Check(test = "level4")
    public void check4(TestInfo info) {
        if (info.isWarmUp()) {
            TestFramework.assertNotCompiled(info.getTest()); // Never compiled without C1
        } else {
            if (TestFramework.isC1Compiled(info.getTest())) {
                throw new RuntimeException("Cannot be compiled with C1"); // Never compiled without C1
            }
            TestFramework.assertCompiledByC2(info.getTest());
        }
    }

    @Test(compLevel = CompLevel.SKIP)
    public void skip() {
    }

    @Check(test = "skip")
    public void checkSkip(TestInfo info) {
        TestFramework.assertNotCompiled(info.getTest()); // Never compiled
    }
}

class TestStopAtLevel1 {
    @Test(compLevel = CompLevel.C1_SIMPLE)
    public int level1() {
        return 34;
    }

    @Check(test = "level1")
    public void check1(int result, TestInfo info) {
        if (info.isWarmUp()) {
            TestFramework.assertNotCompiled(info.getTest()); // Not compiled yet
        } else {
            TestFramework.assertCompiledByC1(info.getTest());
            if (TestFramework.isC2Compiled(info.getTest())) {
                throw new RuntimeException("Cannot be compiled by C2");
            }
            System.out.println("TestStopAtLevel1=" + result);
        }
    }

    @Test(compLevel = CompLevel.C1_LIMITED_PROFILE)
    public void level2() {
    }

    @Check(test = "level2")
    public void check2(TestInfo info) {
        TestFramework.assertNotCompiled(info.getTest()); // Never with level 2
    }

    @Test(compLevel = CompLevel.C1_FULL_PROFILE)
    public void level3() {
    }

    @Check(test = "level3")
    public void check3(TestInfo info) {
        TestFramework.assertNotCompiled(info.getTest()); // Never with level 3
    }

    @Test(compLevel = CompLevel.C2)
    public void level4() {
    }

    @Check(test = "level4")
    public void check4(TestInfo info) {
        TestFramework.assertNotCompiled(info.getTest()); // Never with level 4
    }

    @Test(compLevel = CompLevel.SKIP)
    public void skip() {
    }

    @Check(test = "skip")
    public void checkSkip(TestInfo info) {
        TestFramework.assertNotCompiled(info.getTest()); // Never compiled
    }
}
