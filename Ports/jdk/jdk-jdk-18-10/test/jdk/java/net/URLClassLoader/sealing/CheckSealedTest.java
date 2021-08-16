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
 * @bug 4244970
 * @summary Test to see if sealing violation is detected correctly
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main CheckSealedTest
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CheckSealedTest {
    private static final String ARCHIVE_NAME = "b.jar";
    private static final String TEST_NAME = "CheckSealed";
    public static void main(String[] args)
            throws Throwable {

        String baseDir = System.getProperty("user.dir") + File.separator;
        String javac = JDKToolFinder.getTestJDKTool("javac");
        String java = JDKToolFinder.getTestJDKTool("java");

        setup(baseDir);
        String srcDir = System.getProperty("test.src");
        String cp = srcDir + File.separator + "a" + File.pathSeparator
                + srcDir + File.separator + "b.jar" + File.pathSeparator
                + ".";
        List<String[]> allCMDs = List.of(
                // Compile command
                new String[]{
                        javac, "-cp", cp, "-d", ".",
                        srcDir + File.separator + TEST_NAME + ".java"
                },
                // Run test the first time
                new String[]{
                        java, "-cp", cp, TEST_NAME, "1"
                },
                // Run test the second time
                new String[]{
                        java, "-cp", cp, TEST_NAME, "2"
                }
        );

        for (String[] cmd : allCMDs) {
            ProcessTools.executeCommand(cmd)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .shouldHaveExitValue(0);
        }
    }

    private static void setup(String baseDir) throws IOException {
        Path testJar = Paths.get(System.getProperty("test.src"), ARCHIVE_NAME);
        Files.copy(testJar, Paths.get(baseDir, ARCHIVE_NAME), REPLACE_EXISTING);
    }
}
