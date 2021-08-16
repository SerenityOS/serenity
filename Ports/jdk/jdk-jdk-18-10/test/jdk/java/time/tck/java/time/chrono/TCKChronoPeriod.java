/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2013, Stephen Colebourne & Michael Nascimento Santos
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
package tck.java.time.chrono;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.temporal.Temporal;
import java.time.temporal.UnsupportedTemporalTypeException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class TCKChronoPeriod {

    //-----------------------------------------------------------------------
    // regular data factory for names and descriptions of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendars")
    Chronology[][] data_of_calendars() {
        return new Chronology[][]{
                    {HijrahChronology.INSTANCE},
                    {IsoChronology.INSTANCE},
                    {JapaneseChronology.INSTANCE},
                    {MinguoChronology.INSTANCE},
                    {ThaiBuddhistChronology.INSTANCE}};
    }

    //-----------------------------------------------------------------------
    // Test Serialization of Calendars
    //-----------------------------------------------------------------------
    @Test(dataProvider="calendars")
    public void test_serialization(Chronology chrono) throws Exception {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(period);
        out.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        ObjectInputStream in = new ObjectInputStream(bais);
        ChronoPeriod ser = (ChronoPeriod) in.readObject();
        assertEquals(ser, period, "deserialized ChronoPeriod is wrong");
    }

    @Test(dataProvider="calendars")
    public void test_get(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        assertEquals(period.get(YEARS), 1);
        assertEquals(period.get(MONTHS), 2);
        assertEquals(period.get(DAYS), 3);
    }

    @Test(dataProvider="calendars", expectedExceptions=UnsupportedTemporalTypeException.class)
    public void test_get_unsupported(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        period.get(HOURS);
    }

    @Test(dataProvider="calendars")
    public void test_getUnits(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        assertEquals(period.getUnits().size(), 3);
        assertEquals(period.getUnits().get(0), YEARS);
        assertEquals(period.getUnits().get(1), MONTHS);
        assertEquals(period.getUnits().get(2), DAYS);
    }

    @Test(dataProvider="calendars")
    public void test_getChronology(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        assertEquals(period.getChronology(), chrono);
    }

    @Test(dataProvider="calendars")
    public void test_isZero_isNegative(Chronology chrono) {
        ChronoPeriod periodPositive = chrono.period(1, 2, 3);
        assertEquals(periodPositive.isZero(), false);
        assertEquals(periodPositive.isNegative(), false);

        ChronoPeriod periodZero = chrono.period(0, 0, 0);
        assertEquals(periodZero.isZero(), true);
        assertEquals(periodZero.isNegative(), false);

        ChronoPeriod periodNegative = chrono.period(-1, 0, 0);
        assertEquals(periodNegative.isZero(), false);
        assertEquals(periodNegative.isNegative(), true);
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="calendars")
    public void test_plus(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ChronoPeriod period2 = chrono.period(2, 3, 4);
        ChronoPeriod result = period.plus(period2);
        assertEquals(result, chrono.period(3, 5, 7));
    }

    @Test(dataProvider="calendars", expectedExceptions=DateTimeException.class)
    public void test_plus_wrongChrono(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ChronoPeriod isoPeriod = Period.of(2, 3, 4);
        ChronoPeriod thaiPeriod = ThaiBuddhistChronology.INSTANCE.period(2, 3, 4);
        // one of these two will fail
        period.plus(isoPeriod);
        period.plus(thaiPeriod);
    }

    @Test(dataProvider="calendars")
    public void test_minus(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ChronoPeriod period2 = chrono.period(2, 3, 4);
        ChronoPeriod result = period.minus(period2);
        assertEquals(result, chrono.period(-1, -1, -1));
    }

    @Test(dataProvider="calendars", expectedExceptions=DateTimeException.class)
    public void test_minus_wrongChrono(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ChronoPeriod isoPeriod = Period.of(2, 3, 4);
        ChronoPeriod thaiPeriod = ThaiBuddhistChronology.INSTANCE.period(2, 3, 4);
        // one of these two will fail
        period.minus(isoPeriod);
        period.minus(thaiPeriod);
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="calendars")
    public void test_addTo(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ChronoLocalDate date = chrono.dateNow();
        Temporal result = period.addTo(date);
        assertEquals(result, date.plus(14, MONTHS).plus(3, DAYS));
    }

    @Test(dataProvider="calendars", expectedExceptions=DateTimeException.class)
    public void test_addTo_wrongChrono(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ChronoLocalDate isoDate = LocalDate.of(2000, 1, 1);
        ChronoLocalDate thaiDate = ThaiBuddhistChronology.INSTANCE.date(2000, 1, 1);
        // one of these two will fail
        period.addTo(isoDate);
        period.addTo(thaiDate);
    }

    @Test(dataProvider="calendars")
    public void test_subtractFrom(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ChronoLocalDate date = chrono.dateNow();
        Temporal result = period.subtractFrom(date);
        assertEquals(result, date.minus(14, MONTHS).minus(3, DAYS));
    }

    @Test(dataProvider="calendars", expectedExceptions=DateTimeException.class)
    public void test_subtractFrom_wrongChrono(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        ChronoLocalDate isoDate = LocalDate.of(2000, 1, 1);
        ChronoLocalDate thaiDate = ThaiBuddhistChronology.INSTANCE.date(2000, 1, 1);
        // one of these two will fail
        period.subtractFrom(isoDate);
        period.subtractFrom(thaiDate);
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="calendars")
    public void test_negated(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        assertEquals(period.negated(), chrono.period(-1, -2, -3));
    }

    @Test(dataProvider="calendars")
    public void test_multipliedBy(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        assertEquals(period.multipliedBy(3), chrono.period(3, 6, 9));
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="calendars")
    public void test_equals_equal(Chronology chrono) {
        ChronoPeriod a1 = chrono.period(1, 2, 3);
        ChronoPeriod a2 = chrono.period(1, 2, 3);
        assertEquals(a1, a1);
        assertEquals(a1, a2);
        assertEquals(a2, a1);
        assertEquals(a2, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test(dataProvider="calendars")
    public void test_equals_notEqual(Chronology chrono) {
        ChronoPeriod a = chrono.period(1, 2, 3);
        ChronoPeriod b = chrono.period(2, 2, 3);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(a.equals(""), false);
        assertEquals(a.equals(null), false);
    }

    @Test(dataProvider="calendars")
    public void test_toString(Chronology chrono) {
        ChronoPeriod period = chrono.period(1, 2, 3);
        if (period instanceof Period == false) {
            assertEquals(period.toString(), chrono.getId() + " P1Y2M3D");
        }
    }

}
