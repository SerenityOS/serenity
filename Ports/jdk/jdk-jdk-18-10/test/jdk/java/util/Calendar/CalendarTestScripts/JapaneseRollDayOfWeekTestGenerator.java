/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import java.util.Locale;
import static java.util.Calendar.*;


public class JapaneseRollDayOfWeekTestGenerator {
    private static final String[] ERAS = {
        "BeforeMeiji", "Meiji", "Taisho", "Showa", "Heisei"
    };

    private static final String[] DAYOFWEEK = {
        null, "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private static final int[] DAYOFMONTH = {
        25, 26, 27, 28, 29, 30, 31,
         1,  2,  3,  4,  5,  6,  7
    };

    private static final int DOM_BASE = 7;

    //Usage: JapaneseRollDayOfWeekTestGenerator will generate test script into
    // file rolldow.cts.
    public static void generateTestScript(String[] args) throws Exception{
        Locale.setDefault(Locale.ROOT);

        //This test generator assumes that Heisei has (nYears - 1) or more years.
        int nYears = (args.length == 1) ?
                Math.max(3, Math.min(Integer.parseInt(args[0]), 40)) : 28;

        // Start from Showa 63
        int era = 3;
        int year = 63;
        Path testPath = Paths.get(System.getProperty("test.classes"));
        String scFileName = testPath + "/rolldow.cts";

        try (PrintWriter out = new PrintWriter(scFileName)){
            out.println("locale ja JP JP\n" + "new instance jcal\n" + "use jcal");
            for (int y = 0; y < nYears; y++) {
                out.printf("\nTest %s %d\n", ERAS[era], year);
                for (int fdw = SUNDAY; fdw <= SATURDAY; fdw++) {
                    int endFdw = fdw + 6;
                    if (endFdw > SATURDAY) {
                        endFdw -= 7;
                    }
                    for (int mdifw = 1; mdifw <= 7; mdifw++) {
                        int domIndex = DOM_BASE;
                        out.println(" clear all");
                        out.printf(" set FirstDayOfWeek %s\n", DAYOFWEEK[fdw]);
                        out.printf(" set MinimalDaysInFirstWeek %d\n", mdifw);
                        out.printf(" set date %s %d Jan 1\n", ERAS[era], year);
                        out.println(" get week_of_year\n" + " assign $result $woy");
                        out.println(" get day_of_week\n" + " assign $result $doy");

                        int gyear = year + (year >= 60 ? 1925 : 1988);
                        int next = new GregorianCalendar(gyear, JANUARY, 1).get(DAY_OF_WEEK);
                        for (int i = 0; i < 10; i++) {

                            out.println(" roll day_of_week 1");
                            next = nextFdw(next, fdw, endFdw);
                            if (next == fdw) {
                                domIndex -= 6;
                            } else {
                                domIndex++;
                            }
                            int dom = DAYOFMONTH[domIndex];
                            out.printf("\tcheck date %d %s %d%n", (dom >= 25) ? year - 1 : year,
                                    (dom >= 25) ? "Dec" : "Jan", dom);
                            out.println("\teval $doy + 1");
                            out.println("\tassign $result $doy");
                            out.println("\tassign Sun $doy if $doy > Sat");
                            out.println("\tcheck day_of_week $doy");
                            out.println("\tcheck week_of_year $woy");
                        }

                        for (int i = 0; i < 10; i++) {
                            out.println("\troll day_of_week -1");
                            out.println("\teval $doy - 1");
                            out.println("\tassign $result $doy");
                            out.println("\tassign Sat $doy if $doy < Sun");
                            out.println("\tcheck day_of_week $doy");
                            out.println("\tcheck week_of_year $woy");
                        }
                    }
                }

                if (year == 64 && era == 3) {
                    era++;
                    year = 2;
                } else {
                    year++;
                }
            }
        }
    }

    private static int nextFdw(int x, int start, int end) {
        if (x == end)
            return start;
        x++;
        if (x > SATURDAY)
            x = SUNDAY;
        return x;
    }
}
