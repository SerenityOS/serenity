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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.apps.LingeredApp;

/*
 * @test
 * @summary Unit test for jinfo utility
 *
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jcmd
 *
 * @run main JInfoTest
 */
public class JInfoTest {

    private static ProcessBuilder processBuilder = new ProcessBuilder();

    public static void main(String[] args) throws Exception {
        classNameMatch();
        setMultipleFlags();
        setFlag();
    }

    private static void setFlag() throws Exception {
        System.out.println("#### setFlag ####");
        LingeredApp app1 = new JInfoTestLingeredApp();
        LingeredApp app2 = new JInfoTestLingeredApp();
        try {
            String[] params = new String[0];;
            LingeredApp.startAppExactJvmOpts(app1, params);
            LingeredApp.startAppExactJvmOpts(app2, params);
            OutputAnalyzer output = jinfo("-flag", "MinHeapFreeRatio=1", "JInfoTestLingeredApp");
            output.shouldHaveExitValue(0);
            output = jinfo("-flag", "MinHeapFreeRatio", "JInfoTestLingeredApp");
            output.shouldHaveExitValue(0);
            documentMatch(output.getStdout(), ".*MinHeapFreeRatio=1.*MinHeapFreeRatio=1.*");
        } finally {
            // LingeredApp.stopApp can throw an exception
            try {
                JInfoTestLingeredApp.stopApp(app1);
            } finally {
                JInfoTestLingeredApp.stopApp(app2);
            }
        }
    }

    private static void setMultipleFlags() throws Exception {
        System.out.println("#### setMultipleFlags ####");
        OutputAnalyzer output = jinfo("-sysprops", "-flag", "MinHeapFreeRatio=1", "-flags", "JInfoTestLingeredApp");
        output.shouldHaveExitValue(1);
    }

    private static void classNameMatch() throws Exception {
        System.out.println("#### classNameMatch ####");
        LingeredApp app1 = new JInfoTestLingeredApp();
        LingeredApp app2 = new JInfoTestLingeredApp();
        try {
            String[] params = new String[0];
            LingeredApp.startAppExactJvmOpts(app1, params);
            LingeredApp.startAppExactJvmOpts(app2, params);
            OutputAnalyzer output = jinfo("JInfoTestLingeredApp");
            output.shouldHaveExitValue(0);
            // "Runtime Environment" written once per proc
            documentMatch(output.getStdout(), ".*Runtime Environment.*Runtime Environment.*");
        } finally {
            // LingeredApp.stopApp can throw an exception
            try {
                JInfoTestLingeredApp.stopApp(app1);
            } finally {
                JInfoTestLingeredApp.stopApp(app2);
            }
        }
    }

    private static void documentMatch(String data, String pattern){
        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(data);
        if (!matcher.find()) {
            throw new RuntimeException("'" + pattern + "' missing from stdout \n");
        }
    }

    private static OutputAnalyzer jinfo(String... toolArgs) throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jinfo");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        if (toolArgs != null) {
            for (String toolArg : toolArgs) {
                launcher.addToolArg(toolArg);
            }
        }

        processBuilder.command(launcher.getCommand());
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);

        return output;
    }
}

// Sometime there is LingeredApp's from other test still around
class JInfoTestLingeredApp extends LingeredApp {
}
