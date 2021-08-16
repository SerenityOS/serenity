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

/*
 * @test
 * @bug 4100311
 * @summary Make sure set(DAY_OF_YEAR, 1) works.
 */

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;

public class bug4100311
{
    @SuppressWarnings("deprecation")
    public static void main(String args[])
    {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, 1997);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        Date d = cal.getTime();             // Should be Jan 1
        if (d.getMonth() != 0 || d.getDate() != 1) {
            throw new RuntimeException("Date isn't Jan 1");
        }
    }
}
