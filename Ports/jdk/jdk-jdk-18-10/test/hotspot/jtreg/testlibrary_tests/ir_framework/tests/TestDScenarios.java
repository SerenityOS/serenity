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

import compiler.lib.ir_framework.Scenario;
import compiler.lib.ir_framework.Test;
import compiler.lib.ir_framework.TestFramework;
import compiler.lib.ir_framework.driver.TestVMException;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @requires vm.debug == true & vm.flagless
 * @summary Test -DScenarios property flag. Run with othervm which should not be done when writing tests using the framework.
 * @library /test/lib /
 * @run main/othervm -DScenarios=1,5,10 ir_framework.tests.TestDScenarios test
 * @run main/othervm -DScenarios=1,4 ir_framework.tests.TestDScenarios test
 * @run main/othervm -DScenarios=3,4,9 ir_framework.tests.TestDScenarios test
 * @run driver ir_framework.tests.TestDScenarios test2
 * @run driver ir_framework.tests.TestDScenarios
 */

public class TestDScenarios {
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            switch (args[0]) {
                case "test" -> {
                    Scenario s1 = new Scenario(1);
                    Scenario s2 = new Scenario(5);
                    Scenario s3 = new Scenario(10);
                    Scenario bad = new Scenario(0, "-Flagdoesnotexist"); // not executed
                    new TestFramework().addScenarios(bad, s1, s2, s3).start();
                }
                case "test2" -> {
                    try {
                        TestFramework.run(DScenariosBad.class);
                        throw new RuntimeException("should not reach");
                    } catch (TestVMException e) {
                        System.out.println(e.getExceptionInfo());
                        Asserts.assertTrue(e.getExceptionInfo().contains("Expected DScenariosBad exception"));
                    }
                }
                default -> {
                    // Invalid -DScenarios set and thus exception thrown when Scenario class is statically initialized.
                    Scenario s = new Scenario(3);
                    throw new RuntimeException("should not reach");
                }
            }
        } else {
            // Test invalid -DScenario flag.
            OutputAnalyzer oa;
            ProcessBuilder process = ProcessTools.createJavaProcessBuilder(
                    "-Dtest.jdk=" + Utils.TEST_JDK, "-DScenarios=a,1,b,10",
                    "ir_framework.tests.TestDScenarios", " test3");
            oa = ProcessTools.executeProcess(process);
            oa.shouldNotHaveExitValue(0);
            System.out.println(oa.getOutput());
            Asserts.assertTrue(oa.getOutput().contains("TestRunException: Provided a scenario index"));
        }
    }

    @Test
    public void test() {
    }
}

class DScenariosBad {
    @Test
    public void test() {
        throw new RuntimeException("Expected DScenariosBad exception");
    }
}
