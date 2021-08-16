/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8198908
 * @summary Check that preview minor version and --enable-preview are handled
 *          correctly.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main PreviewVersion
 */

import java.io.File;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.ByteCodeLoader;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class PreviewVersion {

    public static void main(String args[]) throws Throwable {
        System.out.println("Regression test for bug 8198908");

        byte klassbuf[] = InMemoryJavaCompiler.compile("PVTest",
            "public class PVTest { " +
                "public static void main(String argv[]) { " +
                    "System.out.println(\"Hi!\"); } }");

        // Set class's minor version to 65535.
        klassbuf[4] = -1;
        klassbuf[5] = -1;

        // Run the test. This should fail because --enable-preview is not specified.
        ClassFileInstaller.writeClassToDisk("PVTest", klassbuf, System.getProperty("test.classes"));
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-cp", "." + File.pathSeparator + System.getProperty("test.classes"), "PVTest");
        OutputAnalyzer oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("Preview features are not enabled");
        oa.shouldHaveExitValue(1);

        // This should be successful because --enable-preview is specified.
        pb = ProcessTools.createJavaProcessBuilder("--enable-preview",
            "-cp", "." + File.pathSeparator + System.getProperty("test.classes"), "PVTest");
        oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("Hi!");
        oa.shouldHaveExitValue(0);

        // Test -Xlog:class+preview
        pb = ProcessTools.createJavaProcessBuilder("--enable-preview", "-Xlog:class+preview",
            "-cp", "." + File.pathSeparator + System.getProperty("test.classes"), "PVTest");
        oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("[info][class,preview] Loading class PVTest that depends on preview features");
        oa.shouldHaveExitValue(0);

        // Subtract 1 from class's major version.  The class should fail to load
        // because its major_version does not match the JVM current version.
        int prev_major_version = (klassbuf[6] << 8 | klassbuf[7]) - 1;
        klassbuf[6] = (byte)((prev_major_version >> 8) & 0xff);
        klassbuf[7] = (byte)(prev_major_version & 0xff);
        try {
            ByteCodeLoader.load("PVTest", klassbuf);
            throw new RuntimeException("UnsupportedClassVersionError exception not thrown");
        } catch (java.lang.UnsupportedClassVersionError e) {
            if (!e.getMessage().contains("compiled with preview features that are unsupported")) {
              throw new RuntimeException(
                  "Wrong UnsupportedClassVersionError exception: " + e.getMessage());
            }
        }

        // Set class's major version to 45.  The class should load because class
        // version 45.65535 is valid.
        klassbuf[6] = 0;
        klassbuf[7] = 45;
        try {
            ByteCodeLoader.load("PVTest", klassbuf);
        } catch (java.lang.UnsupportedClassVersionError e) {
            throw new RuntimeException(
                "Unexpected UnsupportedClassVersionError exception thrown: " + e.getMessage());
        }

        // Check that a class with a recent older major version > JDK-11 and a minor version
        // that is neither 0 nor 65535 fails to load.
        klassbuf[6] = 0;
        klassbuf[7] = 56;
        klassbuf[4] = 0;
        klassbuf[5] = 2;
        try {
            ByteCodeLoader.load("PVTest", klassbuf);
            throw new RuntimeException("UnsupportedClassVersionError exception not thrown");
        } catch (java.lang.UnsupportedClassVersionError e) {
            if (!e.getMessage().contains("was compiled with an invalid non-zero minor version")) {
                throw new RuntimeException(
                    "Wrong UnsupportedClassVersionError exception: " + e.getMessage());
            }
        }
    }
}
