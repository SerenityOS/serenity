/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import static org.testng.Assert.assertEquals;

class CompactFormatAndParseHelper {

    static void testFormat(NumberFormat cnf, Object number,
            String expected) {
        String result = cnf.format(number);
        assertEquals(result, expected, "Incorrect formatting of the number '"
                + number + "'");
    }

    static void testParse(NumberFormat cnf, String parseString,
            Number expected, ParsePosition position, Class<? extends Number> returnType) throws ParseException {

        Number number;
        if (position == null) {
            number = cnf.parse(parseString);
        } else {
            number = cnf.parse(parseString, position);
        }

        if (returnType != null) {
            assertEquals(number.getClass(), returnType,
                    "Incorrect return type for string '" + parseString + "'");
        }

        assertEquals(number, expected, "Incorrect parsing of the string '"
                + parseString + "'");
    }
}
