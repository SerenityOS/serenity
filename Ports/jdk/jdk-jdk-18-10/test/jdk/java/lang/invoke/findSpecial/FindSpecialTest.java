/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8209005 8209078
 * @library /test/lib
 * @build m1/* FindSpecialTest
 * @run testng/othervm FindSpecialTest
 * @summary Test findSpecial and unreflectSpecial of the declaring class
 *          of the method and the special caller are not in the same module
 *          as the lookup class.
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.Test;

public class FindSpecialTest {
    static final String JAVA_LAUNCHER = JDKToolFinder.getJDKTool("java");
    static final String TEST_CLASSES = System.getProperty("test.classes", ".");
    static final String TEST_CLASS_PATH = System.getProperty("test.class.path");
    static final String TEST_MAIN_CLASS = "test.FindSpecial";
    static final String TEST_MODULE = "m1";

    /*
     * Run test.FindSpecial in unnamed module
     */
    @Test
    public static void callerInUnnamedModule() throws Throwable {
        Path m1 = Paths.get(TEST_CLASSES, "modules", TEST_MODULE);
        if (Files.notExists(m1)) {
            throw new Error(m1 + " not exist");
        }
        String classpath = m1.toString() + File.pathSeparator + TEST_CLASS_PATH;
        ProcessTools.executeCommand(JAVA_LAUNCHER, "-cp", classpath, TEST_MAIN_CLASS)
                    .shouldHaveExitValue(0);
    }

    /*
     * Run test.FindSpecial in a named module
     */
    @Test
    public static void callerInNamedModule() throws Throwable {
        Path modules = Paths.get(TEST_CLASSES, "modules");
        if (Files.notExists(modules)) {
            throw new Error(modules + " not exist");
        }
        ProcessTools.executeCommand(JAVA_LAUNCHER,
                                    "-cp", TEST_CLASS_PATH,
                                    "-p", modules.toString(),
                                    "-m", TEST_MODULE + "/" + TEST_MAIN_CLASS)
                    .shouldHaveExitValue(0);
    }
}
