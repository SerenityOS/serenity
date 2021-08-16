/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8224137
 * @summary Run invokespecial invocation tests
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 * @compile shared/AbstractGenerator.java shared/AccessCheck.java shared/AccessType.java
 *          shared/Caller.java shared/ExecutorGenerator.java shared/Utils.java
 *          shared/ByteArrayClassLoader.java shared/Checker.java shared/GenericClassGenerator.java
 * @compile invokespecial/Checker.java invokespecial/ClassGenerator.java invokespecial/Generator.java
 *
 * @run driver/timeout=1800 invokespecialTests
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class invokespecialTests {

    public static void runTest(String classFileVersion, String option) throws Throwable {
        System.out.println("\ninvokespecial invocation tests, option: " + option +
                           ", class file version: " + classFileVersion);
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xmx128M", option,
            "--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
            "invokespecial.Generator", "--classfile_version=" + classFileVersion);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        try {
            output.shouldContain("EXECUTION STATUS: PASSED");
            output.shouldHaveExitValue(0);
        } catch (Throwable e) {
            System.out.println(
                "\nNote that an entry such as 'B.m/C.m' in the failure chart means that" +
                " the test case failed because method B.m was invoked but the test " +
                "expected method C.m to be invoked. Similarly, a result such as 'AME/C.m'" +
                " means that an AbstractMethodError exception was thrown but the test" +
                " case expected method C.m to be invoked.");
            System.out.println(
                "\nAlso note that passing --dump to invokespecial.Generator will" +
                " dump the generated classes (for debugging purposes).\n");

            throw e;
        }
    }

    public static void main(String args[]) throws Throwable {
        // Get current major class file version and test with it.
        byte klassbuf[] = InMemoryJavaCompiler.compile("blah", "public class blah { }");
        int major_version = klassbuf[6] << 8 | klassbuf[7];
        runTest(String.valueOf(major_version), "-Xint");
        runTest(String.valueOf(major_version), "-Xcomp");

        // Test old class file version.
        runTest("51", "-Xint"); // JDK-7
    }
}
