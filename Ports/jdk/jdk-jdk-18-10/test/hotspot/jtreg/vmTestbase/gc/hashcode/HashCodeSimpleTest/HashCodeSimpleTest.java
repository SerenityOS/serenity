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
 * @summary converted from VM Testbase gc/hashcode/HashCodeSimpleTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Hash code regressiontests.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.hashcode.HashCodeSimpleTest.HashCodeSimpleTest
 */

package gc.hashcode.HashCodeSimpleTest;

import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.ThreadedGCTest;

/**
 * Hash code regressiontests.
 */
public class HashCodeSimpleTest extends ThreadedGCTest {

    @Override
    protected Runnable createRunnable(int i) {



        /**
         * Test verifies that VM provided hashcodes are constant over invocations.
         * @return success if the test passes
         *          failure if some hashvalue has changed
         */
        return new Runnable() {
            long counter = 0;


            @Override
            public void run() {

                Object object = new Object();
                int hashCode0 = object.hashCode();

                for (int i = 0; i < 100; i++) {
                    int hashCode = object.hashCode();

                    if (hashCode != hashCode0) {
                        throw new TestFailure("Repeated hash code queries broken: "
                                + hashCode0 + "!=" + hashCode);
                    }
                }
                if (counter++ % 1000000 == 0) {
                    log.info(counter / 1000000 + " million hashcodes verified");
                }


            }
        };
    }

    public static void main(String args[]) {
        GC.runTest(new HashCodeSimpleTest(), args);
    }
}
