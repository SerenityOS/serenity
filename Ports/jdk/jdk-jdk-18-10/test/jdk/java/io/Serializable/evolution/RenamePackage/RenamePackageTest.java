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

/*
 * @test
 * @bug 4087295 4785472
 * @library /test/lib
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @build RenamePackageTest
 * @run main RenamePackageTest
 * @summary Enable resolveClass() to accommodate package renaming.
 *          This fix enables one to implement a resolveClass method that maps a
 *          Serialiazable class within a serialization stream to the same class
 *          in a different package within the JVM runtime. See run shell script
 *          for instructions on how to run this test.
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;

public class RenamePackageTest {
    public static void main(String args[]) throws Exception {
        setup();

        runTestSerialDriver();
        runInstallSerialDriver();

        runInstallSerialDriver();
        runTestSerialDriver();
    }

    private static final Path SHARE = Paths.get(System.getProperty("test.classes"), "share");
    private static final Path OCLASSES = Paths.get(System.getProperty("test.classes"), "oclasses");
    private static final Path NCLASSES = Paths.get(System.getProperty("test.classes"), "nclasses");

    private static void setup() throws Exception {

        boolean b = CompilerUtils.compile(Paths.get(System.getProperty("test.src"), "extension"),
                                          SHARE);
        assertTrue(b);
        b = CompilerUtils.compile(Paths.get(System.getProperty("test.src"), "test"),
                                  OCLASSES,
                                  "-classpath",
                                  SHARE.toString());
        assertTrue(b);
        b = CompilerUtils.compile(Paths.get(System.getProperty("test.src"), "install"),
                                  NCLASSES,
                                  "-classpath",
                                  SHARE.toString());
        assertTrue(b);
    }

    private static void runTestSerialDriver() throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
                "-classpath",
                SHARE.toString()
                    + File.pathSeparator
                    + OCLASSES.toString(),
                "test.SerialDriver", "-s");
        Process p = ProcessTools.startProcess("test SerialDriver", pb);
        p.waitFor();
        assertTrue(p.exitValue() == 0);
    }

    private static void runInstallSerialDriver() throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
                "-classpath",
                SHARE.toString()
                    + File.pathSeparator
                    + NCLASSES.toString(),
                "install.SerialDriver", "-d");
        Process p = ProcessTools.startProcess("install SerialDriver", pb);
        p.waitFor();
        assertTrue(p.exitValue() == 0);
    }

    private static void assertTrue(boolean b) {
        if (!b) {
            throw new RuntimeException("expected true, get false");
        }
    }
}
