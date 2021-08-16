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
 * @summary Check that Shenandoah cleans up interned strings
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC
 *      TestStringInternCleanup
 */

/*
 * @test id=default
 * @summary Check that Shenandoah cleans up interned strings
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+ShenandoahVerify
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:-ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC
 *      TestStringInternCleanup
 */

/*
 * @test id=iu
 * @summary Check that Shenandoah cleans up interned strings
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahVerify
 *      TestStringInternCleanup
 *
 * @run main/othervm -Xmx64m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+ClassUnloadingWithConcurrentMark
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      TestStringInternCleanup
 */

public class TestStringInternCleanup {

    static final int COUNT = 1_000_000;
    static final int WINDOW = 1_000;

    static final String[] reachable = new String[WINDOW];

    public static void main(String[] args) throws Exception {
        int rIdx = 0;
        for (int c = 0; c < COUNT; c++) {
            reachable[rIdx] = ("LargeInternedString" + c).intern();
            rIdx++;
            if (rIdx >= WINDOW) {
                rIdx = 0;
            }
        }
    }

}
