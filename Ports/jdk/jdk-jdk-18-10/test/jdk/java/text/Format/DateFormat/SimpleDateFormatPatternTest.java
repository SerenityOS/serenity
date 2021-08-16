/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4326988 6990146 8231213
 * @summary test SimpleDateFormat, check its pattern in the constructor
 * @run testng/othervm SimpleDateFormatPatternTest
 */
import java.lang.IllegalArgumentException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SimpleDateFormatPatternTest {
    private static String[] validPat = {
            "yyyy-MM-dd h.mm.ss.a z",
            "yyyy'M'd' ahh'mm'ss' z",
            "dd MMMM yyyy hh:mm:ss",
            "d MMM yy HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss",
            "d' / 'MMMM' / 'yyyy HH:mm:ss z",
            "d.M.yyyy H:mm:ss",
            "d' de 'MMMM' de 'yyyy H'h'm'min's's' z",
            "dd. MMMM yyyy HH:mm:ss z",
            "d-M-yyyy H:mm:ss",
            "EEEE''d MMMM G yyyy, H'  'm'  'ss' '",
            "dd.MMM.yyyy HH:mm:ss",
            "yy-MM-dd h:mm:ss.a",
            "d' de 'MMMM' de 'yyyy hh:mm:ss a z",
            "EEEE d MMMM yyyy H' h 'mm' min 'ss' s 'z",
            "d MMMM yyyy H:mm:ss",
            "d/MM/yyyy hh:mm:ss a",
            "EEEE, d, MMMM yyyy HH:mm:ss z",
            "EEEE, d. MMMM yyyy HH.mm.' h' z",
            "EEEE, d' / 'MMMM' / 'yyyy HH:mm:ss z",
            "d/MM/yyyy HH:mm:ss",
            "d MMMM yyyy H:mm:ss z",
            "MMMM d, yyyy h:mm:ss a z",
            "yyyy. MMMM d. H:mm:ss z",
            "d' de 'MMMM' de 'yyyy H:mm:ss z",
            "EEEE, MMMM d, yyyy h:mm:ss a z",
            "d/M/yyyy H:mm:ss",
            "d-MMM-yy HH:mm:ss",
            "EEEE d' de 'MMMM' de 'yyyy hh:mm:ss a z",
            "yyyy'M'd' ahh'mm'ss'",
            "yyyy'MM'dd' EEEE ahh'mm'ss'",
            "EEEE, d MMMM yyyy HH:mm:ss z",

            //6990146: 'Y' for year; 'X' for time zone; 'u' for day number of the week
            "d/M/YYYY H:mm:ss",
            "d-MMM-YY HH:mm:ss",
            "EEEE d' de 'MMMM' de 'YYYY hh:mm:ss a X",
            "YYYY M d ahh:mm:ss",
            "YYYY MM dd EEEE u ahh/mm/ss",
            "EEEE, u, d MMMM YYYY HH:mm:ss X",
            "YYYY M d Z ahh mm ss",
            "YYYY-MM-dd EEEE u ahh-mm-ss",

            //*added for sr-Latn*
            "EEEE, dd. MMMM y. HH.mm.ss zzzz",
            "dd. MMMM y. HH.mm.ss z",
            "dd.MM.y. HH.mm.ss",
            "d.M.yy. HH.mm"
    };

    private static String[] invalidPat = {
            "yyyy'M'd' ahh:mm:ss",
            "EEEe d MM MM yyyy HH' h 'mm zzzZ",
            "d MMMM\\ yyyy, H'  'm' 'g",
            "EEEE d' @# MMMMde 'yyys HHH'mm z",
            "yyyy'MMe 2 #dd' EEEEahh'mm'ss' z,z",
            "yyyy.M.d H;mm.ses",
            "EEEe, d MMMM yyyy h:mm:ss a z",
            "EEEE, MMMM d, 'y y y y h:mm:ss 'o''clock' a z",
            "dd MMMM yyyy 0HHcl:mm:ss z",
            "d.M_M_y.yy1yy HextH:mm|45:",
            "d,D MMMTTTTTTTTTKM yy|+yy HH:m m:ss z",
            "d-MDtM M-yy H:mm:ss",
            "yyyy/M///m/nM/d Dd H:m$m:s s",
            "EEEE, dd. MMMM yyyy HH:m'''m' Uhr 'z",
            //6990146
            "EEEE d' de 'MMMM' de 'YYYY hh:mm:ss a x",
            "EEEE, U, d MMMM YYYY HH:mm:ss Z"
    };

    private static Locale[] locales = DateFormat.getAvailableLocales();
    private static Object[][] dfAllLocalesObj = createAllLocales();
    private static Object[][] invalidPatObj = createPatternObj(invalidPat);
    private static Object[][] validPatObj = createPatternObj(validPat);

    private static Object[][] createAllLocales() {
        Object[][] objArray = new Object[locales.length][];
        for (int i = 0; i < locales.length; i++) {
            objArray[i] = new Object[1];
            objArray[i][0] = locales[i];
        }
        return objArray;
    }

    private static Object[][] createPatternObj(String[] pattern){
        Object[][] objArray = new Object[locales.length * pattern.length][];
        int k = 0;
        for (int i = 0; i < locales.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                objArray[k] = new Object[2];
                objArray[k][0] = pattern[j];
                objArray[k][1] = locales[i];
                k = k + 1;
            }
        }
        return objArray;
    }

    @DataProvider(name = "dfAllLocalesObj")
    Object[][] dfAllLocalesObj() {
        return dfAllLocalesObj;
    }

    @DataProvider(name = "invalidPatternObj")
    Object[][] invalidPatternObj() {
        return invalidPatObj;
    }

    @DataProvider(name = "validPatternObj")
    Object[][] validPatternObj() {
        return validPatObj;
    }

    //check Constructors for invalid pattern
    @Test(dataProvider = "invalidPatternObj",
            expectedExceptions = IllegalArgumentException.class)
    public void testIllegalArgumentException1(String pattern, Locale loc)
            throws IllegalArgumentException {
        Locale.setDefault(loc);
        new SimpleDateFormat(pattern);
    }

    @Test(dataProvider = "invalidPatternObj",
            expectedExceptions = IllegalArgumentException.class)
    public void testIllegalArgumentException2(String pattern, Locale loc)
            throws IllegalArgumentException {
        Locale.setDefault(loc);
        new SimpleDateFormat(pattern, new DateFormatSymbols());
    }

    @Test(dataProvider = "invalidPatternObj",
            expectedExceptions = IllegalArgumentException.class)
    public void testIllegalArgumentException3 (String pattern, Locale loc)
            throws IllegalArgumentException {
        Locale.setDefault(loc);
        new SimpleDateFormat(pattern, Locale.getDefault());
    }

    @Test(dataProvider = "invalidPatternObj",
            expectedExceptions = IllegalArgumentException.class)
    public void testIllegalArgumentException4(String pattern, Locale loc)
            throws IllegalArgumentException {
        Locale.setDefault(loc);
        new SimpleDateFormat().applyPattern(pattern);
    }

    //check Constructors for null pattern
    @Test(dataProvider = "dfAllLocalesObj",
            expectedExceptions = NullPointerException.class)
    public void testNullPointerException1(Locale loc)
            throws NullPointerException {
        Locale.setDefault(loc);
        new SimpleDateFormat(null);
    }

    @Test(dataProvider = "dfAllLocalesObj",
            expectedExceptions = NullPointerException.class)
    public void testNullPointerException2(Locale loc)
            throws NullPointerException {
        Locale.setDefault(loc);
        new SimpleDateFormat(null, new DateFormatSymbols());
    }

    @Test(dataProvider = "dfAllLocalesObj",
            expectedExceptions = NullPointerException.class)
    public void testNullPointerException3(Locale loc)
            throws NullPointerException {
        Locale.setDefault(loc);
        new SimpleDateFormat(null, Locale.getDefault());
    }

    @Test(dataProvider = "dfAllLocalesObj",
            expectedExceptions = NullPointerException.class)
    public void testNullPointerException4(Locale loc)
            throws NullPointerException {
        Locale.setDefault(loc);
        new SimpleDateFormat().applyPattern(null);
    }

    @Test(dataProvider = "validPatternObj")
    //check Constructors for valid pattern
    public void testValidPattern(String pattern, Locale loc) {
        Locale.setDefault(loc);
        new SimpleDateFormat(pattern);
        new SimpleDateFormat(pattern, new DateFormatSymbols());
        new SimpleDateFormat(pattern, Locale.getDefault());
        new SimpleDateFormat().applyPattern(pattern);
    }
}
