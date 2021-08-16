/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.spi.AttachProvider;

/*
 * @test
 * @bug 6173612 6273707 6277253 6335921 6348630 6342019 6381757
 * @summary Basic unit tests for the VM attach mechanism. The test will attach
 * and detach to/from the running Application.
 *
 * @library /test/lib
 * @modules jdk.attach
 *          jdk.jartool/sun.tools.jar
 *
 * @run build SimpleProvider
 * @run main ProviderTest
 */
public class ProviderTest {

    /*
     * The actual tests are in the nested class TestMain below.
     * The responsibility of this class is to:
     * 1. Build the needed jar.
     * 2. Run tests in ProviderTest.TestMain.
     */
    public static void main(String args[]) throws Throwable {
        try {
            buildJar();
            runTests();
        } catch (Throwable t) {
            System.out.println("TestProvider got unexpected exception: " + t);
            t.printStackTrace();
            throw t;
        }
    }

    /**
     * Runs the actual tests in the nested class TestMain.
     * We need to run the tests in a separate process,
     * because we need to add to the classpath.
     */
    private static void runTests() throws Throwable {
        final String sep = File.separator;
        String testClassPath = System.getProperty("test.class.path", "");
        String testClasses = System.getProperty("test.classes", "") + sep;
        String jdkLib = System.getProperty("test.jdk", ".") + sep + "lib" + sep;

        // Need to add SimpleProvider.jar to classpath.
        String classpath =
                testClassPath + File.pathSeparator +
                testClasses + "SimpleProvider.jar";

        String[] args = {
                "-classpath",
                classpath,
                "ProviderTest$TestMain" };
        OutputAnalyzer output = ProcessTools.executeTestJvm(args);
        output.shouldHaveExitValue(0);
    }

    /**
     * Will build the SimpleProvider.jar.
     */
    private static void buildJar() throws Throwable {
        final String sep = File.separator;
        String testClasses = System.getProperty("test.classes", "?") + sep;
        String testSrc = System.getProperty("test.src", "?") + sep;
        String serviceDir = "META-INF" + sep + "services" + sep;

        RunnerUtil.createJar(
            "-cf", testClasses + "SimpleProvider.jar",
            "-C", testClasses, "SimpleProvider.class",
            "-C", testClasses, "SimpleVirtualMachine.class",
            "-C", testSrc,
            serviceDir + "com.sun.tools.attach.spi.AttachProvider");
    }

    /**
     * This is the actual test code that attaches to the running Application.
     * This class is run in a separate process.
     */
    public static class TestMain {
        public static void main(String args[]) throws Exception {
            // deal with internal builds where classes are loaded from the
            // 'classes' directory rather than rt.jar
            ClassLoader cl = AttachProvider.class.getClassLoader();
            if (cl != ClassLoader.getSystemClassLoader()) {
                System.out.println("Attach API not loaded by system class loader - test skipped");
                return;
            }
            VirtualMachine.attach("simple:1234").detach();
        }
    }
}
