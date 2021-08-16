/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8253525
 * @summary Test for fInst.getObjectSize with 32-bit compressed oops
 * @library /test/lib
 *
 * @build sun.hotspot.WhiteBox
 * @run build GetObjectSizeIntrinsicsTest
 * @run shell MakeJAR.sh basicAgent
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xint
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xbatch -XX:TieredStopAtLevel=1
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xbatch -XX:-TieredCompilation
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 */

/*
 * @test
 * @summary Test for fInst.getObjectSize with zero-based compressed oops
 * @library /test/lib
 * @requires vm.bits == 64
 *
 * @build sun.hotspot.WhiteBox
 * @run build GetObjectSizeIntrinsicsTest
 * @run shell MakeJAR.sh basicAgent
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xint
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xbatch -XX:TieredStopAtLevel=1
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xbatch -XX:-TieredCompilation
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 */

/*
 * @test
 * @summary Test for fInst.getObjectSize without compressed oops
 * @library /test/lib
 * @requires vm.bits == 64
 *
 * @build sun.hotspot.WhiteBox
 * @run build GetObjectSizeIntrinsicsTest
 * @run shell MakeJAR.sh basicAgent
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx128m -XX:-UseCompressedOops
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xint
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m -XX:-UseCompressedOops
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xbatch -XX:TieredStopAtLevel=1
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m -XX:-UseCompressedOops
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xbatch -XX:-TieredCompilation
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 */

/*
 * @test
 * @summary Test for fInst.getObjectSize with 32-bit compressed oops
 * @library /test/lib
 * @requires vm.debug
 *
 * @build sun.hotspot.WhiteBox
 * @run build GetObjectSizeIntrinsicsTest
 * @run shell MakeJAR.sh basicAgent
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xint
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xbatch -XX:TieredStopAtLevel=1
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xbatch -XX:-TieredCompilation
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 */

/*
 * @test
 * @summary Test for fInst.getObjectSize with zero-based compressed oops
 * @library /test/lib
 * @requires vm.bits == 64
 * @requires vm.debug
 *
 * @build sun.hotspot.WhiteBox
 * @run build GetObjectSizeIntrinsicsTest
 * @run shell MakeJAR.sh basicAgent
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xint
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xbatch -XX:TieredStopAtLevel=1
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xbatch -XX:-TieredCompilation
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 */

/*
 * @test
 * @summary Test for fInst.getObjectSize without compressed oops
 * @library /test/lib
 * @requires vm.bits == 64
 * @requires vm.debug
 *
 * @build sun.hotspot.WhiteBox
 * @run build GetObjectSizeIntrinsicsTest
 * @run shell MakeJAR.sh basicAgent
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx128m -XX:-UseCompressedOops
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xint
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m -XX:-UseCompressedOops
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xbatch -XX:TieredStopAtLevel=1
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m -XX:-UseCompressedOops
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:FastAllocateSizeLimit=0
 *                   -Xbatch -XX:-TieredCompilation
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 */

/*
 * @test
 * @summary Test for fInst.getObjectSize with 32-bit compressed oops
 * @library /test/lib
 * @requires vm.bits == 64
 * @requires vm.debug
 *
 * @build sun.hotspot.WhiteBox
 * @run build GetObjectSizeIntrinsicsTest
 * @run shell MakeJAR.sh basicAgent
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:ObjectAlignmentInBytes=32
 *                   -Xint
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:ObjectAlignmentInBytes=32
 *                   -Xbatch -XX:TieredStopAtLevel=1
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:ObjectAlignmentInBytes=32
 *                   -Xbatch -XX:-TieredCompilation
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 */

/*
 * @test
 * @summary Test for fInst.getObjectSize with zero-based compressed oops
 * @library /test/lib
 * @requires vm.bits == 64
 * @requires vm.debug
 *
 * @build sun.hotspot.WhiteBox
 * @run build GetObjectSizeIntrinsicsTest
 * @run shell MakeJAR.sh basicAgent
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:ObjectAlignmentInBytes=32
 *                   -Xint
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:ObjectAlignmentInBytes=32
 *                   -Xbatch -XX:TieredStopAtLevel=1
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 *
 * @run main/othervm -Xmx4g
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:ObjectAlignmentInBytes=32
 *                   -Xbatch -XX:-TieredCompilation
 *                   -javaagent:basicAgent.jar GetObjectSizeIntrinsicsTest GetObjectSizeIntrinsicsTest
 */

