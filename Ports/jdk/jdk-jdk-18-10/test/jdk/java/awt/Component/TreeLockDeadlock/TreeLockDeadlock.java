/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GraphicsConfiguration;
import java.awt.Window;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @test
 * @key headful
 * @bug 8138764
 */
public final class TreeLockDeadlock extends Frame {

    @Override
    public synchronized GraphicsConfiguration getGraphicsConfiguration() {
        return super.getGraphicsConfiguration();
    }

    @Override
    public synchronized void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);
    }

    @Override
    public synchronized float getOpacity() {
        return super.getOpacity();
    }

    public static void main(final String[] args) throws Exception {
        final Window window = new TreeLockDeadlock();
        window.setSize(300, 300);
        test(window);
    }

    private static void test(final Window window) throws Exception {
        final long start = System.nanoTime();
        final long end = start + NANOSECONDS.convert(1, MINUTES);

        final Runnable r1 = () -> {
            while (System.nanoTime() < end) {
                window.setBounds(window.getBounds());
            }
        };
        final Runnable r2 = () -> {
            while (System.nanoTime() < end) {
                window.getGraphicsConfiguration();
                window.getOpacity();
            }
        };

        final Thread t1 = new Thread(r1);
        final Thread t2 = new Thread(r1);
        final Thread t3 = new Thread(r2);
        final Thread t4 = new Thread(r2);

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t1.join();
        t2.join();
        t3.join();
        t4.join();
    }
}
