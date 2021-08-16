/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8266459 8268349 8269543
 * @summary check various warnings
 * @library /test/lib
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SecurityManagerWarnings {

    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            Files.writeString(Path.of("policy"), """
                    grant {
                        permission java.lang.RuntimePermission "setIO";
                        permission java.lang.RuntimePermission "createSecurityManager";
                        permission java.lang.RuntimePermission "setSecurityManager";
                    };
                    """);

            String testClasses = System.getProperty("test.classes");

            allowTest(null, testClasses);
            allowTest("allow", testClasses);
            disallowTest("disallow", testClasses);
            enableTest("", testClasses);
            enableTest("default", testClasses);
            enableTest("java.lang.SecurityManager", testClasses);

            JarUtils.createJarFile(Path.of("a.jar"),
                    Path.of(testClasses),
                    Path.of("SecurityManagerWarnings.class"),
                    Path.of("A.class"),
                    Path.of("B.class"));

            allowTest(null, "a.jar");
        } else {
            System.out.println("SM is enabled: " + (System.getSecurityManager() != null));
            PrintStream oldErr = System.err;
            // Modify System.err, thus make sure warnings are always printed
            // to the original System.err and will not be swallowed.
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            try {
                // Run A.run() twice will show only one warning
                // (setSecurityManager(null) to ensure the next set is permitted)
                // Run B.run() and a new warning will appear
                A.run();    // System.setSecurityManager(null);
                A.run();    // System.setSecurityManager(null);
                B.run();    // System.setSecurityManager(new SecurityManager());
            } catch (Exception e) {
                // Exception messages must show in original stderr
                e.printStackTrace(oldErr);
                throw e;
            }
        }
    }

    // When SM is allowed, no startup warning, has setSM warning
    static void allowTest(String prop, String cp) throws Exception {
        checkInstallMessage(run(prop, cp), cp)
                .shouldHaveExitValue(0)
                .stdoutShouldContain("SM is enabled: false")
                .shouldNotContain("A command line option");
    }

    // When SM is disallowed, no startup warning, setSM fails
    static void disallowTest(String prop, String cp) throws Exception {
        run(prop, cp)
                .shouldNotHaveExitValue(0)
                .stdoutShouldContain("SM is enabled: false")
                .shouldNotContain("A command line option")
                .shouldNotContain("A terminally deprecated method")
                .stderrShouldContain("UnsupportedOperationException: The Security Manager is deprecated and will be removed in a future release");
    }

    // When SM is allowed, has startup warning, has setSM warning
    static void enableTest(String prop, String cp) throws Exception {
        checkInstallMessage(run(prop, cp), cp)
                .shouldHaveExitValue(0)
                .stdoutShouldContain("SM is enabled: true")
                .stderrShouldContain("WARNING: A command line option has enabled the Security Manager")
                .stderrShouldContain("WARNING: The Security Manager is deprecated and will be removed in a future release");
    }

    // Check the setSM warning
    static OutputAnalyzer checkInstallMessage(OutputAnalyzer oa, String cp) {
        String uri = new File(cp).toURI().toString();
        return oa
                .stderrShouldContain("WARNING: A terminally deprecated method in java.lang.System has been called")
                .stderrShouldContain("WARNING: System::setSecurityManager has been called by A (" + uri + ")")
                .stderrShouldContain("WARNING: System::setSecurityManager has been called by B (" + uri + ")")
                .stderrShouldContain("WARNING: Please consider reporting this to the maintainers of A")
                .stderrShouldContain("WARNING: Please consider reporting this to the maintainers of B")
                .stderrShouldContain("WARNING: System::setSecurityManager will be removed in a future release")
                .stderrShouldNotMatch("(?s)by A.*by A");    // "by A" appears only once
    }

    static OutputAnalyzer run(String prop, String cp) throws Exception {
        ProcessBuilder pb;
        if (prop == null) {
            pb = new ProcessBuilder(
                    JDKToolFinder.getJDKTool("java"),
                    "-cp", cp,
                    "SecurityManagerWarnings", "run");
        } else {
            pb = new ProcessBuilder(
                    JDKToolFinder.getJDKTool("java"),
                    "-cp", cp,
                    "-Djava.security.manager=" + prop,
                    "-Djava.security.policy=policy",
                    "SecurityManagerWarnings", "run");
        }
        return ProcessTools.executeProcess(pb)
                .stderrShouldNotContain("AccessControlException");
    }
}

class A {
    static void run() {
        System.setSecurityManager(null);
    }
}

class B {
    static void run() {
        System.setSecurityManager(new SecurityManager());
    }
}
