/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008577 8037343
 * @modules jdk.localedata
 * @summary updating dateformat for es_DO
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug8037343
 */

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Bug8037343
{

    public static void main(String[] arg)
    {
        final Locale esDO = new Locale("es", "DO");
        final String expectedShort = "31/03/12";
        final String expectedMedium = "31/03/2012";

        int errors = 0;
        DateFormat format;
        String result;

        Calendar cal = Calendar.getInstance(esDO);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.YEAR, 2012);

        format = DateFormat.getDateInstance(DateFormat.SHORT, esDO);
        result = format.format(cal.getTime());
        if (!expectedShort.equals(result)) {
            System.out.println(String.format("Short Date format for es_DO is not as expected. Expected: [%s] Actual: [%s]", expectedShort, result));
            errors++;
        }

        format = DateFormat.getDateInstance(DateFormat.MEDIUM, esDO);
        result = format.format(cal.getTime());
        if (!expectedMedium.equals(result)) {
            System.out.println(String.format("Medium Date format for es_DO is not as expected. Expected: [%s] Actual: [%s]", expectedMedium, result));
            errors++;
        }

        if (errors > 0) {
            throw new RuntimeException();
        }
    }

}
