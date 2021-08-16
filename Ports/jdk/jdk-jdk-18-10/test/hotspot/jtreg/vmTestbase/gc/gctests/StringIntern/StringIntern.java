/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/StringIntern.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.StringIntern.StringIntern
 */

package gc.gctests.StringIntern;

import nsk.share.test.*;
import nsk.share.gc.*;

/**
 * Test that strings returned by String.intern() can be collected.
 *
 * Create strings consisting of random characters, call String.intern().
 * Check that intern() contract.
 */
public class StringIntern extends ThreadedGCTest {
        private int maxLength = 1000;
        private int checkCount = 100;

        private class StringGenerator implements Runnable {
                private StringBuffer sb = new StringBuffer();

                private void generateRandomBuffer() {
                        int length = LocalRandom.nextInt(maxLength);
                        for (int i = 0; i < length; ++i)
                                sb.append((char) LocalRandom.nextInt(Integer.MAX_VALUE));
                }

                private String getString() {
                        return sb.toString();
                }

                public void run() {
                        generateRandomBuffer();
                        for (int i = 0; i < checkCount; ++i) {
                                String s1 = getString();
                                String s2 = getString();
                                if (s1.intern() != s2.intern()) {
                                        log.error("Test failed on: " + s1);
                                        setFailed(true);
                                }
                        }
                }
        }

        protected Runnable createRunnable(int i) {
                return new StringGenerator();
        }

        public static void main(String[] args) {
                GC.runTest(new StringIntern(), args);
        }
}
