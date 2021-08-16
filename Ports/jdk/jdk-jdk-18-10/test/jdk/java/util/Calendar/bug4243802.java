/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4243802
 * @summary confirm that Calendar.setTimeInMillis() and
 * getTimeInMillis() can be called from a user program. (They used to
 * be protected methods.)
 * @library /java/text/testlib
 */

import java.util.*;

public class bug4243802 extends IntlTest {

    public static void main(String[] args) throws Exception {
        new bug4243802().run(args);
    }

    /**
     * 4243802: RFE: need way to set the date of a calendar without a Date object
     */
    public void Test4243802() {
        TimeZone saveZone = TimeZone.getDefault();
        Locale saveLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();

            cal1.clear();
            cal2.clear();
            cal1.set(2001, Calendar.JANUARY, 25, 1, 23, 45);
            cal2.setTimeInMillis(cal1.getTimeInMillis());
            if ((cal2.get(Calendar.YEAR) != 2001) ||
                (cal2.get(Calendar.MONTH) != Calendar.JANUARY) ||
                (cal2.get(Calendar.DAY_OF_MONTH) != 25) ||
                (cal2.get(Calendar.HOUR_OF_DAY) != 1) ||
                (cal2.get(Calendar.MINUTE) != 23) ||
                (cal2.get(Calendar.SECOND) != 45) ||
                (cal2.get(Calendar.MILLISECOND) != 0)) {
                 errln("Failed: expected 1/25/2001 1:23:45.000" +
                       ", got " + (cal2.get(Calendar.MONTH)+1) + "/" +
                       cal2.get(Calendar.DAY_OF_MONTH) +"/" +
                       cal2.get(Calendar.YEAR) + " " +
                       cal2.get(Calendar.HOUR_OF_DAY) + ":" +
                       cal2.get(Calendar.MINUTE) + ":" +
                       cal2.get(Calendar.SECOND) + "." +
                       toMillis(cal2.get(Calendar.MILLISECOND)));
            }
            logln("Passed.");
        }
        finally {
            Locale.setDefault(saveLocale);
            TimeZone.setDefault(saveZone);
        }
    }

    private String toMillis(int m) {
        StringBuffer sb = new StringBuffer();
        if (m < 100) {
            sb.append('0');
        }
        if (m < 10) {
            sb.append('0');
        }
        sb.append(m);
        return sb.toString();
    }
}
