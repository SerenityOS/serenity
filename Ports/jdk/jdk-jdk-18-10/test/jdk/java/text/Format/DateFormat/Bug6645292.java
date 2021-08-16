/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6645292
 * @summary Make sure to parse a DST time zone name with which the
 * last DST rule doesn't observe DST.
 */

import java.text.*;
import java.util.*;
import static java.util.Calendar.*;

public class Bug6645292 {
    public static void main(String[] args) {
        Locale loc = Locale.getDefault();
        TimeZone zone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.US);
            // Use "Asia/Shanghai" with an old time stamp rather than
            // "Australia/Perth" because if Perth decides to obserb DST
            // permanently, that decision will make this test case
            // useless. There's the same risk with China, though.
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(1986, JUNE, 1);
            Date d1 = cal.getTime();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzzz");
            String s = df.format(d1);
            Date d2 = null;
            try {
                d2 = df.parse(s);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!d1.equals(d2)) {
                throw new RuntimeException("d1 (" + d1 + ") != d2 (" + d2 + ")");
            }
        } finally {
            Locale.setDefault(loc);
            TimeZone.setDefault(zone);
        }
    }
}
