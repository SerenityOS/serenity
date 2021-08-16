/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4851640
 * @summary Make sure not to set UNSET fields to COMPUTED after time calculation.
 */

import java.util.GregorianCalendar;
import static java.util.Calendar.*;

public class Bug4851640 {

    public static void main(String args[]) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(YEAR, 2003);
        long t = cal.getTime().getTime();

        // For the time calculation, the MONTH and DAY_OF_MONTH fields
        // (with the default values) have been used for determining
        // the date.  However, both the MONTH and DAY_OF_MONTH fields
        // should be kept UNSET after the time calculation.
        if (cal.isSet(MONTH) || cal.isSet(DAY_OF_MONTH)) {
            throw new RuntimeException("After getTime(): MONTH field=" + cal.isSet(MONTH)
                                       + ", DAY_OF_MONTH field=" + cal.isSet(DAY_OF_MONTH));
        }

        // After calling get() for any field, all field values are
        // recalculated and their field states are set to
        // COMPUTED. isSet() must return true.
        int y = cal.get(YEAR);
        if (!(cal.isSet(MONTH) && cal.isSet(DAY_OF_MONTH))) {
            throw new RuntimeException("After get(): MONTH field=" + cal.isSet(MONTH)
                                       + ", DAY_OF_MONTH field=" + cal.isSet(DAY_OF_MONTH));
        }
    }
}
