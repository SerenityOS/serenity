/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6234795
 * @summary Rolling of HOUR or HOUR_OF_SET must set the other hour field.
 */

import java.util.GregorianCalendar;
import static java.util.Calendar.AM;
import static java.util.Calendar.AM_PM;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.SEPTEMBER;
import java.util.Locale;
import java.util.TimeZone;

public class Bug6234795 {
    public static void main(String[] args) {
        testRoll(HOUR);
        testRoll(HOUR_OF_DAY);
    }

    static void testRoll(int field) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
        cal.clear();
        cal.set(2005, SEPTEMBER, 12);

        int otherField = (field == HOUR) ? HOUR_OF_DAY : HOUR;
        int unit = (field == HOUR) ? 12 : 24;
        int h;
        for (h = 0; h <= 72; h++) {
            int hour = cal.get(otherField);
            int expected = h % 12;
            if (hour != expected) {
                throw new RuntimeException((field == HOUR ? "HOUR" : "HOUR_OF_DAY")
                                           + "+: h=" + h + ", got " + hour
                                           + ", expected " + expected);
            }
            if (field == HOUR_OF_DAY) {
                int ampm = cal.get(AM_PM);
                expected = (h % unit) / 12;
                if (ampm != expected) {
                    throw new RuntimeException((field == HOUR ? "HOUR" : "HOUR_OF_DAY")
                                               + "+: h=" + h + ", got "
                                               + toString(ampm)
                                               + ", expected " + toString(expected));
                }
            }
            cal.roll(field, +1);
        }
        for (; h >= 0; h--) {
            int hour = cal.get(otherField);
            int expected = h % 12;
            if (hour != expected) {
                throw new RuntimeException((field == HOUR ? "HOUR" : "HOUR_OF_DAY")
                                           + "-: h=" + h + ", got " + hour
                                           + ", expected " + expected);
            }
            if (field == HOUR_OF_DAY) {
                int ampm = cal.get(AM_PM);
                expected = (h % unit) / 12;
                if (ampm != expected) {
                    throw new RuntimeException((field == HOUR ? "HOUR" : "HOUR_OF_DAY")
                                               + "-: h=" + h + ", got " + toString(ampm)
                                               + ", expected " + toString(expected));
                }
            }
            cal.roll(field, -1);
        }
    }

    static String toString(int ampm) {
        return ampm == AM ? "AM" : "PM";
    }
}
