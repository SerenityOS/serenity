/*
* Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.stress.gcold;

/*
 * @test id=passive
 * @key stress randomness
 * @library / /test/lib
 * @requires vm.gc.Shenandoah
 * @summary Stress the GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=600 -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm/timeout=600 -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm/timeout=600 -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm/timeout=600 -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 */

/*
 * @test id=aggressive
 * @key stress randomness
 * @library / /test/lib
 * @requires vm.gc.Shenandoah
 * @summary Stress the GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 */

/*
 * @test id=adaptive
 * @key stress randomness
 * @library / /test/lib
 * @requires vm.gc.Shenandoah
 * @summary Stress the GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=600 -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+ShenandoahVerify
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 */

/*
 * @test id=compact
 * @key stress randomness
 * @library / /test/lib
 * @requires vm.gc.Shenandoah
 * @summary Stress the GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 */

/*
 * @test id=iu-aggressive
 * @key stress randomness
 * @library / /test/lib
 * @requires vm.gc.Shenandoah
 * @summary Stress the GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 */

/*
 * @test id=iu
 * @key stress randomness
 * @library / /test/lib
 * @requires vm.gc.Shenandoah
 * @summary Stress the GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=600 -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 *
 * @run main/othervm -Xmx384M -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      gc.stress.gcold.TestGCOld 50 1 20 10 10000
 */

public class TestGCOldWithShenandoah {

    public static void main(String[] args) {
        TestGCOld.main(args);
    }
}
