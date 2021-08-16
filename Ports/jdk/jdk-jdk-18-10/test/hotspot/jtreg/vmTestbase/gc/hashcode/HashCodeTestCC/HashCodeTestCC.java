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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/hashcode/HashCodeTestCC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Test that verifies external hash codes. This class tests the scenario
 * with double FullGC(compaction).
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.hashcode.HashCodeTestCC.HashCodeTestCC
 */

package gc.hashcode.HashCodeTestCC;

import gc.hashcode.HCHelper;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.test.Stresser;

/**
 * Test that verifies external hash codes. This class tests the scenario
 * with double compaction.
 */
public class HashCodeTestCC extends GCTestBase {


    /**
     * Test external hash codes when two compactions have been performed.
     *
     * @return Success if all hash codes matches original hash codes
     */
    @Override
    public final void run() {
        HCHelper hch = new HCHelper(512, 3584, runParams.getSeed(),
                0.7, 10240);

        hch.setupLists();
        // Remove evac list 1 to free some space
        hch.clearList(HCHelper.EVAC_LIST_1);
        Stresser stresser = new Stresser(runParams.getStressOptions());
        stresser.start(0);
        GarbageUtils.eatMemory(stresser);
        if (!stresser.continueExecution()) {
            return;// we didn't trigger GC, nothing
        }

        // Remove evac list 2 to free some more space
        hch.clearList(HCHelper.EVAC_LIST_2);
        GarbageUtils.eatMemory(stresser);
        if (!stresser.continueExecution()) {
            return;// we didn't trigger GC, nothing
        }

        boolean testResult = hch.verifyHashCodes();
        hch.cleanupLists();

         if(!testResult) {
            throw new TestFailure("Some hash codes didn't match");
        }
    }

    public static void main(String[] args) {
        GC.runTest(new HashCodeTestCC(), args);
    }
}
