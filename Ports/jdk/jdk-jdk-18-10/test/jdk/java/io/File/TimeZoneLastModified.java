/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6212869
 * @summary Determine if lastModified() works after TimeZone.setDefault()
 * @run main/othervm TimeZoneLastModified
 */

import java.io.File;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

public class TimeZoneLastModified {
    // Tue, 04 Jun 2002 13:56:50.002 GMT
    private static final long TIME = 1023199010002L;

    public static void main(String[] args) throws Throwable {
        int failures = test(null);
        for (String timeZoneID : TimeZone.getAvailableIDs()) {
            failures += test(timeZoneID);
        }
        if (failures != 0) {
            throw new RuntimeException("TimeZoneLastModified failed");
        }
        System.out.println("TimeZoneLastModified passed");
    }

    private static int test(String timeZoneID) throws Throwable {
        File f = new File("test-timezone.txt");
        int failures = 0;
        try {
            f.createNewFile();

            if (timeZoneID != null) {
                TimeZone.setDefault(TimeZone.getTimeZone(timeZoneID));
            }

            boolean succeeded = f.setLastModified(TIME);
            if (!succeeded) {
                System.err.format
                    ("Setting time to %d failed for time zone %s%n",
                    TIME, timeZoneID);
                failures++;
            }

            long time = f.lastModified();
            if (Math.abs(time - TIME) > 999) { // account for second precision
                System.err.format
                    ("Wrong modification time (ms): expected %d, obtained %d%n",
                    TIME, time);
                failures++;
            }
        } finally {
            f.delete();
        }

        return failures;
    }
}
