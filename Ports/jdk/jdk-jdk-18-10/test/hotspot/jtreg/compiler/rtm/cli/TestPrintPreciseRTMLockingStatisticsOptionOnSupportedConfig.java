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
 * @bug 8031320
 * @summary Verify PrintPreciseRTMLockingStatistics on CPUs and OSs with
 *          rtm support and on VM with rtm locking support,
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @run driver compiler.rtm.cli.TestPrintPreciseRTMLockingStatisticsOptionOnSupportedConfig
 */

package compiler.rtm.cli;

import jdk.test.lib.cli.CommandLineOptionTest;

public class TestPrintPreciseRTMLockingStatisticsOptionOnSupportedConfig
        extends TestPrintPreciseRTMLockingStatisticsBase {

    @Override
    protected void verifyOptionValues() throws Throwable {
        super.verifyOptionValues();
        // verify default value
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName,
                TestPrintPreciseRTMLockingStatisticsBase.DEFAULT_VALUE,
                String.format("Option '%s' should have '%s' default value on"
                        + " supported CPU", optionName,
                TestPrintPreciseRTMLockingStatisticsBase.DEFAULT_VALUE),
                CommandLineOptionTest.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:+UseRTMLocking");

        CommandLineOptionTest.verifyOptionValueForSameVM(optionName,
                TestPrintPreciseRTMLockingStatisticsBase.DEFAULT_VALUE,
                String.format("Option '%s' should have '%s' default value on"
                        + " supported CPU when -XX:-UseRTMLocking flag set",
                        optionName,
                       TestPrintPreciseRTMLockingStatisticsBase.DEFAULT_VALUE),
                CommandLineOptionTest.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:-UseRTMLocking", prepareOptionValue("true"));

        // verify that option could be turned on
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "true",
                String.format("Option '%s' should have 'true' value when set "
                        + "on supported CPU and -XX:+UseRTMLocking flag set",
                        optionName),
                CommandLineOptionTest.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:+UseRTMLocking", prepareOptionValue("true"));
    }

    public static void main(String args[]) throws Throwable {
        new TestPrintPreciseRTMLockingStatisticsOptionOnSupportedConfig()
                .runTestCases();
    }
}
