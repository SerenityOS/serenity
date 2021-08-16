/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.Utils;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jtreg.SkippedException;

/**
 * @test
 * @bug 8240990
 * @summary Test clhsdb dumpclass command
 * @requires vm.hasSA
 * @library /test/lib
 * @run driver ClhsdbDumpclass
 */

public class ClhsdbDumpclass {
    static final String APP_DOT_CLASSNAME = LingeredApp.class.getName();
    static final String APP_SLASH_CLASSNAME = APP_DOT_CLASSNAME.replace('.', '/');

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbDumpclass test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();

            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            // Run "dumpclass jdk/test/lib/apps/LingeredApp"
            String cmd = "dumpclass " + APP_DOT_CLASSNAME;
            List<String> cmds = List.of(cmd);
            Map<String, List<String>> unExpStrMap = new HashMap<>();
            unExpStrMap.put(cmd, List.of("class not found"));
            test.run(theApp.getPid(), cmds, null, unExpStrMap);
            File classFile = new File(APP_SLASH_CLASSNAME + ".class");
            if (!classFile.exists()) {
                throw new RuntimeException("FAILED: Cannot find dumped .class file");
            }

            // Run javap on the generated class file to make sure it's valid.
            JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("javap");
            launcher.addVMArgs(Utils.getTestJavaOpts());
            launcher.addToolArg(classFile.toString());
            System.out.println("> javap " + classFile.toString());
            List<String> cmdStringList = Arrays.asList(launcher.getCommand());
            ProcessBuilder pb = new ProcessBuilder(cmdStringList);
            Process javap = pb.start();
            OutputAnalyzer out = new OutputAnalyzer(javap);
            javap.waitFor();
            System.out.println(out.getStdout());
            System.err.println(out.getStderr());
            out.shouldHaveExitValue(0);
            out.shouldMatch("public class " + APP_DOT_CLASSNAME);
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
