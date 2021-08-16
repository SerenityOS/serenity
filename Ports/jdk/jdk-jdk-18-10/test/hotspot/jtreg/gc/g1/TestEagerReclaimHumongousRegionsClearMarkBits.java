/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test TestEagerReclaimHumongousRegionsClearMarkBits
 * @bug 8051973
 * @summary Test to make sure that eager reclaim of humongous objects correctly clears
 * mark bitmaps at reclaim.
 * @key randomness
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.g1.TestEagerReclaimHumongousRegionsClearMarkBits
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

// An object that has a few references to other instances to slow down marking.
class ObjectWithSomeRefs {
    public ObjectWithSomeRefs other1;
    public ObjectWithSomeRefs other2;
    public ObjectWithSomeRefs other3;
    public ObjectWithSomeRefs other4;
}

class TestEagerReclaimHumongousRegionsClearMarkBitsReclaimRegionFast {
    public static final long MAX_MILLIS_FOR_RUN = 50 * 1000; // The maximum runtime for the actual test.

    public static final int M = 1024*1024;

    public static LinkedList<Object> garbageList = new LinkedList<Object>();

    public static void genGarbage(Object large) {
        for (int i = 0; i < 64*1024; i++) {
            Object[] garbage = new Object[50];
            garbage[0] = large;
            garbageList.add(garbage);
        }
        garbageList.clear();
    }

    public static ArrayList<ObjectWithSomeRefs> longList = new ArrayList<ObjectWithSomeRefs>();

    public static void main(String[] args) {

        for (int i = 0; i < 16*1024; i++) {
             longList.add(new ObjectWithSomeRefs());
        }

        Random rnd = Utils.getRandomInstance();
        for (int i = 0; i < longList.size(); i++) {
             int len = longList.size();
             longList.get(i).other1 = longList.get(rnd.nextInt(len));
             longList.get(i).other2 = longList.get(rnd.nextInt(len));
             longList.get(i).other3 = longList.get(rnd.nextInt(len));
             longList.get(i).other4 = longList.get(rnd.nextInt(len));
        }

        int[] large1 = new int[M];
        int[] large2 = null;
        int[] large3 = null;
        int[] large4 = null;

        Object ref_from_stack = large1;

        long start_millis = System.currentTimeMillis();

        for (int i = 0; i < 20; i++) {
            long current_millis = System.currentTimeMillis();
            if ((current_millis - start_millis) > MAX_MILLIS_FOR_RUN) {
              System.out.println("Finishing test because maximum runtime exceeded");
              break;
            }
            // A set of large objects that will be reclaimed eagerly - and hopefully marked.
            large1 = new int[M - 20];
            large2 = new int[M - 20];
            large3 = new int[M - 20];
            large4 = new int[M - 20];
            genGarbage(large1);
            // Make sure that the compiler cannot completely remove
            // the allocation of the large object until here.
            System.out.println(large1 + " " + large2 + " " + large3 + " " + large4);
        }

        // Keep the reference to the first object alive.
        System.out.println(ref_from_stack);
    }
}

public class TestEagerReclaimHumongousRegionsClearMarkBits {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UseG1GC",
            "-Xms128M",
            "-Xmx128M",
            "-Xmn2M",
            "-XX:G1HeapRegionSize=1M",
            "-XX:InitiatingHeapOccupancyPercent=0", // Want to have as much as possible mark cycles.
            "-Xlog:gc",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+VerifyAfterGC",
            "-XX:ConcGCThreads=1", // Want to make marking as slow as possible.
            "-XX:+IgnoreUnrecognizedVMOptions", // G1VerifyBitmaps is develop only.
            "-XX:+G1VerifyBitmaps",
            TestEagerReclaimHumongousRegionsClearMarkBitsReclaimRegionFast.class.getName());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }
}

