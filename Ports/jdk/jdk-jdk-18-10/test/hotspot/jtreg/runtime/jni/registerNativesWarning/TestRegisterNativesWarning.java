/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @bug 8238460
 * @summary Check that re-registering a native method of a boot class
 *          generates a warning when not done from a boot class
 *
 * @library /test/lib
 * @run main/native TestRegisterNativesWarning
 */

public class TestRegisterNativesWarning {

    static {
        System.loadLibrary("registerNativesWarning");
    }

    /*
     * We will replace:
     *   java/lang/Thread.java:    public static native void yield();
     *
     * as it is simple and innocuous.
     */
    native static void test(Class<?> jlThread);

    // Using a nested class that invokes an enclosing method makes it
    // easier to setup and use the native library.
    static class Tester {
        public static void main(String[] args) throws Exception {
            System.out.println("Running test() in class loader " +
                               Tester.class.getClassLoader());
            test(Thread.class);
            Thread.yield();
        }
    }

    public static void main(String[] args) throws Exception {
        String warning = "Re-registering of platform native method: java.lang.Thread.yield()V from code in a different classloader";

        String cp = Utils.TEST_CLASS_PATH;
        String libp = Utils.TEST_NATIVE_PATH;
        OutputAnalyzer output = ProcessTools.executeTestJvm("-Djava.library.path=" + libp,
                                                            Tester.class.getName());
        output.shouldContain(warning);
        output.shouldHaveExitValue(0);
        output.reportDiagnosticSummary();

        // If we run everything from the "boot" loader there should be no warning
        output = ProcessTools.executeTestJvm("-Djava.library.path=" + libp,
                                             "-Xbootclasspath/a:" + cp,
                                             "-Dsun.boot.library.path=" + libp,
                                             Tester.class.getName());
        output.shouldNotContain(warning);
        output.shouldHaveExitValue(0);
        output.reportDiagnosticSummary();
    }
}
