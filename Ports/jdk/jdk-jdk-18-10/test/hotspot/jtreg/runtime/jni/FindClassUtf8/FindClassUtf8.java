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

/* @test
 * @bug 8166358
 * @summary verify that -Xcheck:jni finds a bad utf8 name for class name.
 * @library /test/lib
 * @run main/native FindClassUtf8 test
 */

import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public final class FindClassUtf8 {

    static {
        System.loadLibrary("FindClassUtf8");
    }

    native static void nTest();

    public static void main(String... args) throws Exception {
        if (args.length == 1) {
            // run java -Xcheck:jni FindClassUtf8 and check that the -Xcheck:jni message comes out.
            ProcessTools.executeTestJvm("-Djava.library.path=" + Utils.TEST_NATIVE_PATH,
                                        "-Xcheck:jni",
                                        "-XX:-CreateCoredumpOnCrash",
                                        "FindClassUtf8")
                      .shouldContain("JNI class name is not a valid UTF8 string")
                      .shouldNotHaveExitValue(0);  // you get a core dump from -Xcheck:jni failures
        } else {
            // Run the test
            nTest();
        }
    }
}
