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
import java.util.concurrent.CountDownLatch;

/**
 * @test
 * @bug 6986863
 * @summary Verifies MT safety of profile activation while a profile is accessed
 */
public final class ProfileActivationDuringPropertyAccess {

    private static volatile boolean failed;
    private static volatile boolean end;

    public static void main(String[] args) throws Exception {
        test(ICC_Profile.getInstance(ColorSpace.CS_sRGB));
        test(ICC_Profile.getInstance(ColorSpace.CS_GRAY));
        test(ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ));
        test(ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB));
        test(ICC_Profile.getInstance(ColorSpace.CS_PYCC));
    }

    private static void test(ICC_Profile profile) throws Exception {
        Thread[] ts = new Thread[100];
        CountDownLatch latch = new CountDownLatch(ts.length);
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> {
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException ex) {
                }
                try {
                    while (!end) {
                        profile.getColorSpaceType(); // try use deferred info
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
        Thread.sleep(1500);
        profile.getPCSType(); // activate profile
        end = true;
        for (Thread t : ts) {
            t.join();
        }
        if (failed) {
            throw new RuntimeException();
        }
    }
}
