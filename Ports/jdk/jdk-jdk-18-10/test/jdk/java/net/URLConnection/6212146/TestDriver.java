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
 * @bug 6212146
 * @summary URLConnection.connect() fails on JAR Entry it creates
 * file handler leak
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
    private static final String BASE_DIR = System.getProperty("user.dir")
            +  "/jars/";
    private static final String ARCHIVE_NAME = "test.jar";
    private static final String CMD_ULIMIT = "ulimit -n 300;";

    public static void main(String[] args)
            throws Throwable {
        setup(BASE_DIR);
        String testCMD = CMD_ULIMIT + JDKToolFinder.getTestJDKTool("java")
                + " Test " + BASE_DIR + " " + ARCHIVE_NAME;
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        if (isWindows) {
            testCMD = testCMD.replace("\\", "/");
        }
        ProcessTools.executeCommand("sh", "-c", testCMD)
                    .outputTo(System.out)
                    .errorTo(System.err)
                    .shouldHaveExitValue(0);
    }

    private static void setup(String baseDir) throws IOException {
        Path testJar = Paths.get(System.getProperty("test.src"), ARCHIVE_NAME);
        Path targetDir = Paths.get(baseDir);
        Files.createDirectories(targetDir);
        Files.copy(testJar, targetDir.resolve(ARCHIVE_NAME), REPLACE_EXISTING);
    }
}
