/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8193222
 * @summary Check EnsureLocalCapacity doesn't shrink unexpectedly
 * @library /test/lib
 * @run main/native TestCheckedEnsureLocalCapacity launch
 */
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestCheckedEnsureLocalCapacity {

    static {
        System.loadLibrary("TestCheckedEnsureLocalCapacity");
    }

    // Calls EnsureLocalCapacity(capacity) and then creates "copies" number
    // of LocalRefs to "o".
    // If capacity > copies no warning should ensue (with the bug fixed).
    // If copies > capacity + warning-threshold then we still get a warning.
    private static native void ensureCapacity(Object o, int capacity, int copies);

    private static int[][] testArgs = {
        { 60, 45 }, // good: capacity > copies
        { 1, 45 }   // bad: copies >> capacity
    };

    // Patterns EXCEED_WARNING and WARNING are not anchored to the beginning
    // of lines to allow matching interleaved output.

    private static final String EXCEED_WARNING =
        "WARNING: JNI local refs: \\d++, exceeds capacity:";

    private static final String WARNING = "WARNING:";

    public static void main(String[] args) throws Throwable {
        if (args.length == 2) {
            ensureCapacity(new Object(),
                           Integer.parseInt(args[0]),
                           Integer.parseInt(args[1]));
            return;
        }

        // No warning
        ProcessTools.executeTestJvm("-Xcheck:jni",
                                    "-Djava.library.path=" + Utils.TEST_NATIVE_PATH,
                                    "TestCheckedEnsureLocalCapacity",
                                    Integer.toString(testArgs[0][0]),
                                    Integer.toString(testArgs[0][1])).
            shouldHaveExitValue(0).
            // check no capacity warning
            stdoutShouldNotMatch(EXCEED_WARNING).
            // check no other warning
            stdoutShouldNotMatch(WARNING).
            reportDiagnosticSummary();

        // Warning
        ProcessTools.executeTestJvm("-Xcheck:jni",
                                    "-Djava.library.path=" + Utils.TEST_NATIVE_PATH,
                                    "TestCheckedEnsureLocalCapacity",
                                    Integer.toString(testArgs[1][0]),
                                    Integer.toString(testArgs[1][1])).
            shouldHaveExitValue(0).
            // check for capacity warning
            stdoutShouldMatch(EXCEED_WARNING).
            reportDiagnosticSummary();
    }
}
