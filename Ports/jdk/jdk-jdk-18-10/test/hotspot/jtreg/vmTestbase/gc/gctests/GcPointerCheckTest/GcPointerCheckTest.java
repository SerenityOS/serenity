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
 * @summary converted from VM Testbase gc/gctests/GcPointerCheckTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Test that no pointers are broken.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.GcPointerCheckTest.GcPointerCheckTest
 */

package gc.gctests.GcPointerCheckTest;

import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.ThreadedGCTest;
import nsk.share.test.ExecutionController;

/**
 * Test that no pointers are broken.
 */
public class GcPointerCheckTest extends ThreadedGCTest {

    @Override
    protected Runnable createRunnable(int i) {
        return new Test();
    }

    class Test implements Runnable {

        /**
         * Helper class for linking objects together.
         */
        private class PointerHelper {

            public PointerHelper next;
        }
        private PointerHelper first;
        ExecutionController stresser;

        @Override
        public void run() {
            if (stresser == null) {
                stresser = getExecutionController();
            }
            while (stresser.continueExecution()) {
                testGcPointers();
            }
        }

        /**
         * Create a lot of objects and link them together, then verify
         * that the pointers are pointing to the correct type of objects.
         *
         * @return success if all references points to the correct type
         * of object.
         */
        public void testGcPointers() {

            int innerIters = 1;
            int outerIters = 200;

            PointerHelper tmp1;
            PointerHelper tmp2;

            while (outerIters > 0) {
                int i = 0;
                tmp1 = new PointerHelper();
                this.first = tmp1;

                while (i != innerIters) {
                    i++;
                    tmp2 = new PointerHelper();
                    tmp1.next = tmp2;
                    tmp1 = tmp2;
                    tmp2 = new PointerHelper();
                }

                outerIters--;

                if (!checkRefs()) {
                    throw new TestFailure("Some references were bad");
                }
            }
        }

        private boolean checkRefs() {
            PointerHelper iter = this.first;

            for (int i = 0; iter != null; iter = iter.next) {
                i++;

                if (iter.getClass() != PointerHelper.class) {
                    //("GC causer bad ref on " + i);
                    return false;
                }
            }

            return true;
        }
    }

    public static void main(String[] args) {
        GC.runTest(new GcPointerCheckTest(), args);
    }
}
