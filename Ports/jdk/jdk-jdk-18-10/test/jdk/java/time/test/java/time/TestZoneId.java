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
package test.java.time;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.testng.annotations.Test;

/**
 * Test ZoneId.
 */
@Test
public class TestZoneId extends AbstractTest {

    private static final int OVERLAP = 2;
    private static final int GAP = 0;

    //-----------------------------------------------------------------------
    // Basics
    //-----------------------------------------------------------------------
    public void test_immutable() {
        // cannot use standard test as ZoneId is abstract
        Class<ZoneId> cls = ZoneId.class;
        assertTrue(Modifier.isPublic(cls.getModifiers()));
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) == false) {
                assertTrue(Modifier.isPrivate(field.getModifiers()));
                assertTrue(Modifier.isFinal(field.getModifiers()) ||
                        (Modifier.isVolatile(field.getModifiers()) && Modifier.isTransient(field.getModifiers())));
            }
        }
    }

    //-----------------------------------------------------------------------
    // UTC
    //-----------------------------------------------------------------------
    public void test_constant_UTC() {
        ZoneId test = ZoneOffset.UTC;
        assertEquals(test.getId(), "Z");
        assertEquals(test.getDisplayName(TextStyle.FULL, Locale.UK), "Z");
        assertEquals(test.getRules().isFixedOffset(), true);
        assertEquals(test.getRules().getOffset(Instant.ofEpochSecond(0L)), ZoneOffset.UTC);
        checkOffset(test.getRules(), createLDT(2008, 6, 30), ZoneOffset.UTC, 1);
    }

    //-----------------------------------------------------------------------
    // system default
    //-----------------------------------------------------------------------
    public void test_systemDefault() {
        ZoneId test = ZoneId.systemDefault();
        assertEquals(test.getId(), TimeZone.getDefault().getID());
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_systemDefault_unableToConvert_badFormat() {
        TimeZone current = TimeZone.getDefault();
        try {
            TimeZone.setDefault(new SimpleTimeZone(127, "Something Weird"));
            ZoneId.systemDefault();
        } finally {
            TimeZone.setDefault(current);
        }
    }

    @Test(expectedExceptions = ZoneRulesException.class)
    public void test_systemDefault_unableToConvert_unknownId() {
        TimeZone current = TimeZone.getDefault();
        try {
            TimeZone.setDefault(new SimpleTimeZone(127, "SomethingWeird"));
            ZoneId.systemDefault();
        } finally {
            TimeZone.setDefault(current);
        }
    }

    //-----------------------------------------------------------------------
    // Europe/London
    //-----------------------------------------------------------------------
    public void test_London() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals(test.getId(), "Europe/London");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    public void test_London_getOffset() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals(test.getRules().getOffset(createInstant(2008, 1, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 2, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 4, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 5, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 6, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 7, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 8, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 9, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 12, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
    }

    public void test_London_getOffset_toDST() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 24, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 25, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 26, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 27, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 28, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 29, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 31, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        // cutover at 01:00Z
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
    }

    public void test_London_getOffset_fromDST() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 24, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 25, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 27, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 28, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 29, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 30, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 31, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
        // cutover at 01:00Z
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, ZoneOffset.UTC)), ZoneOffset.ofHours(0));
    }

    public void test_London_getOffsetInfo() {
        ZoneId test = ZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), ZoneOffset.ofHours(0), 1);
    }

    public void test_London_getOffsetInfo_toDST() {
        ZoneId test = ZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 3, 24), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 25), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 26), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 27), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 29), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 30), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 31), ZoneOffset.ofHours(1), 1);
        // cutover at 01:00Z
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 0, 59, 59, 999999999), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 1, 30, 0, 0), ZoneOffset.ofHours(0), GAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 2, 0, 0, 0), ZoneOffset.ofHours(1), 1);
    }

    public void test_London_getOffsetInfo_fromDST() {
        ZoneId test = ZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 10, 24), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 25), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 26), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 27), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 29), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 30), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 31), ZoneOffset.ofHours(0), 1);
        // cutover at 01:00Z
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 0, 59, 59, 999999999), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 1, 30, 0, 0), ZoneOffset.ofHours(1), OVERLAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 2, 0, 0, 0), ZoneOffset.ofHours(0), 1);
    }

    public void test_London_getOffsetInfo_gap() {
        ZoneId test = ZoneId.of("Europe/London");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 30, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(0), GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), ZoneOffset.ofHours(0));
        assertEquals(trans.getOffsetAfter(), ZoneOffset.ofHours(1));
        assertEquals(trans.getInstant(), dateTime.toInstant(ZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), LocalDateTime.of(2008, 3, 30, 1, 0));
        assertEquals(trans.getDateTimeAfter(), LocalDateTime.of(2008, 3, 30, 2, 0));
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(0)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(1)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(2)), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T01:00Z to +01:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(0)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_London_getOffsetInfo_overlap() {
        ZoneId test = ZoneId.of("Europe/London");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 10, 26, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(1), OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), ZoneOffset.ofHours(1));
        assertEquals(trans.getOffsetAfter(), ZoneOffset.ofHours(0));
        assertEquals(trans.getInstant(), dateTime.toInstant(ZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), LocalDateTime.of(2008, 10, 26, 2, 0));
        assertEquals(trans.getDateTimeAfter(), LocalDateTime.of(2008, 10, 26, 1, 0));
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(0)), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(1)), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(2)), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T02:00+01:00 to Z]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(1)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    //-----------------------------------------------------------------------
    // Europe/Paris
    //-----------------------------------------------------------------------
    public void test_Paris() {
        ZoneId test = ZoneId.of("Europe/Paris");
        assertEquals(test.getId(), "Europe/Paris");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    public void test_Paris_getOffset() {
        ZoneId test = ZoneId.of("Europe/Paris");
        assertEquals(test.getRules().getOffset(createInstant(2008, 1, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 2, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 4, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 5, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 6, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 7, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 8, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 9, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 12, 1, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
    }

    public void test_Paris_getOffset_toDST() {
        ZoneId test = ZoneId.of("Europe/Paris");
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 24, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 25, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 26, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 27, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 28, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 29, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 31, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        // cutover at 01:00Z
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
    }

    public void test_Paris_getOffset_fromDST() {
        ZoneId test = ZoneId.of("Europe/Paris");
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 24, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 25, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 27, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 28, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 29, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 30, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 31, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
        // cutover at 01:00Z
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, ZoneOffset.UTC)), ZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, ZoneOffset.UTC)), ZoneOffset.ofHours(1));
    }

    public void test_Paris_getOffsetInfo() {
        ZoneId test = ZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), ZoneOffset.ofHours(1), 1);
    }

    public void test_Paris_getOffsetInfo_toDST() {
        ZoneId test = ZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 3, 24), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 25), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 26), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 27), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 29), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 30), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 31), ZoneOffset.ofHours(2), 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 1, 59, 59, 999999999), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 2, 30, 0, 0), ZoneOffset.ofHours(1), GAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 3, 0, 0, 0), ZoneOffset.ofHours(2), 1);
    }

    public void test_Paris_getOffsetInfo_fromDST() {
        ZoneId test = ZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 10, 24), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 25), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 26), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 27), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 29), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 30), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 31), ZoneOffset.ofHours(1), 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 1, 59, 59, 999999999), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 2, 30, 0, 0), ZoneOffset.ofHours(2), OVERLAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 3, 0, 0, 0), ZoneOffset.ofHours(1), 1);
    }

    public void test_Paris_getOffsetInfo_gap() {
        ZoneId test = ZoneId.of("Europe/Paris");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 30, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(1), GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), ZoneOffset.ofHours(1));
        assertEquals(trans.getOffsetAfter(), ZoneOffset.ofHours(2));
        assertEquals(trans.getInstant(), createInstant(2008, 3, 30, 1, 0, 0, 0, ZoneOffset.UTC));
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(0)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(1)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(2)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(3)), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T02:00+01:00 to +02:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(1)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherDis = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherDis));
        assertEquals(trans.hashCode(), otherDis.hashCode());
    }

    public void test_Paris_getOffsetInfo_overlap() {
        ZoneId test = ZoneId.of("Europe/Paris");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 10, 26, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(2), OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), ZoneOffset.ofHours(2));
        assertEquals(trans.getOffsetAfter(), ZoneOffset.ofHours(1));
        assertEquals(trans.getInstant(), createInstant(2008, 10, 26, 1, 0, 0, 0, ZoneOffset.UTC));
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(0)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(1)), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(2)), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(3)), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T03:00+02:00 to +01:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(2)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherDis = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherDis));
        assertEquals(trans.hashCode(), otherDis.hashCode());
    }

    //-----------------------------------------------------------------------
    // America/New_York
    //-----------------------------------------------------------------------
    public void test_NewYork() {
        ZoneId test = ZoneId.of("America/New_York");
        assertEquals(test.getId(), "America/New_York");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    public void test_NewYork_getOffset() {
        ZoneId test = ZoneId.of("America/New_York");
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        assertEquals(test.getRules().getOffset(createInstant(2008, 1, 1, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 2, 1, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 1, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 4, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 5, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 6, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 7, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 8, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 9, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 12, 1, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 1, 28, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 2, 28, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 4, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 5, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 6, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 7, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 8, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 9, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 28, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 12, 28, offset)), ZoneOffset.ofHours(-5));
    }

    public void test_NewYork_getOffset_toDST() {
        ZoneId test = ZoneId.of("America/New_York");
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 8, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 9, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 10, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 11, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 12, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 13, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 14, offset)), ZoneOffset.ofHours(-4));
        // cutover at 02:00 local
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 9, 1, 59, 59, 999999999, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 9, 2, 0, 0, 0, offset)), ZoneOffset.ofHours(-4));
    }

    public void test_NewYork_getOffset_fromDST() {
        ZoneId test = ZoneId.of("America/New_York");
        ZoneOffset offset = ZoneOffset.ofHours(-4);
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 2, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 3, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 4, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 5, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 6, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 7, offset)), ZoneOffset.ofHours(-5));
        // cutover at 02:00 local
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 2, 1, 59, 59, 999999999, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 2, 2, 0, 0, 0, offset)), ZoneOffset.ofHours(-5));
    }

    public void test_NewYork_getOffsetInfo() {
        ZoneId test = ZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 1, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 28), ZoneOffset.ofHours(-5), 1);
    }

    public void test_NewYork_getOffsetInfo_toDST() {
        ZoneId test = ZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 3, 8), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 9), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 10), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 11), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 12), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 13), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 14), ZoneOffset.ofHours(-4), 1);
        // cutover at 02:00 local
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 9, 1, 59, 59, 999999999), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 9, 2, 30, 0, 0), ZoneOffset.ofHours(-5), GAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 9, 3, 0, 0, 0), ZoneOffset.ofHours(-4), 1);
    }

    public void test_NewYork_getOffsetInfo_fromDST() {
        ZoneId test = ZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 11, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 2), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 3), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 4), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 5), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 6), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 7), ZoneOffset.ofHours(-5), 1);
        // cutover at 02:00 local
        checkOffset(test.getRules(), LocalDateTime.of(2008, 11, 2, 0, 59, 59, 999999999), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 11, 2, 1, 30, 0, 0), ZoneOffset.ofHours(-4), OVERLAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 11, 2, 2, 0, 0, 0), ZoneOffset.ofHours(-5), 1);
    }

    public void test_NewYork_getOffsetInfo_gap() {
        ZoneId test = ZoneId.of("America/New_York");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 9, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(-5), GAP);
        assertEquals(trans.getOffsetBefore(), ZoneOffset.ofHours(-5));
        assertEquals(trans.getOffsetAfter(), ZoneOffset.ofHours(-4));
        assertEquals(trans.getInstant(), createInstant(2008, 3, 9, 2, 0, 0, 0, ZoneOffset.ofHours(-5)));
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-6)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-5)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-4)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-3)), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-09T02:00-05:00 to -04:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(-5)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));

        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_NewYork_getOffsetInfo_overlap() {
        ZoneId test = ZoneId.of("America/New_York");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 11, 2, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(-4), OVERLAP);
        assertEquals(trans.getOffsetBefore(), ZoneOffset.ofHours(-4));
        assertEquals(trans.getOffsetAfter(), ZoneOffset.ofHours(-5));
        assertEquals(trans.getInstant(), createInstant(2008, 11, 2, 2, 0, 0, 0, ZoneOffset.ofHours(-4)));
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-5)), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-4)), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(2)), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-11-02T02:00-04:00 to -05:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(-4)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));

        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    //-----------------------------------------------------------------------
    // getXxx() isXxx()
    //-----------------------------------------------------------------------
    public void test_get_Tzdb() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals(test.getId(), "Europe/London");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    public void test_get_TzdbFixed() {
        ZoneId test = ZoneId.of("+01:30");
        assertEquals(test.getId(), "+01:30");
        assertEquals(test.getRules().isFixedOffset(), true);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    private Instant createInstant(int year, int month, int day, ZoneOffset offset) {
        return LocalDateTime.of(year, month, day, 0, 0).toInstant(offset);
    }

    private Instant createInstant(int year, int month, int day, int hour, int min, int sec, int nano, ZoneOffset offset) {
        return LocalDateTime.of(year, month, day, hour, min, sec, nano).toInstant(offset);
    }

    private ZonedDateTime createZDT(int year, int month, int day, int hour, int min, int sec, int nano, ZoneId zone) {
        return LocalDateTime.of(year, month, day, hour, min, sec, nano).atZone(zone);
    }

    private LocalDateTime createLDT(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 0, 0);
    }

    private ZoneOffsetTransition checkOffset(ZoneRules rules, LocalDateTime dateTime, ZoneOffset offset, int type) {
        List<ZoneOffset> validOffsets = rules.getValidOffsets(dateTime);
        assertEquals(validOffsets.size(), type);
        assertEquals(rules.getOffset(dateTime), offset);
        if (type == 1) {
            assertEquals(validOffsets.get(0), offset);
            return null;
        } else {
            ZoneOffsetTransition zot = rules.getTransition(dateTime);
            assertNotNull(zot);
            assertEquals(zot.isOverlap(), type == 2);
            assertEquals(zot.isGap(), type == 0);
            assertEquals(zot.isValidOffset(offset), type == 2);
            return zot;
        }
    }

}
