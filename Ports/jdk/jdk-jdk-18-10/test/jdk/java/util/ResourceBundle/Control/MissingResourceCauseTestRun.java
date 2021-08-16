/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4354216 8213127
 * @summary Test for the cause support when throwing a
 *          MissingResourceBundle. (This test exists under
 *          ResourceBundle/Control because bad resource bundle data can be
 *          shared with other test cases.)
 * @library /test/lib
 * @build jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Utils
 *        jdk.test.lib.process.ProcessTools
 *        MissingResourceCauseTest
 *        NonResourceBundle
 *        PrivateConstructorRB
 *        AbstractRB
 *        BadStaticInitRB
 *        NoNoArgConstructorRB
 * @run main MissingResourceCauseTestRun
 */

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

public class MissingResourceCauseTestRun {
    public static void main(String[] args) throws Throwable {
        Path path = Paths.get("UnreadableRB.properties");
        Files.deleteIfExists(path);
        try {
            writeFile(path);
            runCmd();
        } finally {
            deleteFile(path);
        }
    }

    /**
     * Create an unreadable properties file
     */
    private static void writeFile(Path path) throws Throwable {
        String str = "type=unreadable";
        Files.createFile(path);
        try (FileWriter fw = new FileWriter(path.toString())) {
            fw.write(str);
        }
        ProcessTools.executeCommand("chmod", "000", path.toString())
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldHaveExitValue(0);
    }

    private static void runCmd() throws Throwable {
        // Class files are in Utils.TEST_CLASSES
        // MalformedDataRB_en.properties is in Utils.TEST_SRC
        // UnreadableRB.properties is in current directory
        String cp = Utils.TEST_CLASSES + File.pathSeparator + Utils.TEST_SRC
                + File.pathSeparator + ".";
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        launcher.addToolArg("-ea")
                .addToolArg("-esa")
                .addToolArg("-cp")
                .addToolArg(cp)
                .addToolArg("MissingResourceCauseTest");

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                .getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Execution of the test failed. "
                    + "Unexpected exit code: " + exitCode);
        }
    }

    private static void deleteFile(Path path) throws Throwable {
        if(path.toFile().exists()) {
            ProcessTools.executeCommand("chmod", "666", path.toString())
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .shouldHaveExitValue(0);
            Files.delete(path);
        }
    }
}
