/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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


/*
 * @test
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/gctests/StringInternGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:+ExplicitGCInvokesConcurrent gc.gctests.StringInternGC.StringInternGC
 */

package gc.gctests.StringInternGC;

import nsk.share.test.*;
import nsk.share.gc.*;

/**
 * Test that strings returned by String.intern() can be collected.
 *
 * Create strings consisting of random characters, call String.intern().
 * String pool should not overflow.
 */
public class StringInternGC extends ThreadedGCTest {
        private int maxLength = 1000; // Maximum number of characters to add per operation.
        private int maxTotalLength = 128 * 1024; // Total maximum length of the string until a new StringBuffer will be allocated.
        private long lastTime = System.currentTimeMillis();

        private class StringGenerator implements Runnable {
                private StringBuffer sb = new StringBuffer();

                private String generateString() {
                        int length = LocalRandom.nextInt(maxLength);
                        if (sb.length() > maxTotalLength) {
                                sb = new StringBuffer();
                        }

                        for (int i = 0; i < length; ++i)
                                sb.append((char) LocalRandom.nextInt(Integer.MAX_VALUE));
                        return sb.toString();
                }


                public void run() {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastTime > 5000) { // Cause a full gc every 5s.
                                lastTime = currentTime;
                                System.gc();
                        }

                        generateString().intern();
                }
        }

        protected Runnable createRunnable(int i) {
                return new StringGenerator();
        }

        public static void main(String[] args) {
                GC.runTest(new StringInternGC(), args);
        }
}
