/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6645263
 * @summary Test field normalization in non-lenient from the partially normalized state
 */

import java.util.*;

public class Bug6645263 {
    public static void main(String[] args) {
        Calendar cal = new GregorianCalendar(Locale.US);
        cal.setLenient(false);
        cal.set(Calendar.YEAR, 2007);
        cal.set(Calendar.MONTH, Calendar.NOVEMBER);
        cal.set(Calendar.WEEK_OF_MONTH, 4);
        cal.set(Calendar.DAY_OF_WEEK, 1);
        // Let cal calculate the time from the given fields
        cal.getTime();

        // Change DAY_OF_MONTH
        cal.set(Calendar.DAY_OF_MONTH, 1);
        // The following line shouldn't throw an IllegalArgumentException.
        cal.getTime();
   }
}
