/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4372743
 * @summary test that checks transitions of ERA and YEAR which are caused by add(MONTH).
 * @library /java/text/testlib
 */

import java.util.GregorianCalendar;
import java.util.TimeZone;

import static java.util.GregorianCalendar.*;

public class bug4372743 extends IntlTest {

    public static void main(String[] args) throws Exception {
        new bug4372743().run(args);
    }

    private int[][] data = {
        {AD, 2, MARCH},
        {AD, 2, FEBRUARY},
        {AD, 2, JANUARY},
        {AD, 1, DECEMBER},
        {AD, 1, NOVEMBER},
        {AD, 1, OCTOBER},
        {AD, 1, SEPTEMBER},
        {AD, 1, AUGUST},
        {AD, 1, JULY},
        {AD, 1, JUNE},
        {AD, 1, MAY},
        {AD, 1, APRIL},
        {AD, 1, MARCH},
        {AD, 1, FEBRUARY},
        {AD, 1, JANUARY},
        {BC, 1, DECEMBER},
        {BC, 1, NOVEMBER},
        {BC, 1, OCTOBER},
        {BC, 1, SEPTEMBER},
        {BC, 1, AUGUST},
        {BC, 1, JULY},
        {BC, 1, JUNE},
        {BC, 1, MAY},
        {BC, 1, APRIL},
        {BC, 1, MARCH},
        {BC, 1, FEBRUARY},
        {BC, 1, JANUARY},
        {BC, 2, DECEMBER},
        {BC, 2, NOVEMBER},
        {BC, 2, OCTOBER}};
    private int tablesize = data.length;

    private void check(GregorianCalendar gc, int index) {
        if (gc.get(ERA) != data[index][ERA]) {
            errln("Invalid era :" + gc.get(ERA)
                    + ", expected :" + data[index][ERA]);
        }
        if (gc.get(YEAR) != data[index][YEAR]) {
            errln("Invalid year :" + gc.get(YEAR)
                    + ", expected :" + data[index][YEAR]);
        }
        if (gc.get(MONTH) != data[index][MONTH]) {
            errln("Invalid month :" + gc.get(MONTH)
                    + ", expected :" + data[index][MONTH]);
        }
    }

    public void Test4372743() {
        GregorianCalendar gc;
        TimeZone saveZone = TimeZone.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("PST"));

            /* Set March 3, A.D. 2 */
            gc = new GregorianCalendar(2, MARCH, 3);
            for (int i = 0; i < tablesize; i++) {
                check(gc, i);
                gc.add(MONTH, -1);
            }

            /* Again, Set March 3, A.D. 2 */
            gc = new GregorianCalendar(2, MARCH, 3);
            for (int i = 0; i < tablesize; i += 7) {
                check(gc, i);
                gc.add(MONTH, -7);
            }

            /* Set March 10, 2 B.C. */
            gc = new GregorianCalendar(2, OCTOBER, 10);
            gc.add(YEAR, -3);
            for (int i = tablesize - 1; i >= 0; i--) {
                check(gc, i);
                gc.add(MONTH, 1);
            }

            /* Again, Set March 10, 2 B.C. */
            gc = new GregorianCalendar(2, OCTOBER, 10);
            gc.add(YEAR, -3);
            for (int i = tablesize - 1; i >= 0; i -= 8) {
                check(gc, i);
                gc.add(MONTH, 8);
            }
        } finally {
            TimeZone.setDefault(saveZone);
        }
    }
}
