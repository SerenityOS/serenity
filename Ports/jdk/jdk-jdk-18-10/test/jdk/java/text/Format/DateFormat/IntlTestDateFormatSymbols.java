/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @library /java/text/testlib
 * @summary test International Date Format Symbols
 */
/*
(C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
(C) Copyright IBM Corp. 1996, 1997 - All Rights Reserved

  The original version of this source code and documentation is copyrighted and
owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These materials are
provided under terms of a License Agreement between Taligent and Sun. This
technology is protected by multiple US and International patents. This notice and
attribution to Taligent may not be removed.
  Taligent is a registered trademark of Taligent, Inc.
*/

import java.text.*;
import java.util.*;

public class IntlTestDateFormatSymbols extends IntlTest
{
    public static void main(String[] args) throws Exception {
        new IntlTestDateFormatSymbols().run(args);
    }

    // Test getMonths
    public void TestGetMonths()
    {
        final String[] month;
        DateFormatSymbols symbol;

        symbol=new DateFormatSymbols(Locale.getDefault());

        month=symbol.getMonths();
        int cnt = month.length;

        logln("size = " + cnt);

        for (int i=0; i<cnt; ++i)
        {
            logln(month[i]);
        }
    }

    // Test the API of DateFormatSymbols; primarily a simple get/set set.
    public void TestSymbols()
    {
        DateFormatSymbols fr = new DateFormatSymbols(Locale.FRENCH);

        DateFormatSymbols en = new DateFormatSymbols(Locale.ENGLISH);

        if(en.equals(fr)) {
            errln("ERROR: English DateFormatSymbols equal to French");
        }

        // just do some VERY basic tests to make sure that get/set work

        long count;
        final String[] eras = en.getEras();
        fr.setEras(eras);
        final String[] eras1 = fr.getEras();
        count = eras.length;
        if( count != eras1.length) {
            errln("ERROR: setEras() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! eras[i].equals(eras1[i])) {
                    errln("ERROR: setEras() failed (different string values)");
                }
            }
        }


        final String[] months = en.getMonths();
        fr.setMonths(months);
        final String[] months1 = fr.getMonths();
        count = months.length;
        if( count != months1.length) {
            errln("ERROR: setMonths() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! months[i].equals(months1[i])) {
                    errln("ERROR: setMonths() failed (different string values)");
                }
            }
        }

        final String[] shortMonths = en.getShortMonths();
        fr.setShortMonths(shortMonths);
        final String[] shortMonths1 = fr.getShortMonths();
        count = shortMonths.length;
        if( count != shortMonths1.length) {
            errln("ERROR: setShortMonths() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! shortMonths[i].equals(shortMonths1[i])) {
                    errln("ERROR: setShortMonths() failed (different string values)");
                }
            }
        }

        final String[] weekdays = en.getWeekdays();
        fr.setWeekdays(weekdays);
        final String[] weekdays1 = fr.getWeekdays();
        count = weekdays.length;
        if( count != weekdays1.length) {
            errln("ERROR: setWeekdays() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! weekdays[i].equals(weekdays1[i])) {
                    errln("ERROR: setWeekdays() failed (different string values)");
                }
            }
        }

        final String[] shortWeekdays = en.getShortWeekdays();
        fr.setShortWeekdays(shortWeekdays);
        final String[] shortWeekdays1 = fr.getShortWeekdays();
        count = shortWeekdays.length;
        if( count != shortWeekdays1.length) {
            errln("ERROR: setShortWeekdays() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! shortWeekdays[i].equals(shortWeekdays1[i])) {
                    errln("ERROR: setShortWeekdays() failed (different string values)");
                }
            }
        }

        final String[] ampms = en.getAmPmStrings();
        fr.setAmPmStrings(ampms);
        final String[] ampms1 = fr.getAmPmStrings();
        count = ampms.length;
        if( count != ampms1.length) {
            errln("ERROR: setAmPmStrings() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! ampms[i].equals(ampms1[i])) {
                    errln("ERROR: setAmPmStrings() failed (different string values)");
                }
            }
        }

        long rowCount = 0, columnCount = 0;
        final String[][] strings = en.getZoneStrings();
        fr.setZoneStrings(strings);
        final String[][] strings1 = fr.getZoneStrings();
        rowCount = strings.length;
        for(int i = 0; i < rowCount; i++) {
            columnCount = strings[i].length;
            for(int j = 0; j < columnCount; j++) {
                if( strings[i][j] != strings1[i][j] ) {
                    errln("ERROR: setZoneStrings() failed");
                }
            }
        }

//        final String pattern = DateFormatSymbols.getPatternChars();

        String localPattern, pat1, pat2;
        localPattern = en.getLocalPatternChars();
        fr.setLocalPatternChars(localPattern);
        if(! en.getLocalPatternChars().equals(fr.getLocalPatternChars())) {
            errln("ERROR: setLocalPatternChars() failed");
        }


        DateFormatSymbols foo = new DateFormatSymbols();

        en = (DateFormatSymbols) fr.clone();

        if(! en.equals(fr)) {
            errln("ERROR: Clone failed");
        }
    }
}
