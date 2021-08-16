/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5103449
 * @summary REGRESSION: getResourceAsStream is broken in JDK1.5.0-rc
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        Test
 * @run main/othervm TestDriver
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TestDriver {
    private static final String ARCHIVE_NAME = "test.jar";
    private static final String TEST_NAME = "Test";
    private static final String POLICY_FILE = "policy";
    public static void main(String[] args)
            throws Throwable {

        Path userDir = Paths.get(System.getProperty("user.dir"));
        String java = JDKToolFinder.getTestJDKTool("java");
        String basename = userDir.getFileName().toString();
        setup(userDir);
        ProcessBuilder[] tests = new ProcessBuilder[]{
                new ProcessBuilder(
                        java, TEST_NAME, "./" + ARCHIVE_NAME
                ),
                new ProcessBuilder(
                        java, "-cp", ".",
                        "-Djava.security.policy=file:./policy",
                        "-Djava.security.manager",
                        TEST_NAME, "./" + ARCHIVE_NAME
                ),
                new ProcessBuilder(
                        java, "-cp", ".",
                        "-Djava.security.policy=file:./policy",
                        "-Djava.security.manager",
                        TEST_NAME, "./" + ARCHIVE_NAME
                ),
                new ProcessBuilder(
                        java, "-cp", "..",
                        "-Djava.security.policy=file:../policy",
                        "-Djava.security.manager",
                        TEST_NAME, "../" + ARCHIVE_NAME
                ).directory(userDir.resolve("tmp").toFile()),
                new ProcessBuilder(
                        java, "-cp", basename,
                        "-Djava.security.policy=file:" + basename + "/policy",
                        "-Djava.security.manager",
                        TEST_NAME, basename + "/" + ARCHIVE_NAME
                ).directory(userDir.resolve("..").toFile())};
        for (ProcessBuilder test : tests) {
            runTest(test);
        }
    }

    private static void setup(Path userDir) throws IOException {
        Path src = Paths.get(System.getProperty("test.src"));
        Path testJar = src.resolve(ARCHIVE_NAME);
        Path policy = src.resolve(POLICY_FILE);
        Path testClass = Paths.get(System.getProperty("test.classes"),
                                   TEST_NAME + ".class");
        Files.copy(testJar, userDir.resolve(ARCHIVE_NAME), REPLACE_EXISTING);
        Files.copy(policy, userDir.resolve(POLICY_FILE), REPLACE_EXISTING);
        Files.copy(testClass, userDir.resolve(TEST_NAME + ".class"),
                   REPLACE_EXISTING);
        Files.createDirectories(userDir.resolve("tmp"));
    }

    private static void runTest(ProcessBuilder pb) throws Exception {
        System.out.println("Testing with command: [" + pb.command() + "]");
        ProcessTools.executeProcess(pb)
                    .outputTo(System.out)
                    .errorTo(System.err)
                    .shouldHaveExitValue(0);
    }
}
