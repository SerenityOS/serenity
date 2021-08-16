/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package test.java.time;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.time.Clock;
import java.time.LocalTime;

import org.testng.annotations.Test;

/**
 * Test LocalTime.
 */
@Test
public class TestLocalTime extends AbstractTest {
    static final long NANOS_PER_SECOND = 1_000_000_000L;
    static final long NANOS_PER_MINUTE = 60 * NANOS_PER_SECOND;
    static final long NANOS_PER_DAY = 24 * 60 * NANOS_PER_MINUTE;

    //-----------------------------------------------------------------------
    @Test
    public void test_immutable() {
        assertImmutable(LocalTime.class);
    }

    //-----------------------------------------------------------------------
    private void check(LocalTime time, int h, int m, int s, int n) {
        assertEquals(time.getHour(), h);
        assertEquals(time.getMinute(), m);
        assertEquals(time.getSecond(), s);
        assertEquals(time.getNano(), n);
    }

    //-----------------------------------------------------------------------
    @Test
    public void constant_MIDNIGHT() {
        check(LocalTime.MIDNIGHT, 0, 0, 0, 0);
    }

    @Test
    public void constant_MIDNIGHT_same() {
        assertSame(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);
        assertSame(LocalTime.MIDNIGHT, LocalTime.of(0, 0));
    }

    @Test
    public void constant_MIDDAY() {
        check(LocalTime.NOON, 12, 0, 0, 0);
    }

    @Test
    public void constant_MIDDAY_same() {
        assertSame(LocalTime.NOON, LocalTime.NOON);
        assertSame(LocalTime.NOON, LocalTime.of(12, 0));
    }

    //-----------------------------------------------------------------------
    @Test
    public void constant_MIN_TIME() {
        check(LocalTime.MIN, 0, 0, 0, 0);
    }

    @Test
    public void constant_MIN_TIME_same() {
        assertSame(LocalTime.MIN, LocalTime.of(0, 0));
    }

    @Test
    public void constant_MAX_TIME() {
        check(LocalTime.MAX, 23, 59, 59, 999999999);
    }

    @Test
    public void constant_MAX_TIME_same() {
        assertSame(LocalTime.NOON, LocalTime.NOON);
        assertSame(LocalTime.NOON, LocalTime.of(12, 0));
    }

    @Test
    public void factory_time_2ints_singletons() {
        for (int i = 0; i < 24; i++) {
            LocalTime test1 = LocalTime.of(i, 0);
            LocalTime test2 = LocalTime.of(i, 0);
            assertSame(test1, test2);
        }
    }

    @Test
    public void factory_time_3ints_singletons() {
        for (int i = 0; i < 24; i++) {
            LocalTime test1 = LocalTime.of(i, 0, 0);
            LocalTime test2 = LocalTime.of(i, 0, 0);
            assertSame(test1, test2);
        }
    }

    @Test
    public void factory_time_4ints_singletons() {
        for (int i = 0; i < 24; i++) {
            LocalTime test1 = LocalTime.of(i, 0, 0, 0);
            LocalTime test2 = LocalTime.of(i, 0, 0, 0);
            assertSame(test1, test2);
        }
    }

    @Test
    public void factory_ofSecondOfDay_singletons() {
        for (int i = 0; i < 24; i++) {
            LocalTime test1 = LocalTime.ofSecondOfDay(i * 60L * 60L);
            LocalTime test2 = LocalTime.of(i, 0);
            assertSame(test1, test2);
        }
    }

    @Test
    public void factory_ofNanoOfDay_singletons() {
        for (int i = 0; i < 24; i++) {
            LocalTime test1 = LocalTime.ofNanoOfDay(i * 1000000000L * 60L * 60L);
            LocalTime test2 = LocalTime.of(i, 0);
            assertSame(test1, test2);
        }
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unused")
    public void now() {
        // Warmup the TimeZone data so the following test does not include
        // one-time initialization
        LocalTime.now(Clock.systemDefaultZone());

        long diff = Integer.MAX_VALUE;
        for (int i = 0; i < 2; i++) {
            LocalTime expected = LocalTime.now(Clock.systemDefaultZone());
            LocalTime test = LocalTime.now();
            diff = test.toNanoOfDay() - expected.toNanoOfDay();
            // Normalize for wrap-around midnight
            diff = Math.floorMod(NANOS_PER_DAY + diff, NANOS_PER_DAY);
            if (diff < 100000000) {
                break;
            }
            // A second iteration may be needed if the clock changed
            // due to a DST change between the two calls to now.
        }
        assertTrue(diff < 100000000,   // less than 0.1 sec
                "LocalTime.now  vs LocalTime.now(Clock.systemDefaultZone()) not close");
    }

}
