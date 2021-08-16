/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @bug 8177758
 * @requires os.family == "windows"
 * @summary Regression in java.awt.FileDialog
 * @run main FileDialogLeakTest
 */

public class FileDialogLeakTest {
    static CountDownLatch latch = new CountDownLatch(3);
    static boolean passed;

    public static void main(String[] args) throws Exception {
        test();
        System.gc();
        System.runFinalization();
        latch.await(1, TimeUnit.SECONDS);
        if (!passed) {
            throw new RuntimeException("Test failed.");
        }
    }

    private static void test() throws Exception {
        FileDialog fd = new FileDialog((Frame) null) {
            @Override
            protected void finalize() throws Throwable {
                System.out.println("Finalize");
                super.finalize();
                passed = true;
                latch.countDown();
            }
        };

        new Thread(() -> {
            latch.countDown();
            fd.setVisible(true);
            latch.countDown();
        }).start();
        latch.await(1, TimeUnit.SECONDS);
        fd.dispose();
        latch.await(1, TimeUnit.SECONDS);
    }

}

