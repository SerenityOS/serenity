/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test %i%
 * @bug 4807540 8008577
 * @modules jdk.localedata
 * @summary updating dateformat for sl_SI
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug4807540
 */

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class Bug4807540 {

    public static void main(String[] args) {
        Locale si = new Locale("sl", "si");

        String expected = "30.4.2008";
        DateFormat dfSi = DateFormat.getDateInstance (DateFormat.MEDIUM, si);

        String siString = new String (dfSi.format(new Date(108, Calendar.APRIL, 30)));

        if (expected.compareTo(siString) != 0) {
            throw new RuntimeException("Error: " + siString  + " should be " + expected);
        }
    }
}
