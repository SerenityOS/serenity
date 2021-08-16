/*
 * Copyright (c) 2016, 2018, Red Hat, Inc. All rights reserved.
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

/*
 * @test id=passive
 * @summary Make sure Shenandoah can recover from humongous allocation fragmentation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC
 *      TestAllocHumongousFragment
 */

/*
 * @test id=aggressive
 * @summary Make sure Shenandoah can recover from humongous allocation fragmentation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot -XX:+ShenandoahVerify
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot -XX:+ShenandoahVerify
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      TestAllocHumongousFragment
 */

/*
 * @test id=adaptive
 * @summary Make sure Shenandoah can recover from humongous allocation fragmentation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+ShenandoahVerify
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      TestAllocHumongousFragment
 */

/*
 * @test id=static
 * @summary Make sure Shenandoah can recover from humongous allocation fragmentation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=static
 *      TestAllocHumongousFragment
 */

/*
 * @test id=compact
 * @summary Make sure Shenandoah can recover from humongous allocation fragmentation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      TestAllocHumongousFragment
 */

/*
 * @test id=iu-aggressive
 * @summary Make sure Shenandoah can recover from humongous allocation fragmentation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx1g -Xms1g -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot -XX:+ShenandoahVerify
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx1g -Xms1g -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot -XX:+ShenandoahVerify
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx1g -Xms1g -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx1g -Xms1g -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      TestAllocHumongousFragment
 */

/*
 * @test id=iu
 * @summary Make sure Shenandoah can recover from humongous allocation fragmentation
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx1g -Xms1g -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify
 *      TestAllocHumongousFragment
 *
 * @run main/othervm -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -Xmx1g -Xms1g -XX:ShenandoahTargetNumRegions=2048
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      TestAllocHumongousFragment
 */

import java.util.*;
import jdk.test.lib.Utils;

public class TestAllocHumongousFragment {

    static final long TARGET_MB = Long.getLong("target", 30_000); // 30 Gb allocations
    static final long LIVE_MB   = Long.getLong("occupancy", 700); // 700 Mb alive

    static volatile Object sink;

    static List<int[]> objects;

    public static void main(String[] args) throws Exception {
        final int min = 128 * 1024;
        final int max = 16 * 1024 * 1024;
        final long count = TARGET_MB * 1024 * 1024 / (16 + 4 * (min + (max - min) / 2));

        objects = new ArrayList<>();
        long current = 0;

        Random rng = Utils.getRandomInstance();
        for (long c = 0; c < count; c++) {
            while (current > LIVE_MB * 1024 * 1024) {
                int idx = rng.nextInt(objects.size());
                int[] remove = objects.remove(idx);
                current -= remove.length * 4 + 16;
            }

            int[] newObj = new int[min + rng.nextInt(max - min)];
            current += newObj.length * 4 + 16;
            objects.add(newObj);
            sink = new Object();

            System.out.println("Allocated: " + (current / 1024 / 1024) + " Mb");
        }
    }

}
