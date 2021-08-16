/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary a jtreg wrapper for gtest tests
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.xml
 * @requires vm.flagless
 * @run main/native GTestWrapper
 */

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GTestWrapper {
    public static void main(String[] args) throws Throwable {
        // gtestLauncher is located in <test_image>/hotspot/gtest/<vm_variant>/
        // nativePath points either to <test_image>/hotspot/jtreg/native or to <test_image>/hotspot/gtest
        Path nativePath = Paths.get(Utils.TEST_NATIVE_PATH);
        String jvmVariantDir = getJVMVariantSubDir();
        // let's assume it's <test_image>/hotspot/gtest
        Path path = nativePath.resolve(jvmVariantDir);
        if (!path.toFile().exists()) {
            // maybe it is <test_image>/hotspot/jtreg/native
            path = nativePath.getParent()
                             .getParent()
                             .resolve("gtest")
                             .resolve(jvmVariantDir);
        }
        if (!path.toFile().exists()) {
            throw new Error("TESTBUG: the library has not been found in " + nativePath + ". Did you forget to use --with-gtest to configure?");
        }

        Path execPath = path.resolve("gtestLauncher" + (Platform.isWindows() ? ".exe" : ""));
        ProcessBuilder pb = new ProcessBuilder();
        Map<String, String> env = pb.environment();

        // The GTestWrapper was started using the normal java launcher, which
        // may have set LD_LIBRARY_PATH or LIBPATH to point to the jdk libjvm. In
        // that case, prepend the path with the location of the gtest library."

        String pathVar = Platform.sharedLibraryPathVariableName();
        String ldLibraryPath = System.getenv(pathVar);
        if (ldLibraryPath != null) {
            env.put(pathVar, path + File.pathSeparator + ldLibraryPath);
        }

        Path resultFile = Paths.get("test_result.xml");

        ArrayList<String> command = new ArrayList<>();
        command.add(execPath.toAbsolutePath().toString());
        command.add("-jdk");
        command.add(Utils.TEST_JDK);
        command.add("--gtest_output=xml:" + resultFile);
        command.add("--gtest_catch_exceptions=0");
        for (String a : args) {
            command.add(a);
        }
        pb.command(command);
        int exitCode = ProcessTools.executeCommand(pb).getExitValue();
        if (exitCode != 0) {
            List<String> failedTests = failedTests(resultFile);
            String message = "gtest execution failed; exit code = " + exitCode + ".";
            if (!failedTests.isEmpty()) {
                message += " the failed tests: " + failedTests;
            }
            throw new AssertionError(message);
        }
    }

    private static List<String> failedTests(Path xml) {
        if (!Files.exists(xml)) {
            System.err.println("WARNING: test result file (" + xml + ") hasn't been found");
        }

        try {
            return new GTestResultParser(xml).failedTests();
        } catch (Throwable t) {
            System.err.println("WARNING: failed to parse result file (" + xml + ") " + t);
            t.printStackTrace();
        }
        return Collections.emptyList();
    }

    private static String getJVMVariantSubDir() {
        if (Platform.isServer()) {
            return "server";
        } else if (Platform.isClient()) {
            return "client";
        } else if (Platform.isMinimal()) {
            return "minimal";
        } else if (Platform.isZero()) {
            return "zero";
        } else {
            throw new Error("TESTBUG: unsupported vm variant");
        }
    }
}
