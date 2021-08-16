/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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
 * @summary Acceptance tests: collector can deal with retained objects
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC
 *      TestSieveObjects
 */

/*
 * @test id=aggressive
 * @summary Acceptance tests: collector can deal with retained objects
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      TestSieveObjects
 */

/*
 * @test id=adaptive
 * @summary Acceptance tests: collector can deal with retained objects
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+ShenandoahVerify
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      TestSieveObjects
 *
 */

/*
 * @test id=static
 * @summary Acceptance tests: collector can deal with retained objects
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=static
 *      TestSieveObjects
 */

/*
 * @test id=compact
 * @summary Acceptance tests: collector can deal with retained objects
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      TestSieveObjects
 */

/*
 * @test id=no-tlab
 * @summary Acceptance tests: collector can deal with retained objects
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm/timeout=240 -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC
 *      -XX:-UseTLAB -XX:+ShenandoahVerify
 *      TestSieveObjects
 */

/*
 * @test id=iu-aggressive
 * @summary Acceptance tests: collector can deal with retained objects
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      TestSieveObjects
 */

/*
 * @test id=iu
 * @summary Acceptance tests: collector can deal with retained objects
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify
 *      TestSieveObjects
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      TestSieveObjects
 */

import java.util.Random;
import jdk.test.lib.Utils;

public class TestSieveObjects {

    static final int COUNT = 100_000_000;
    static final int WINDOW = 1_000_000;
    static final int PAYLOAD = 100;

    static final MyObject[] arr = new MyObject[WINDOW];

    public static void main(String[] args) throws Exception {
        int rIdx = 0;
        Random rng = Utils.getRandomInstance();
        for (int c = 0; c < COUNT; c++) {
            MyObject v = arr[rIdx];
            if (v != null) {
                if (v.x != rIdx) {
                    throw new IllegalStateException("Illegal value at index " + rIdx + ": " + v.x);
                }
                if (rng.nextInt(1000) > 100) {
                    arr[rIdx] = null;
                }
            } else {
                if (rng.nextInt(1000) > 500) {
                    arr[rIdx] = new MyObject(rIdx);
                }
            }
            rIdx++;
            if (rIdx >= WINDOW) {
                rIdx = 0;
            }
        }
    }

    public static class MyObject {
        public int x;
        public byte[] payload;

        public MyObject(int x) {
            this.x = x;
            this.payload = new byte[PAYLOAD];
        }
    }

}
