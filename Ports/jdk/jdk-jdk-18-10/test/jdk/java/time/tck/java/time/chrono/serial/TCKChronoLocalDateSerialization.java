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
package tck.java.time.chrono.serial;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectStreamConstants;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;



import tck.java.time.AbstractTCKTest;

/**
 * Test serialization of built-in chronologies.
 */
@Test
public class TCKChronoLocalDateSerialization extends AbstractTCKTest {

    static final int CHRONO_TYPE = 1;            // java.time.chrono.Ser.CHRONO_TYPE
    static final int JAPANESE_DATE_TYPE = 4;     // java.time.chrono.Ser.JAPANESE_DATE_TYPE
    static final int HIJRAH_DATE_TYPE = 6;       // java.time.chrono.Ser.HIJRAH_DATE_TYPE
    static final int MINGUO_DATE_TYPE = 7;       // java.time.chrono.Ser.MINGUO_DATE_TYPE
    static final int THAIBUDDHIST_DATE_TYPE = 8; // java.time.chrono.Ser.THAIBUDDHIST_DATE_TYPE

    //-----------------------------------------------------------------------
    // Regular data factory for names and descriptions of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendars")
    Object[][] data_of_calendars() {
        return new Object[][]{
            {JapaneseDate.of(JapaneseEra.HEISEI, 25, 01, 05), JAPANESE_DATE_TYPE},
            {MinguoDate.of(102, 01, 05),                      MINGUO_DATE_TYPE},
            {ThaiBuddhistDate.of(2556, 01, 05),               THAIBUDDHIST_DATE_TYPE},
        };
    }


    //-----------------------------------------------------------------------
    // Test Serialization of Calendars
    //-----------------------------------------------------------------------
    @Test( dataProvider="calendars")
    public void test_ChronoSerialization(ChronoLocalDate date, int dateType) throws Exception {
        assertSerializable(date);
    }

    //-----------------------------------------------------------------------
    // Test that serialization produces exact sequence of bytes
    //-----------------------------------------------------------------------
    @Test(dataProvider="calendars")
    private void test_serialization_format(ChronoLocalDate date, int dateType) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos) ) {
            dos.writeByte(dateType);
            dos.writeInt(date.get(YEAR));
            dos.writeByte(date.get(MONTH_OF_YEAR));
            dos.writeByte(date.get(DAY_OF_MONTH));
        }
        byte[] bytes = baos.toByteArray();
        assertSerializedBySer(date, bytes);
    }

    //-----------------------------------------------------------------------
    // Test HijrajDate serialization is a type, Chronology, year, month, day
    //-----------------------------------------------------------------------
    @Test()
    public void test_hijrahSerialization_format() throws Exception {
        HijrahChronology chrono = HijrahChronology.INSTANCE;
        HijrahDate date = HijrahDate.of(1433, 10, 29);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Expect the type of the HijrahDate in the stream
        byte[] hijrahDateBytes = new byte[] {HIJRAH_DATE_TYPE};

        // Literal reference to Hijrah-Umalqura Chronology
        byte[] hijrahChronoBytes = new byte[] {
            115, 113, 0, 126, 0, 0,                        /* p w \u0001 \u0006 s q \u0000 ~ \u0000 \u0000 */
            119, 18, 1, 0, 15, 72, 105, 106, 114, 97,      /* w \u0012 \u0001 \u0000 \u000f H i j r a */
            104, 45, 117, 109, 97, 108, 113, 117, 114, 97, /* h - u m a l q u r a */
            120,                                           /*  \u001d x */
        };

        // Build the sequence that represents the data in the stream
        baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos) ) {
            dos.writeByte(ObjectStreamConstants.TC_BLOCKDATA);
            dos.writeByte(6);   // 6 bytes follow
            dos.writeInt(date.get(YEAR));
            dos.writeByte(date.get(MONTH_OF_YEAR));
            dos.writeByte(date.get(DAY_OF_MONTH));
            dos.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA);
        }
        byte[] dateBytes = baos.toByteArray();

        assertSerializedBySer(date, hijrahDateBytes, hijrahChronoBytes, dateBytes);
    }


    //-----------------------------------------------------------------------
    // Regular data factory for names and descriptions of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "invalidSerialformClasses")
    Object[][] invalid_serial_classes() {
        return new Object[][]{
            {JapaneseEra.class},
            {JapaneseDate.class},
            {MinguoDate.class},
            {ThaiBuddhistDate.class},
            {HijrahDate.class},
        };
    }

    @Test(dataProvider="invalidSerialformClasses")
    public void test_invalid_serialform(Class<?> clazz) throws Exception {
        assertNotSerializable(clazz);
    }

}
