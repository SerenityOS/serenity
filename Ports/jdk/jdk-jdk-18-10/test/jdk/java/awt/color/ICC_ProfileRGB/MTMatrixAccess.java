/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ProfileRGB;
import java.util.concurrent.CountDownLatch;

/**
 * @test
 * @bug 6986863
 * @summary Verifies MT safety of ICC_ProfileRGB#getMatrix method
 */
public final class MTMatrixAccess {

    private static volatile boolean failed;

    public static void main(String[] args) throws Exception {
        test((ICC_ProfileRGB) ICC_Profile.getInstance(ColorSpace.CS_sRGB));
        test((ICC_ProfileRGB) ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB));
    }

    private static void test(ICC_ProfileRGB rgb) throws InterruptedException {
        Thread[] threads = new Thread[100];
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    rgb.getMatrix();
                } catch (Throwable t) {
                    t.printStackTrace();
                    failed = true;
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        go.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        if (failed) {
            throw new RuntimeException();
        }
    }
}
