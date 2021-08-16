/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6609750
 * @summary Make sure that SimpleDateFormat.format() formats years correctly.
 */
import java.text.*;
import java.util.*;

public class Bug6609750 {

    public static void main(String[] args) {
        boolean error = false;

        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);

        @SuppressWarnings("deprecation")
        Date[] dates = {
            new Date(9-1900,     Calendar.JUNE, 12),
            new Date(99-1900,    Calendar.JUNE, 12),
            new Date(999-1900,   Calendar.JUNE, 12),
            new Date(2009-1900,  Calendar.JUNE, 12),
            new Date(30009-1900, Calendar.JUNE, 12),
        };

        String[] patterns = {
           "y", "yy", "yyy", "yyyy", "yyyyy", "yyyyyy"
        };
        String[][] expectedResults = {
           {"9",     "09", "009",   "0009",  "00009", "000009"},
           {"99",    "99", "099",   "0099",  "00099", "000099"},
           {"999",   "99", "999",   "0999",  "00999", "000999"},
           {"2009",  "09", "2009",  "2009",  "02009", "002009"},
           {"30009", "09", "30009", "30009", "30009", "030009"},
        };

        SimpleDateFormat sdf = new SimpleDateFormat();
        for (int dateNo = 0; dateNo < dates.length; dateNo++) {
            Date date = dates[dateNo];
            for (int patternNo = 0; patternNo < patterns.length; patternNo++) {
                sdf.applyPattern(patterns[patternNo]);
                String got = sdf.format(date);
                if (!expectedResults[dateNo][patternNo].equals(got)) {
                    error = true;
                    System.err.println("Failed: Unexpected format result: " +
                        "Expected: \"" + expectedResults[dateNo][patternNo] +
                        "\", Got: \"" + got + "\" for date " + date +
                        " with pattern \"" + patterns[patternNo] + "\"");
                }
            }
        }

        Locale.setDefault(defaultLocale);
        if (error) {
            throw new RuntimeException("SimpleDateFormat.format() error.");
        };
    }

}
