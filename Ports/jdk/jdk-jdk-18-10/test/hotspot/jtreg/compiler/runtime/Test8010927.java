/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8010927
 * @summary Kitchensink crashed with SIGSEGV, Problematic frame: v ~StubRoutines::checkcast_arraycopy
 * @library /test/lib
 * @modules java.base/jdk.internal.misc:+open
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions
 *                   -XX:+WhiteBoxAPI -Xbootclasspath/a:. -Xmx128m -XX:NewSize=20971520
 *                   -XX:MaxNewSize=32m -XX:-UseTLAB -XX:-UseAdaptiveSizePolicy
 *                   compiler.runtime.Test8010927
 */

package compiler.runtime;

import jdk.internal.misc.Unsafe;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Field;

/**
 * The test creates uncommitted space between oldgen and young gen
 * by specifying MaxNewSize bigger than NewSize.
 * NewSize = 20971520 = (512*4K) * 10 for 4k pages
 * Then it tries to execute arraycopy() with elements type check
 * to the array at the end of survive space near unused space.
 */

public class Test8010927 {

    private static final Unsafe U;

    static {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static Object[] o;

    public static final boolean debug = Boolean.getBoolean("debug");

    // 2 different obect arrays but same element types
    static Test8010927[] masterA;
    static Object[] masterB;
    static final Test8010927 elem = new Test8010927();
    static final WhiteBox wb = WhiteBox.getWhiteBox();

    static final int obj_header_size = U.ARRAY_OBJECT_BASE_OFFSET;
    static final int heap_oop_size = wb.getHeapOopSize();
    static final int card_size = 512;
    static final int one_card = (card_size - obj_header_size) / heap_oop_size;

    static final int surv_size = 2112 * 1024;

    // The size is big to not fit into survive space.
    static final Object[] cache = new Object[(surv_size / card_size)];

    public static void main(String[] args) {
        masterA = new Test8010927[one_card];
        masterB = new Object[one_card];
        for (int i = 0; i < one_card; ++i) {
            masterA[i] = elem;
            masterB[i] = elem;
        }

        // Move cache[] to the old gen.
        long low_limit = wb.getObjectAddress(cache);
        System.gc();
        // Move 'cache' to oldgen.
        long upper_limit = wb.getObjectAddress(cache);
        if ((low_limit - upper_limit) > 0) { // substaction works with unsigned values
            // OldGen is placed before youngger for ParallelOldGC.
            upper_limit = low_limit + 21000000l; // +20971520
        }
        // Each A[one_card] size is 512 bytes,
        // it will take about 40000 allocations to trigger GC.
        // cache[] has 8192 elements so GC should happen
        // each 5th iteration.
        for (long l = 0; l < 20; l++) {
            fill_heap();
            if (debug) {
                System.out.println("test oop_disjoint_arraycopy");
            }
            testA_arraycopy();
            if (debug) {
                System.out.println("test checkcast_arraycopy");
            }
            testB_arraycopy();
            // Execute arraycopy to the topmost array in young gen
            if (debug) {
                int top_index = get_top_address(low_limit, upper_limit);
                if (top_index >= 0) {
                    long addr = wb.getObjectAddress(cache[top_index]);
                    System.out.println("top_addr: 0x" + Long.toHexString(addr) + ", 0x" + Long.toHexString(addr + 512));
                }
            }
        }
    }

    static void fill_heap() {
        for (int i = 0; i < cache.length; ++i) {
            o = new Test8010927[one_card];
            System.arraycopy(masterA, 0, o, 0, masterA.length);
            cache[i] = o;
        }
        for (long j = 0; j < 256; ++j) {
            o = new Long[10000]; // to trigger GC
        }
    }

    static void testA_arraycopy() {
        for (int i = 0; i < cache.length; ++i) {
            System.arraycopy(masterA, 0, cache[i], 0, masterA.length);
        }
    }

    static void testB_arraycopy() {
        for (int i = 0; i < cache.length; ++i) {
            System.arraycopy(masterB, 0, cache[i], 0, masterB.length);
        }
    }

    static int get_top_address(long min, long max) {
        int index = -1;
        long addr = min;
        for (int i = 0; i < cache.length; ++i) {
            long test = wb.getObjectAddress(cache[i]);
            if (((test - addr) > 0) && ((max - test) > 0)) { // substaction works with unsigned values
                addr = test;
                index = i;
            }
        }
        return index;
    }
}
