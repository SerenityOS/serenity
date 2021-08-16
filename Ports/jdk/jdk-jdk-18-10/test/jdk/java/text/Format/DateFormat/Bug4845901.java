/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4845901
 * @summary Make sure that SimpleDateFormat.parse() can distinguish
 * the same time zone abbreviation for standard and daylight saving
 * time.
 * @library /java/text/testlib
 * @run main Bug4845901
 */

import java.util.*;
import java.text.SimpleDateFormat;

public class Bug4845901 {
    public static void main (String args[]) {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            System.out.println("Skipping this test because locale is " + locale);
            return;
        }

        TimeZone savedTZ = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Australia/Sydney"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS z");
        try {
            testParse(sdf, "2003.01.13 11:10:00.802 AEDT", 11);
            testParse(sdf, "2003.06.12 11:10:00.802 AEST", 11);
            testParse(sdf, "2004.12.24 10:10:00.002 AEDT", 10);
            testParse(sdf, "2004.08.10 10:10:00.002 AEST", 10);
        } finally {
            TimeZone.setDefault(savedTZ);
        }
    }

    @SuppressWarnings("deprecation")
    static void testParse(SimpleDateFormat sdf, String str, int expectedHour) {
        try {
            Date parsedDate = sdf.parse(str);
            if (parsedDate.getHours() != expectedHour) {
                throw new RuntimeException(
                        "parsed date has wrong hour: " + parsedDate.getHours()
                        + ", expected: " + expectedHour
                        + "\ngiven string: " + str
                        + "\nparsedDate = " + parsedDate);
            }
        } catch (java.text.ParseException e) {
            throw new RuntimeException("parse exception", e);
        }
    }
}
