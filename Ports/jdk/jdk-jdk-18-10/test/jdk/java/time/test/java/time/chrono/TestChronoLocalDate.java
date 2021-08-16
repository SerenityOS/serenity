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
package test.java.time.chrono;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

/**
 * Test chrono local date.
 */
@Test
public class TestChronoLocalDate {
    // this class primarily tests whether the generics work OK

    //-----------------------------------------------------------------------
    public void test_date_comparator_checkGenerics_ISO() {
        List<ChronoLocalDate> dates = new ArrayList<>();
        ChronoLocalDate date = LocalDate.of(2013, 1, 1);

        // Insert dates in order, no duplicates
        dates.add(date.minus(10, ChronoUnit.YEARS));
        dates.add(date.minus(1, ChronoUnit.YEARS));
        dates.add(date.minus(1, ChronoUnit.MONTHS));
        dates.add(date.minus(1, ChronoUnit.WEEKS));
        dates.add(date.minus(1, ChronoUnit.DAYS));
        dates.add(date);
        dates.add(date.plus(1, ChronoUnit.DAYS));
        dates.add(date.plus(1, ChronoUnit.WEEKS));
        dates.add(date.plus(1, ChronoUnit.MONTHS));
        dates.add(date.plus(1, ChronoUnit.YEARS));
        dates.add(date.plus(10, ChronoUnit.YEARS));

        List<ChronoLocalDate> copy = new ArrayList<>(dates);
        Collections.shuffle(copy);
        Collections.sort(copy, ChronoLocalDate.timeLineOrder());
        assertEquals(copy, dates);
        assertTrue(ChronoLocalDate.timeLineOrder().compare(copy.get(0), copy.get(1)) < 0);
    }

    public void test_date_comparator_checkGenerics_LocalDate() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = LocalDate.of(2013, 1, 1);

        // Insert dates in order, no duplicates
        dates.add(date.minus(10, ChronoUnit.YEARS));
        dates.add(date.minus(1, ChronoUnit.YEARS));
        dates.add(date.minus(1, ChronoUnit.MONTHS));
        dates.add(date.minus(1, ChronoUnit.WEEKS));
        dates.add(date.minus(1, ChronoUnit.DAYS));
        dates.add(date);
        dates.add(date.plus(1, ChronoUnit.DAYS));
        dates.add(date.plus(1, ChronoUnit.WEEKS));
        dates.add(date.plus(1, ChronoUnit.MONTHS));
        dates.add(date.plus(1, ChronoUnit.YEARS));
        dates.add(date.plus(10, ChronoUnit.YEARS));

        List<LocalDate> copy = new ArrayList<>(dates);
        Collections.shuffle(copy);
        Collections.sort(copy, ChronoLocalDate.timeLineOrder());
        assertEquals(copy, dates);
        assertTrue(ChronoLocalDate.timeLineOrder().compare(copy.get(0), copy.get(1)) < 0);
    }

    //-----------------------------------------------------------------------
    public void test_date_checkGenerics_genericsMethod() {
        Chronology chrono = ThaiBuddhistChronology.INSTANCE;
        ChronoLocalDate date = chrono.dateNow();
        date = processOK(date);
        date = processClassOK(ThaiBuddhistDate.class);
        date = dateSupplier();

        date = processClassWeird(ThaiBuddhistDate.class);
    }

    public void test_date_checkGenerics_genericsMethod_concreteType() {
        ThaiBuddhistChronology chrono = ThaiBuddhistChronology.INSTANCE;
        ThaiBuddhistDate date = chrono.dateNow();
        date = ThaiBuddhistDate.now();
        date = processOK(date);
        date = processClassOK(ThaiBuddhistDate.class);
        date = dateSupplier();

        // date = processClassWeird(ThaiBuddhistDate.class);  // does not compile (correct)
    }

    public <D extends ChronoLocalDate> void test_date_checkGenerics_genericsMethod_withType() {
        Chronology chrono = ThaiBuddhistChronology.INSTANCE;
        @SuppressWarnings("unchecked")
        D date = (D) chrono.dateNow();
        date = processOK(date);
        // date = processClassOK(ThaiBuddhistDate.class);  // does not compile (correct)
        date = dateSupplier();

        // date = processWeird(date);  // does not compile (correct)
        // date = processClassWeird(ThaiBuddhistDate.class);  // does not compile (correct)
    }

    @SuppressWarnings("unchecked")
    private <D extends ChronoLocalDate> D dateSupplier() {
        return (D) ThaiBuddhistChronology.INSTANCE.dateNow();
    }

    // decent generics signatures that need to work
    @SuppressWarnings("unchecked")
    private <D extends ChronoLocalDate> D processOK(D date) {
        return (D) date.plus(1, ChronoUnit.DAYS);
    }
    private <D extends ChronoLocalDate> D processClassOK(Class<D> cls) {
        return null;
    }

    // weird generics signatures that shouldn't really work
    private <D extends ChronoLocalDate> ChronoLocalDate processClassWeird(Class<D> cls) {
        return null;
    }

    public void test_date_checkGenerics_chronoLocalDateTime1() {
        LocalDateTime now = LocalDateTime.now();
        Chronology chrono = ThaiBuddhistChronology.INSTANCE;
        ChronoLocalDateTime<?> ldt = chrono.localDateTime(now);
        ldt = processCLDT(ldt);
    }

    public void test_date_checkGenerics_chronoLocalDateTime2() {
        LocalDateTime now = LocalDateTime.now();
        Chronology chrono = ThaiBuddhistChronology.INSTANCE;
        ChronoLocalDateTime<? extends ChronoLocalDate> ldt = chrono.localDateTime(now);
        ldt = processCLDT(ldt);
    }

    private <D extends ChronoLocalDate> ChronoLocalDateTime<D> processCLDT(ChronoLocalDateTime<D> dt) {
        return dt;
    }
}
