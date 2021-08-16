/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7029048 8217340 8217216
 * @summary Ensure that the launcher defends against user settings of the
 *          LD_LIBRARY_PATH environment variable on Unixes
 * @requires os.family != "windows" & os.family != "mac" & !vm.musl & os.family != "aix"
 * @library /test/lib
 * @compile -XDignore.symbol.file ExecutionEnvironment.java Test7029048.java
 * @run main/othervm -DexpandedLdLibraryPath=false Test7029048
 */

/**
 * @test
 * @bug 7029048 8217340 8217216
 * @summary Ensure that the launcher defends against user settings of the
 *          LD_LIBRARY_PATH environment variable on Unixes
 * @requires os.family == "aix" | vm.musl
 * @library /test/lib
 * @compile -XDignore.symbol.file ExecutionEnvironment.java Test7029048.java
 * @run main/othervm -DexpandedLdLibraryPath=true Test7029048
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test7029048 extends TestHelper {

    private static final String LIBJVM = ExecutionEnvironment.LIBJVM;
    private static final String LD_LIBRARY_PATH =
            ExecutionEnvironment.LD_LIBRARY_PATH;
    private static final String LD_LIBRARY_PATH_64 =
            ExecutionEnvironment.LD_LIBRARY_PATH_64;

    private static final File libDir =
            new File(System.getProperty("sun.boot.library.path"));
    private static final File srcServerDir = new File(libDir, "server");
    private static final File srcLibjvmSo = new File(srcServerDir, LIBJVM);

    private static final File dstLibDir = new File("lib");
    private static final File dstServerDir = new File(dstLibDir, "server");
    private static final File dstServerLibjvm = new File(dstServerDir, LIBJVM);

    private static final File dstClientDir = new File(dstLibDir, "client");
    private static final File dstClientLibjvm = new File(dstClientDir, LIBJVM);

    static final boolean IS_EXPANDED_LD_LIBRARY_PATH =
            Boolean.getBoolean("expandedLdLibraryPath");

    static String getValue(String name, List<String> in) {
        for (String x : in) {
            String[] s = x.split("=");
            if (name.equals(s[0].trim())) {
                return s[1].trim();
            }
        }
        return null;
    }

    static boolean run(int nLLPComponents, File variantDir, String caseID) {

        Map<String, String> env = new HashMap<>();
        env.put(LD_LIBRARY_PATH, variantDir.getAbsolutePath());
        env.put(ExecutionEnvironment.JLDEBUG_KEY, "true");
        List<String> cmdsList = new ArrayList<>();
        cmdsList.add(javaCmd);
        cmdsList.add("-server");
        cmdsList.add("-jar");
        cmdsList.add(ExecutionEnvironment.testJarFile.getAbsolutePath());
        String[] cmds = new String[cmdsList.size()];
        TestResult tr = doExec(env, cmdsList.toArray(cmds));
        System.out.println(tr);
        int len = getLLPComponents(tr);
        if (len == nLLPComponents) {
            System.out.printf("Test7029048 OK %s%n", caseID);
            return true;
        } else {
            System.out.printf("Test7029048 FAIL %s: expected %d but got %d%n",
                    caseID, nLLPComponents, len);
            return false;
        }
    }

    static int getLLPComponents(TestResult tr) {
        String envValue = getValue(LD_LIBRARY_PATH, tr.testOutput);
       /*
        * the envValue can never be null, since the test code should always
        * print a "null" string.
        */
        if (envValue == null) {
            throw new RuntimeException("NPE, likely a program crash ??");
        }

        if (envValue.equals("null")) {
            return 0;
        }

        return envValue.split(File.pathSeparator).length;
    }

    /*
     * Describe the cases that we test.  Each case sets the environment
     * variable LD_LIBRARY_PATH to a different value.  The value associated
     * with a case is the number of path elements that we expect the launcher
     * to add to that variable.
     */
    private static enum TestCase {
        NO_DIR(0),                      // Directory does not exist
        NO_LIBJVM(0),                   // Directory exists, but no libjvm.so
        LIBJVM(3);                      // Directory exists, with a libjvm.so
        private final int value;
        TestCase(int i) {
            this.value = i;
        }
    }

    /*
     * test for 7029048
     */
    static boolean runTest() throws IOException {
        String desc = null;
        boolean pass = true;
        for (TestCase v : TestCase.values()) {
            switch (v) {
                case LIBJVM:
                    // copy the files into the directory structures
                    copyFile(srcLibjvmSo, dstServerLibjvm);
                    // does not matter if it is client or a server
                    copyFile(srcLibjvmSo, dstClientLibjvm);
                    desc = "LD_LIBRARY_PATH should be set";
                    break;
                case NO_LIBJVM:
                    if (!dstClientDir.exists()) {
                        Files.createDirectories(dstClientDir.toPath());
                    } else {
                        Files.deleteIfExists(dstClientLibjvm.toPath());
                    }

                    if (!dstServerDir.exists()) {
                        Files.createDirectories(dstServerDir.toPath());
                    } else {
                        Files.deleteIfExists(dstServerLibjvm.toPath());
                    }

                    desc = "LD_LIBRARY_PATH should not be set (no libjvm.so)";
                    if (IS_EXPANDED_LD_LIBRARY_PATH) {
                        printSkipMessage(desc);
                        continue;
                    }
                    break;
                case NO_DIR:
                    if (dstLibDir.exists()) {
                        recursiveDelete(dstLibDir);
                    }
                    desc = "LD_LIBRARY_PATH should not be set (no directory)";
                    if (IS_EXPANDED_LD_LIBRARY_PATH) {
                        printSkipMessage(desc);
                        continue;
                    }
                    break;
                default:
                    throw new RuntimeException("unknown case");
            }

            // Add one to account for our setting
            int nLLPComponents = v.value + 1;

            /*
             * Case 1: set the server path
             */
            boolean pass1 = run(nLLPComponents, dstServerDir, "Case 1: " + desc);

            /*
             * Case 2: repeat with client path
             */
            boolean pass2 = run(nLLPComponents, dstClientDir, "Case 2: " + desc);

            pass &= pass1 && pass2;
        }
        return pass;
    }

    private static void printSkipMessage(String description) {
        System.out.printf("Skipping test case '%s' because the Aix and musl launchers" +
                          " add the paths in any case.%n", description);
    }

    public static void main(String... args) throws Exception {
        if (!TestHelper.haveServerVM) {
            System.out.println("Note: test relies on server vm, not found, exiting");
            return;
        }
        // create our test jar first
        ExecutionEnvironment.createTestJar();

        if (!runTest()) {
            throw new Exception("Test7029048 fails");
        }
    }
}
