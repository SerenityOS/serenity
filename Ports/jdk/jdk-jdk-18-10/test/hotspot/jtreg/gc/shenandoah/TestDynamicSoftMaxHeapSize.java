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
 *
 */

/*
 * @test id=passive
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xms16m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC
 *      -Dtarget=10000
 *      TestDynamicSoftMaxHeapSize
 *
 * @run main/othervm -Xms16m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC
 *      -Dtarget=10000
 *      TestDynamicSoftMaxHeapSize
 */

/*
 * @test id=aggressive
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xms16m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -Dtarget=1000
 *      TestDynamicSoftMaxHeapSize
 */

/*
 * @test id=adaptive
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xms16m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -Dtarget=10000
 *      TestDynamicSoftMaxHeapSize
 */

/*
 * @test id=static
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xms16m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=static
 *      -Dtarget=10000
 *      TestDynamicSoftMaxHeapSize
 */

/*
 * @test id=compact
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xms16m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      -Dtarget=1000
 *     TestDynamicSoftMaxHeapSize
 */

/*
 * @test id=iu-aggressive
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xms16m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -Dtarget=1000
 *      TestDynamicSoftMaxHeapSize
 */

/*
 * @test id=iu
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xms16m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -Dtarget=10000
 *      TestDynamicSoftMaxHeapSize
 */

import java.util.Random;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.dcmd.PidJcmdExecutor;

public class TestDynamicSoftMaxHeapSize {

    static final long TARGET_MB = Long.getLong("target", 10_000); // 10 Gb allocation
    static final long STRIDE = 10_000_000;

    static volatile Object sink;

    public static void main(String[] args) throws Exception {
        long count = TARGET_MB * 1024 * 1024 / 16;
        Random r = Utils.getRandomInstance();
        PidJcmdExecutor jcmd = new PidJcmdExecutor();

        for (long c = 0; c < count; c += STRIDE) {
            // Sizes specifically include heaps below Xms and above Xmx to test saturation code.
            jcmd.execute("VM.set_flag SoftMaxHeapSize " + r.nextInt(768*1024*1024), true);
            for (long s = 0; s < STRIDE; s++) {
                sink = new Object();
            }
            Thread.sleep(1);
        }
    }

}
