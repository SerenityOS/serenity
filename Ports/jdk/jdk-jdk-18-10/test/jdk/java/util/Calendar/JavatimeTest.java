/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 *@test
 *@bug 8007520 8008254
 *@summary Test those bridge methods to/from java.time date/time classes
 * @key randomness
 */

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;

public class JavatimeTest {

    static final int NANOS_PER_SECOND = 1000_000_000;

    public static void main(String[] args) throws Throwable {

        int N = 10000;
        @SuppressWarnings("deprecation")
        long t1970 = new java.util.Date(70, 0, 01).getTime();
        Random r = new Random();
        for (int i = 0; i < N; i++) {
            int days = r.nextInt(50) * 365 + r.nextInt(365);
            long secs = t1970 + days * 86400 + r.nextInt(86400);
            int nanos = r.nextInt(NANOS_PER_SECOND);
            int nanos_ms = nanos / 1000000 * 1000000; // millis precision
            long millis = secs * 1000 + r.nextInt(1000);
            LocalDateTime ldt = LocalDateTime.ofEpochSecond(secs, nanos, ZoneOffset.UTC);
            LocalDateTime ldt_ms = LocalDateTime.ofEpochSecond(secs, nanos_ms, ZoneOffset.UTC);
            Instant inst = Instant.ofEpochSecond(secs, nanos);
            Instant inst_ms = Instant.ofEpochSecond(secs, nanos_ms);
            ///////////// java.util.Date /////////////////////////
            Date jud = new java.util.Date(millis);
            Instant inst0 = jud.toInstant();
            if (jud.getTime() != inst0.toEpochMilli()
                    || !jud.equals(Date.from(inst0))) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                throw new RuntimeException("FAILED: j.u.d -> instant -> j.u.d");
            }
            // roundtrip only with millis precision
            Date jud0 = Date.from(inst_ms);
            if (jud0.getTime() != inst_ms.toEpochMilli()
                    || !inst_ms.equals(jud0.toInstant())) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                throw new RuntimeException("FAILED: instant -> j.u.d -> instant");
            }
            //////////// java.util.GregorianCalendar /////////////
            GregorianCalendar cal = new GregorianCalendar();
            // non-roundtrip of tz name between j.u.tz and j.t.zid
            cal.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
            cal.setGregorianChange(new java.util.Date(Long.MIN_VALUE));
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setMinimalDaysInFirstWeek(4);
            cal.setTimeInMillis(millis);
            ZonedDateTime zdt0 = cal.toZonedDateTime();
            if (cal.getTimeInMillis() != zdt0.toInstant().toEpochMilli()
                    || !cal.equals(GregorianCalendar.from(zdt0))) {
                System.out.println("cal:" + cal);
                System.out.println("zdt:" + zdt0);
                System.out.println("calNew:" + GregorianCalendar.from(zdt0));
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                throw new RuntimeException("FAILED: gcal -> zdt -> gcal");
            }
            inst0 = cal.toInstant();
            if (cal.getTimeInMillis() != inst0.toEpochMilli()) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                throw new RuntimeException("FAILED: gcal -> zdt");
            }
            ZonedDateTime zdt = ZonedDateTime.of(ldt_ms, ZoneId.systemDefault());
            GregorianCalendar cal0 = GregorianCalendar.from(zdt);
            if (zdt.toInstant().toEpochMilli() != cal0.getTimeInMillis()
                    || !zdt.equals(GregorianCalendar.from(zdt).toZonedDateTime())) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                throw new RuntimeException("FAILED: zdt -> gcal -> zdt");
            }
        }

        ///////////// java.util.TimeZone /////////////////////////
        for (String zidStr : TimeZone.getAvailableIDs()) {
            // TBD: tzdt intergration
            if (zidStr.startsWith("SystemV")
                    || zidStr.contains("Riyadh8")
                    || zidStr.equals("US/Pacific-New")
                    || zidStr.equals("EST")
                    || zidStr.equals("HST")
                    || zidStr.equals("MST")) {
                continue;
            }
            ZoneId zid = ZoneId.of(zidStr, ZoneId.SHORT_IDS);
            if (!zid.equals(TimeZone.getTimeZone(zid).toZoneId())) {
                throw new RuntimeException("FAILED: zid -> tz -> zid :" + zidStr);
            }
            TimeZone tz = TimeZone.getTimeZone(zidStr);
            // no round-trip for alias and "GMT"
            if (!tz.equals(TimeZone.getTimeZone(tz.toZoneId()))
                    && !ZoneId.SHORT_IDS.containsKey(zidStr)
                    && !zidStr.startsWith("GMT")) {
                throw new RuntimeException("FAILED: tz -> zid -> tz :" + zidStr);
            }
        }
        System.out.println("Passed!");
    }
}
