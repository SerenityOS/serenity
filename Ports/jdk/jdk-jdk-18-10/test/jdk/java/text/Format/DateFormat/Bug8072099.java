/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072099
 * @summary check the date time pattern for <NUMERIC_FIELD> which should
 * not throw ParseException
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Bug8072099 {

    private static String[][] shouldPass = {
        {"ha", "11AM"},
        {"hma", "33AM"},
        {"ka", "24AM"},
        {"yyyMMM", "2016May"},
        {"yyyyDDEEE", "2016366Sat"},
        {"ddmyyyyz", "22111980GMT+5:30"}
    };

    public static void main(String[] args) {

        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            // check the date time pattern which should pass
            for (String[] pattern : shouldPass) {
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat(pattern[0]);
                parseDateTimeInput(dateTimeFormat, pattern[1]);
            }
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    private static void parseDateTimeInput(SimpleDateFormat format,
                                           String inputString) {
        try {
            format.parse(inputString);
        } catch (ParseException ex) {
            throw new RuntimeException("[FAILED: Unable to parse date time"
                    + " string " + inputString + "]");
        }
    }

}
