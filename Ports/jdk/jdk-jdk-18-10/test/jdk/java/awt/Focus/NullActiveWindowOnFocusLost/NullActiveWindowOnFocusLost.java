/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Button;
import java.awt.Frame;
import java.util.concurrent.TimeUnit;

import sun.awt.SunToolkit;

/**
 * @test
 * @key headful
 * @bug 8211435
 * @modules java.desktop/sun.awt
 */
public final class NullActiveWindowOnFocusLost {

    private static volatile long endtime;
    private static Throwable failed;

    public static void main(final String[] args) throws Exception {
        // Will run the test no more than 30 seconds
        endtime = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> failed = e);

        final Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = testThread(i);
        }
        for (final Thread thread : threads) {
            thread.start();
        }
        for (final Thread thread : threads) {
            thread.join();
        }
        if (failed != null) {
            failed.printStackTrace();
            throw new RuntimeException(failed);
        }
    }

    private static Thread testThread(int index) {
        return new Thread(new ThreadGroup("TG " + index), () -> {
            SunToolkit.createNewAppContext();
            while (!isComplete()) {
                final Frame frame = new Frame();
                frame.setSize(300, 300);
                frame.add(new Button("Button"));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                try {
                    Thread.sleep(index); // increase probability of the failure
                } catch (InterruptedException ignored) {
                }
                frame.dispose();
            }
        });
    }

    private static boolean isComplete() {
        return endtime - System.nanoTime() < 0 || failed != null;
    }
}
