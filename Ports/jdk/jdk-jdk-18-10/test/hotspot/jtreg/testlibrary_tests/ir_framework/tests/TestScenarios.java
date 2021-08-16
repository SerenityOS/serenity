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
import compiler.lib.ir_framework.shared.TestRunException;
import jdk.test.lib.Asserts;

/*
 * @test
 * @requires vm.debug == true & vm.compMode != "Xint" & vm.compiler2.enabled & vm.flagless
 * @summary Test scenarios with the framework.
 * @library /test/lib /
 * @run driver ir_framework.tests.TestScenarios
 */

public class TestScenarios {
    public static void main(String[] args) {
        Scenario sDefault = new Scenario(0);
        Scenario s1 = new Scenario(1, "-XX:TLABRefillWasteFraction=51");
        Scenario s2 = new Scenario(2, "-XX:TLABRefillWasteFraction=52");
        Scenario s3 = new Scenario(3, "-XX:TLABRefillWasteFraction=53");
        Scenario s3dup = new Scenario(3, "-XX:TLABRefillWasteFraction=53");
        try {
            new TestFramework().addScenarios(sDefault, s1, s2, s3).start();
            Asserts.fail("Should not reach");
        } catch (TestRunException e) {
            Asserts.assertTrue(e.getMessage().contains("The following scenarios have failed: #0, #1, #3"), e.getMessage());
        }
        try {
            new TestFramework().addScenarios(s1, s2, s3).start();
            Asserts.fail("Should not reach");
        } catch (TestRunException e) {
            Asserts.assertTrue(e.getMessage().contains("The following scenarios have failed: #1, #3"), e.getMessage());
        }
        new TestFramework(ScenarioTest.class).addScenarios(s1, s2, s3).start();
        try {
            new TestFramework().addScenarios(s1, s3dup, s2, s3).start();
            Asserts.fail("Should not reach");
        } catch (RuntimeException e) {
            Asserts.assertTrue(e.getMessage().contains("Cannot define two scenarios with the same index 3"), e.getMessage());
        }
        try {
            new TestFramework(MyExceptionTest.class).addScenarios(s1, s2, s3).start();
            Asserts.fail("Should not reach");
        } catch (TestRunException e) {
            Asserts.assertTrue(s1.getTestVMOutput().contains("Caused by: ir_framework.tests.MyScenarioException"));
            Asserts.assertTrue(s2.getTestVMOutput().contains("Caused by: ir_framework.tests.MyScenarioException"));
            Asserts.assertTrue(s3.getTestVMOutput().contains("Caused by: ir_framework.tests.MyScenarioException"));
        } catch (Exception e) {
            Asserts.fail("Should not catch other exceptions");
        }

    }

    @Test
    @IR(applyIf = {"TLABRefillWasteFraction", "64"}, counts = {IRNode.CALL, "1"})
    public void failDefault() {
    }

    @Test
    @IR(applyIf = {"TLABRefillWasteFraction", "51"}, counts = {IRNode.CALL, "1"})
    @IR(applyIf = {"TLABRefillWasteFraction", "53"}, counts = {IRNode.CALL, "1"})
    public void failS3() {
    }
}

class ScenarioTest {
    @Test
    @IR(applyIf = {"TLABRefillWasteFraction", "54"}, counts = {IRNode.CALL, "1"})
    public void doesNotFail() {
    }
}

class MyExceptionTest {
    int iFld;
    @Test
    @IR(failOn = IRNode.STORE) // Not evaluated due to MyScenarioException
    public void test() {
        iFld = 42;
        throw new MyScenarioException();
    }
}

class MyScenarioException extends RuntimeException {}
