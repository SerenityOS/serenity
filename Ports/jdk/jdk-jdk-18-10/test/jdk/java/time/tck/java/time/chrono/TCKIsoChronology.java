/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
package tck.java.time.chrono;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.HijrahEra;
import java.time.chrono.IsoChronology;
import java.time.chrono.IsoEra;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TCKIsoChronology {
    // Can only work with IsoChronology here
    // others may be in separate module
    private static final ZoneOffset OFFSET_P0100 = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_M0100 = ZoneOffset.ofHours(-1);

    @Test
    public void factory_from_TemporalAccessor_dateWithChronlogy() {
        assertEquals(Chronology.from(LocalDate.of(2012, 6, 30)), IsoChronology.INSTANCE);
    }

    @Test
    public void factory_from_TemporalAccessor_chronology() {
        assertEquals(Chronology.from(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getLong(TemporalField field) {
                throw new UnsupportedOperationException();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.chronology()) {
                    return (R) IsoChronology.INSTANCE;
                }
                throw new UnsupportedOperationException();
            }
        }), IsoChronology.INSTANCE);
    }

    @Test
    public void factory_from_TemporalAccessor_noChronology() {
        assertEquals(Chronology.from(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getLong(TemporalField field) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.chronology()) {
                    return null;
                }
                throw new UnsupportedOperationException();
            }
        }), IsoChronology.INSTANCE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_from_TemporalAccessor_null() {
        Chronology.from(null);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_date_TemporalAccessor() {
        assertEquals(IsoChronology.INSTANCE.date(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                if (field == ChronoField.EPOCH_DAY) {
                    return true;
                }
                throw new UnsupportedOperationException();
            }

            @Override
            public long getLong(TemporalField field) {
                if (field == ChronoField.EPOCH_DAY) {
                    return LocalDate.of(2012, 6, 30).toEpochDay();
                }
                throw new UnsupportedOperationException();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.localDate()) {
                    return (R) LocalDate.of(2012, 6, 30);
                }
                throw new UnsupportedOperationException();
            }
        }), LocalDate.of(2012, 6, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_date_TemporalAccessor_null() {
        IsoChronology.INSTANCE.date(null);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_localDateTime_TemporalAccessor() {
        assertEquals(IsoChronology.INSTANCE.localDateTime(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                if (field == ChronoField.EPOCH_DAY || field == ChronoField.NANO_OF_DAY) {
                    return true;
                }
                throw new UnsupportedOperationException();
            }

            @Override
            public long getLong(TemporalField field) {
                if (field == ChronoField.EPOCH_DAY) {
                    return LocalDate.of(2012, 6, 30).toEpochDay();
                }
                if (field == ChronoField.NANO_OF_DAY) {
                    return LocalTime.of(12, 30, 40).toNanoOfDay();
                }
                throw new UnsupportedOperationException();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.localDate()) {
                    return (R) LocalDate.of(2012, 6, 30);
                }
                if (query == TemporalQueries.localTime()) {
                    return (R) LocalTime.of(12, 30, 40);
                }
                throw new UnsupportedOperationException();
            }
        }), LocalDateTime.of(2012, 6, 30, 12, 30, 40));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_localDateTime_TemporalAccessor_null() {
        IsoChronology.INSTANCE.localDateTime(null);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_zonedDateTime_TemporalAccessor() {
        assertEquals(IsoChronology.INSTANCE.zonedDateTime(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                if (field == ChronoField.EPOCH_DAY || field == ChronoField.NANO_OF_DAY ||
                        field == ChronoField.INSTANT_SECONDS || field == ChronoField.NANO_OF_SECOND) {
                    return true;
                }
                throw new UnsupportedOperationException();
            }

            @Override
            public long getLong(TemporalField field) {
                if (field == ChronoField.INSTANT_SECONDS) {
                    return ZonedDateTime.of(2012, 6, 30, 12, 30, 40, 0, ZoneId.of("Europe/London")).toEpochSecond();
                }
                if (field == ChronoField.NANO_OF_SECOND) {
                    return 0;
                }
                if (field == ChronoField.EPOCH_DAY) {
                    return LocalDate.of(2012, 6, 30).toEpochDay();
                }
                if (field == ChronoField.NANO_OF_DAY) {
                    return LocalTime.of(12, 30, 40).toNanoOfDay();
                }
                throw new UnsupportedOperationException();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.localDate()) {
                    return (R) LocalDate.of(2012, 6, 30);
                }
                if (query == TemporalQueries.localTime()) {
                    return (R) LocalTime.of(12, 30, 40);
                }
                if (query == TemporalQueries.zoneId() || query == TemporalQueries.zone()) {
                    return (R) ZoneId.of("Europe/London");
                }
                throw new UnsupportedOperationException();
            }
        }), ZonedDateTime.of(2012, 6, 30, 12, 30, 40, 0, ZoneId.of("Europe/London")));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_zonedDateTime_TemporalAccessor_null() {
        IsoChronology.INSTANCE.zonedDateTime(null);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_yearOfEra")
    Object[][] data_resolve_yearOfEra() {
        return new Object[][] {
                // era only
                {ResolverStyle.STRICT, -1, null, null, null, null},
                {ResolverStyle.SMART, -1, null, null, null, null},
                {ResolverStyle.LENIENT, -1, null, null, null, null},

                {ResolverStyle.STRICT, 0, null, null, ChronoField.ERA, 0},
                {ResolverStyle.SMART, 0, null, null, ChronoField.ERA, 0},
                {ResolverStyle.LENIENT, 0, null, null, ChronoField.ERA, 0},

                {ResolverStyle.STRICT, 1, null, null, ChronoField.ERA, 1},
                {ResolverStyle.SMART, 1, null, null, ChronoField.ERA, 1},
                {ResolverStyle.LENIENT, 1, null, null, ChronoField.ERA, 1},

                {ResolverStyle.STRICT, 2, null, null, null, null},
                {ResolverStyle.SMART, 2, null, null, null, null},
                {ResolverStyle.LENIENT, 2, null, null, null, null},

                // era and year-of-era
                {ResolverStyle.STRICT, -1, 2012, null, null, null},
                {ResolverStyle.SMART, -1, 2012, null, null, null},
                {ResolverStyle.LENIENT, -1, 2012, null, null, null},

                {ResolverStyle.STRICT, 0, 2012, null, ChronoField.YEAR, -2011},
                {ResolverStyle.SMART, 0, 2012, null, ChronoField.YEAR, -2011},
                {ResolverStyle.LENIENT, 0, 2012, null, ChronoField.YEAR, -2011},

                {ResolverStyle.STRICT, 1, 2012, null, ChronoField.YEAR, 2012},
                {ResolverStyle.SMART, 1, 2012, null, ChronoField.YEAR, 2012},
                {ResolverStyle.LENIENT, 1, 2012, null, ChronoField.YEAR, 2012},

                {ResolverStyle.STRICT, 2, 2012, null, null, null},
                {ResolverStyle.SMART, 2, 2012, null, null, null},
                {ResolverStyle.LENIENT, 2, 2012, null, null, null},

                // year-of-era only
                {ResolverStyle.STRICT, null, 2012, null, ChronoField.YEAR_OF_ERA, 2012},
                {ResolverStyle.SMART, null, 2012, null, ChronoField.YEAR, 2012},
                {ResolverStyle.LENIENT, null, 2012, null, ChronoField.YEAR, 2012},

                {ResolverStyle.STRICT, null, Integer.MAX_VALUE, null, null, null},
                {ResolverStyle.SMART, null, Integer.MAX_VALUE, null, null, null},
                {ResolverStyle.LENIENT, null, Integer.MAX_VALUE, null, ChronoField.YEAR, Integer.MAX_VALUE},

                // year-of-era and year
                {ResolverStyle.STRICT, null, 2012, 2012, ChronoField.YEAR, 2012},
                {ResolverStyle.SMART, null, 2012, 2012, ChronoField.YEAR, 2012},
                {ResolverStyle.LENIENT, null, 2012, 2012, ChronoField.YEAR, 2012},

                {ResolverStyle.STRICT, null, 2012, -2011, ChronoField.YEAR, -2011},
                {ResolverStyle.SMART, null, 2012, -2011, ChronoField.YEAR, -2011},
                {ResolverStyle.LENIENT, null, 2012, -2011, ChronoField.YEAR, -2011},

                {ResolverStyle.STRICT, null, 2012, 2013, null, null},
                {ResolverStyle.SMART, null, 2012, 2013, null, null},
                {ResolverStyle.LENIENT, null, 2012, 2013, null, null},

                {ResolverStyle.STRICT, null, 2012, -2013, null, null},
                {ResolverStyle.SMART, null, 2012, -2013, null, null},
                {ResolverStyle.LENIENT, null, 2012, -2013, null, null},
        };
    }

    @Test(dataProvider = "resolve_yearOfEra")
    public void test_resolve_yearOfEra(ResolverStyle style, Integer e, Integer yoe, Integer y, ChronoField field, Integer expected) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        if (e != null) {
            fieldValues.put(ChronoField.ERA, (long) e);
        }
        if (yoe != null) {
            fieldValues.put(ChronoField.YEAR_OF_ERA, (long) yoe);
        }
        if (y != null) {
            fieldValues.put(ChronoField.YEAR, (long) y);
        }
        if (field != null) {
            LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, style);
            assertEquals(date, null);
            assertEquals(fieldValues.get(field), (Long) expected.longValue());
            assertEquals(fieldValues.size(), 1);
        } else {
            try {
                IsoChronology.INSTANCE.resolveDate(fieldValues, style);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_ymd")
    Object[][] data_resolve_ymd() {
        return new Object[][] {
                {2012, 1, -365, date(2010, 12, 31), false, false},
                {2012, 1, -364, date(2011, 1, 1), false, false},
                {2012, 1, -31, date(2011, 11, 30), false, false},
                {2012, 1, -30, date(2011, 12, 1), false, false},
                {2012, 1, -12, date(2011, 12, 19), false, false},
                {2012, 1, 1, date(2012, 1, 1), true, true},
                {2012, 1, 27, date(2012, 1, 27), true, true},
                {2012, 1, 28, date(2012, 1, 28), true, true},
                {2012, 1, 29, date(2012, 1, 29), true, true},
                {2012, 1, 30, date(2012, 1, 30), true, true},
                {2012, 1, 31, date(2012, 1, 31), true, true},
                {2012, 1, 59, date(2012, 2, 28), false, false},
                {2012, 1, 60, date(2012, 2, 29), false, false},
                {2012, 1, 61, date(2012, 3, 1), false, false},
                {2012, 1, 365, date(2012, 12, 30), false, false},
                {2012, 1, 366, date(2012, 12, 31), false, false},
                {2012, 1, 367, date(2013, 1, 1), false, false},
                {2012, 1, 367 + 364, date(2013, 12, 31), false, false},
                {2012, 1, 367 + 365, date(2014, 1, 1), false, false},

                {2012, 2, 1, date(2012, 2, 1), true, true},
                {2012, 2, 28, date(2012, 2, 28), true, true},
                {2012, 2, 29, date(2012, 2, 29), true, true},
                {2012, 2, 30, date(2012, 3, 1), date(2012, 2, 29), false},
                {2012, 2, 31, date(2012, 3, 2), date(2012, 2, 29), false},
                {2012, 2, 32, date(2012, 3, 3), false, false},

                {2012, -12, 1, date(2010, 12, 1), false, false},
                {2012, -11, 1, date(2011, 1, 1), false, false},
                {2012, -1, 1, date(2011, 11, 1), false, false},
                {2012, 0, 1, date(2011, 12, 1), false, false},
                {2012, 1, 1, date(2012, 1, 1), true, true},
                {2012, 12, 1, date(2012, 12, 1), true, true},
                {2012, 13, 1, date(2013, 1, 1), false, false},
                {2012, 24, 1, date(2013, 12, 1), false, false},
                {2012, 25, 1, date(2014, 1, 1), false, false},

                {2012, 6, -31, date(2012, 4, 30), false, false},
                {2012, 6, -30, date(2012, 5, 1), false, false},
                {2012, 6, -1, date(2012, 5, 30), false, false},
                {2012, 6, 0, date(2012, 5, 31), false, false},
                {2012, 6, 1, date(2012, 6, 1), true, true},
                {2012, 6, 30, date(2012, 6, 30), true, true},
                {2012, 6, 31, date(2012, 7, 1), date(2012, 6, 30), false},
                {2012, 6, 61, date(2012, 7, 31), false, false},
                {2012, 6, 62, date(2012, 8, 1), false, false},

                {2011, 2, 1, date(2011, 2, 1), true, true},
                {2011, 2, 28, date(2011, 2, 28), true, true},
                {2011, 2, 29, date(2011, 3, 1), date(2011, 2, 28), false},
                {2011, 2, 30, date(2011, 3, 2), date(2011, 2, 28), false},
                {2011, 2, 31, date(2011, 3, 3), date(2011, 2, 28), false},
                {2011, 2, 32, date(2011, 3, 4), false, false},
        };
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_lenient(int y, int m, int d, LocalDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_smart(int y, int m, int d, LocalDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (Boolean.TRUE.equals(smart)) {
            LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else if (smart instanceof LocalDate) {
            LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, smart);
        } else {
            try {
                IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_strict(int y, int m, int d, LocalDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (strict) {
            LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_yd")
    Object[][] data_resolve_yd() {
        return new Object[][] {
                {2012, -365, date(2010, 12, 31), false, false},
                {2012, -364, date(2011, 1, 1), false, false},
                {2012, -31, date(2011, 11, 30), false, false},
                {2012, -30, date(2011, 12, 1), false, false},
                {2012, -12, date(2011, 12, 19), false, false},
                {2012, -1, date(2011, 12, 30), false, false},
                {2012, 0, date(2011, 12, 31), false, false},
                {2012, 1, date(2012, 1, 1), true, true},
                {2012, 2, date(2012, 1, 2), true, true},
                {2012, 31, date(2012, 1, 31), true, true},
                {2012, 32, date(2012, 2, 1), true, true},
                {2012, 59, date(2012, 2, 28), true, true},
                {2012, 60, date(2012, 2, 29), true, true},
                {2012, 61, date(2012, 3, 1), true, true},
                {2012, 365, date(2012, 12, 30), true, true},
                {2012, 366, date(2012, 12, 31), true, true},
                {2012, 367, date(2013, 1, 1), false, false},
                {2012, 367 + 364, date(2013, 12, 31), false, false},
                {2012, 367 + 365, date(2014, 1, 1), false, false},

                {2011, 59, date(2011, 2, 28), true, true},
                {2011, 60, date(2011, 3, 1), true, true},
        };
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_lenient(int y, int d, LocalDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_smart(int y, int d, LocalDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        if (smart) {
            LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_strict(int y, int d, LocalDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        if (strict) {
            LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_ymaa")
    Object[][] data_resolve_ymaa() {
        return new Object[][] {
                {2012, 1, 1, -365, date(2010, 12, 31), false, false},
                {2012, 1, 1, -364, date(2011, 1, 1), false, false},
                {2012, 1, 1, -31, date(2011, 11, 30), false, false},
                {2012, 1, 1, -30, date(2011, 12, 1), false, false},
                {2012, 1, 1, -12, date(2011, 12, 19), false, false},
                {2012, 1, 1, 1, date(2012, 1, 1), true, true},
                {2012, 1, 1, 59, date(2012, 2, 28), false, false},
                {2012, 1, 1, 60, date(2012, 2, 29), false, false},
                {2012, 1, 1, 61, date(2012, 3, 1), false, false},
                {2012, 1, 1, 365, date(2012, 12, 30), false, false},
                {2012, 1, 1, 366, date(2012, 12, 31), false, false},
                {2012, 1, 1, 367, date(2013, 1, 1), false, false},
                {2012, 1, 1, 367 + 364, date(2013, 12, 31), false, false},
                {2012, 1, 1, 367 + 365, date(2014, 1, 1), false, false},

                {2012, 2, 0, 1, date(2012, 1, 25), false, false},
                {2012, 2, 0, 7, date(2012, 1, 31), false, false},
                {2012, 2, 1, 1, date(2012, 2, 1), true, true},
                {2012, 2, 1, 7, date(2012, 2, 7), true, true},
                {2012, 2, 2, 1, date(2012, 2, 8), true, true},
                {2012, 2, 2, 7, date(2012, 2, 14), true, true},
                {2012, 2, 3, 1, date(2012, 2, 15), true, true},
                {2012, 2, 3, 7, date(2012, 2, 21), true, true},
                {2012, 2, 4, 1, date(2012, 2, 22), true, true},
                {2012, 2, 4, 7, date(2012, 2, 28), true, true},
                {2012, 2, 5, 1, date(2012, 2, 29), true, true},
                {2012, 2, 5, 2, date(2012, 3, 1), true, false},
                {2012, 2, 5, 7, date(2012, 3, 6), true, false},
                {2012, 2, 6, 1, date(2012, 3, 7), false, false},
                {2012, 2, 6, 7, date(2012, 3, 13), false, false},

                {2012, 12, 1, 1, date(2012, 12, 1), true, true},
                {2012, 12, 5, 1, date(2012, 12, 29), true, true},
                {2012, 12, 5, 2, date(2012, 12, 30), true, true},
                {2012, 12, 5, 3, date(2012, 12, 31), true, true},
                {2012, 12, 5, 4, date(2013, 1, 1), true, false},
                {2012, 12, 5, 7, date(2013, 1, 4), true, false},

                {2012, -12, 1, 1, date(2010, 12, 1), false, false},
                {2012, -11, 1, 1, date(2011, 1, 1), false, false},
                {2012, -1, 1, 1, date(2011, 11, 1), false, false},
                {2012, 0, 1, 1, date(2011, 12, 1), false, false},
                {2012, 1, 1, 1, date(2012, 1, 1), true, true},
                {2012, 12, 1, 1, date(2012, 12, 1), true, true},
                {2012, 13, 1, 1, date(2013, 1, 1), false, false},
                {2012, 24, 1, 1, date(2013, 12, 1), false, false},
                {2012, 25, 1, 1, date(2014, 1, 1), false, false},

                {2011, 2, 1, 1, date(2011, 2, 1), true, true},
                {2011, 2, 4, 7, date(2011, 2, 28), true, true},
                {2011, 2, 5, 1, date(2011, 3, 1), true, false},
        };
    }

    @Test(dataProvider = "resolve_ymaa")
    public void test_resolve_ymaa_lenient(int y, int m, int w, int d, LocalDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.ALIGNED_WEEK_OF_MONTH, (long) w);
        fieldValues.put(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH, (long) d);
        LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_ymaa")
    public void test_resolve_ymaa_smart(int y, int m, int w, int d, LocalDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.ALIGNED_WEEK_OF_MONTH, (long) w);
        fieldValues.put(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH, (long) d);
        if (smart) {
            LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_ymaa")
    public void test_resolve_ymaa_strict(int y, int m, int w, int d, LocalDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.ALIGNED_WEEK_OF_MONTH, (long) w);
        fieldValues.put(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH, (long) d);
        if (strict) {
            LocalDate date = IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                IsoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @DataProvider(name = "epochSecond_dataProvider")
    Object[][] data_epochSecond() {
        return new Object[][] {
                {2008, 3, 3, 1, 2, 2, OFFSET_P0100},
                {2008, 3, 3, 1, 2, 2, OFFSET_M0100},
                {2008, 2, 28, 1, 2, 2, OFFSET_P0100},
                {2009, 3, 3, 1, 2, 2, OFFSET_P0100},
                {2009, 3, 3, 1, 2, 2, OFFSET_M0100},
                {2009, 2, 28, 1, 2, 2, OFFSET_P0100},
                {1968, 3, 3, 1, 2, 2, OFFSET_P0100},
                {1968, 3, 3, 1, 2, 2, OFFSET_M0100},
                {1968, 2, 28, 1, 2, 2, OFFSET_P0100},
                {1969, 3, 3 , 1, 2, 2, OFFSET_P0100},
                {1969, 3, 3, 1, 2, 2, OFFSET_M0100},
                {1969, 2, 28, 1, 2, 2, OFFSET_P0100},
                {1970, 1, 1, 1, 2, 2, OFFSET_P0100},
                {1970, 1, 1, 1, 2, 2, OFFSET_M0100},
                {-4, 3, 3 , 1, 2, 2, OFFSET_P0100},
                {-1, 3, 3 , 1, 2, 2, OFFSET_P0100},
        };
    }

    @Test(dataProvider = "epochSecond_dataProvider")
    public void test_epochSecond_1(int y, int m, int d, int h , int min, int s, ZoneOffset offset) {
        assertEquals(IsoChronology.INSTANCE.epochSecond(y, m, d, h, min, s, offset),
                     OffsetDateTime.of(y, m, d, h, min, s, 0, offset).toEpochSecond());
    }

    @Test
    public void test_epochSecond_2() {
        assertEquals(IsoChronology.INSTANCE.epochSecond(2008, 3, 3, 1, 2, 2, OFFSET_P0100),
                     ZonedDateTime.of(2008, 3, 3, 1, 2, 2, 0, ZoneId.of("+01:00")).toEpochSecond());
        assertEquals(IsoChronology.INSTANCE.epochSecond(1969, 3, 3, 1, 2, 2, OFFSET_P0100),
                     ZonedDateTime.of(1969, 3, 3, 1, 2, 2, 0, ZoneId.of("+01:00")).toEpochSecond());
    }

    @Test
    public void test_epochSecond_max() {
        assertEquals(IsoChronology.INSTANCE.epochSecond(Year.MAX_VALUE, 12, 31, 23, 59, 59, ZoneOffset.MIN),
                     OffsetDateTime.of(Year.MAX_VALUE, 12, 31, 23, 59, 59, 0, ZoneOffset.MIN).toEpochSecond());
    }

    @Test
    public void test_epochSecond_min() {
        assertEquals(IsoChronology.INSTANCE.epochSecond(Year.MIN_VALUE, 1, 1, 0, 0, 0, ZoneOffset.MAX),
                     OffsetDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0, 0, 0, ZoneOffset.MAX).toEpochSecond());
    }

    @DataProvider(name = "bad_epochSecond_dataProvider")
    Object[][]  bad_data_epochSecond() {
        return new Object[][] {
            {2009, 13, 1, 1, 1, 1, OFFSET_P0100},
            {2009, 2, 29, 1, 1, 1, OFFSET_P0100},
            {2009, 1, 1, 25, 1, 1, OFFSET_P0100},
            {2009, 1, 1, 1, 60, 1, OFFSET_P0100},
            {2009, 1, 1, 1, 1, -11, OFFSET_P0100},
        };
    }
    @Test(dataProvider = "bad_epochSecond_dataProvider", expectedExceptions = DateTimeException.class)
    public void test_epochSecond_bad(int y, int m, int d, int h , int min, int s, ZoneOffset offset) {
        IsoChronology.INSTANCE.epochSecond(y, m, d, h, min, s, offset);
    }

    @DataProvider(name = "era_epochSecond_dataProvider")
    Object[][] data_era_epochSecond() {
        return new Object[][] {
                {IsoEra.CE, 2008, 3, 3, 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 2008, 3, 3, 1, 2, 2, OFFSET_M0100},
                {IsoEra.CE, 2008, 2, 28, 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 2009, 3, 3, 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 2009, 3, 3, 1, 2, 2, OFFSET_M0100},
                {IsoEra.CE, 2009, 2, 28, 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 1968, 3, 3, 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 1968, 3, 3, 1, 2, 2, OFFSET_M0100},
                {IsoEra.CE, 1968, 2, 28, 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 1969, 3, 3 , 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 1969, 3, 3, 1, 2, 2, OFFSET_M0100},
                {IsoEra.CE, 1969, 2, 28, 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 1970, 1, 1, 1, 2, 2, OFFSET_P0100},
                {IsoEra.CE, 1970, 1, 1, 1, 2, 2, OFFSET_M0100},
                {IsoEra.BCE, 5, 3, 3 , 1, 2, 2, OFFSET_P0100},
                {IsoEra.BCE, 2, 3, 3 , 1, 2, 2, OFFSET_P0100},
        };
    }

    @Test(dataProvider = "era_epochSecond_dataProvider")
    public void test_era_epochSecond_1(Era era, int y, int m, int d, int h , int min, int s, ZoneOffset offset) {
        assertEquals(IsoChronology.INSTANCE.epochSecond(era, y, m, d, h, min, s, offset),
                     OffsetDateTime.of(IsoChronology.INSTANCE.date(era, y, m, d), LocalTime.of(h, min, s), offset)
                                   .toEpochSecond());
    }

    @Test
    public void test_era_epochSecond_2() {
        assertEquals(IsoChronology.INSTANCE.epochSecond(IsoEra.CE, 2008, 3, 3, 1, 2, 2, OFFSET_P0100),
                     ZonedDateTime.of(2008, 3, 3, 1, 2, 2, 0, ZoneId.of("+01:00")).toEpochSecond());
        assertEquals(IsoChronology.INSTANCE.epochSecond(IsoEra.CE, 1969, 3, 3, 1, 2, 2, OFFSET_P0100),
                     ZonedDateTime.of(1969, 3, 3, 1, 2, 2, 0, ZoneId.of("+01:00")).toEpochSecond());
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void test_era_epochSecond_bad() {
        IsoChronology.INSTANCE.epochSecond(HijrahEra.AH, 2009, 2, 29, 1, 2, 2, OFFSET_P0100);
    }


    //-----------------------------------------------------------------------
    private static LocalDate date(int y, int m, int d) {
        return LocalDate.of(y, m, d);
    }
}
