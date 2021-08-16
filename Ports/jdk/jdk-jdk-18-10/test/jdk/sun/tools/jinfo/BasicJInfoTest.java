/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @summary Unit test for jinfo utility
 * @library /test/lib
 * @run main BasicJInfoTest
 */
public class BasicJInfoTest {

    private static ProcessBuilder processBuilder = new ProcessBuilder();

    public static void main(String[] args) throws Exception {
        testJinfoNoArgs();
        testJinfoFlags();
        testJinfoProps();
        testJinfoFlagInvalid();
    }

    private static void testJinfoNoArgs() throws Exception {
        OutputAnalyzer output = jinfo();
        output.shouldContain("-XX");
        output.shouldContain("test.jdk=");
        output.shouldHaveExitValue(0);
    }

    private static void testJinfoFlagInvalid() throws Exception {
        OutputAnalyzer output = jinfo("-flag");
        output.shouldHaveExitValue(1);
    }

    private static void testJinfoFlags() throws Exception {
        OutputAnalyzer output = jinfo("-flags");
        output.shouldContain("-XX");
        output.shouldHaveExitValue(0);
    }

    private static void testJinfoProps() throws Exception {
        OutputAnalyzer output = jinfo("-props");
        output.shouldContain("test.jdk=");
        output.shouldHaveExitValue(0);
    }

    private static OutputAnalyzer jinfo(String... toolArgs) throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jinfo");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        if (toolArgs != null) {
            for (String toolArg : toolArgs) {
                launcher.addToolArg(toolArg);
            }
        }
        launcher.addToolArg(Long.toString(ProcessTools.getProcessId()));

        processBuilder.command(launcher.getCommand());
        System.out.println(Arrays.toString(processBuilder.command().toArray()).replace(",", ""));
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
        System.out.println(output.getOutput());

        return output;
    }

}
