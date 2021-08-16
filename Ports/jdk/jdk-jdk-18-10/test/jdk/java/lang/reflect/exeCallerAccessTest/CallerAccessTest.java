/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/**
 * @test
 * @bug 8221530
 * @summary Test uses custom launcher that starts VM using JNI that verifies
 *          reflection API with null caller class
 * @library /test/lib
 * @requires os.family != "aix"
 * @run main/native CallerAccessTest
 */

// Test disabled on AIX since we cannot invoke the JVM on the primordial thread.

import java.io.File;
import java.util.Map;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallerAccessTest {
    public static void main(String[] args) throws IOException {
        Path launcher = Paths.get(System.getProperty("test.nativepath"), "CallerAccessTest");
        ProcessBuilder pb = new ProcessBuilder(launcher.toString());
        Map<String, String> env = pb.environment();

        String libDir = Platform.libDir().toString();
        String vmDir = Platform.jvmLibDir().toString();

        // set up shared library path
        String sharedLibraryPathEnvName = Platform.sharedLibraryPathVariableName();
        env.compute(sharedLibraryPathEnvName,
                    (k, v) -> (v == null) ? libDir : v + File.pathSeparator + libDir);
        env.compute(sharedLibraryPathEnvName,
                    (k, v) -> (v == null) ? vmDir : v + File.pathSeparator + vmDir);

        System.out.println("Launching: " + launcher + " shared library path: " +
                           env.get(sharedLibraryPathEnvName));
        new OutputAnalyzer(pb.start()).shouldHaveExitValue(0);
    }
}

