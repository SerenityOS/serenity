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

package gc.stress.gcbasher;

import java.io.IOException;

/*
 * @test id=passive
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Shenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahVerify -XX:+ShenandoahDegeneratedGC
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahVerify -XX:-ShenandoahDegeneratedGC
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=aggressive
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Shenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=adaptive
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Shenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+ShenandoahVerify
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=compact
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Shenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=iu-aggressive
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Shenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=iu
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Shenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=passive-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Shenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+ShenandoahVerify -XX:+ShenandoahDegeneratedGC
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+ShenandoahVerify -XX:-ShenandoahDegeneratedGC
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=aggressive-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Shenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+ShenandoahAllocFailureALot
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=adaptive-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Shenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+ShenandoahVerify
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=compact-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Shenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=iu-aggressive-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Shenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+ShenandoahAllocFailureALot
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */

/*
 * @test id=iu-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Shenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+ShenandoahVerify
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithShenandoah 120000
 */


public class TestGCBasherWithShenandoah {
    public static void main(String[] args) throws IOException {
        TestGCBasher.main(args);
    }
}
