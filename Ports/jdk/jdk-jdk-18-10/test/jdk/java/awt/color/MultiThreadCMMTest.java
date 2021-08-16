/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.DirectColorModel;

/**
 * @test
 * @bug 6245283
 * @summary Checks the behavior of the DirectColorModel.getRed(int)
 *          with multiple threads.
 */
public final class MultiThreadCMMTest extends Thread {
    /* Number of concurent threads creating and accessing
     * DirectColorModel object
     */
    private static final int THREAD_COUNT = 100;
    private static final int ITERATION_COUNT = 20;

    private static volatile boolean failed = false;
    private static volatile Exception failureException = null;

    private static synchronized void setStatusFailed(Exception e) {
        /* Store first occurred exception */
        if (!failed) {
            failureException = e;
            failed = true;
        }
    }

    public static void main(String [] args) throws Exception {

        Thread [] threadArray = new Thread [THREAD_COUNT];
        for (int i = 0; i < ITERATION_COUNT; i++) {
            for (int j = 0; j < threadArray.length; j++) {
                threadArray[j] = new MultiThreadCMMTest();
            };

            for (int j = 0; j < threadArray.length; j++) {
                threadArray[j].start();
            }

            /* Ensure that all threads are finished */
            for (int j = 0; j < threadArray.length; j++) {
                threadArray[j].join();
                if (failed) {
                    throw new RuntimeException(failureException);
                }
            }
        }
    }

    public void run() {
       int rMask16 = 0xF800;
       int gMask16 = 0x07C0;
       int bMask16 = 0x003E;
       int r;
       try {
           for(int i=0; i < 1000; i++) {
               DirectColorModel dcm =
                   new DirectColorModel(16, rMask16, gMask16, bMask16);
                r = dcm.getRed(10);
           }
       } catch(Exception e) {
            setStatusFailed(e);
       }
    }
}
