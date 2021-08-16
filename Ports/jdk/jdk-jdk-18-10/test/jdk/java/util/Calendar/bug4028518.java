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
 * @bug 4028518
 * @summary Make sure cloned GregorianCalendar is unchanged by modifying its original.
 */

import java.util.GregorianCalendar ;
import static java.util.Calendar.*;

public class bug4028518 {

    public static void main(String[] args)
    {
        GregorianCalendar cal1 = new GregorianCalendar() ;
        GregorianCalendar cal2 = (GregorianCalendar) cal1.clone() ;

        printdate(cal1, "cal1: ") ;
        printdate(cal2, "cal2 - cloned(): ") ;
        cal1.add(DAY_OF_MONTH, 1) ;
        printdate(cal1, "cal1 after adding 1 day: ") ;
        printdate(cal2, "cal2 should be unmodified: ") ;
        if (cal1.get(DAY_OF_MONTH) == cal2.get(DAY_OF_MONTH)) {
            throw new RuntimeException("cloned GregorianCalendar modified");
        }
    }

    private static void printdate(GregorianCalendar cal, String string)
    {
        System.out.println(string + (cal.get(MONTH) + 1)
                           + "/" + cal.get(DAY_OF_MONTH)
                           + "/" + cal.get(YEAR)) ;
    }
}
