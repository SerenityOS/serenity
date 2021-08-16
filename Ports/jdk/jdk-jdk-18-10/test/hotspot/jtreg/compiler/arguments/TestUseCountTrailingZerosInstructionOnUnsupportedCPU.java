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
 *          on CPU without TZCNT instruction (BMI1 feature) support.
 * @library /test/lib /
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   compiler.arguments.TestUseCountTrailingZerosInstructionOnUnsupportedCPU
 */

package compiler.arguments;

import jdk.test.lib.cli.CommandLineOptionTest;

public class TestUseCountTrailingZerosInstructionOnUnsupportedCPU
        extends BMIUnsupportedCPUTest {
    private static final String ENABLE_BMI = "-XX:+UseBMI1Instructions";

    public TestUseCountTrailingZerosInstructionOnUnsupportedCPU() {
        super("UseCountTrailingZerosInstruction", TZCNT_WARNING, "bmi1");
    }

    @Override
    public void unsupportedX86CPUTestCases() throws Throwable {

        super.unsupportedX86CPUTestCases();

        /*
          Verify that option will not be turned on during UseBMI1Instructions
          processing. VM will be launched with following options:
          -XX:+UseBMI1Instructions -version
        */
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                "Feature bmi1 is not supported on current CPU. Option "
                    + "UseCountTrailingZerosInstruction should have 'false'"
                    + " value",
                TestUseCountTrailingZerosInstructionOnUnsupportedCPU.
                        ENABLE_BMI);

        /*
          VM will be launched with following options:
          -XX:+UseCountTrailingZerosInstruction -XX:+UseBMI1Instructions
          -version
        */
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                    "Feature bmi1 is not supported on current CPU. Option "
                    + "UseCountTrailingZerosInstruction should have 'false'"
                    + " value",
                CommandLineOptionTest.prepareBooleanFlag(optionName, true),
                TestUseCountTrailingZerosInstructionOnUnsupportedCPU.
                        ENABLE_BMI);
    }

    public static void main(String args[]) throws Throwable {
        new TestUseCountTrailingZerosInstructionOnUnsupportedCPU().test();
    }
}

