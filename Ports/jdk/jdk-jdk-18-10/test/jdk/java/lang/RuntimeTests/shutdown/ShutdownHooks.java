/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6829503
 * @summary  1) Test Console and DeleteOnExitHook can be initialized
 *              while shutdown is in progress
 *           2) Test if files that are added by the application shutdown
 *              hook are deleted on exit during shutdown
 * @library /test/lib
 * @run testng/othervm ShutdownHooks
 */

import jdk.test.lib.process.ProcessTools;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Files;

import static jdk.test.lib.Asserts.assertFalse;

public class ShutdownHooks {

    private static final String TEST_FILE_NAME = "fileToBeDeleted";

    private static final File TEST_FILE = new File(TEST_FILE_NAME);

    private static final String TEST_CLASSES = System.getProperty(
            "test.classes", ".");

    @BeforeTest
    public static void setUp() throws Exception {
        // Make sure file does not exist before test
        Files.deleteIfExists(TEST_FILE.toPath());
    }

    @Test
    public void testShutdownHooks() throws Exception {
        // Run in a new process in order to evaluate shutdown hook results
        String[] testCommand = new String[] {"-classpath", TEST_CLASSES,
                ShutdownHooksProcess.class.getName()};
        ProcessTools.executeTestJvm(testCommand).shouldHaveExitValue(0);

        String errorMsg = "File exists despite shutdown hook has been run";
        assertFalse(Files.exists(TEST_FILE.toPath()), errorMsg);
    }

    // This class main will be the entry point for test subprocesses
    static class ShutdownHooksProcess {
        public static void main(String[] args) throws Exception {
            Runtime.getRuntime().addShutdownHook(new Cleaner());

            System.out.println("Writing to "+ TEST_FILE);
            try (PrintWriter pw = new PrintWriter(TEST_FILE)) {
                pw.println("Shutdown begins");
            }
        }

        static class Cleaner extends Thread {
            public void run() {
                // register the Console's shutdown hook while the application
                // shutdown hook is running
                Console cons = System.console();
                // register the DeleteOnExitHook while the application
                // shutdown hook is running
                TEST_FILE.deleteOnExit();
                try (PrintWriter pw = new PrintWriter(TEST_FILE)) {
                    pw.println("File is being deleted");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
