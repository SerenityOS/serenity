/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6448234
 * @summary Make sure indexing of DAY_OF_WEEK is correct in JapaneseImperialCalendar.getDisplayName.
 */

import java.util.Calendar;
import java.util.Locale;
import static java.util.Calendar.*;

public class Bug6448234 {
    public static void main(String[] args) {
        Calendar jcal = Calendar.getInstance(new Locale("ja", "JP", "JP"));
        Calendar gcal = Calendar.getInstance(Locale.US);

        for (int i = SUNDAY; i <= SATURDAY; i++) {
            jcal.set(DAY_OF_WEEK, i);
            gcal.set(DAY_OF_WEEK, i);

            // Test LONG
            String j = jcal.getDisplayName(DAY_OF_WEEK, LONG, Locale.US);
            String g = gcal.getDisplayName(DAY_OF_WEEK, LONG, Locale.US);
            if (!j.equals(g)) {
                throw new RuntimeException("Got " + j + ", expected " + g);
            }

            // Test SHORT
            j = jcal.getDisplayName(DAY_OF_WEEK, SHORT, Locale.US);
            g = gcal.getDisplayName(DAY_OF_WEEK, SHORT, Locale.US);
            if (!j.equals(g)) {
                throw new RuntimeException("Got " + j + ", expected " + g);
            }
        }
    }
}
