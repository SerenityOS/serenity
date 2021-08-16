/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7124089 7131021 8042469 8066185 8074373 8258917
 * @summary Checks for Launcher special flags, such as MacOSX specific flags.
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile -XDignore.symbol.file TestSpecialArgs.java EnvironmentVariables.java
 * @run main TestSpecialArgs
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestSpecialArgs extends TestHelper {

    public static void main(String... args) throws Exception {
        new TestSpecialArgs().run(args);
    }

    @Test
    void testDocking() {
        final Map<String, String> envMap = new HashMap<>();
        envMap.put("_JAVA_LAUNCHER_DEBUG", "true");
        TestResult tr = doExec(envMap, javaCmd, "-XstartOnFirstThread", "-version");
        if (isMacOSX) {
            if (!tr.contains("In same thread")) {
                System.out.println(tr);
                throw new RuntimeException("Error: not running in the same thread ?");
            }
            if (!tr.isOK()) {
                System.out.println(tr);
                throw new RuntimeException("Error: arg was rejected ????");
            }
        } else {
            if (tr.isOK()) {
                System.out.println(tr);
                throw new RuntimeException("Error: argument was accepted ????");
            }
        }

        tr = doExec(javaCmd, "-Xdock:/tmp/not-available", "-version");
        if (isMacOSX) {
            if (!tr.isOK()) {
                System.out.println(tr);
                throw new RuntimeException("Error: arg was rejected ????");
            }
        } else {
            if (tr.isOK()) {
                System.out.println(tr);
                throw new RuntimeException("Error: argument was accepted ????");
            }
        }
        // MacOSX specific tests ensue......
        if (!isMacOSX) {
            return;
        }
        Set<String> envToRemove = new HashSet<>();
        Map<String, String> map = System.getenv();
        for (String s : map.keySet()) {
            if (s.startsWith("JAVA_MAIN_CLASS_")
                    || s.startsWith("APP_NAME_")
                    || s.startsWith("APP_ICON_")) {
                envToRemove.add(s);
            }
        }
        runTest(envToRemove, javaCmd, "-cp", TEST_CLASSES_DIR.getAbsolutePath(),
                "EnvironmentVariables", "JAVA_MAIN_CLASS_*",
                "EnvironmentVariables");

        runTest(envToRemove, javaCmd, "-cp", TEST_CLASSES_DIR.getAbsolutePath(),
                "-Xdock:name=TestAppName", "EnvironmentVariables",
                "APP_NAME_*", "TestAppName");

        runTest(envToRemove, javaCmd, "-cp", TEST_CLASSES_DIR.getAbsolutePath(),
                "-Xdock:icon=TestAppIcon", "EnvironmentVariables",
                "APP_ICON_*", "TestAppIcon");
    }

    void runTest(Set<String> envToRemove, String... args) {
        TestResult tr = doExec(null, envToRemove, args);
        if (!tr.isOK()) {
            System.err.println(tr.toString());
            throw new RuntimeException("Test Fails");
        }
    }

    void checkTestResult(TestResult tr) {
        if (!tr.isOK()) {
            System.err.println(tr.toString());
            throw new RuntimeException("Test Fails");
        }
    }
}
