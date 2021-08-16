/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test Testexecstack.java
 * @bug 7107135
 * @bug 8021296
 * @bug 8025519
 * @summary Stack guard pages lost after loading library with executable stack.
 * @requires (os.family == "linux")
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @compile Test.java
 * @compile TestMT.java
 * @run driver Testexecstack
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class Testexecstack {

    public static void main(String[] args) throws Throwable {

        // Get the library path property
        String libpath = System.getProperty("java.library.path");

        // Create a new java process for the Test Java/JNI test without
        // an executeable stack
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Djava.library.path=" + libpath + ":.", "Test", "test-rw");

        // Start the process and check the output
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        // Create a new java process for the Test Java/JNI test with an
        // executable stack
        pb = ProcessTools.createJavaProcessBuilder(
            "-Djava.library.path=" + libpath + ":.", "Test", "test-rwx");

        // Start the process and check the output
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        // Create a new java process for the TestMT Java/JNI test with an
        // executable stack
        pb = ProcessTools.createJavaProcessBuilder(
            "-Djava.library.path=" + libpath + ":.", "TestMT", "test-rwx");

        // Start the process and check the output
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }
}
