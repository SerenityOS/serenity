/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.text.*;

/**
 * Test GregorianCalendar limits, which should not exist.
 * @test
 * @bug 4056585
 * @summary Make sure that GregorianCalendar works far in the past and future.
 * @author Alan Liu
 */
public class Limit {
    static final long ONE_DAY = 24*60*60*1000L;

    public static void main(String args[]) throws Exception {
        GregorianCalendar c = new GregorianCalendar();
        DateFormat fmt = new SimpleDateFormat("EEEE, MMMM dd, yyyy G", Locale.US);
        long bigMillis = 300000000000000L;

        try {
            // We check two things:
            // 1. That handling millis in the range of +/- bigMillis works.
            //    bigMillis is a value that used to blow up.
            // 2. The round-trip format/parse works in these extreme areas.
            c.setTime(new Date(-bigMillis));
            String s = fmt.format(c.getTime());
            Date d = fmt.parse(s);
            if (Math.abs(d.getTime() + bigMillis) >= ONE_DAY) {
                throw new Exception(s + " != " + fmt.format(d));
            }

            c.setTime(new Date(+bigMillis));
            s = fmt.format(c.getTime());
            d = fmt.parse(s);
            if (Math.abs(d.getTime() - bigMillis) >= ONE_DAY) {
                throw new Exception(s + " != " + fmt.format(d));
            }
        } catch (IllegalArgumentException | ParseException e) {
            throw e;
        }
    }
}
