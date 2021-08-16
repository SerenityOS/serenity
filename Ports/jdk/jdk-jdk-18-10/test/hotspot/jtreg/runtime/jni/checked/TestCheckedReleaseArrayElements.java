/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8258077
 * @summary verify multiple release calls on a copied array work when checked
 * @requires vm.flagless
 * @library /test/lib
 * @run main/native TestCheckedReleaseArrayElements launch
 */

import java.util.Arrays;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;
import jtreg.SkippedException;

public class TestCheckedReleaseArrayElements {

    static {
        System.loadLibrary("TestCheckedReleaseArrayElements");
    }

    public static void main(String[] args) throws Throwable {
        if (args == null || args.length == 0) {
            test();
        } else {
            // Uses executeProcess() instead of executeTestJvm() to avoid passing options
            // that might generate output on stderr (which should be empty for this test).
            ProcessBuilder pb =
                ProcessTools.createJavaProcessBuilder("-Xcheck:jni",
                                                      "-Djava.library.path=" + Utils.TEST_NATIVE_PATH,
                                                      "TestCheckedReleaseArrayElements");
            OutputAnalyzer output = ProcessTools.executeProcess(pb);
            output.shouldHaveExitValue(0);
            output.stderrShouldBeEmpty();
            output.stdoutShouldNotBeEmpty();
        }
    }

    /*
     * If GetIntArrayElements returns a copy, we update the array in slices
     * calling ReleaseIntArrayElements with JNI_COMMIT to write-back the
     * updates, which are then checked. Finally we use JNI_ABORT to free
     * the copy.
     */
    public static void test() {
        final int slices = 3;
        final int sliceLength = 3;
        int[] arr = new int[slices * sliceLength];

        if (!init(arr)) {
            throw new SkippedException("Test skipped as GetIntArrayElements did not make a copy");
        }

        System.out.println("Array before: " + Arrays.toString(arr));

        // We fill the array in slices so that arr[i] = i
        for (int i = 0; i < slices; i++) {
            int start = i * sliceLength;
            fill(arr, start, sliceLength);
            System.out.println("Array during: " + Arrays.toString(arr));
            check(arr, (i + 1) * sliceLength);
        }
        System.out.println("Array after: " + Arrays.toString(arr));
        cleanup(arr);
    }

    /*
     * Calls GetIntArrayElements and stashes the native pointer for
     * use by fill() if a copy was made.
     * Returns true if a copy was made else false.
    */
    static native boolean init(int[] arr);

    /*
     * Fills in target[start] to target[start+count-1], so that
     * target[i] == i. The update is done natively using the raw
     * pointer into the array.
     */
    static native void fill(int[] target, int start, int count);

    /*
     * Properly release the copied array
     */
    static native void cleanup(int[] target);


    static void check(int[] source, int count) {
        for (int i = 0; i < count; i++) {
            if (source[i] != i) {
                System.out.println("Failing source array: " + Arrays.toString(source));
                throw new RuntimeException("Expected source[" + i + "] == " +
                                           i + " but got " + source[i]);
            }
        }
        for (int i = count; i < source.length; i++) {
            if (source[i] != 0) {
                System.out.println("Failing source array: " + Arrays.toString(source));
                throw new RuntimeException("Expected source[" + i +
                                           "] == 0 but got " + source[i]);
            }
        }

    }
}
