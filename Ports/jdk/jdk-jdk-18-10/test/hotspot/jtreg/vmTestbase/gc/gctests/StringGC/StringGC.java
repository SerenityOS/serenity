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
 * @key stress
 *
 * @summary converted from VM Testbase gc/gctests/StringGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.StringGC.StringGC
 */

package gc.gctests.StringGC;

import nsk.share.test.*;
import nsk.share.gc.*;

/**
 * Test that added strings are collected.
 *
 * Idea based on old tests:
 *      gc/gctests/StringGC02
 *      gc/gctests/TestStringGC
 */
public class StringGC extends ThreadedGCTest {
        private final String toAdd = "abcdef";
        private int maxLength;

        private class StringAdder implements Runnable {
                private String s;

                public void run() {
                        s = s + toAdd;
                        if (s.length() > maxLength)
                                s = "";
                }
        }

        protected Runnable createRunnable(int i) {
                return new StringAdder();
        }

        public void run() {
                maxLength = (int) Math.min(
                        runParams.getTestMemory() / runParams.getNumberOfThreads() / toAdd.length(),
                        Integer.MAX_VALUE);
                super.run();
        }

        public static void main(String[] args) {
                GC.runTest(new StringGC(), args);
        }
}
