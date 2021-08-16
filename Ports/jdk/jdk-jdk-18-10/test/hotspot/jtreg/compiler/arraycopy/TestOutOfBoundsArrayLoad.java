/*
 * Copyright (c) 2021 SAP SE. All rights reserved.
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

/**
 * @test
 * @requires vm.gc.Serial
 * @bug 8262295
 * @library /test/lib /
 * @summary Out of bounds array load on clone source crashes GC which
 *          interpretes the loaded value as oop. A small heap is configured to
 *          get a lot of GCs.
 *
 * @comment C2 generates the out of bounds load with serial, parallel and
 *          shenandoah gc but not with g1 and z gc. For simplicity serial gc is
 *          configured.
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UseSerialGC -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:-BackgroundCompilation
 *                   -XX:CompileCommand=dontinline,*::*_dontinline
 *                   compiler.arraycopy.TestOutOfBoundsArrayLoad
 */

package compiler.arraycopy;

import compiler.whitebox.CompilerWhiteBoxTest;

public class TestOutOfBoundsArrayLoad {

    public static Object escape1;
    public static Object escape2;

    public static void main(String[] args_ignored) {
        try {
            Object[] arrNotEmpty = {null, null, null, null, null, };

            // Warm-up
            for (int i = CompilerWhiteBoxTest.THRESHOLD; i > 0; i--) {
                testMethod_dontinline(arrNotEmpty);
            }
            // Call testmethod with empty array often enough to trigger GC.
            // GC is assumed to crash.
            for (int i = 20_000_000; i > 0; i--) {
                // Trick for ParallelGC: empty[4] will be loaded in the testmethod
                // (out of bounds!) and interpreted as oop (or
                // narrowOop). PSScavenge::should_scavenge() will skip the loaded
                // value if it is before the young generation. So before calling the
                // test method we allocate the empty array and an array of -1 values
                // right behind it. So empty[4] will likely result in
                // 0xffffffffffffffff Which is not before the young generation.
                Object[] empty = new Object[0];
                long[] l = new long[4];
                l[0] = -1L; l[1] = -1L; l[2] = -1L; l[3] = -1L;
                escape2 = l;
                testMethod_dontinline(empty);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static void testMethod_dontinline(Object[] src) throws Exception {
        Object[] clone = src.clone();
        // Load L below is executed speculatively at this point from src without range check.
        // The result is put into the OopMap of the allocation in the next line.
        // If src.length is 0 then the loaded value is no heap reference and GC crashes.
        escape1 = new Object();
        if (src.length > 4) {
            escape2 = clone[4]; // Load L
        }
    }
}
