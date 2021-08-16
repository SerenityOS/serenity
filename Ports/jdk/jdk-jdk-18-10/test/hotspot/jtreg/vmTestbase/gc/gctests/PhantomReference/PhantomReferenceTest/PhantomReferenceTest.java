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
 * @summary converted from VM Testbase gc/gctests/PhantomReference/PhantomReferenceTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Test that verifies the PhantomReference handling in JRockit.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      -XX:-UseGCOverheadLimit
 *      gc.gctests.PhantomReference.PhantomReferenceTest.PhantomReferenceTest
 */

package gc.gctests.PhantomReference.PhantomReferenceTest;

import gc.gctests.PhantomReference.PRHelper;
import gc.gctests.PhantomReference.PhantomHelper;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.test.Stresser;

/**
 * Tests for the PhantomReference handling in JRockit.
 */
public class PhantomReferenceTest extends GCTestBase {

    /**
     * Test that verifies the PhantomReference handling in JRockit.
     *
     * @return success if all phantom references were enqueued
     */
    public final void run() {
        long seed;
        int minSize;
        int maxSize;
        int nrOfObjs;
        int sleepTime;
        long maxWaitTime;


        seed = runParams.getSeed();
        minSize = 2048;
        maxSize = 32768;
        nrOfObjs = 1000;
        sleepTime = 10000;
        maxWaitTime = 30000;

        Random rndGenerator = new Random(seed);
        long multiplier = maxSize - minSize;
        ReferenceQueue rq = new ReferenceQueue();
        HashMap hmHelper = new HashMap();
        ArrayList alPhantomRefs = new ArrayList();

        for (int i = 0; i < nrOfObjs; i++) {
            int allocationSize = ((int) (rndGenerator.nextDouble()
                    * multiplier)) + minSize;
            byte[] tmp = new byte[allocationSize];
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
            prh = null;
            tmp = null;
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

        // Make sure the ArrayList:s are live at the end of the test
        // to make sure that the references gets enqueued.
        alPhantomRefs.clear();
        hmHelper.clear();

        alPhantomRefs = null;
        hmHelper = null;
    }

    public static void main(String[] args) {
        GC.runTest(new PhantomReferenceTest(), args);
    }
}
