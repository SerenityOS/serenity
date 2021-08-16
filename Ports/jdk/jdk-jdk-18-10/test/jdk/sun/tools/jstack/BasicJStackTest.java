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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.JDKToolLauncher;

/*
 * @test
 * @summary Unit test for jstack utility
 * @library /test/lib
 * @run main BasicJStackTest
 */
public class BasicJStackTest {

    private static ProcessBuilder processBuilder = new ProcessBuilder();

    public static void main(String[] args) throws Exception {
        testJstackNoArgs();
        testJstack_l();
        testJstackUTF8Encoding();
    }

    private static void testJstackNoArgs() throws Exception {
        String marker = "testJstackNoArgs";
        OutputAnalyzer output = jstack(marker);
        output.shouldHaveExitValue(0);
        output.shouldContain(marker);
    }

    private static void testJstack_l() throws Exception {
        String marker = "testJstack_l";
        OutputAnalyzer output = jstack(marker, "-l");
        output.shouldHaveExitValue(0);
        output.shouldContain(marker);
    }

    private static void testJstackUTF8Encoding() throws Exception {
        String marker = "markerName" + "\u00e4\u0bb5".repeat(60);
        OutputAnalyzer output = jstack(marker);
        output.shouldHaveExitValue(0);
        output.shouldContain(marker);
    }

    private static OutputAnalyzer jstack(String marker, String... toolArgs) throws Exception {
        Charset cs = StandardCharsets.UTF_8;
        Thread.currentThread().setName(marker);
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jstack");
        launcher.addVMArgs(Utils.getFilteredTestJavaOpts("-XX:+UsePerfData"));
        launcher.addVMArg("-XX:+UsePerfData");
        launcher.addVMArg("-Dfile.encoding=" + cs);
        if (toolArgs != null) {
            for (String toolArg : toolArgs) {
                launcher.addToolArg(toolArg);
            }
        }
        launcher.addToolArg(Long.toString(ProcessTools.getProcessId()));

        processBuilder.command(launcher.getCommand());
        System.out.println(Arrays.toString(processBuilder.command().toArray()).replace(",", ""));
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder, null, cs);
        System.out.println(output.getOutput());

        return output;
    }

}
