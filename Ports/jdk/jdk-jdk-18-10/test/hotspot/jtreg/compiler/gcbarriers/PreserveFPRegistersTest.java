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

/**
 * @test
 * @bug 8148175
 * @requires vm.gc=="G1" | vm.gc=="null"
 * @library /test/lib
 * @run main/bootclasspath/othervm -Xbatch -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI -Xmx300m -XX:+UseG1GC
 *      compiler.gcbarriers.PreserveFPRegistersTest
 */

package compiler.gcbarriers;

import sun.hotspot.WhiteBox;

public class PreserveFPRegistersTest {

    public static void main(String... args) throws InterruptedException {
        new PreserveFPRegistersTest().go();
    }

    private static WhiteBox wb = WhiteBox.getWhiteBox();

    public final Object[][] storage;

    /**
     * Number of objects per region.
     */
    public final int K = 10;

    /**
     * Length of object array: sizeOf(Object[N]) ~= regionSize / K .
     */
    public final int N;

    /**
     * How many regions involved into testing.
     */
    public final int regionCount;

    PreserveFPRegistersTest() {
        long regionSize = wb.g1RegionSize();
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long totalFree = rt.maxMemory() - used;
        regionCount = (int) ( (totalFree / regionSize) * 0.9);
        int refSize = wb.getHeapOopSize();
        N = (int) ((regionSize / K ) / refSize) - 5;

        System.out.println("%% Memory");
        System.out.println("%%   used          :        " + used / 1024 + "M");
        System.out.println("%%   available     :        " + totalFree / 1024 + "M");
        System.out.println("%%   G1 Region Size:        " + regionSize / 1024 + "M");
        System.out.println("%%   region count  :        " + regionCount);

        System.out.println("%% Objects storage");
        System.out.println("%%   N (array length)      : " + N);
        System.out.println("%%   K (objects in regions): " + K);
        System.out.println("%%   Reference size        : " + refSize);

        try {
            storage = new Object[regionCount * K][];
            for (int i = 0; i < storage.length; i++) {
                storage[i] = new Object[N];
            }
        } catch(OutOfMemoryError e) {
            throw new AssertionError("Test Failed with unexpected OutOfMemoryError exception");
        }
    }

    public void go() throws InterruptedException {
        final float FINAL = getValue();

        for (int to = 0; to < regionCount; to++) {
            Object celebrity = storage[to * K];
            for (int from = 0; from < regionCount; from++) {
                for (int rn = 0; rn != 100; rn++) {
                    storage[getY(to, from, rn)][getX(to, from, rn)] = celebrity;
                }
                if (FINAL != getValue()) {
                    throw new AssertionError("Final value has changed: " + FINAL + " != " + getValue());
                }
            }
        }

        System.out.println("TEST PASSED");
    }

    public float getValue() {
        return 6;
    }

    private int getX(int to, int from, int rn) {
        return (rn*regionCount + to) % N;
    }

    private int getY(int to, int from, int rn) {
        return ((rn*regionCount + to) / N + from * K) % (regionCount*K) ;
    }
}
