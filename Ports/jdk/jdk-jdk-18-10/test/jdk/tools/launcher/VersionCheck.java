/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6545058 6611182 8016209 8139986 8162746
 * @summary validate and test -version, -fullversion, and internal, as well as
 *          sanity checks if a tool can be launched.
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile VersionCheck.java
 * @run main VersionCheck
 */

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VersionCheck extends TestHelper {

    // tools that do not accept -J-option
    static final String[] BLACKLIST_JOPTION = {
        "controlpanel",
        "jabswitch",
        "java-rmi",
        "java-rmi.cgi",
        "java",
        "javacpl",
        "jaccessinspector",
        "jaccessinspector-32",
        "jaccesswalker",
        "jaccesswalker-32",
        "javaw",
        "javaws",
        "jcontrol",
        "jmc",
        "jmc.ini",
        "jweblauncher",
        "jpackage",
        "ssvagent"
    };

    // tools that do not accept -version
    static final String[] BLACKLIST_VERSION = {
        "appletviewer",
        "controlpanel",
        "jaccessinspector",
        "jaccessinspector-32",
        "jaccesswalker",
        "jaccesswalker-32",
        "jar",
        "jarsigner",
        "java-rmi",
        "java-rmi.cgi",
        "javadoc",
        "javacpl",
        "javaws",
        "jcmd",
        "jconsole",
        "jcontrol",
        "jdeprscan",
        "jdeps",
        "jfr",
        "jimage",
        "jinfo",
        "jlink",
        "jmap",
        "jmod",
        "jmc",
        "jmc.ini",
        "jps",
        "jrunscript",
        "jjs",
        "jstack",
        "jstat",
        "jstatd",
        "jweblauncher",
        "keytool",
        "kinit",
        "klist",
        "ktab",
        "jpackage",
        "rmiregistry",
        "serialver",
        "servertool",
        "ssvagent"
    };

    // expected reference strings
    static String refVersion;
    static String refFullVersion;

    static String getAllVersionLines(String... argv) {
        return getVersion0(true, argv);
    }

    static String getVersion(String... argv) {
        return getVersion0(false, argv);
    }

    static String getVersion0(boolean allLines, String... argv) {
        TestHelper.TestResult tr = doExec(argv);
        StringBuilder out = new StringBuilder();
        // remove the HotSpot line
        for (String x : tr.testOutput) {
            if (allLines || !x.matches(".*Client.*VM.*|.*Server.*VM.*")) {
                out = out.append(x + "\n");
            }
        }
        return out.toString();
    }

    /*
     * Checks if the tools accept "-version" option (exit code is zero).
     * The output of the tools run with "-version" is not verified.
     */
    static String testToolVersion() {
        System.out.println("=== testToolVersion === ");
        Set<String> failed = new HashSet<>();
        for (File f : new File(JAVA_BIN).listFiles(new ToolFilter(BLACKLIST_VERSION))) {
            String x = f.getAbsolutePath();
            TestResult tr = doExec(x, "-version");
            System.out.println("Testing " + f.getName());
            System.out.println("#> " + x + " -version");
            tr.testOutput.forEach(System.out::println);
            System.out.println("#> echo $?");
            System.out.println(tr.exitValue);
            if (!tr.isOK()) {
                System.out.println("failed");
                failed.add(f.getName());
            }
        }
        if (failed.isEmpty()) {
            System.out.println("testToolVersion passed");
            return "";
        } else {
            System.out.println("testToolVersion failed");
            return "testToolVersion: " + failed + "; ";
        }

    }

    static String testJVersionStrings() {
        System.out.println("=== testJVersionStrings === ");
        Set<String> failed = new HashSet<>();
        for (File f : new File(JAVA_BIN).listFiles(new ToolFilter(BLACKLIST_JOPTION))) {
            System.out.println("Testing " + f.getName());
            String x = f.getAbsolutePath();
            String testStr = getVersion(x, "-J-version");
            if (refVersion.compareTo(testStr) != 0) {
                failed.add(f.getName());
                System.out.println("Error: " + x +
                                   " fails -J-version comparison");
                System.out.println("Expected:");
                System.out.print(refVersion);
                System.out.println("Actual:");
                System.out.print(testStr);
            }

            testStr = getVersion(x, "-J-fullversion");
            if (refFullVersion.compareTo(testStr) != 0) {
                failed.add(f.getName());
                System.out.println("Error: " + x +
                                   " fails -J-fullversion comparison");
                System.out.println("Expected:");
                System.out.print(refFullVersion);
                System.out.println("Actual:");
                System.out.print(testStr);
            }
        }
        if (failed.isEmpty()) {
            System.out.println("testJVersionStrings passed");
            return "";
        } else {
            System.out.println("testJVersionStrings failed");
            return "testJVersionStrings: " + failed + "; ";
        }
    }

    static String testInternalStrings() {
        System.out.println("=== testInternalStrings === ");
        String bStr = refVersion.substring(refVersion.indexOf("build") +
                                           "build".length() + 1,
                                           refVersion.lastIndexOf(")"));

        String expectedFullVersion = "fullversion:" + bStr;

        Map<String, String> envMap = new HashMap<>();
        envMap.put(TestHelper.JLDEBUG_KEY, "true");
        TestHelper.TestResult tr = doExec(envMap, javaCmd, "-version");
        List<String> alist = new ArrayList<>();
        tr.testOutput.stream().map(String::trim).forEach(alist::add);

        if (alist.contains(expectedFullVersion)) {
            System.out.println("testInternalStrings passed");
            return "";
        } else {
            System.out.println("Error: could not find " + expectedFullVersion);
            tr.testOutput.forEach(System.out::println);
            System.out.println("testInternalStrings failed");
            return "testInternalStrings; ";
        }
    }

    static String testDebugVersion() {
        System.out.println("=== testInternalStrings === ");
        String jdkType = System.getProperty("jdk.debug", "release");
        String versionLines = getAllVersionLines(javaCmd, "-version");
        if ("release".equals(jdkType)) {
            jdkType = "";
        } else {
            jdkType = jdkType + " ";
        }

        String tofind = "(" + jdkType + "build";

        int idx = versionLines.indexOf(tofind);
        if (idx < 0) {
            System.out.println("versionLines " + versionLines);
            System.out.println("Did not find first instance of " + tofind);
            return "testDebugVersion; ";
        }
        idx =  versionLines.indexOf(tofind, idx + 1);
        if (idx < 0) {
            System.out.println("versionLines " + versionLines);
            System.out.println("Did not find second instance of " + tofind);
            return "testDebugVersion; ";
        }
        System.out.println("testDebugVersion passed");
        return "";
    }

    // Initialize
    static void init() {
        refVersion = getVersion(javaCmd, "-version");
        refFullVersion = getVersion(javaCmd, "-fullversion");
    }

    public static void main(String[] args) {
        init();
        String errorMessage = "";
        errorMessage += testJVersionStrings();
        errorMessage += testInternalStrings();
        errorMessage += testToolVersion();
        errorMessage += testDebugVersion();
        if (errorMessage.isEmpty()) {
            System.out.println("All Version string comparisons: PASS");
        } else {
            throw new AssertionError("VersionCheck failed: " + errorMessage);
        }
    }
}
