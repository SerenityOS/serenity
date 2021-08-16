/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5008227
 * @summary Make sure that changes to the Date class don't break java.sql.Timestamp.
 * @modules java.sql
 * @library /java/text/testlib
 */

import java.util.*;
import java.sql.Timestamp;

public class TimestampTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        new TimestampTest().run(args);
    }

    /**
     * 5008227: java.sql.Timestamp.after() is not returning correct result
     *
     * Test before(), after(), equals(), compareTo() and getTime().
     */
    public void Test5008227() {
        long t = System.currentTimeMillis();
        Timestamp ts1 = new Timestamp(t), ts2 = new Timestamp(t);
        ts1.setNanos(999999999);
        ts2.setNanos(  1000000);
        compareTimestamps(ts1, ts2, 1);

        ts1.setTime(t + 1000);
        ts2.setTime(t);
        ts1.setNanos(   999999);
        ts2.setNanos(999999999);
        compareTimestamps(ts1, ts2, 1);

        ts1.setTime(t);
        ts2.setTime(t);
        ts1.setNanos(123456789);
        ts2.setNanos(123456789);
        compareTimestamps(ts1, ts2, 0);

        ts1.setTime(t);
        ts2.setTime(t);
        ts1.setNanos(1);
        ts2.setNanos(2);
        compareTimestamps(ts1, ts2, -1);

        ts1.setTime(t);
        ts2.setTime(t+1000);
        ts1.setNanos(999999);
        ts2.setNanos(     0);
        compareTimestamps(ts1, ts2, -1);
    }

    /**
     * Compares two Timestamps with the expected result.
     *
     * @param ts1 the first Timestamp
     * @param ts2 the second Timestamp
     * @param expect the expected relation between ts1 and ts2; 0 if
     * ts1 equals to ts2, or 1 if ts1 is after ts2, or -1 if ts1 is
     * before ts2.
     */
    private void compareTimestamps(Timestamp ts1, Timestamp ts2, int expected) {
        boolean expectedResult = expected > 0;
        boolean result = ts1.after(ts2);
        if (result != expectedResult) {
            errln("ts1.after(ts2) returned " + result
                  + ". (ts1=" + ts1 + ", ts2=" + ts2 + ")");
        }

        expectedResult = expected < 0;
        result = ts1.before(ts2);
        if (result != expectedResult) {
            errln("ts1.before(ts2) returned " + result
                  + ". (ts1=" + ts1 + ", ts2=" + ts2 + ")");
        }

        expectedResult = expected == 0;
        result = ts1.equals(ts2);
        if (result != expectedResult) {
            errln("ts1.equals(ts2) returned " + result
                  + ". (ts1=" + ts1 + ", ts2=" + ts2 + ")");
        }

        int x = ts1.compareTo(ts2);
        int y = (x > 0) ? 1 : (x < 0) ? -1 : 0;
        if (y != expected) {
            errln("ts1.compareTo(ts2) returned " + x + ", expected "
                  + relation(expected, "") + "0"
                  + ". (ts1=" + ts1 + ", ts2=" + ts2 + ")");
        }
        long t1 = ts1.getTime();
        long t2 = ts2.getTime();
        int z = (t1 > t2) ? 1 : (t1 < t2) ? -1 : 0;
        if (z == 0) {
            int n1 = ts1.getNanos();
            int n2 = ts2.getNanos();
            z = (n1 > n2) ? 1 : (n1 < n2) ? -1 : 0;
        }
        if (z != expected) {
            errln("ts1.getTime() " + relation(z, "==") + " ts2.getTime(), expected "
                  + relation(expected, "==")
                  + ". (ts1=" + ts1 + ", ts2=" + ts2 + ")");
        }
    }

    private static String relation(int x, String whenEqual) {
        return (x > 0) ? ">" : (x < 0) ? "<" : whenEqual;
    }
}
