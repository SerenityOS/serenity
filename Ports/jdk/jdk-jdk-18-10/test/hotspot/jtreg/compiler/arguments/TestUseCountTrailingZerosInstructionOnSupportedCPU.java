/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031321
 * @summary Verify processing of UseCountTrailingZerosInstruction option
 *          on CPU with TZCNT (BMI1 feature) support.
 * @library /test/lib /
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   compiler.arguments.TestUseCountTrailingZerosInstructionOnSupportedCPU
 */

package compiler.arguments;

import jdk.test.lib.cli.CommandLineOptionTest;

public class TestUseCountTrailingZerosInstructionOnSupportedCPU
        extends BMISupportedCPUTest {
    private static final String DISABLE_BMI = "-XX:-UseBMI1Instructions";

    public TestUseCountTrailingZerosInstructionOnSupportedCPU() {
        super("UseCountTrailingZerosInstruction", TZCNT_WARNING, "bmi1");
    }

    @Override
    public void runTestCases() throws Throwable {

        super.runTestCases();

        /*
          Verify that option will be disabled if all BMI1 instructions
          are explicitly disabled. VM will be launched with following options:
          -XX:-UseBMI1Instructions -version
        */
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                "Option 'UseCountTrailingZerosInstruction' should have "
                    + "'false' value if all BMI1 instructions are explicitly"
                    + " disabled (-XX:-UseBMI1Instructions flag used)",
                TestUseCountTrailingZerosInstructionOnSupportedCPU.DISABLE_BMI);

        /*
          Verify that option could be turned on even if other BMI1
          instructions were turned off. VM will be launched with following
          options: -XX:-UseBMI1Instructions
          -XX:+UseCountTrailingZerosInstruction -version
        */
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "true",
                "Option 'UseCountTrailingZerosInstruction' should be able to "
                    + "be turned on even if all BMI1 instructions are "
                    + "disabled (-XX:-UseBMI1Instructions flag used)",
                TestUseCountTrailingZerosInstructionOnSupportedCPU.DISABLE_BMI,
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));
    }

    public static void main(String args[]) throws Throwable {
        new TestUseCountTrailingZerosInstructionOnSupportedCPU().test();
    }
}

