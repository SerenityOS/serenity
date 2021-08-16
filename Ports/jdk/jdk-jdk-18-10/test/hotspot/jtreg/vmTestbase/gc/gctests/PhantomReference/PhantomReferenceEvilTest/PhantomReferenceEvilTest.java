/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/PhantomReference/PhantomReferenceEvilTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Test that verifies the PhantomReference handling in a more evil way.
 * In this test, it will only keep every Xth object, thus causing more
 * fragmentation and fill the heap with unused objects. This test should
 * not throw any OOME during the test execution.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      -XX:-UseGCOverheadLimit
 *      gc.gctests.PhantomReference.PhantomReferenceEvilTest.PhantomReferenceEvilTest
 */

package gc.gctests.PhantomReference.PhantomReferenceEvilTest;

import gc.gctests.PhantomReference.PhantomHelper;
import gc.gctests.PhantomReference.PRHelper;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.gc.Memory;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.test.Stresser;

/**
 * Tests for the PhantomReference handling in a more evil way.
 *
 * This test must be run with a mx value set to ensure
 * Runtime.maxMemory() doesn't return 0.
 */
public class PhantomReferenceEvilTest extends GCTestBase {

    /**
     * Test that verifies the PhantomReference handling in a more evil way.
     * In this test, it will only keep every Xth object, thus causing more
     * fragmentation and fill the heap with unused objects. This test should
     * not throw any OOME during the test execution.
     *
     * @return success if all phantom references were enqueued
     */
    public final void run() {
        long seed;
        int minSize;
        int maxSize;
        int keepEveryXthObject;
        double memPercentToFill;
        long maxWaitTime;
        long counter = 0;
        long totalMemAlloced = 0;
        long memToAlloc = 0;
        long nrOfPrs = 0;
        Runtime r = Runtime.getRuntime();


        seed = runParams.getSeed();
        minSize = 2048;
        maxSize = 32768;
        keepEveryXthObject = 5;
        memPercentToFill = 0.45;
        maxWaitTime = 30000;
        memToAlloc = (long) (r.maxMemory() * memPercentToFill);
        Random rndGenerator = new Random(seed);
        long multiplier = maxSize - minSize;
        ReferenceQueue rq = new ReferenceQueue();
        HashMap hmHelper = new HashMap();
        ArrayList alPhantomRefs = new ArrayList();

        try {
            try {
                while (totalMemAlloced + Memory.getReferenceSize()
                        * hmHelper.size() < memToAlloc) {
                    int allocationSize = ((int) (rndGenerator.nextDouble()
                            * multiplier)) + minSize;
                    byte[] tmp = new byte[allocationSize];

                    if (counter % keepEveryXthObject == 0) {
                        Integer ik = Integer.valueOf(tmp.hashCode());
                        if (hmHelper.containsKey(ik)) {
                            PhantomHelper ph = (PhantomHelper) hmHelper.get(ik);
                            ph.increaseHashCounter();
                            hmHelper.put(ik, ph);
                        } else {
                            hmHelper.put(ik, new PhantomHelper(tmp.hashCode()));
                        }

                        PRHelper prh = new PRHelper(tmp, rq);
                        prh.setReferentHashCode(tmp.hashCode());
                        alPhantomRefs.add(prh);
                        totalMemAlloced +=
                                Memory.getArraySize(allocationSize, Memory.getByteSize())
                                + Memory.getReferenceSize()
                                + Memory.getReferenceObjectSize();

                        //Make sure the temporary object is dereferenced
                        prh = null;
                        nrOfPrs++;
                    }

                    // Make sure the temporary object is dereferenced
                    tmp = null;
                    counter++;
                    if (counter == Long.MAX_VALUE) {
                        counter = 0;
                    }
                }
            } catch (OutOfMemoryError oome) {
                alPhantomRefs.clear();
                hmHelper.clear();
                log.info(nrOfPrs + " phantom refs had been allocated when "
                        + "OOME occured");
                throw new TestFailure("OutOfMemoryException was thrown. This should "
                        + "not happen during the execution of this test.");
            }


            Stresser stresser = new Stresser(runParams.getStressOptions());
            stresser.start(0);
            GarbageUtils.eatMemory(stresser);
            if (!stresser.continueExecution()) {
                return; //we couldn't be sure that FullGC is triggered
            }

            String retInfo = PhantomHelper.checkAllHashCodes(
                    rq, hmHelper, maxWaitTime);
            if (retInfo != null) {
                alPhantomRefs.clear();
                hmHelper.clear();
                throw new TestFailure(retInfo);
            }

            log.info(nrOfPrs + " phantom refs were allocated during the test");
        } finally {
            // Make sure the ArrayList:s are live at the end of the test
            // to make sure that the references gets enqueued.
            alPhantomRefs.clear();
            hmHelper.clear();
            alPhantomRefs = null;
            hmHelper = null;
        }
    }

    public static void main(String[] args) {
        GC.runTest(new PhantomReferenceEvilTest(), args);
    }
}
