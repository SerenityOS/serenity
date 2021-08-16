/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6476665 7033534
 * @summary Verifies MT safety of color conversion of Component and Direct Color Model based images
 * @run main MTColConvTest
 */

public class MTColConvTest {
    public final static int THREAD_COUNT = 10;
    public static void main(String [] args) throws InterruptedException {
        Thread [] threads = new Thread[THREAD_COUNT];
        ColConvTest [] tests = new ColConvTest[THREAD_COUNT];
        for (int i = 0; i < threads.length; i+=2) {
            tests[i]= new ColConvDCMTest();
            tests[i].init();
            threads[i] = new Thread(tests[i]);
            tests[i+1] = new ColConvCCMTest();
            tests[i+1].init();
            threads[i+1] = new Thread(tests[i+1]);
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        boolean isPassed = true;
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
            isPassed = isPassed && tests[i].isPassed();
        }

        if (!isPassed) {
            throw new RuntimeException("MT Color Conversion error");
        }
    }
}
