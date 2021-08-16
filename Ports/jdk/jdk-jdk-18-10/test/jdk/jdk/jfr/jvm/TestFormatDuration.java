/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.jvm;

import jdk.jfr.internal.Utils;
import jdk.test.lib.Asserts;

import java.time.Duration;
import java.util.Locale;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm jdk.jfr.jvm.TestFormatDuration
 */
public class TestFormatDuration {
    public static void main(String[] args) throws Exception{
        Locale.setDefault(Locale.US);

        // Nanoseconds
        assertDuration("0 ns   ", "0 s");
        assertDuration("1 ns   ", "0.000001 ms");
        assertDuration("10 ns  ", "0.000010 ms");
        assertDuration("100 ns ", "0.000100 ms");
        assertDuration("999 ns ", "0.000999 ms");
        assertDuration("1000 ns", "0.00100 ms");
        assertDuration("1004 ns", "0.00100 ms");
        assertDuration("1005 ns", "0.00101 ms");

        // 10 us
        assertDuration("9 us 994 ns", "0.00999 ms");
        assertDuration("9 us 995 ns", "0.0100 ms");
        assertDuration("10 us      ", "0.0100 ms");
        assertDuration("10 us 49 ns", "0.0100 ms");
        assertDuration("10 us 50 ns", "0.0101 ms");

        // 100 us
        assertDuration("99 us 949 ns ", "0.0999 ms");
        assertDuration("99 us 950 ns ", "0.100 ms");
        assertDuration("100 us       ", "0.100 ms");
        assertDuration("100 us 499 ns", "0.100 ms");
        assertDuration("100 us 500 ns", "0.101 ms");

        // 1 ms
        assertDuration("999 us 499 ns       ", "0.999 ms");
        assertDuration("999 us 500 ns       ", "1.00 ms");
        assertDuration("1 ms                ", "1.00 ms");
        assertDuration("1 ms 4 us 999 ns    ", "1.00 ms");
        assertDuration("1 ms 5 us", "1.01 ms");

        // 10 ms
        assertDuration("9 ms 994 us 999 ns", "9.99 ms");
        assertDuration("9 ms 995 us       ", "10.0 ms");
        assertDuration("10 ms             ", "10.0 ms");
        assertDuration("10 ms 49 us 999 ns", "10.0 ms");
        assertDuration("10 ms 50 us 999 ns", "10.1 ms");

        // 100 ms
        assertDuration("99 ms 949 us 999 ns ", "99.9 ms");
        assertDuration("99 ms 950 us 000 ns ", "100 ms");
        assertDuration("100 ms              ", "100 ms");
        assertDuration("100 ms 499 us 999 ns", "100 ms");
        assertDuration("100 ms 500 us       ", "101 ms");

        // 1 second
        assertDuration("999 ms 499 us 999 ns  ", "999 ms");
        assertDuration("999 ms 500 us         ", "1.00 s");
        assertDuration("1 s                   ", "1.00 s");
        assertDuration("1 s 4 ms 999 us 999 ns", "1.00 s");
        assertDuration("1 s 5 ms              ", "1.01 s");

        // 10 seconds
        assertDuration("9 s 994 ms 999 us 999 ns ", "9.99 s");
        assertDuration("9 s 995 ms               ", "10.0 s");
        assertDuration("10 s                     ", "10.0 s");
        assertDuration("10 s 049 ms 999 us 999 ns", "10.0 s");
        assertDuration("10 s 050 ms              ", "10.1 s");

        // 1 minute
        assertDuration("59 s 949 ms 999 us 999 ns", "59.9 s");
        assertDuration("59 s 950 ms              ", "1 m 0 s");
        assertDuration("1 m 0 s                  ", "1 m 0 s");
        assertDuration("60 s 499 ms 999 us 999 ns", "1 m 0 s");
        assertDuration("60 s 500 ms              ", "1 m 1 s");

        // 10 minutes
        assertDuration("10 m 0 s", "10 m 0 s");

        // 1 hour
        assertDuration("59 m 59 s 499 ms 999 us 999 ns", "59 m 59 s");
        assertDuration("59 m 59 s 500 ms              ", "1 h 0 m");
        assertDuration("1 h 0 m                       ", "1 h 0 m");
        assertDuration("1 h 29 s 999 ms 999 us 999 ns ", "1 h 0 m");
        assertDuration("1 h 30 s                      ", "1 h 1 m");

        // 1 day
        assertDuration("23 h 59 m 29 s 999 ms 999 us 999 ns", "23 h 59 m");
        assertDuration("23 h 59 m 30 s                     ", "1 d 0 h");
        assertDuration("1 d 0 h                            ", "1 d 0 h");
        assertDuration("1 d 29 m 59 s 999 ms 999 us 999 ns ", "1 d 0 h");
        assertDuration("1 d 30 m                           ", "1 d 1 h");

        // 100 days
        assertDuration("100 d 13 h", "100 d 13 h");

        // 1000 days
        assertDuration("1000 d 13 h", "1000 d 13 h");
    }

    private static void assertDuration(String value, String expected) throws Exception {
        long nanos = parse(value);
        System.out.println(value + " == " + expected + " ? (" + nanos + " ns) ");
        Asserts.assertEquals(Utils.formatDuration(Duration.ofNanos(nanos)), expected);
        if (nanos != 0) {
            Asserts.assertEquals(Utils.formatDuration(Duration.ofNanos(-nanos)), "-" + expected);
        }
    }


    private static long parse(String duration) throws Exception {
        String[] t = duration.trim().split(" ");
        long nanos = 0;
        for (int i = 0; i < t.length - 1; i += 2) {
            nanos += Long.parseLong(t[i]) * parseUnit(t[i + 1]);
        }
        return nanos;
    }

    private static long parseUnit(String unit) throws Exception {
        switch (unit) {
            case "ns":
                return 1L;
            case "us":
                return 1_000L;
            case "ms":
                return 1_000_000L;
            case "s":
                return 1_000_000_000L;
            case "m":
                return 60 * 1_000_000_000L;
            case "h":
                return 3600 * 1_000_000_000L;
            case "d":
                return 24 * 3600 * 1_000_000_000L;
        }
        throw new Exception("Test error. Unknown unit " + unit);
    }
}
