/*
 * Copyright (c) 2017, 2020, Red Hat, Inc. All rights reserved.
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
 * @summary Tests that we pass at least one jcstress-like test with all verification turned on
 * @requires vm.gc.Shenandoah
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:+ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      TestVerifyJCStress
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive
 *      -XX:-ShenandoahDegeneratedGC -XX:+ShenandoahVerify
 *      TestVerifyJCStress
 */

/*
 * @test id=default
 * @summary Tests that we pass at least one jcstress-like test with all verification turned on
 * @requires vm.gc.Shenandoah
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive
 *      -XX:+ShenandoahVerify -XX:+IgnoreUnrecognizedVMOptions -XX:+ShenandoahVerifyOptoBarriers
 *      TestVerifyJCStress
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact
 *      -XX:+ShenandoahVerify -XX:+IgnoreUnrecognizedVMOptions -XX:+ShenandoahVerifyOptoBarriers
 *      TestVerifyJCStress
 */

/*
 * @test id=iu
 * @summary Tests that we pass at least one jcstress-like test with all verification turned on
 * @requires vm.gc.Shenandoah
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify -XX:+IgnoreUnrecognizedVMOptions -XX:+ShenandoahVerifyOptoBarriers
 *      TestVerifyJCStress
 *
 * @run main/othervm -Xmx1g -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu
 *      -XX:+ShenandoahVerify -XX:+IgnoreUnrecognizedVMOptions -XX:TieredStopAtLevel=1
 *      TestVerifyJCStress
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class TestVerifyJCStress {

    public static void main(String[] args) throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(
                2,
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                }
        );

        for (int c = 0; c < 10000; c++) {
            final Test[] tests = new Test[10000];
            for (int t = 0; t < tests.length; t++) {
                tests[t] = new Test();
            }

            Future<?> f1 = service.submit(() -> {
                IntResult2 r = new IntResult2();
                for (Test test : tests) {
                    test.RL_Us(r);
                }
            });
            Future<?> f2 = service.submit(() -> {
                for (Test test : tests) {
                    test.WLI_Us();
                }
            });

            f1.get();
            f2.get();
        }
    }

    public static class IntResult2 {
        int r1, r2;
    }

    public static class Test {
        final StampedLock lock = new StampedLock();

        int x, y;

        public void RL_Us(IntResult2 r) {
            StampedLock lock = this.lock;
            long stamp = lock.readLock();
            r.r1 = x;
            r.r2 = y;
            lock.unlock(stamp);
        }

        public void WLI_Us() {
            try {
                StampedLock lock = this.lock;
                long stamp = lock.writeLockInterruptibly();
                x = 1;
                y = 2;
                lock.unlock(stamp);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
