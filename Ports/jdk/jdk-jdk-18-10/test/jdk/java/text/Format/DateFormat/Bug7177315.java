/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7177315
 * @summary Make sure that space characters are properly skipped when
 *          parsing 2-digit year values.
 */

import java.text.*;
import java.util.*;

public class Bug7177315 {
    private static final String EXPECTED = "01/01/2012";
    private static final String[] DATA = {
        "01/01/12",
        "01/01/ 12",
        "01/01/       12",
        "1/1/12",
        "1/1/  12"
    };

    public static void main (String[] args) throws ParseException {
        SimpleDateFormat parseFormat = new SimpleDateFormat("MM/dd/yy", Locale.US);
        Calendar cal = new GregorianCalendar(2012-80, Calendar.JANUARY, 1);
        parseFormat.set2DigitYearStart(cal.getTime());
        SimpleDateFormat fmtFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

        for (String text : DATA) {
            Date date = parseFormat.parse(text);
            String got = fmtFormat.format(date);
            if (!EXPECTED.equals(got)) {
                throw new RuntimeException("got: " + got + ", expected: " + EXPECTED);
            }
        }
    }
}
