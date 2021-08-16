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
 * @summary Verify UseRTMDeopt option processing on CPUs or OSs without rtm support
 *          or on VMs without rtm locking support.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @requires !vm.rtm.cpu & vm.rtm.compiler
 * @run driver compiler.rtm.cli.TestUseRTMDeoptOptionOnUnsupportedConfig
 */

package compiler.rtm.cli;

import jdk.test.lib.cli.CommandLineOptionTest;

public class TestUseRTMDeoptOptionOnUnsupportedConfig
        extends RTMGenericCommandLineOptionTest {
    private static final String DEFAULT_VALUE = "false";

    private TestUseRTMDeoptOptionOnUnsupportedConfig() {
        super("UseRTMDeopt", true, false,
                TestUseRTMDeoptOptionOnUnsupportedConfig.DEFAULT_VALUE,
                "true");
    }

    @Override
    protected void runX86SupportedVMTestCases() throws Throwable {
        super.verifyJVMStartup();
        // verify default value
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName,
                defaultValue, String.format("'%s' should have '%s' "
                        + "default value on unsupported configs.",
                        optionName, DEFAULT_VALUE));
        // verify that until RTMLocking is not used, value
        // will be set to default false.
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName,
                defaultValue, String.format("'%s' should be off on unsupported"
                        + " configs even if '-XX:+%s' flag set", optionName,
                        optionName),
                "-XX:+UseRTMDeopt");
    }

    public static void main(String args[]) throws Throwable {
        new TestUseRTMDeoptOptionOnUnsupportedConfig().runTestCases();
    }
}
