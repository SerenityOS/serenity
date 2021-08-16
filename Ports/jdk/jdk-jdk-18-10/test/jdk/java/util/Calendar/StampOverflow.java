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
 * @bug 4404619 6348819
 * @summary Make sure that Calendar doesn't cause nextStamp overflow.
 * @modules java.base/java.util:open
 */

import java.lang.reflect.*;
import java.util.*;
import static java.util.Calendar.*;

// Calendar fails when turning negative to positive (zero), not
// positive to negative with nextStamp. If a negative value was set to
// nextStamp, it would fail even with the fix. So, there's no way to
// reproduce the symptom in a short time -- at leaset it would take a
// couple of hours even if we started with Integer.MAX_VALUE. So, this
// test case just checks that set() calls don't cause any nextStamp
// overflow.

public class StampOverflow {
    public static void main(String[] args) throws IllegalAccessException {
        // Get a Field for "nextStamp".
        Field nextstamp = null;
        try {
            nextstamp = Calendar.class.getDeclaredField("nextStamp");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("implementation changed?", e);
        }

        nextstamp.setAccessible(true);

        Calendar cal = new GregorianCalendar();
        int initialValue = nextstamp.getInt(cal);
        // Set nextStamp to a very large number
        nextstamp.setInt(cal, Integer.MAX_VALUE - 100);

        for (int i = 0; i < 1000; i++) {
            invoke(cal);
            int stampValue = nextstamp.getInt(cal);
            // nextStamp must not be less than initialValue.
            if (stampValue < initialValue) {
                throw new RuntimeException("invalid nextStamp: " + stampValue);
            }
        }
    }

    static void invoke(Calendar cal) {
        cal.clear();
        cal.set(2000, NOVEMBER, 2, 0, 0, 0);
        int y = cal.get(YEAR);
        int m = cal.get(MONTH);
        int d = cal.get(DAY_OF_MONTH);
        if (y != 2000 || m != NOVEMBER || d != 2) {
            throw new RuntimeException("wrong date produced ("
                                       + y + "/" + (m+1) + "/" + d + ")");
        }
    }
}
