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
 * @bug 8193234 8258077
 * @summary Check ReleasePrimitiveArrayCritical doesn't leak memory with
 * Xcheck:jni and JNI_COMMIT mode
 * @comment This is a manual test as you need to verify memory usage externally.
 * @library /test/lib
 * @run main/othervm/native/manual -Xms4m -Xmx4m -Xcheck:jni TestCheckedReleaseCriticalArray
 */
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jtreg.SkippedException;

public class TestCheckedReleaseCriticalArray {

    static {
        System.loadLibrary("TestCheckedReleaseCriticalArray");
    }

    /*
     * We repeatedly modify an array via the JNI critical functions, using
     * JNI_COMMIT mode. No memory leak should be observed on a VM that
     * provides direct array access.
     */
    public static void main(String[] args) {
        int[] array = new int[] { 1, 2, 3 };
        if (!modifyArray(array)) {
            // If the VM makes copies then we will leak them if we only ever use
            // JNI_COMMIT mode.
            throw new SkippedException("Test skipped as GetPrimitiveArrayCritical made a copy");
        }
        while (true) {
            modifyArray(array);
        }
    }

    private static native boolean modifyArray(int[] array);
}
