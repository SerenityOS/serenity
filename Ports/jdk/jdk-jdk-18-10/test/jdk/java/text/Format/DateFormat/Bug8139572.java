/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139572
 * @summary SimpleDateFormat parse month stand-alone format bug
 * @compile -encoding utf-8 Bug8139572.java
 * @modules jdk.localedata
 * @run main Bug8139572
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class Bug8139572 {

    private static final Locale RUSSIAN = new Locale("ru");
    private static final Date SEPT12 = new GregorianCalendar(2015, Calendar.SEPTEMBER, 12).getTime();

    private static final String[] PATTERNS = {
        "L",
        "dd L",
        "dd L yy",
        "dd L yyyy",
        "LL",
        "dd LL",
        "dd LL yy",
        "dd LL yyyy",
        "LLL",
        "dd LLL",
        "dd LLL yy",
        "dd LLL yyyy",
        "LLLL",
        "dd LLLL",
        "dd LLLL yy",
        "dd LLLL yyyy"
    };

    private static final String[] APPLIED = {
        "9",
        "12 09",
        "12 09 15",
        "12 09 2015",
        "09",
        "12 09",
        "12 09 15",
        "12 09 2015",
        "сентября",
        "12 сентября",
        "12 сентября 15",
        "12 сентября 2015",
        "сентября",
        "12 сентября",
        "12 сентября 15",
        "12 сентября 2015"
    };

    private static final String[] EXPECTED = {
        "9",
        "12 9",
        "12 9 15",
        "12 9 2015",
        "09",
        "12 09",
        "12 09 15",
        "12 09 2015",
        "сент.",
        "12 сент.",
        "12 сент. 15",
        "12 сент. 2015",
        "сентябрь",
        "12 сентябрь",
        "12 сентябрь 15",
        "12 сентябрь 2015"
    };

    public static void main(String[] args) throws ParseException {

        for (int i = 0; i < PATTERNS.length; i++) {
            SimpleDateFormat fmt = new SimpleDateFormat(PATTERNS[i], RUSSIAN);
            Date standAloneDate = fmt.parse(APPLIED[i]);
            String str = fmt.format(standAloneDate);
            if (!EXPECTED[i].equals(str)) {
                throw new RuntimeException("bad result: got '" + str + "', expected '" + EXPECTED[i] + "'");
            }
        }

        SimpleDateFormat fmt = new SimpleDateFormat("", RUSSIAN);
        for (int j = 0; j < PATTERNS.length; j++) {
            fmt.applyPattern(PATTERNS[j]);
            String str = fmt.format(SEPT12);
            if (!EXPECTED[j].equals(str)) {
                throw new RuntimeException("bad result: got '" + str + "', expected '" + EXPECTED[j] + "'");
            }
        }
    }
}
