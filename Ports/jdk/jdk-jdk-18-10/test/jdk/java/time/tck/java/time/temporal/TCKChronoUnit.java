/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Copyright (c) 2008-2012, Stephen Colebourne & Michael Nascimento Santos
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
package tck.java.time.temporal;

import static java.time.temporal.ChronoUnit.CENTURIES;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.DECADES;
import static java.time.temporal.ChronoUnit.ERAS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.HALF_DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLENNIA;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TCKChronoUnit {

    //-----------------------------------------------------------------------
    // isDateBased(), isTimeBased() and isDurationEstimated()
    //-----------------------------------------------------------------------
    @DataProvider(name="chronoUnit")
    Object[][] data_chronoUnit() {
        return new Object[][] {
                {FOREVER, false, false, true},
                {ERAS, true, false, true},
                {MILLENNIA, true, false, true},
                {CENTURIES, true, false, true},
                {DECADES, true, false, true},
                {YEARS, true, false, true},
                {MONTHS, true, false, true},
                {WEEKS, true, false, true},
                {DAYS, true, false, true},

                {HALF_DAYS, false, true, false},
                {HOURS, false, true, false},
                {MINUTES, false, true, false},
                {SECONDS, false, true, false},
                {MICROS, false, true, false},
                {MILLIS, false, true, false},
                {NANOS, false, true, false},

        };
    }

    @Test(dataProvider = "chronoUnit")
    public void test_unitType(ChronoUnit unit, boolean isDateBased, boolean isTimeBased, boolean isDurationEstimated) {
        assertEquals(unit.isDateBased(), isDateBased);
        assertEquals(unit.isTimeBased(), isTimeBased);
        assertEquals(unit.isDurationEstimated(), isDurationEstimated);
    }

    //-----------------------------------------------------------------------
    // isSupportedBy(), addTo() and between()
    //-----------------------------------------------------------------------
    @DataProvider(name="unitAndTemporal")
    Object[][] data_unitAndTemporal() {
        return new Object[][] {
                {CENTURIES, LocalDate.of(2000, 1, 10), true, 1, LocalDate.of(2100, 1, 10)},
                {DECADES, LocalDate.of(2000, 1, 10), true, 1, LocalDate.of(2010, 1, 10)},
                {YEARS, LocalDate.of(2000, 1, 10), true, 1, LocalDate.of(2001, 1, 10)},
                {MONTHS, LocalDate.of(2000, 1, 10), true, 1, LocalDate.of(2000, 2, 10)},
                {WEEKS, LocalDate.of(2000, 1, 10), true, 1, LocalDate.of(2000, 1, 17)},
                {DAYS, LocalDate.of(2000, 1, 10), true, 1, LocalDate.of(2000, 1, 11)},

                {HALF_DAYS, LocalTime.of(1, 2, 3, 400), true, 1, LocalTime.of(13, 2, 3, 400)},
                {HOURS, LocalTime.of(1, 2, 3, 400), true, 1, LocalTime.of(2, 2, 3, 400)},
                {MINUTES, LocalTime.of(1, 2, 3, 400), true, 1, LocalTime.of(1, 3, 3, 400)},
                {SECONDS, LocalTime.of(1, 2, 3, 400), true, 1, LocalTime.of(1, 2, 4, 400)},
                {MICROS, LocalTime.of(1, 2, 3, 400), true, 1, LocalTime.of(1, 2, 3, 1000 + 400)},
                {MILLIS, LocalTime.of(1, 2, 3, 400), true, 1, LocalTime.of(1, 2, 3, 1000*1000 + 400)},
                {NANOS, LocalTime.of(1, 2, 3, 400), true, 1, LocalTime.of(1, 2, 3, 1 + 400)},

                {CENTURIES, LocalTime.of(1, 2, 3, 400), false, 1, null},
                {DECADES, LocalTime.of(1, 2, 3, 400), false, 1, null},
                {YEARS, LocalTime.of(1, 2, 3, 400), false, 1, null},
                {MONTHS, LocalTime.of(1, 2, 3, 400), false, 1, null},
                {WEEKS, LocalTime.of(1, 2, 3, 400), false, 1, null},
                {DAYS, LocalTime.of(1, 2, 3, 400), false, 1, null},

                {HALF_DAYS, LocalDate.of(2000, 2, 29), false, 1, null},
                {HOURS, LocalDate.of(2000, 2, 29), false, 1, null},
                {MINUTES, LocalDate.of(2000, 2, 29), false, 1, null},
                {SECONDS, LocalDate.of(2000, 2, 29), false, 1, null},
                {MICROS, LocalDate.of(2000, 2, 29), false, 1, null},
                {MILLIS, LocalDate.of(2000, 2, 29), false, 1, null},
                {NANOS, LocalDate.of(2000, 2, 29), false, 1, null},

        };
    }

    @Test(dataProvider = "unitAndTemporal")
    public void test_unitAndTemporal(ChronoUnit unit, Temporal base, boolean isSupportedBy, long amount,  Temporal target) {
        assertEquals(unit.isSupportedBy(base), isSupportedBy);
        if (isSupportedBy) {
            Temporal result = unit.addTo(base, amount);
            assertEquals(result, target);
            assertEquals(unit.between(base, result), amount);
        }
    }

    //-----------------------------------------------------------------------
    // valueOf()
    //-----------------------------------------------------------------------
    @Test
    public void test_valueOf() {
        for (ChronoUnit unit : ChronoUnit.values()) {
            assertEquals(ChronoUnit.valueOf(unit.name()), unit);
        }
    }
}
