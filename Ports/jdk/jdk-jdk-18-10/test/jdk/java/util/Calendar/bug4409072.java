/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4409072
 * @summary tests for set(), add(), and roll() with various week parameters.
 * @library /java/text/testlib
 * @run main bug4409072
 */

import  java.util.*;
import static java.util.Calendar.*;

public class bug4409072 extends IntlTest {

    public static void main(String[] args) throws Exception {
        new bug4409072().run(args);
    }

    /* Confirm some public methods' behavior in Calendar.
     * (e.g. add(), roll(), set())
     */
    public void Test4409072() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedTZ = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            testSet();
            testAdd();
            testRoll();
        } finally {
            TimeZone.setDefault(savedTZ);
            Locale.setDefault(savedLocale);
        }
    }

    /*
     * Golden data for set() test
     */
    static final int[][][] resultWOMForSetTest = {
        { /* For year1998 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11,11}, {11,11},
          /* Mon */ {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11,11},
          /* Tue */ {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4},
          /* Wed */ {10,27}, {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4},
          /* Thu */ {10,27}, {10,27}, {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11, 4},
          /* Fri */ {10,27}, {10,27}, {10,27}, {11, 4}, {11, 4}, {11, 4}, {11, 4},
          /* Sat */ {11, 4}, {11, 4}, {11, 4}, {11, 4}, {11,11}, {11,11}, {11,11},
        },
        { /* For year1999 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11,10}, {11,10}, {11,10},
          /* Mon */ {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11,10}, {11,10},
          /* Tue */ {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11,10},
          /* Wed */ {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3},
          /* Thu */ {10,26}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3},
          /* Fri */ {10,26}, {10,26}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3},
          /* Sat */ {11, 3}, {11, 3}, {11, 3}, {11,10}, {11,10}, {11,10}, {11,10},
        },
        { /* For year2000 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {11, 1}, {11, 1}, {11, 8}, {11, 8}, {11, 8}, {11, 8}, {11, 8},
          /* Mon */ {11, 1}, {11, 1}, {11, 1}, {11, 8}, {11, 8}, {11, 8}, {11, 8},
          /* Tue */ {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 8}, {11, 8}, {11, 8},
          /* Wed */ {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 8}, {11, 8},
          /* Thu */ {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 8},
          /* Fri */ {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1},
          /* Sat */ {11, 1}, {11, 8}, {11, 8}, {11, 8}, {11, 8}, {11, 8}, {11, 8},
        },
        { /* For year2001 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {10,30}, {11, 7}, {11, 7}, {11, 7}, {11, 7}, {11, 7}, {11, 7},
          /* Mon */ {10,30}, {10,30}, {11, 7}, {11, 7}, {11, 7}, {11, 7}, {11, 7},
          /* Tue */ {10,30}, {10,30}, {10,30}, {11, 7}, {11, 7}, {11, 7}, {11, 7},
          /* Wed */ {10,30}, {10,30}, {10,30}, {10,30}, {11, 7}, {11, 7}, {11, 7},
          /* Thu */ {10,30}, {10,30}, {10,30}, {10,30}, {10,30}, {11, 7}, {11, 7},
          /* Fri */ {10,30}, {10,30}, {10,30}, {10,30}, {10,30}, {10,30}, {11, 7},
          /* Sat */ {11, 7}, {11, 7}, {11, 7}, {11, 7}, {11, 7}, {11, 7}, {11, 7},
        },
        { /* For year2002 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11, 6},
          /* Mon */ {10,29}, {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11, 6},
          /* Tue */ {10,29}, {10,29}, {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11, 6},
          /* Wed */ {10,29}, {10,29}, {10,29}, {11, 6}, {11, 6}, {11, 6}, {11, 6},
          /* Thu */ {10,29}, {10,29}, {10,29}, {10,29}, {11, 6}, {11, 6}, {11, 6},
          /* Fri */ {10,29}, {10,29}, {10,29}, {10,29}, {10,29}, {11, 6}, {11, 6},
          /* Sat */ {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11, 6}, {11,13},
        },
        { /* For year2003 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11,12},
          /* Mon */ {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5},
          /* Tue */ {10,28}, {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5},
          /* Wed */ {10,28}, {10,28}, {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5},
          /* Thu */ {10,28}, {10,28}, {10,28}, {11, 5}, {11, 5}, {11, 5}, {11, 5},
          /* Fri */ {10,28}, {10,28}, {10,28}, {10,28}, {11, 5}, {11, 5}, {11, 5},
          /* Sat */ {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11, 5}, {11,12}, {11,12},
        },
        { /* For year2004 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11,10}, {11,10}, {11,10},
          /* Mon */ {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11,10}, {11,10},
          /* Tue */ {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11,10},
          /* Wed */ {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3},
          /* Thu */ {10,26}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3},
          /* Fri */ {10,26}, {10,26}, {11, 3}, {11, 3}, {11, 3}, {11, 3}, {11, 3},
          /* Sat */ {11, 3}, {11, 3}, {11, 3}, {11,10}, {11,10}, {11,10}, {11,10},
        },
        { /* For year2005 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {11, 2}, {11, 2}, {11, 2}, {11, 9}, {11, 9}, {11, 9}, {11, 9},
          /* Mon */ {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 9}, {11, 9}, {11, 9},
          /* Tue */ {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 9}, {11, 9},
          /* Wed */ {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 9},
          /* Thu */ {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 2},
          /* Fri */ {10,25}, {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 2}, {11, 2},
          /* Sat */ {11, 2}, {11, 2}, {11, 9}, {11, 9}, {11, 9}, {11, 9}, {11, 9},
        },
        { /* For year2006 */
          /* Min =     1        2        3        4        5        6        7  */
          /* Sun */ {11, 1}, {11, 1}, {11, 8}, {11, 8}, {11, 8}, {11, 8}, {11, 8},
          /* Mon */ {11, 1}, {11, 1}, {11, 1}, {11, 8}, {11, 8}, {11, 8}, {11, 8},
          /* Tue */ {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 8}, {11, 8}, {11, 8},
          /* Wed */ {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 8}, {11, 8},
          /* Thu */ {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 8},
          /* Fri */ {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1}, {11, 1},
          /* Sat */ {11, 1}, {11, 8}, {11, 8}, {11, 8}, {11, 8}, {11, 8}, {11, 8},
        },
    };

    static final int[][][] resultWOYForSetTest1 = {
        { /* For year1998 */
            /* FirstDayOfWeek = Sunday */
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 9},
              {1998, 0, 9}, {1998, 0, 9}, {1998, 0, 9},
            /* FirstDayOfWeek = Monday */
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2},
              {1998, 0, 9}, {1998, 0, 9}, {1998, 0, 9},
            /* FirstDayOfWeek = Tuesday */
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2},
              {1998, 0, 2}, {1998, 0, 9}, {1998, 0, 9},
            /* FirstDayOfWeek = Wednesday */
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2},
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 9},
            /* FirstDayOfWeek = Thursday */
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2},
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2},
            /* FirstDayOfWeek = Friday */
              {1997,11,26}, {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2},
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 2},
            /* FirstDayOfWeek = Saturday */
              {1998, 0, 2}, {1998, 0, 2}, {1998, 0, 9}, {1998, 0, 9},
              {1998, 0, 9}, {1998, 0, 9}, {1998, 0, 9},
        },
        { /* For year1999 */
            /* FirstDayOfWeek = Sunday */
              {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 8}, {1999, 0, 8},
              {1999, 0, 8}, {1999, 0, 8}, {1999, 0, 8},
            /* FirstDayOfWeek = Monday */
              {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 8},
              {1999, 0, 8}, {1999, 0, 8}, {1999, 0, 8},
            /* FirstDayOfWeek = Tuesday */
              {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1},
              {1999, 0, 8}, {1999, 0, 8}, {1999, 0, 8},
            /* FirstDayOfWeek = Wednesday */
              {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1},
              {1999, 0, 1}, {1999, 0, 8}, {1999, 0, 8},
            /* FirstDayOfWeek = Thursday */
              {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1},
              {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 8},
            /* FirstDayOfWeek = Friday */
              {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1},
              {1999, 0, 1}, {1999, 0, 1}, {1999, 0, 1},
            /* FirstDayOfWeek = Saturday */
              {1999, 0, 1}, {1999, 0, 8}, {1999, 0, 8}, {1999, 0, 8},
              {1999, 0, 8}, {1999, 0, 8}, {1999, 0, 8},
        },
        { /* For year2000 */
            /* FirstDayOfWeek = Sunday */
              {1999,11,31}, {2000, 0, 7}, {2000, 0, 7}, {2000, 0, 7},
              {2000, 0, 7}, {2000, 0, 7}, {2000, 0, 7},
            /* FirstDayOfWeek = Monday */
              {1999,11,31}, {1999,11,31}, {2000, 0, 7}, {2000, 0, 7},
              {2000, 0, 7}, {2000, 0, 7}, {2000, 0, 7},
            /* FirstDayOfWeek = Tuesday */
              {1999,11,31}, {1999,11,31}, {1999,11,31}, {2000, 0, 7},
              {2000, 0, 7}, {2000, 0, 7}, {2000, 0, 7},
            /* FirstDayOfWeek = Wednesday */
              {1999,11,31}, {1999,11,31}, {1999,11,31}, {1999,11,31},
              {2000, 0, 7}, {2000, 0, 7}, {2000, 0, 7},
            /* FirstDayOfWeek = Thursday */
              {1999,11,31}, {1999,11,31}, {1999,11,31}, {1999,11,31},
              {1999,11,31}, {2000, 0, 7}, {2000, 0, 7},
            /* FirstDayOfWeek = Friday */
              {1999,11,31}, {1999,11,31}, {1999,11,31}, {1999,11,31},
              {1999,11,31}, {1999,11,31}, {2000, 0, 7},
            /* FirstDayOfWeek = Saturday */
              {2000, 0, 7}, {2000, 0, 7}, {2000, 0, 7}, {2000, 0, 7},
              {2000, 0, 7}, {2000, 0, 7}, {2000, 0, 7},
        },
        { /* For year2001 */
            /* FirstDayOfWeek = Sunday */
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0,12},
            /* FirstDayOfWeek = Monday */
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
            /* FirstDayOfWeek = Tuesday */
              {2000,11,29}, {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
            /* FirstDayOfWeek = Wednesday */
              {2000,11,29}, {2000,11,29}, {2001, 0, 5}, {2001, 0, 5},
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
            /* FirstDayOfWeek = Thursday */
              {2000,11,29}, {2000,11,29}, {2000,11,29}, {2001, 0, 5},
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
            /* FirstDayOfWeek = Friday */
              {2000,11,29}, {2000,11,29}, {2000,11,29}, {2000,11,29},
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
            /* FirstDayOfWeek = Saturday */
              {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5}, {2001, 0, 5},
              {2001, 0, 5}, {2001, 0,12}, {2001, 0,12},
        },
        { /* For year2002 */
            /* FirstDayOfWeek = Sunday */
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
              {2002, 0, 4}, {2002, 0,11}, {2002, 0,11},
            /* FirstDayOfWeek = Monday */
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0,11},
            /* FirstDayOfWeek = Tuesday */
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
            /* FirstDayOfWeek = Wednesday */
              {2001,11,28}, {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
            /* FirstDayOfWeek = Thursday */
              {2001,11,28}, {2001,11,28}, {2002, 0, 4}, {2002, 0, 4},
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
            /* FirstDayOfWeek = Friday */
              {2001,11,28}, {2001,11,28}, {2001,11,28}, {2002, 0, 4},
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
            /* FirstDayOfWeek = Saturday */
              {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4}, {2002, 0, 4},
              {2002, 0,11}, {2002, 0,11}, {2002, 0,11},
        },
        { /* For year2003 */
            /* FirstDayOfWeek = Sunday */
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3},
              {2003, 0,10}, {2003, 0,10}, {2003, 0,10},
            /* FirstDayOfWeek = Monday */
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3},
              {2003, 0, 3}, {2003, 0,10}, {2003, 0,10},
            /* FirstDayOfWeek = Tuesday */
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3},
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0,10},
            /* FirstDayOfWeek = Wednesday */
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3},
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3},
            /* FirstDayOfWeek = Thursday */
              {2002,11,27}, {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3},
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3},
            /* FirstDayOfWeek = Friday */
              {2002,11,27}, {2002,11,27}, {2003, 0, 3}, {2003, 0, 3},
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3},
            /* FirstDayOfWeek = Saturday */
              {2003, 0, 3}, {2003, 0, 3}, {2003, 0, 3}, {2003, 0,10},
              {2003, 0,10}, {2003, 0,10}, {2003, 0,10},
        },
        { /* For year2004 */
            /* FirstDayOfWeek = Sunday */
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 9},
              {2004, 0, 9}, {2004, 0, 9}, {2004, 0, 9},
            /* FirstDayOfWeek = Monday */
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2},
              {2004, 0, 9}, {2004, 0, 9}, {2004, 0, 9},
            /* FirstDayOfWeek = Tuesday */
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2},
              {2004, 0, 2}, {2004, 0, 9}, {2004, 0, 9},
            /* FirstDayOfWeek = Wednesday */
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2},
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 9},
            /* FirstDayOfWeek = Thursday */
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2},
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2},
            /* FirstDayOfWeek = Friday */
              {2003,11,26}, {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2},
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 2},
            /* FirstDayOfWeek = Saturday */
              {2004, 0, 2}, {2004, 0, 2}, {2004, 0, 9}, {2004, 0, 9},
              {2004, 0, 9}, {2004, 0, 9}, {2004, 0, 9},
        },
        { /* For year2005 */
            /* FirstDayOfWeek = Sunday */
              {2004,11,31}, {2005, 0, 7}, {2005, 0, 7}, {2005, 0, 7},
              {2005, 0, 7}, {2005, 0, 7}, {2005, 0, 7},
            /* FirstDayOfWeek = Monday */
              {2004,11,31}, {2004,11,31}, {2005, 0, 7}, {2005, 0, 7},
              {2005, 0, 7}, {2005, 0, 7}, {2005, 0, 7},
            /* FirstDayOfWeek = Tuesday */
              {2004,11,31}, {2004,11,31}, {2004,11,31}, {2005, 0, 7},
              {2005, 0, 7}, {2005, 0, 7}, {2005, 0, 7},
            /* FirstDayOfWeek = Wednesday */
              {2004,11,31}, {2004,11,31}, {2004,11,31}, {2004,11,31},
              {2005, 0, 7}, {2005, 0, 7}, {2005, 0, 7},
            /* FirstDayOfWeek = Thursday */
              {2004,11,31}, {2004,11,31}, {2004,11,31}, {2004,11,31},
              {2004,11,31}, {2005, 0, 7}, {2005, 0, 7},
            /* FirstDayOfWeek = Friday */
              {2004,11,31}, {2004,11,31}, {2004,11,31}, {2004,11,31},
              {2004,11,31}, {2004,11,31}, {2005, 0, 7},
            /* FirstDayOfWeek = Saturday */
              {2005, 0, 7}, {2005, 0, 7}, {2005, 0, 7}, {2005, 0, 7},
              {2005, 0, 7}, {2005, 0, 7}, {2005, 0, 7},
        },
        { /* For year2006 */
            /* FirstDayOfWeek = Sunday */
              {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6},
              {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6},
            /* FirstDayOfWeek = Monday */
              {2005,11,30}, {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6},
              {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6},
            /* FirstDayOfWeek = Tuesday */
              {2005,11,30}, {2005,11,30}, {2006, 0, 6}, {2006, 0, 6},
              {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6},
            /* FirstDayOfWeek = Wednesday */
              {2005,11,30}, {2005,11,30}, {2005,11,30}, {2006, 0, 6},
              {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6},
            /* FirstDayOfWeek = Thursday */
              {2005,11,30}, {2005,11,30}, {2005,11,30}, {2005,11,30},
              {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6},
            /* FirstDayOfWeek = Friday */
              {2005,11,30}, {2005,11,30}, {2005,11,30}, {2005,11,30},
              {2005,11,30}, {2006, 0, 6}, {2006, 0, 6},
            /* FirstDayOfWeek = Saturday */
              {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6}, {2006, 0, 6},
              {2006, 0, 6}, {2006, 0, 6}, {2006, 0,13},
        }
    };

    static final int[][] resultWOYForSetTest2 = {
          /* Min =    1       2       3       4       5       6       7  */
          /* Sun */ {4,25}, {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1},
          /* Mon */ {4,25}, {4,25}, {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1},
          /* Tue */ {4,25}, {4,25}, {4,25}, {5, 1}, {5, 1}, {5, 1}, {5, 1},
          /* Wed */ {4,25}, {4,25}, {4,25}, {4,25}, {5, 1}, {5, 1}, {5, 1},
          /* Thu */ {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 8}, {5, 8},
          /* Fri */ {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 8},
          /* Sat */ {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1}, {5, 1},
    };

    /**
     * Test for set()
     */
    void testSet() {
        boolean noerror = true;
        Calendar cal = Calendar.getInstance();
        int sYear=1998;
        int eYear=2006;

        // Loop for FirstDayOfWeek: SUNDAY..SATURDAY
        for (int dow = SUNDAY; dow <= SATURDAY; dow++) {

            // Loop for MinimalDaysInFirstWeek: 1..7
            for (int minDow = 1; minDow <= 7; minDow++) {
                int index = (dow-1)*7 + (minDow-1);

                cal.clear();
                cal.setLenient(true);
                cal.setMinimalDaysInFirstWeek(minDow);
                cal.setFirstDayOfWeek(dow);
                cal.set(YEAR, 2005);
                cal.set(DAY_OF_WEEK, WEDNESDAY);
                cal.set(WEEK_OF_YEAR, 22);

                int y = 2005;
                int m = resultWOYForSetTest2[index][0];
                int d = resultWOYForSetTest2[index][1];
                int year = cal.get(YEAR);
                int month = cal.get(MONTH);
                int date = cal.get(DATE);

                if (cal.get(WEEK_OF_YEAR) != 22) {
                    noerror = false;
                    errln("Failed : set(WEEK_OF_YEAR=22)" +
                          " *** get(WEEK_OF_YEAR=" +
                          cal.get(WEEK_OF_YEAR) + ")" +
                          ", got " + (month+1)+"/"+date+"/"+year +
                          ", expected " + (m+1)+"/"+d+"/"+2005 +
                          ", MinFirstDOW=" + minDow +
                          ", FirstDOW=" + dow);
                } else if ((year != y) || (month != m) || (date != d)) {
                    noerror = false;
                    errln("Failed : set(WEEK_OF_YEAR=22)" +
                          " got " + (month+1)+"/"+date+"/"+year +
                          ", expected " + (m+1)+"/"+d+"/"+y +
                          ", MinFirstDOW=" + minDow +
                          ", FirstDOW=" + dow);
                }

                for (int targetYear = sYear; targetYear<= eYear; targetYear++) {
                    cal.clear();
                    cal.setLenient(true);
                    cal.setMinimalDaysInFirstWeek(minDow);
                    cal.setFirstDayOfWeek(dow);
                    cal.set(YEAR, targetYear);
                    cal.set(DAY_OF_WEEK, FRIDAY);
                    cal.set(MONTH, DECEMBER);
                    cal.set(WEEK_OF_MONTH, 1);

                    y = targetYear;
                    m = resultWOMForSetTest[targetYear-sYear][index][0];
                    d = resultWOMForSetTest[targetYear-sYear][index][1];
                    year = cal.get(YEAR);
                    month = cal.get(MONTH);
                    date = cal.get(DATE);

                    if ((year != y) || (month != m) || (date != d)) {
                        noerror = false;
                        errln("Failed : set(WEEK_OF_MONTH=1)" +
                              " got " + (month+1)+"/"+date+"/"+year +
                              ", expected " + (m+1)+"/"+d+"/"+y +
                              ", MinFirstDOW=" + minDow +
                              ", FirstDOW=" + dow);
                    }

                    cal.clear();
                    cal.setLenient(true);
                    cal.setMinimalDaysInFirstWeek(minDow);
                    cal.setFirstDayOfWeek(dow);
                    cal.set(YEAR, targetYear);
                    cal.set(DAY_OF_WEEK, FRIDAY);
                    cal.set(WEEK_OF_YEAR, 1);

                    y = resultWOYForSetTest1[targetYear-sYear][index][0];
                    m = resultWOYForSetTest1[targetYear-sYear][index][1];
                    d = resultWOYForSetTest1[targetYear-sYear][index][2];
                    year = cal.get(YEAR);
                    month = cal.get(MONTH);
                    date = cal.get(DATE);

                    if (cal.get(WEEK_OF_YEAR) != 1) {
                        noerror = false;
                        errln("Failed : set(WEEK_OF_YEAR=1)" +
                              " *** get(WEEK_OF_YEAR=" +
                              cal.get(WEEK_OF_YEAR) + ")" +
                              ", got " + (month+1)+"/"+date+"/"+year +
                              ", expected " + (m+1)+"/"+d+"/"+y +
                              ", MinFirstDOW=" + minDow +
                              ", FirstDOW=" + dow);
                    } else if ((year != y) || (month != m) || (date != d)) {
                        noerror = false;
                        errln("Failed : set(WEEK_OF_YEAR=1)" +
                              " got " + (month+1)+"/"+date+"/"+year +
                              ", expected " + (m+1)+"/"+d+"/"+y +
                              ", MinFirstDOW=" + minDow +
                              ", FirstDOW=" + dow);
                    }
                }
            }
        }

        if (noerror) {
            logln("Passed : set() test");
        }
    }

    /**
     * Test for add()
     */
    void testAdd() {
        boolean noerror = true;
        Calendar cal = Calendar.getInstance();

        // Loop for FirstDayOfWeek: SUNDAY..SATURDAY
        for (int dow = SUNDAY; dow <= SATURDAY; dow++) {

            // Loop for MinimalDaysInFirstWeek: 1..7
            for (int minDow = 1; minDow <= 7; minDow++) {
                int oldWOY, newWOY;

                cal.clear();
                cal.setLenient(true);
                cal.setMinimalDaysInFirstWeek(minDow);
                cal.setFirstDayOfWeek(dow);
                cal.set(2005, DECEMBER, 7);
                oldWOY = cal.get(WEEK_OF_YEAR);

                for (int cnt = 0; cnt < 7; cnt++) {
                    cal.add(WEEK_OF_YEAR, 1);
                }

                int year = cal.get(YEAR);
                int month = cal.get(MONTH);
                int date = cal.get(DATE);

                if ((year != 2006) || (month != 0) || (date != 25)) {
                    noerror = false;
                    errln("Failed : add(WEEK_OF_YEAR+1)" +
                          " got " + (month+1)+"/"+date+"/"+year +
                          ", expected 1/25/2006" +
                          ", MinFirstDOW=" + minDow +
                          ", FirstDOW=" + dow);
                }

                for (int cnt = 0; cnt < 10; cnt++) {
                    cal.add(WEEK_OF_YEAR, -1);
                }
                newWOY = cal.get(WEEK_OF_YEAR);

                year = cal.get(YEAR);
                month = cal.get(MONTH);
                date = cal.get(DATE);

                if ((oldWOY - newWOY) != 3) {
                    errln("Failed : add(WEEK_OF_YEAR-1)" +
                          " got " + (month+1)+"/"+date+"/"+year +
                          ", expected 11/16/2005" +
                          ", MinFirstDOW=" + minDow +
                          ", FirstDOW=" + dow +
                          ", WEEK_OF_YEAR=" + newWOY +
                          " should be " + (oldWOY-3));
                } else if ((year != 2005) || (month != 10) || (date != 16)) {
                    errln("Failed : add(-1)" +
                          " got " + (month+1)+"/"+date+"/"+year +
                          ", expected 11/16/2005" +
                          ", MinFirstDOW=" + minDow +
                          ", FirstDOW=" + dow);
                }
            }
        }

        if (noerror) {
            logln("Passed : add() test");
        }
    }

    /*
     * Golden data for roll() test
     */
    static final int[] resultWOMForRollTest = {
          /* Min =    1   2   3   4   5   6   7  */
          /* Sun */   1,  1,  1, 26, 26, 26, 26,
          /* Mon */   1,  1,  1,  1, 26, 26, 26,
          /* Tue */  31, 31, 31, 31, 31, 24, 24,
          /* Wed */  31, 31, 31, 31, 31, 31, 24,
          /* Thu */  31, 31, 31, 31, 31, 31, 31,
          /* Fri */   1, 31, 31, 31, 31, 31, 31,
          /* Sat */   1,  1, 31, 31, 31, 31, 31,
    };

    static final int[][] resultWOYForRollTest = {
          /* Min =    1       2       3       4       5       6       7  */
          /* Sun */ {0,26}, {0,26}, {0,26}, {0,26}, {0,26}, {0,26}, {0,26},
          /* Mon */ {1, 2}, {0,26}, {0,26}, {0,26}, {0,26}, {0,26}, {0,26},
          /* Tue */ {1, 2}, {1, 2}, {0,26}, {0,26}, {0,26}, {0,26}, {0,26},
          /* Wed */ {1, 2}, {1, 2}, {1, 2}, {0,26}, {0,26}, {0,26}, {0,26},
          /* Thu */ {0,26}, {0,26}, {0,26}, {0,26}, {0,26}, {1, 2}, {1, 2},
          /* Fri */ {0,26}, {0,26}, {0,26}, {0,26}, {0,26}, {0,26}, {1, 2},
          /* Sat */ {0,26}, {0,26}, {0,26}, {0,26}, {0,26}, {0,26}, {0,26},
    };

    /**
     * Test for roll()
     */
    void testRoll() {
        boolean noerror = true;
        Calendar cal = Calendar.getInstance();

        // Loop for FirstDayOfWeek: SUNDAY..SATURDAY
        for (int dow = SUNDAY; dow <= SATURDAY; dow++) {

            // Loop for MinimalDaysInFirstWeek: 1..7
            for (int minDow = 1; minDow <= 7; minDow++) {
                int oldWOY, newWOY;
                int index = (dow-1)*7 + (minDow-1);

                cal.clear();
                cal.setLenient(true);
                cal.setMinimalDaysInFirstWeek(minDow);
                cal.setFirstDayOfWeek(dow);
                cal.set(2005, DECEMBER, 12);
                oldWOY = cal.get(WEEK_OF_YEAR);
                for (int cnt = 0; cnt < 2; cnt++) {
                    cal.roll(WEEK_OF_MONTH, -1);
                }
                int y = 2005;
                int m = DECEMBER;
                int d = resultWOMForRollTest[index];
                int year = cal.get(YEAR);
                int month = cal.get(MONTH);
                int date = cal.get(DATE);

                if ((year != y) || (month != m) || (date != d)) {
                    noerror = false;
                    errln("Failed : roll(WEEK_OF_MONTH-1)" +
                          " got " + (month+1) + "/" + date + "/" + year +
                          ", expected " + (m+1) + "/" + d + "/" + y +
                          ", MinFirstDOW=" + minDow +
                          ", FirstDOW=" + dow);
                }

                cal.clear();
                cal.setLenient(true);
                cal.setMinimalDaysInFirstWeek(minDow);
                cal.setFirstDayOfWeek(dow);
                cal.set(2005, DECEMBER, 7);
                oldWOY = cal.get(WEEK_OF_YEAR);

                for (int cnt = 0; cnt < 7; cnt++) {
                    cal.roll(WEEK_OF_YEAR, 1);
                }

                y = 2005;
                m = resultWOYForRollTest[index][0];
                d = resultWOYForRollTest[index][1];
                year = cal.get(YEAR);
                month = cal.get(MONTH);
                date = cal.get(DATE);

                if ((year != y) || (month != m) || (date != d)) {
                    noerror = false;
                    errln("Failed : roll(WEEK_OF_YEAR+1)" +
                          " got " + (month+1) + "/" + date + "/" + year +
                          ", expected " + (m+1) + "/" + d + "/" + y +
                          ", MinFirstDOW=" + minDow +
                          ", FirstDOW=" + dow);
                }

                for (int cnt = 0; cnt < 10; cnt++) {
                    cal.roll(WEEK_OF_YEAR, -1);
                }
                newWOY = cal.get(WEEK_OF_YEAR);

                y = 2005;
                m = NOVEMBER;
                d = 16;
                year = cal.get(YEAR);
                month = cal.get(MONTH);
                date = cal.get(DATE);

                if ((year != y) || (month != m) || (date != d)) {
                    noerror = false;
                    errln("Failed : roll(WEEK_OF_YEAR-1)" +
                          " got " + (month+1)+"/"+date+"/"+year +
                          ", expected " + (m+1)+"/"+d+"/"+y +
                          ", MinFirstDOW=" + minDow +
                          ", FirstDOW=" + dow);
                }
            }
        }

        if (noerror) {
            logln("Passed : roll() test");
        }
    }
}
