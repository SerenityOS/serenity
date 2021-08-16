/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @bug 8254370
 * @summary Verifies MT safety of ICC_ColorSpace#To/From methods
 */
public final class MTICC_ColorSpaceToFrom {

    private enum Method {
        FROM_RGB, FROM_XYZ, TO_RGB, TO_XYZ;
    }

    static volatile long endtime;
    static volatile boolean failed;

    public static void main(String[] args) throws Exception {
        ICC_Profile srgb = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        ICC_Profile gray = ICC_Profile.getInstance(ColorSpace.CS_GRAY);
        ICC_Profile xyz = ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ);
        ICC_Profile lrgb = ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB);
        ICC_Profile pycc = ICC_Profile.getInstance(ColorSpace.CS_PYCC);

        // Will run the test no more than 15 seconds
        endtime = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        for (int i = 0; i < 1000 && !isComplete(); i++) {
            for (Method method : new Method[]{Method.FROM_RGB, Method.FROM_XYZ,
                                              Method.TO_RGB, Method.TO_XYZ}) {
                test(new ICC_ColorSpace(srgb), method);
                test(new ICC_ColorSpace(gray), method);
                test(new ICC_ColorSpace(xyz), method);
                test(new ICC_ColorSpace(lrgb), method);
                test(new ICC_ColorSpace(pycc), method);
            }
        }
        if (failed) {
            throw new RuntimeException();
        }
    }

    private static void test(ColorSpace cs, Method method) throws Exception {
        Thread[] ts = new Thread[10];
        CountDownLatch latch = new CountDownLatch(ts.length);
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> {
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException ex) {
                }
                try {
                    switch (method) {
                        case TO_RGB -> cs.toRGB(new float[3]);
                        case FROM_RGB -> cs.fromRGB(new float[3]);
                        case TO_XYZ -> cs.toCIEXYZ(new float[3]);
                        case FROM_XYZ -> cs.fromCIEXYZ(new float[3]);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    failed = true;
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        for (Thread t : ts) {
            t.join();
        }
    }

    private static boolean isComplete() {
        return endtime - System.nanoTime() < 0 || failed;
    }
}
