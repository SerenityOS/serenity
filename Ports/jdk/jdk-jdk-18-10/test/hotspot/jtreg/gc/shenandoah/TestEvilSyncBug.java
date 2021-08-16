/*
 * Copyright (c) 2016, 2020, Red Hat, Inc. All rights reserved.
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
 * @test
 * @summary Tests for crash/assert when attaching init thread during shutdown
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver/timeout=480 TestEvilSyncBug
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestEvilSyncBug {

    private static final int NUM_RUNS = 100;

    static Thread[] hooks = new MyHook[10000];

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            test();
        } else {
            // Use 1/4 of available processors to avoid over-saturation.
            int numJobs = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
            ExecutorService pool = Executors.newFixedThreadPool(numJobs);
            Future<?>[] fs = new Future<?>[NUM_RUNS];

            for (int c = 0; c < NUM_RUNS; c++) {
                Callable<Void> task = () -> {
                    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xms128m",
                            "-Xmx128m",
                            "-XX:+UnlockExperimentalVMOptions",
                            "-XX:+UnlockDiagnosticVMOptions",
                            "-XX:+UseShenandoahGC",
                            "-XX:ShenandoahGCHeuristics=aggressive",
                            "TestEvilSyncBug", "test");
                    OutputAnalyzer output = new OutputAnalyzer(pb.start());
                    output.shouldHaveExitValue(0);
                    return null;
                };
                fs[c] = pool.submit(task);
            }

            for (Future<?> f : fs) {
                f.get();
            }

            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.HOURS);
        }
    }

    private static void test() throws Exception {

        for (int t = 0; t < hooks.length; t++) {
            hooks[t] = new MyHook();
        }

        ExecutorService service = Executors.newFixedThreadPool(
                2,
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                }
        );

        List<Future<?>> futures = new ArrayList<>();
        for (int c = 0; c < 100; c++) {
            Runtime.getRuntime().addShutdownHook(hooks[c]);
            final Test[] tests = new Test[1000];
            for (int t = 0; t < tests.length; t++) {
                tests[t] = new Test();
            }

            Future<?> f1 = service.submit(() -> {
                Runtime.getRuntime().addShutdownHook(new MyHook());
                IntResult2 r = new IntResult2();
                for (Test test : tests) {
                    test.RL_Us(r);
                }
            });
            Future<?> f2 = service.submit(() -> {
                Runtime.getRuntime().addShutdownHook(new MyHook());
                for (Test test : tests) {
                    test.WLI_Us();
                }
            });

            futures.add(f1);
            futures.add(f2);
        }

        for (Future<?> f : futures) {
            f.get();
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

    private static class MyHook extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(10);
            } catch (Exception e) {}
        }
    }

}
