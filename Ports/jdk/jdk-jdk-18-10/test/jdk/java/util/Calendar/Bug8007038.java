/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8007038 8247781
 * @summary Verify ArrayIndexOutOfBoundsException is not thrown on
 *     on calling localizedDateTime().print() with JapaneseChrono
 * @modules java.base/sun.util.locale.provider
 * @modules jdk.localedata
 * @compile -XDignore.symbol.file Bug8007038.java
 * @run main/othervm -Djava.locale.providers=COMPAT Bug8007038 COMPAT
 * @run main/othervm -Djava.locale.providers=CLDR Bug8007038 CLDR
 */

import java.util.*;
import static java.util.Calendar.*;
import sun.util.locale.provider.CalendarDataUtility;

public class Bug8007038 {
    private static final String[] calTypes = {
        "gregory",
        "buddhist",
        "japanese",
        "roc",
        "islamic",
    };
    private static final int[][] eraMinMax = {
        {GregorianCalendar.BC, GregorianCalendar.AD},
        {0, 1},
        {0, 5},
        {0, 1},
        {0, 1},
        {0, 1},
    };
    private static final Locale[] testLocs = {
        Locale.ROOT,
        Locale.forLanguageTag("ja-JP-u-ca-japanese"),
        Locale.forLanguageTag("th-TH"),
        Locale.forLanguageTag("th-TH-u-ca-buddhist"),
        Locale.forLanguageTag("zh-TW-u-ca-roc"),
        Locale.forLanguageTag("ar-EG-u-ca-islamic"),
        Locale.forLanguageTag("xx-YY-u-ca-bogus"),
    };

    public static void main(String[] args) {
        for (int calIdx  = 0; calIdx  < calTypes.length; calIdx++) {
            for (int locIdx = 0; locIdx < testLocs.length; locIdx++) {
                // era
                for (int fieldIdx = eraMinMax[calIdx][0]; fieldIdx <= eraMinMax[calIdx][1]; fieldIdx++) {
                    checkValueRange(calTypes[calIdx], ERA, fieldIdx, LONG, testLocs[locIdx], true);
                }
                checkValueRange(calTypes[calIdx], ERA, eraMinMax[calIdx][0]-1, LONG,
                                testLocs[locIdx], false);
                checkValueRange(calTypes[calIdx], ERA, eraMinMax[calIdx][1]+1,
                                LONG, testLocs[locIdx], false);

                // month
                for (int fieldIdx = JANUARY; fieldIdx <= UNDECIMBER ; fieldIdx++) {
                    checkValueRange(calTypes[calIdx], MONTH, fieldIdx, LONG, testLocs[locIdx], true);
                }
                checkValueRange(calTypes[calIdx], MONTH, JANUARY-1, LONG, testLocs[locIdx], false);
                checkValueRange(calTypes[calIdx], MONTH, UNDECIMBER+1, LONG, testLocs[locIdx], false);

                // day-of-week
                for (int fieldIdx = SUNDAY; fieldIdx <= SATURDAY; fieldIdx++) {
                    checkValueRange(calTypes[calIdx], DAY_OF_WEEK, fieldIdx, LONG, testLocs[locIdx], true);
                }
                checkValueRange(calTypes[calIdx], DAY_OF_WEEK, SUNDAY-1, LONG, testLocs[locIdx], false);
                checkValueRange(calTypes[calIdx], DAY_OF_WEEK, SATURDAY+1, LONG, testLocs[locIdx], false);

                // am/pm
                int lastIndex = args[0].equals("CLDR") ? 11 : PM;
                for (int fieldIdx = AM; fieldIdx <= lastIndex; fieldIdx++) {
                    checkValueRange(calTypes[calIdx], AM_PM, fieldIdx, LONG, testLocs[locIdx], true);
                }
                checkValueRange(calTypes[calIdx], AM_PM, AM-1, LONG, testLocs[locIdx], false);
                checkValueRange(calTypes[calIdx], AM_PM, lastIndex+1, LONG, testLocs[locIdx], false);
            }
        }
    }

    private static void checkValueRange(String calType, int field, int value, int style, Locale l, boolean isNonNull) {
        String ret = CalendarDataUtility.retrieveJavaTimeFieldValueName(calType, field, value, style, l);
        System.out.print("retrieveFieldValueName("+calType+", "+field+", "+value+", "+style+", "+l+")");
        if ((ret != null) == isNonNull) {
            System.out.println(" returned "+ret);
        } else {
            throw new RuntimeException("The call returned "+ret);
        }
    }
}
