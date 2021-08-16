/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8072466
 * @summary Deadlock when initializing MulticastSocket and DatagramSocket
 * @library /test/lib
 * @run main/othervm MultiDead
 */

import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;

public class MultiDead {
    private static final int THREAD_PAIR_COUNT = 4;
    private static final int CHILDREN_COUNT = 20;
    // at least 2.5 seconds for a child to complete
    private static final long CHILD_TIMEOUT = 2500;
    private static final long TIMEOUT =
        Utils.adjustTimeout(CHILDREN_COUNT * CHILD_TIMEOUT * 2);

    public static void main(String[] args) throws Throwable {
        if (args.length == 0 || args[0].equals("parent")) {
            parentProcess();
        }

        if (args.length > 0 && args[0].equals("child")) {
            childProcess();
        }
    }

    private static void parentProcess() throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher
                .createUsingTestJDK("java")
                .addToolArg("MultiDead")
                .addToolArg("child");
        ProcessBuilder pb = new ProcessBuilder(launcher.getCommand());

        AtomicReference<Process> child = new AtomicReference<>();
        AtomicBoolean stopFlag = new AtomicBoolean(false);

        Thread th = new Thread(() -> {
            for (int i = 0; i < CHILDREN_COUNT; ++i) {
                System.out.println("child #" + (i + 1) + " of " +
                        CHILDREN_COUNT);
                long start = System.nanoTime();
                try {
                    child.set(pb.start());
                    child.get().waitFor();
                    if (stopFlag.get()) {
                        break;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (System.nanoTime() - start >
                        MILLISECONDS.toNanos(CHILD_TIMEOUT)) {
                    System.err.println("Machine is too slow, " +
                            "skipping the test...");
                    break;
                }
            }
        });

        th.start();
        th.join(TIMEOUT);

        stopFlag.set(true);
        if (th.isAlive()) {
            if (child.get() != null) {
                child.get().destroyForcibly();
            }
            throw new RuntimeException("Failed to complete on time.");
        }
    }

    private static void childProcess() {
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < THREAD_PAIR_COUNT; ++i) {
            new Thread(() -> {
                try {
                    latch.await();
                    try (MulticastSocket a = new MulticastSocket(6000)) {
                    }
                } catch (Exception ignore) {
                }
            }).start();

            new Thread(() -> {
                try {
                    latch.await();
                    try (DatagramSocket b = new DatagramSocket(6000)) {
                    }
                } catch (Exception ignore) {
                }
            }).start();
        }
        latch.countDown();
    }
}
