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

/**
 * @test
 * @bug 8080774
 * @modules jdk.localedata
 * @run testng/othervm -Djava.locale.providers=JRE,CLDR LocaleDateFormats
 * @summary This file contains tests for JRE locales date formats
 */

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LocaleDateFormats {

    @Test(dataProvider = "dateFormats")
    public void testDateFormat(Locale loc, int style, int year, int month, int date, String expectedString) {
        Calendar cal = Calendar.getInstance(loc);
        cal.set(year, month-1, date);
        // Create date formatter based on requested style and test locale
        DateFormat df = DateFormat.getDateInstance(style, loc);
        // Test the date format
        assertEquals(df.format(cal.getTime()), expectedString);
    }

    @DataProvider(name = "dateFormats" )
    private Object[][] dateFormats() {
        return new Object[][] {
            //8080774
            //Locale, Format type, year, month, date, expected result
            {localeEnSG, DateFormat.SHORT, 2015, 5, 6, "6/5/15"},
            {localeEnSG, DateFormat.MEDIUM, 2015, 5, 6, "6 May, 2015"},
            {localeEnSG, DateFormat.LONG, 2015, 5, 6, "6 May, 2015"},
            {localeEnSG, DateFormat.FULL, 2015, 5, 6, "Wednesday, 6 May, 2015"}
        };
    }
    // en_SG Locale instance
    private static final Locale localeEnSG = new Locale("en", "SG");
}
