/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.1 11/02/01
 * @bug 4527203
 * @modules jdk.localedata
 * @summary In Hungary and Ukraine first day of week is Monday not Sunday
 */

import java.util.Calendar;
import java.util.Locale;

public class Bug4527203 {

    public static void main(String[] args) {
        Calendar huCalendar = Calendar.getInstance(new Locale("hu","HU"));
        int hufirstDayOfWeek = huCalendar.getFirstDayOfWeek();
        if (hufirstDayOfWeek != Calendar.MONDAY) {
            throw new RuntimeException();
        }

        Calendar ukCalendar = Calendar.getInstance(new Locale("uk","UA"));
        int ukfirstDayOfWeek = ukCalendar.getFirstDayOfWeek();
        if (ukfirstDayOfWeek != Calendar.MONDAY) {
            throw new RuntimeException();
        }

    }
}
