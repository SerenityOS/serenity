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

/*
 * @test
 * @bug 8074791
 * @modules jdk.localedata
 * @summary Make sure that Finnish month names are correct in formatted text.
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import static java.text.DateFormat.LONG;
import static java.util.Calendar.JANUARY;

public class Bug8074791 {
    private static Locale FINNISH = new Locale("fi");
    private static String JAN_FORMAT = "tammikuuta";
    private static String JAN_STANDALONE = "tammikuu";

    public static void main(String[] arg) {
        int errors = 0;

        DateFormat df = DateFormat.getDateInstance(LONG, FINNISH);
        Date jan20 = new GregorianCalendar(2015, JANUARY, 20).getTime();
        String str = df.format(jan20).toString();
        // Extract the month name (locale data dependent)
        String month = str.replaceAll(".+\\s([a-z]+)\\s\\d+$", "$1");
        if (!month.equals(JAN_FORMAT)) {
            errors++;
            System.err.println("wrong format month name: got '" + month
                               + "', expected '" + JAN_FORMAT + "'");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("LLLL", FINNISH); // stand-alone month name
        month = sdf.format(jan20);
        if (!month.equals(JAN_STANDALONE)) {
            errors++;
            System.err.println("wrong stand-alone month name: got '" + month
                               + "', expected '" + JAN_STANDALONE + "'");
        }

        if (errors > 0) {
            throw new RuntimeException();
        }
    }
}
