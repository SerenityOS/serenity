/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.Graphics;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @bug 8235638 8235739
 * @key headful
 */
public final class GetGraphicsStressTest {

    static volatile Throwable failed;
    static volatile long endtime;

    public static void main(final String[] args) throws Exception {
        // Catch all uncaught exceptions and treat them as test failure
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> failed = e);

        // Will run the test no more than 20 seconds
        for (int i = 0; i < 4; i++) {
            endtime = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            test();
        }
    }

    private static void test() throws Exception {
        Frame f = new Frame();
        f.setSize(100, 100);
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        Thread thread1 = new Thread(() -> {
            while (!isComplete()) {
                f.removeNotify();
                f.addNotify();
            }
        });
        Thread thread2 = new Thread(() -> {
            while (!isComplete()) {
                Graphics g = f.getGraphics();
                if (g != null) {
                    g.dispose();
                }
            }
        });
        Thread thread3 = new Thread(() -> {
            while (!isComplete()) {
                Graphics g = f.getGraphics();
                if (g != null) {
                    g.dispose();
                }
            }
        });
        Thread thread4 = new Thread(() -> {
            while (!isComplete()) {
                Graphics g = f.getGraphics();
                if (g != null) {
                    g.drawLine(0, 0, 4, 4); // just in case...
                    g.dispose();
                }
            }
        });
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        f.dispose();
        if (failed != null) {
            System.err.println("Test failed");
            failed.printStackTrace();
            throw new RuntimeException(failed);
        }
    }

    private static boolean isComplete() {
        return endtime - System.nanoTime() < 0 || failed != null;
    }
}
