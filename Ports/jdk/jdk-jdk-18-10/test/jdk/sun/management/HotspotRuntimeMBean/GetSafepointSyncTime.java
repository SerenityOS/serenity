/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4858522 8174734
 * @summary Basic unit test of HotspotRuntimeMBean.getSafepointSyncTime()
 * @author  Steve Bohne
 *
 * @run main/othervm -XX:+UsePerfData GetSafepointSyncTime
 */

/*
 * This test is just a sanity check and does not check for the correct value.
 */

import sun.management.*;

public class GetSafepointSyncTime {

    private static HotspotRuntimeMBean mbean =
        (HotspotRuntimeMBean)ManagementFactoryHelper.getHotspotRuntimeMBean();

    private static final long NUM_THREAD_DUMPS = 300;

    static void checkPositive(long value, String label) {
        if (value < 0)
            throw new RuntimeException(label + " had a negative value of "
                                       + value);
    }

    static void validate(long count1, long count2, long time1, long time2,
                         String label) {
        checkPositive(count1, label + ":count1");
        checkPositive(count2, label + ":count2");
        checkPositive(time1, label + ":time1");
        checkPositive(time2, label + ":time2");

        long countDiff = count2 - count1;
        long timeDiff = time2 - time1;

        if (countDiff < NUM_THREAD_DUMPS) {
            throw new RuntimeException(label +
                                       ": Expected at least " + NUM_THREAD_DUMPS +
                                       " safepoints but only got " + countDiff);
        }

        // getSafepointSyncTime is the accumulated time spent getting to a
        // safepoint, so each safepoint will add a little to this, but the
        // resolution is only milliseconds so we may not see it.
        if (timeDiff < 0) {
            throw new RuntimeException(label + ": Safepoint sync time " +
                                       "decreased unexpectedly " +
                                       "(time1 = " + time1 + "; " +
                                       "time2 = " + time2 + ")");
        }

        System.out.format("%s: Safepoint count=%d (diff=%d), sync time=%d ms (diff=%d)%n",
                          label, count2, countDiff, time2, timeDiff);

    }

    public static void main(String args[]) throws Exception {
        long count = mbean.getSafepointCount();
        long time = mbean.getSafepointSyncTime();

        checkPositive(count, "count");
        checkPositive(time, "time");

        // Thread.getAllStackTraces() should cause a safepoint.

        for (int i = 0; i < NUM_THREAD_DUMPS; i++) {
            Thread.getAllStackTraces();
        }

        long count1 = mbean.getSafepointCount();
        long time1 = mbean.getSafepointSyncTime();

        validate(count, count1, time, time1, "Pass 1");

        // repeat the experiment

        for (int i = 0; i < NUM_THREAD_DUMPS; i++) {
            Thread.getAllStackTraces();
        }

        long count2 = mbean.getSafepointCount();
        long time2 = mbean.getSafepointSyncTime();

        validate(count1, count2, time1, time2, "Pass 2");

        System.out.println("Test passed.");
    }
}
