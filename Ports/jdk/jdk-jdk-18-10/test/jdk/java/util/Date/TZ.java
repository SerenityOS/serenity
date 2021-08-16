/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug      4108737
 * @summary  java.util.Date doesn't fail if current TimeZone is changed
 */

import java.util.TimeZone;
import java.util.Date;

public class TZ {

    public static void main(String args[]) {
        TimeZone tz = TimeZone.getDefault();
        try {
            testMain();
        } finally {
            TimeZone.setDefault(tz);
        }
    }

    static void testMain() {
        String expectedResult = "Sat Feb 01 00:00:00 PST 1997";

        // load the java.util.Date class in the GMT timezone
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        new Date(); // load the class (to run static initializers)

        // use the class in different timezone
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        @SuppressWarnings("deprecation")
        Date date = new Date(97, 1, 1);
        if (!date.toString().equals(expectedResult)) {
            throw new RuntimeException("Regression bug id #4108737 - Date fails if default time zone changed");
        }
    }
}
