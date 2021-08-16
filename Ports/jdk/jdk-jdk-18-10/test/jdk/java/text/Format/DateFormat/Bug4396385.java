/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4396385
 * @summary Make sure to detect invalid values for 1-based hour formats.
 */

import java.text.*;
import java.util.*;

public class Bug4396385 {
    private static int errorCount = 0;
    private static String[][] data = {
        { "hh:mma", "-1:30AM" },
        { "hh:mma", "00:30AM" },
        { "hh:mma", "13:30AM" },
        { "kk:mm",  "-1:12" },
        { "kk:mm",  "00:12" },
        { "kk:mm",  "25:12" },
    };

    public static void main(String[] args) {
        TimeZone tz = TimeZone.getDefault();
        // Use "GMT" to avoid any surprises related to offset
        // transitions.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        try {
            for (String[] item : data) {
                test(item[0], item[1]);
            }
        } finally {
            // Restore the default time zone
            TimeZone.setDefault(tz);
        }

        if (errorCount > 0) {
            throw new RuntimeException("Failed with " + errorCount + " error(s).");
        }
    }

    private static void test(String pattern, String src) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
        sdf.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        System.out.printf("parse: \"%s\" with \"%s\"", src, pattern);
        Date date = sdf.parse(src, pos);
        System.out.printf(": date = %s, errorIndex = %d", date, pos.getErrorIndex());
        if (date != null || pos.getErrorIndex() == -1) {
            System.out.println(": failed");
            errorCount++;
        } else {
            System.out.println(": passed");
        }
    }
}
