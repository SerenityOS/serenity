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
 * @bug 5078053
 * @summary Make sure that Calendar.complete() normalizes stamp[] to
 * COMPUTED. This can be observed through add() and roll().
 */

import java.util.TimeZone;
import static java.util.Calendar.*;

public class Bug5078053 {
    static int errorCount = 0;

    public static void main(String[] args) {
        TimeZone defaultTz = TimeZone.getDefault();

        try {
            TimeZone tz = TimeZone.getTimeZone("Australia/Adelaide");
            TimeZone.setDefault(tz);
            Koyomi cal = new Koyomi();
            cal.setFirstDayOfWeek(2);
            cal.setMinimalDaysInFirstWeek(4);

            // test roll()
            cal.clear();
            // 2002-01-01T00:00:00 in Australia/Adelaide
            cal.setTimeInMillis(1009805400000L);
            System.out.println(cal.getTime());
            // The following set calls shouldn't affect roll() and add()
            cal.set(DAY_OF_WEEK, cal.get(DAY_OF_WEEK));
            cal.set(WEEK_OF_YEAR, cal.get(WEEK_OF_YEAR));
            cal.getTime();
            cal.roll(MONTH, +1);
            System.out.println("roll: " + cal.getTime());
            if (!cal.checkDate(2002, FEBRUARY, 1)) {
                error("roll(MONTH, +1): " + cal.getMessage());
            }
            cal.roll(MONTH, -1);
            if (!cal.checkDate(2002, JANUARY, 1)) {
                error("roll(MONTH, -1): " + cal.getMessage());
            }

            // test add()
            cal.clear();
            // 2002-01-01T00:00:00+0930 in Australia/Adelaide
            cal.setTimeInMillis(1009805400000L);
            cal.set(DAY_OF_WEEK, cal.get(DAY_OF_WEEK));
            cal.set(WEEK_OF_YEAR, cal.get(WEEK_OF_YEAR));
            cal.getTime();
            cal.add(MONTH, +1);
            System.out.println(" add: " + cal.getTime());
            if (!cal.checkDate(2002, FEBRUARY, 1)) {
                error("add(MONTH, +1): " + cal.getMessage());
            }
            cal.add(MONTH, -1);
            if (!cal.checkDate(2002, JANUARY, 1)) {
                error("add(MONTH, -1): " + cal.getMessage());
            }
        }
        finally {
            TimeZone.setDefault(defaultTz);
        }

        checkErrors();
    }

    static void error(String s) {
        System.out.println(s);
        errorCount++;
    }

    static void checkErrors() {
        if (errorCount > 0) {
            throw new RuntimeException("Failed: " + errorCount + " error(s)");
        }
    }
}