import java.util.*;

import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;

public class GetObjectSizeIntrinsicsTest extends ASimpleInstrumentationTestCase {

    static final Boolean compressedOops = WhiteBox.getWhiteBox().getBooleanVMFlag("UseCompressedOops");
    static final int REF_SIZE = (compressedOops == null || compressedOops == true) ?  4 : 8;

    static final Long align = WhiteBox.getWhiteBox().getIntxVMFlag("ObjectAlignmentInBytes");
    static final int OBJ_ALIGN = (align == null ? 8 : align.intValue());

    public GetObjectSizeIntrinsicsTest(String name) {
        super(name);
    }

    public static void main(String[] args)throws Throwable {
        new GetObjectSizeIntrinsicsTest(args[0]).runTest();
    }

    public static final int ITERS = 200_000;

    public static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new IllegalStateException(
               "Error: expected: " + expected + " (" + Long.toHexString(expected) +
                "), actual: " + actual + " (" + Long.toHexString(actual) + ")");
        }
    }

    public static void assertNotEquals(long notExpected, long actual) {
        if (notExpected == actual) {
            throw new IllegalStateException(
               "Error: not expected: " + notExpected + " (" + Long.toHexString(notExpected) +
                "), actual: " + actual + " (" + Long.toHexString(actual) + ")");
        }
    }

    public static void assertFail() {
        throw new IllegalStateException("Should not be here");
    }

    protected final void doRunTest() throws Throwable {
        testSize_newObject();
        testSize_localObject();
        testSize_fieldObject();

        testSize_newSmallByteArray();
        testSize_localSmallByteArray();
        testSize_fieldSmallByteArray();

        testSize_newSmallObjArray();
        testSize_localSmallObjArray();
        testSize_fieldSmallObjArray();

        testNulls();
    }

    private static int roundUp(int v, int a) {
        return (v + a - 1) / a * a;
    }

    private void testSize_newObject() {
        int expected = roundUp(Platform.is64bit() ? 16 : 8, OBJ_ALIGN);
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(new Object()));
        }
    }

    private void testSize_localObject() {
        int expected = roundUp(Platform.is64bit() ? 16 : 8, OBJ_ALIGN);
        Object o = new Object();
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(o));
        }
    }

    static Object staticO = new Object();

    private void testSize_fieldObject() {
        int expected = roundUp(Platform.is64bit() ? 16 : 8, OBJ_ALIGN);
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(staticO));
        }
    }

    private void testSize_newSmallByteArray() {
        int expected = roundUp(1024 + 16, OBJ_ALIGN);
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(new byte[1024]));
        }
    }

    private void testSize_localSmallByteArray() {
        byte[] arr = new byte[1024];
        int expected = roundUp(arr.length + 16, OBJ_ALIGN);
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(arr));
        }
    }

    static byte[] smallArr = new byte[1024];

    private void testSize_fieldSmallByteArray() {
        int expected = roundUp(smallArr.length + 16, OBJ_ALIGN);
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(smallArr));
        }
    }

    private void testSize_newSmallObjArray() {
        int expected = roundUp(1024*REF_SIZE + 16, OBJ_ALIGN);
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(new Object[1024]));
        }
    }

    private void testSize_localSmallObjArray() {
        Object[] arr = new Object[1024];
        int expected = roundUp(arr.length*REF_SIZE + 16, OBJ_ALIGN);
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(arr));
        }
    }

    static Object[] smallObjArr = new Object[1024];

    private void testSize_fieldSmallObjArray() {
        int expected = roundUp(smallArr.length*REF_SIZE + 16, OBJ_ALIGN);
        for (int c = 0; c < ITERS; c++) {
            assertEquals(expected, fInst.getObjectSize(smallObjArr));
        }
    }

    private void testNulls() {
        for (int c = 0; c < ITERS; c++) {
            try {
                fInst.getObjectSize(null);
                assertFail();
            } catch (NullPointerException e) {
                // expected
            }
        }
    }

}
