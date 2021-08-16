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
 * @summary converted from VM Testbase gc/gctests/ObjectMonitorCleanup.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Verifies that object monitor objects are cleared
 * out just like PhantomReferences are.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.ObjectMonitorCleanup.ObjectMonitorCleanup
 */

package gc.gctests.ObjectMonitorCleanup;

import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.test.Stresser;


public class ObjectMonitorCleanup extends GCTestBase {

    /**
     * Verifies that object monitor objects are cleared out
     * just like PhantomReferences are.
     *
     * @return True if successful.
     */
    @Override
    public void run() {
        Stresser stresser = new Stresser(runParams.getStressOptions());
        stresser.start(0);


        MonitorThread mt = new MonitorThread(stresser);
        mt.start();

        try {
            while (stresser.continueExecution()) {
                MonitorThread.otherObject = new byte[(int) (runParams.getTestMemory() / 10000)];
                synchronized (MonitorThread.otherObject) {
                    MonitorThread.otherObject.wait(10);
                }
            }
        } catch (InterruptedException e) {
            synchronized (mt) {
                mt.keepRunning = false;
            }

            try {
                Thread.sleep(runParams.getSleepTime());
            } catch (InterruptedException e1) {
            }

            throw new TestFailure("Problem doing synchronization.");
        }

        try {
            mt.join();

            if (!mt.completedOk) {
                throw new TestFailure("Test thread didn't report "
                        + "successful completion");
            }
        } catch (InterruptedException e) {
            throw new TestFailure("Couldn't wait for thread to finish.");
        }
    }

    public static void main(String[] args) {
        GC.runTest(new ObjectMonitorCleanup(), args);
    }
}
