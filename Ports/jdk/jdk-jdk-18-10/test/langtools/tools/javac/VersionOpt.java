/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6728697
 * @summary tools/javac/versionOpt.sh fails on OpenJDK builds
 * Test checks the version strings displayed by javac, using
 * strings that come out of the Java runtime.
 * @modules jdk.compiler
 */

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

public class VersionOpt {
    public static void main(String... args) throws Exception {
        new VersionOpt().run();
    }

    void run() throws Exception {
        // Test functions by comparing the version string from javac against
        // a "golden" version generated automatically from the underlying JVM.
        // As such, it is only effective in testing the "standard" compiler,
        // and not any development version being tested via --patch-modules.
        // Check the version of the compiler being used, and let the test pass
        // automatically if is is a development version.
        Class<?> javacClass = com.sun.tools.javac.Main.class;
        URL location = javacClass.getProtectionDomain().getCodeSource().getLocation();
        if (!location.toString().equals("jrt:/jdk.compiler")) {
            System.err.println("javac not found in system image: " + location);
            System.err.println("rest of test skipped");
            return;
        }

        System.out.println("javac found in " + location);

        String javaVersion = System.getProperty("java.version");
        String javaRuntimeVersion = System.getProperty("java.runtime.version");
        System.out.println("java.version: " + javaVersion);
        System.out.println("java.runtime.version: " + javaRuntimeVersion);

        StringWriter sw = new StringWriter();
        com.sun.tools.javac.Main.compile(new String[] { "-version" }, new PrintWriter(sw));
        String javacVersion = sw.toString().trim();

        sw = new StringWriter();
        com.sun.tools.javac.Main.compile(new String[] { "-fullversion" }, new PrintWriter(sw));
        String javacFullVersion = sw.toString().trim();
        System.out.println("javac -version: " + javacVersion);
        System.out.println("javac -fullversion: " + javacFullVersion);

        checkEqual("javac -version", javacVersion, "javac " + javaVersion);
        checkEqual("javac -fullversion", javacFullVersion, "javac full version \"" + javaRuntimeVersion + "\"");

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void checkEqual(String kind, String found, String expect) {
        if (!found.equals(expect)) {
            System.err.println("error: unexpected value for " + kind);
            System.err.println("expect: >>" + expect + "<<");
            System.err.println(" found: >>" + found + "<<");
            errors++;
        }
    }

    int errors;
}
