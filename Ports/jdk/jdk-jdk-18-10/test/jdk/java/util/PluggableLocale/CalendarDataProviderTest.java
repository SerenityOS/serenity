/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7058207 8000986 8062588 8210406
 * @summary CalendarDataProvider tests
 * @library providersrc/foobarutils
 *          providersrc/barprovider
 * @build com.foobar.Utils
 *        com.bar.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI CalendarDataProviderTest
 */

import java.util.Calendar;
import java.util.Locale;

import static java.util.Calendar.WEDNESDAY;

/**
 * Test case for CalendarDataProvider.
 *
 * Test strategy:
 * com.bar.CalendarDataProviderImpl supports only ja_JP_kids locale. It returns
 * unusual week parameter values, WEDNESDAY - first day of week, 7 - minimal
 * days in the first week.
 *
 * A Calendar instance created with ja_JP_kids should use the week parameters
 * provided by com.bar.CalendarDataProviderImpl.
 */
public class CalendarDataProviderTest {

    public static void main(String[] s) {
        new CalendarDataProviderTest().test();
    }

    void test() {
        Locale kids = new Locale("ja", "JP", "kids"); // test provider's supported locale
        Calendar kcal = Calendar.getInstance(kids);

        // check the week parameters
        checkResult("firstDayOfWeek", kcal.getFirstDayOfWeek(), WEDNESDAY);
        checkResult("minimalDaysInFirstWeek", kcal.getMinimalDaysInFirstWeek(), 7);
    }

    private <T> void checkResult(String msg, T got, T expected) {
        if (!expected.equals(got)) {
            String s = String.format("%s: got='%s', expected='%s'", msg, got, expected);
            throw new RuntimeException(s);
        }
    }
}