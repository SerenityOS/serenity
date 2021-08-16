/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.cli.*;

/*
 * @test
 * @bug 8061611
 * @summary Test that various alias options correctly set the target options. See aliased_jvm_flags in arguments.cpp.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver VMAliasOptions
 */
public class VMAliasOptions {

    /**
     * each entry is {[0]: alias name, [1]: alias target, [2]: value to set
     * (true/false/n/string)}.
     */
    public static final String[][] ALIAS_OPTIONS = {
        {"DefaultMaxRAMFraction",   "MaxRAMFraction", "1032"},
        {"CreateMinidumpOnCrash",   "CreateCoredumpOnCrash", "false" },
    };

    static void testAliases(String[][] optionInfo) throws Throwable {
        String aliasNames[]     = new String[optionInfo.length];
        String optionNames[]    = new String[optionInfo.length];
        String expectedValues[] = new String[optionInfo.length];
        for (int i = 0; i < optionInfo.length; i++) {
            aliasNames[i]     = optionInfo[i][0];
            optionNames[i]    = optionInfo[i][1];
            expectedValues[i] = optionInfo[i][2];
        }

        OutputAnalyzer output = CommandLineOptionTest.startVMWithOptions(aliasNames, expectedValues, "-XX:+PrintFlagsFinal");
        CommandLineOptionTest.verifyOptionValuesFromOutput(output, optionNames, expectedValues);
    }

    public static void main(String[] args) throws Throwable {
        testAliases(ALIAS_OPTIONS);
    }
}
