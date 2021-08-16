/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 *@bug 8007520
 *@summary Test those bridge methods to/from java.time date/time classes
 * @key randomness
 */

import java.util.Random;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class JavatimeTest {

    static final int NANOS_PER_SECOND = 1000000000;

    public static void main(String[] args) throws Throwable {
        int N = 10000;
        long t1970 = new java.util.Date(70, 0, 01).getTime();
        Random r = new Random();
        for (int i = 0; i < N; i++) {
            int days  = r.nextInt(50) * 365 + r.nextInt(365);
            long secs = t1970 + days * 86400 + r.nextInt(86400);
            int nanos = r.nextInt(NANOS_PER_SECOND);
            int nanos_ms = nanos / 1000000 * 1000000; // millis precision
            long millis = secs * 1000 + r.nextInt(1000);

            LocalDateTime ldt = LocalDateTime.ofEpochSecond(secs, nanos, ZoneOffset.UTC);
            LocalDateTime ldt_ms = LocalDateTime.ofEpochSecond(secs, nanos_ms, ZoneOffset.UTC);
            Instant inst = Instant.ofEpochSecond(secs, nanos);
            Instant inst_ms = Instant.ofEpochSecond(secs, nanos_ms);
            //System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);

            /////////// Timestamp ////////////////////////////////
            Timestamp ta = new Timestamp(millis);
            ta.setNanos(nanos);
            if (!isEqual(ta.toLocalDateTime(), ta)) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                print(ta.toLocalDateTime(), ta);
                throw new RuntimeException("FAILED: j.s.ts -> ldt");
            }
            if (!isEqual(ldt, Timestamp.valueOf(ldt))) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                print(ldt, Timestamp.valueOf(ldt));
                throw new RuntimeException("FAILED: ldt -> j.s.ts");
            }
            Instant inst0 = ta.toInstant();
            if (ta.getTime() != inst0.toEpochMilli() ||
                ta.getNanos() != inst0.getNano() ||
                !ta.equals(Timestamp.from(inst0))) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                throw new RuntimeException("FAILED: j.s.ts -> instant -> j.s.ts");
            }
            inst = Instant.ofEpochSecond(secs, nanos);
            Timestamp ta0 = Timestamp.from(inst);
            if (ta0.getTime() != inst.toEpochMilli() ||
                ta0.getNanos() != inst.getNano() ||
                !inst.equals(ta0.toInstant())) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                throw new RuntimeException("FAILED: instant -> timestamp -> instant");
            }

            ////////// java.sql.Date /////////////////////////////
            // j.s.d/t uses j.u.d.equals() !!!!!!!!
            java.sql.Date jsd = new java.sql.Date(millis);
            if (!isEqual(jsd.toLocalDate(), jsd)) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                print(jsd.toLocalDate(), jsd);
                throw new RuntimeException("FAILED: j.s.d -> ld");
            }
            LocalDate ld = ldt.toLocalDate();
            if (!isEqual(ld, java.sql.Date.valueOf(ld))) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                print(ld, java.sql.Date.valueOf(ld));
                throw new RuntimeException("FAILED: ld -> j.s.d");
            }
            ////////// java.sql.Time /////////////////////////////
            java.sql.Time jst = new java.sql.Time(millis);
            if (!isEqual(jst.toLocalTime(), jst)) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                print(jst.toLocalTime(), jst);
                throw new RuntimeException("FAILED: j.s.t -> lt");
            }
            // millis precision
            LocalTime lt = ldt_ms.toLocalTime();
            if (!isEqual(lt, java.sql.Time.valueOf(lt))) {
                System.out.printf("ms: %16d  ns: %10d  ldt:[%s]%n", millis, nanos, ldt);
                print(lt, java.sql.Time.valueOf(lt));
                throw new RuntimeException("FAILED: lt -> j.s.t");
            }
        }
        System.out.println("Passed!");
    }

    private static boolean isEqual(LocalDateTime ldt, Timestamp ts) {
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
        return zdt.getYear() == ts.getYear() + 1900 &&
               zdt.getMonthValue() == ts.getMonth() + 1 &&
               zdt.getDayOfMonth() == ts.getDate() &&
               zdt.getHour() == ts.getHours() &&
               zdt.getMinute() == ts.getMinutes() &&
               zdt.getSecond() == ts.getSeconds() &&
               zdt.getNano() == ts.getNanos();
    }

    private static void print(LocalDateTime ldt, Timestamp ts) {
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
        System.out.printf("ldt:ts  %d/%d, %d/%d, %d/%d, %d/%d, %d/%d, %d/%d, nano:[%d/%d]%n",
               zdt.getYear(), ts.getYear() + 1900,
               zdt.getMonthValue(), ts.getMonth() + 1,
               zdt.getDayOfMonth(), ts.getDate(),
               zdt.getHour(), ts.getHours(),
               zdt.getMinute(), ts.getMinutes(),
               zdt.getSecond(), ts.getSeconds(),
               zdt.getNano(), ts.getNanos());
    }

    private static boolean isEqual(LocalDate ld, java.sql.Date d) {
        return ld.getYear() == d.getYear() + 1900 &&
               ld.getMonthValue() == d.getMonth() + 1 &&
               ld.getDayOfMonth() == d.getDate();
    }

    private static void print(LocalDate ld, java.sql.Date d) {
        System.out.printf("%d/%d, %d/%d, %d/%d%n",
               ld.getYear(), d.getYear() + 1900,
               ld.getMonthValue(), d.getMonth() + 1,
               ld.getDayOfMonth(), d.getDate());
    }

    private static boolean isEqual(LocalTime lt, java.sql.Time t) {
        return lt.getHour() == t.getHours() &&
               lt.getMinute() == t.getMinutes() &&
               lt.getSecond() == t.getSeconds();
    }

    private static void print(LocalTime lt, java.sql.Time t) {
        System.out.printf("%d/%d, %d/%d, %d/%d%n",
                          lt.getHour(), t.getHours(),
                          lt.getMinute(), t.getMinutes(),
                          lt.getSecond(), t.getSeconds());
    }
}
