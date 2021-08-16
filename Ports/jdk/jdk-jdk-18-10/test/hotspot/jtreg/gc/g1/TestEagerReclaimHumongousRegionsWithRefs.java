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
 * @test TestEagerReclaimHumongousRegionsWithRefs
 * @bug 8048179
 * @summary Test to make sure that eager reclaim of humongous objects that have previously
 * been referenced by other old gen regions work. We simply try to fill
 * up the heap with humongous objects and create a remembered set entry from an object by
 * referencing that we know is in the old gen. After changing this reference, the object
 * should still be eagerly reclaimable to avoid Full GC.
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.g1.TestEagerReclaimHumongousRegionsWithRefs
 */

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.LinkedList;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import static jdk.test.lib.Asserts.*;

class RefHolder {
  Object ref;
}

class TestEagerReclaimHumongousRegionsWithRefsReclaimRegionFast {

    public static final int M = 1024*1024;

    public static LinkedList<Object> garbageList = new LinkedList<Object>();

    public static void genGarbage() {
        for (int i = 0; i < 32*1024; i++) {
            garbageList.add(new int[100]);
        }
        garbageList.clear();
    }


    // A large object referenced by a static.
    static int[] filler = new int[10 * M];

    // Old gen object referencing the large object, generating remembered
    // set entries.
    static RefHolder fromOld = new RefHolder();

    public static void main(String[] args) {

        int[] large = new int[M];

        Object ref_from_stack = large;

        for (int i = 0; i < 100; i++) {
            // A large object that will be reclaimed eagerly.
            large = new int[6*M];
            fromOld.ref = large;
            genGarbage();
        }

        // Keep the reference to the first object alive.
        System.out.println(ref_from_stack);
    }
}

public class TestEagerReclaimHumongousRegionsWithRefs {

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UseG1GC",
            "-Xms128M",
            "-Xmx128M",
            "-Xmn16M",
            "-Xlog:gc",
            TestEagerReclaimHumongousRegionsWithRefsReclaimRegionFast.class.getName());

        Pattern p = Pattern.compile("Full GC");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        int found = 0;
        Matcher m = p.matcher(output.getStdout());
        while (m.find()) {
            found++;
        }
        System.out.println("Issued " + found + " Full GCs");

        assertLessThan(found, 10, "Found that " + found + " Full GCs were issued. This is larger than the bound. Eager reclaim of objects once referenced from old gen seems to not work at all");
        output.shouldHaveExitValue(0);
    }
}

