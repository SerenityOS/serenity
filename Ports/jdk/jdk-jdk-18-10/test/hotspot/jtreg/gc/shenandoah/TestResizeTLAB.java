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
 * @key randomness
 * @summary Test that Shenandoah is able to work with(out) resizeable TLABs
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      -XX:+ShenandoahVerify
 *      -XX:+ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      -XX:+ShenandoahVerify
 *      -XX:-ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      -XX:+ShenandoahVerify
 *      -XX:+ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      -XX:+ShenandoahVerify
 *      -XX:-ResizeTLAB
 *      TestResizeTLAB
 */

/*
 * @test id=aggressive
 * @key randomness
 * @summary Test that Shenandoah is able to work with(out) resizeable TLABs
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahVerify
 *      -XX:+ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahVerify
 *      -XX:-ResizeTLAB
 *      TestResizeTLAB
 */

/*
 * @test id=adaptive
 * @key randomness
 * @summary Test that Shenandoah is able to work with(out) resizeable TLABs
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+ShenandoahVerify
 *      -XX:+ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+ShenandoahVerify
 *      -XX:-ResizeTLAB
 *      TestResizeTLAB
 */

/*
 * @test id=static
 * @key randomness
 * @summary Test that Shenandoah is able to work with(out) resizeable TLABs
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=static
 *      -XX:+ShenandoahVerify
 *      -XX:+ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=static
 *      -XX:+ShenandoahVerify
 *      -XX:-ResizeTLAB
 *      TestResizeTLAB
 */

/*
 * @test id=compact
 * @key randomness
 * @summary Test that Shenandoah is able to work with(out) resizeable TLABs
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      -XX:+ShenandoahVerify
 *      -XX:+ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      -XX:+ShenandoahVerify
 *      -XX:-ResizeTLAB
 *      TestResizeTLAB
 */

/*
 * @test id=iu-aggressive
 * @key randomness
 * @summary Test that Shenandoah is able to work with(out) resizeable TLABs
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahVerify
 *      -XX:+ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahVerify
 *      -XX:-ResizeTLAB
 *      TestResizeTLAB
 */

/*
 * @test id=iu
 * @key randomness
 * @summary Test that Shenandoah is able to work with(out) resizeable TLABs
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify
 *      -XX:+ResizeTLAB
 *      TestResizeTLAB
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify
 *      -XX:-ResizeTLAB
 *      TestResizeTLAB
 */

import java.util.Random;
import jdk.test.lib.Utils;

public class TestResizeTLAB {

    static final long TARGET_MB = Long.getLong("target", 10_000); // 10 Gb allocation

    static volatile Object sink;

    public static void main(String[] args) throws Exception {
        final int min = 0;
        final int max = 384 * 1024;
        long count = TARGET_MB * 1024 * 1024 / (16 + 4 * (min + (max - min) / 2));

        Random r = Utils.getRandomInstance();
        for (long c = 0; c < count; c++) {
            sink = new int[min + r.nextInt(max - min)];
        }
    }

}
