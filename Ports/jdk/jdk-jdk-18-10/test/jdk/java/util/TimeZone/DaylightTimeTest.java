/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6936350
 * @summary Test case for TimeZone.observesDaylightTime()
 */

import java.util.*;
import static java.util.GregorianCalendar.*;

public class DaylightTimeTest {
    private static final int ONE_HOUR = 60 * 60 * 1000; // one hour
    private static final int INTERVAL = 24 * ONE_HOUR;  // one day
    private static final String[] ZONES = TimeZone.getAvailableIDs();
    private static int errors = 0;

    public static void main(String[] args) {

        // Test default TimeZone
        for (String id : ZONES) {
            TimeZone tz = TimeZone.getTimeZone(id);
            long now = System.currentTimeMillis();
            boolean observes = tz.observesDaylightTime();
            boolean found = findDSTTransition(tz, now);
            if (observes != found) {
                // There's a critical section. If DST ends after the
                // System.currentTimeMills() call, there should be
                // inconsistency in the determination. Try the same
                // thing again to see the inconsistency was due to the
                // critical section.
                now = System.currentTimeMillis();
                observes = tz.observesDaylightTime();
                found = findDSTTransition(tz, now);
                if (observes != found) {
                    System.err.printf("%s: observesDaylightTime() should return %s at %d%n",
                                      tz.getID(), found, now);
                    errors++;
                }
            }
        }

        // Test SimpleTimeZone in which observesDaylightTime() is
        // equivalent to useDaylightTime().
        testSimpleTimeZone(new SimpleTimeZone(-8*ONE_HOUR, "X",
                                              APRIL, 1, -SUNDAY, 2*ONE_HOUR,
                                              OCTOBER, -1, SUNDAY, 2*ONE_HOUR,
                                              1*ONE_HOUR));
        testSimpleTimeZone(new SimpleTimeZone(-8*ONE_HOUR, "Y"));

        if (errors > 0) {
            throw new RuntimeException("DaylightTimeTest: failed");
        }
    }

    /**
     * Returns true if it's `now' in DST or there's any
     * standard-to-daylight transition within 50 years after `now'.
     */
    private static boolean findDSTTransition(TimeZone tz, long now) {
        GregorianCalendar cal = new GregorianCalendar(tz, Locale.US);
        cal.setTimeInMillis(now);
        cal.add(YEAR, 50);
        long end = cal.getTimeInMillis();

        for (long t = now; t < end; t += INTERVAL) {
            cal.setTimeInMillis(t);
            if (cal.get(DST_OFFSET) > 0) {
                return true;
            }
        }
        return false;
    }

    private static void testSimpleTimeZone(SimpleTimeZone stz) {
        if (stz.useDaylightTime() != stz.observesDaylightTime()) {
            System.err.printf("Failed: useDaylightTime=%b, observesDaylightTime()=%b%n\t%s%n",
                              stz.useDaylightTime(),stz.observesDaylightTime(), stz);
            errors++;
        }
    }
}
