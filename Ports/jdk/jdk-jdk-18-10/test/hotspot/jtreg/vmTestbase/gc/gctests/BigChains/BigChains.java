/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/BigChains.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Creates big chains of allocated objects, with a mix of objects
 * with and without finalizers.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.BigChains.BigChains
 */

package gc.gctests.BigChains;

import nsk.share.gc.GC;
import nsk.share.gc.ThreadedGCTest;

/**
 * Test ported from dev test suite. Creates big chains of
 * allocated objects, with a mix of objects with and without
 * finalizers.
 */
public class BigChains extends ThreadedGCTest {

    public static void main(String[] args) {
        GC.runTest(new BigChains(), args);
    }

    @Override
    protected Runnable createRunnable(int i) {

        return new Runnable() {

            /**
             * Create a big chain of mixed objects and make sure nothing
             * crashes.
             *
             * @return success if the program test could run without any
             * problems.
             */
            public void run() {

                createChain(100000, true);


                int i = 0;

                while (i++ < 100) {
                    createChain(10000, false);
                }
            }

            private void createChain(int noof, boolean firstIsFinalizable) {
                BaseClass first;

                if (firstIsFinalizable) {
                    first = new FinalizerYes();
                } else {
                    first = new FinalizerNo();
                }

                BaseClass current = first;

                for (int i = 0; i < noof; i++) {
                    current.next = new FinalizerNo();
                    current = current.next;
                }

                current.next = first;
            }
        };
    }
}

/**
 * Helper base class for the BigChains test.
 */
class BaseClass {

    /**
     * Base class.
     */
    public BaseClass next;
}

/**
 * Helper class withfinalizer and counter for number of
 * calls to the finalize() method.
 */
class FinalizerYes extends BaseClass {

    private static int noOfFinalized = 0;

    /**
     * Finalizer for the help class, that increments a counter
     * for each call to this method.
     */
    public final void finalize() {
        synchronized (this.getClass()) {
            FinalizerYes.noOfFinalized++;
        }
    }
}

/**
 * Helper class without finalizer.
 */
class FinalizerNo extends BaseClass {
}
