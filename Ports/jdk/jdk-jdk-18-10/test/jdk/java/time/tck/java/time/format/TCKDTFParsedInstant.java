/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package tck.java.time.format;

import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Testing DateTimeFormatter Parsing with 4 different test conditions:
 * 1. When Zone and Offset not provided
 * 2. When Zone and Offset provided
 * 3. When Offset is not provided and Zone is provided
 * 4. When Zone is not provided and Offset is provided
 */

@Test
public class TCKDTFParsedInstant {

    private static final ZoneId EUROPE_BERLIN = ZoneId.of("Europe/Berlin");
    private static final ZoneId ASIA_ISTANBUL = ZoneId.of("Asia/Istanbul");

    private DateTimeFormatter dtFormatter;
    private ZonedDateTime zdt1, zdt2;
    private LocalDateTime ldt1;
    private OffsetDateTime odt1;

    @BeforeMethod
    public void setUp() throws Exception {
        dtFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    }

    @DataProvider(name="parseWithoutZoneWithoutOffset")
    Object[][] data_parse_WithoutOffset_WithoutZone() {
        return new Object[][] {
            {"1966-12-31T00:01:10", LocalDateTime.of(1966, 12, 31, 0, 1, 10)},
            {"1970-01-01T00:00:00", LocalDateTime.of(1970, 1, 1, 0, 0, 0)},
            {"2004-02-29T00:30:00", LocalDateTime.of(2004, 2, 29, 0, 30, 0)},
            {"2015-12-31T23:59:59", LocalDateTime.of(2015, 12, 31, 23, 59, 59)}
        };
    }

    @Test(dataProvider="parseWithoutZoneWithoutOffset")
    public void testWithoutZoneWithoutOffset(String ldtString, LocalDateTime expectedLDT) {
        dtFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        ldt1 = LocalDateTime.parse(ldtString, dtFormatter);
        assertEquals(expectedLDT, ldt1);
    }

    @DataProvider(name="parseWithZoneWithOffset")
    Object[][] data_parse_WithZone_WithOffset() {
        return new Object[][] {
            {"2012-10-28T01:45:00-02:30[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 1, 45, 0, 0), ZoneOffset.of("-02:30"), EUROPE_BERLIN},
            {"2012-10-28T01:45:00-01:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 1, 45, 0, 0), ZoneOffset.of("-01:00"), EUROPE_BERLIN},
            {"2012-10-28T01:45:00-00:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 1, 45, 0, 0), ZoneOffset.of("-00:00"), EUROPE_BERLIN},
            {"2012-10-28T01:45:00+00:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 1, 45, 0, 0), ZoneOffset.of("+00:00"), EUROPE_BERLIN},
            {"2012-10-28T01:45:00+01:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 1, 45, 0, 0), ZoneOffset.of("+01:00"), EUROPE_BERLIN},
            {"2012-10-28T01:45:00+02:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 1, 45, 0, 0), ZoneOffset.of("+02:00"), EUROPE_BERLIN},
            {"2012-10-28T01:45:00+03:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 1, 45, 0, 0), ZoneOffset.of("+03:00"), EUROPE_BERLIN},
            {"2012-10-28T02:45:00-02:30[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("-02:30"), EUROPE_BERLIN},
            {"2012-10-28T02:45:00-01:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("-01:00"), EUROPE_BERLIN},
            {"2012-10-28T02:45:00-00:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("-00:00"), EUROPE_BERLIN},
            {"2012-10-28T02:45:00+00:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("+00:00"), EUROPE_BERLIN},
            {"2012-10-28T02:45:00+01:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("+01:00"), EUROPE_BERLIN},
            {"2012-10-28T02:45:00+02:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("+02:00"), EUROPE_BERLIN},
            {"2012-10-28T02:45:00+03:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("+03:00"), EUROPE_BERLIN},
            {"2012-10-28T03:45:00-02:30[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("-02:30"), EUROPE_BERLIN},
            {"2012-10-28T03:45:00-01:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("-01:00"), EUROPE_BERLIN},
            {"2012-10-28T03:45:00-00:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("-00:00"), EUROPE_BERLIN},
            {"2012-10-28T03:45:00+00:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("+00:00"), EUROPE_BERLIN},
            {"2012-10-28T03:45:00+01:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("+01:00"), EUROPE_BERLIN},
            {"2012-10-28T03:45:00+02:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("+02:00"), EUROPE_BERLIN},
            {"2012-10-28T03:45:00+03:00[Europe/Berlin]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("+03:00"), EUROPE_BERLIN},

            {"2012-10-28T02:45:00-02:30[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("-02:30"), ASIA_ISTANBUL},
            {"2012-10-28T02:45:00-01:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("-01:00"), ASIA_ISTANBUL},
            {"2012-10-28T02:45:00-00:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("-00:00"), ASIA_ISTANBUL},
            {"2012-10-28T02:45:00+00:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("+00:00"), ASIA_ISTANBUL},
            {"2012-10-28T02:45:00+01:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("+01:00"), ASIA_ISTANBUL},
            {"2012-10-28T02:45:00+02:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("+02:00"), ASIA_ISTANBUL},
            {"2012-10-28T02:45:00+03:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 2, 45, 0, 0), ZoneOffset.of("+03:00"), ASIA_ISTANBUL},
            {"2012-10-28T03:45:00-02:30[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("-02:30"), ASIA_ISTANBUL},
            {"2012-10-28T03:45:00-01:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("-01:00"), ASIA_ISTANBUL},
            {"2012-10-28T03:45:00-00:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("-00:00"), ASIA_ISTANBUL},
            {"2012-10-28T03:45:00+00:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("+00:00"), ASIA_ISTANBUL},
            {"2012-10-28T03:45:00+01:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("+01:00"), ASIA_ISTANBUL},
            {"2012-10-28T03:45:00+02:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("+02:00"), ASIA_ISTANBUL},
            {"2012-10-28T03:45:00+03:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 3, 45, 0, 0), ZoneOffset.of("+03:00"), ASIA_ISTANBUL},
            {"2012-10-28T04:45:00-02:30[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 4, 45, 0, 0), ZoneOffset.of("-02:30"), ASIA_ISTANBUL},
            {"2012-10-28T04:45:00-01:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 4, 45, 0, 0), ZoneOffset.of("-01:00"), ASIA_ISTANBUL},
            {"2012-10-28T04:45:00-00:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 4, 45, 0, 0), ZoneOffset.of("-00:00"), ASIA_ISTANBUL},
            {"2012-10-28T04:45:00+00:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 4, 45, 0, 0), ZoneOffset.of("+00:00"), ASIA_ISTANBUL},
            {"2012-10-28T04:45:00+01:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 4, 45, 0, 0), ZoneOffset.of("+01:00"), ASIA_ISTANBUL},
            {"2012-10-28T04:45:00+02:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 4, 45, 0, 0), ZoneOffset.of("+02:00"), ASIA_ISTANBUL},
            {"2012-10-28T04:45:00+03:00[Asia/Istanbul]",
                LocalDateTime.of(2012, 10, 28, 4, 45, 0, 0), ZoneOffset.of("+03:00"), ASIA_ISTANBUL}
        };
    }

    @Test(dataProvider="parseWithZoneWithOffset")
    public void testWithZoneWithOffset(String zdtString, LocalDateTime ldt, ZoneOffset offset, ZoneId zone) {
        dtFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        zdt1 = ZonedDateTime.ofInstant(ldt, offset, zone);
        zdt2 = ZonedDateTime.parse(zdtString, dtFormatter);
        assertEquals(zdt1, zdt2);
    }

    @DataProvider(name="parseWithZoneWithoutOffset")
    Object[][] data_parse_WithZone_WithoutOffset() {
        return new Object[][] {
            {"28 Oct 00:45:00 2012 Europe/Berlin", ZonedDateTime.of(2012, 10, 28, 0, 45, 0, 0, EUROPE_BERLIN)},
            {"28 Oct 01:45:00 2012 Europe/Berlin", ZonedDateTime.of(2012, 10, 28, 1, 45, 0, 0, EUROPE_BERLIN)},
            {"28 Oct 02:45:00 2012 Europe/Berlin", ZonedDateTime.of(2012, 10, 28, 2, 45, 0, 0, EUROPE_BERLIN)},
            {"28 Oct 03:45:00 2012 Europe/Berlin", ZonedDateTime.of(2012, 10, 28, 3, 45, 0, 0, EUROPE_BERLIN)},
            {"28 Oct 04:45:00 2012 Europe/Berlin", ZonedDateTime.of(2012, 10, 28, 4, 45, 0, 0, EUROPE_BERLIN)},

            {"28 Oct 01:45:00 2012 Asia/Istanbul", ZonedDateTime.of(2012, 10, 28, 1, 45, 0, 0, ASIA_ISTANBUL)},
            {"28 Oct 02:45:00 2012 Asia/Istanbul", ZonedDateTime.of(2012, 10, 28, 2, 45, 0, 0, ASIA_ISTANBUL)},
            {"28 Oct 03:45:00 2012 Asia/Istanbul", ZonedDateTime.of(2012, 10, 28, 3, 45, 0, 0, ASIA_ISTANBUL)},
            {"28 Oct 04:45:00 2012 Asia/Istanbul", ZonedDateTime.of(2012, 10, 28, 4, 45, 0, 0, ASIA_ISTANBUL)},
            {"28 Oct 05:45:00 2012 Asia/Istanbul", ZonedDateTime.of(2012, 10, 28, 5, 45, 0, 0, ASIA_ISTANBUL)}
        };
    }

    @Test(dataProvider="parseWithZoneWithoutOffset")
    public void testWithZoneWithoutOffset(String withZoneWithoutOffset, ZonedDateTime expectedZDT) {
        dtFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm:ss uuuu VV").withLocale(Locale.ENGLISH);
        zdt1 = ZonedDateTime.parse(withZoneWithoutOffset, dtFormatter);
        assertEquals(expectedZDT, zdt1);
    }

    @DataProvider(name="parseWithOffsetWithoutZone")
    Object[][] data_parse_WithOffset_WithoutZone() {
        return new Object[][] {
            {"2015-12-14T00:45:00-11:30", OffsetDateTime.of(2015, 12, 14, 0, 45, 0, 0, ZoneOffset.of("-11:30"))},
            {"2015-12-14T01:45:00-05:00", OffsetDateTime.of(2015, 12, 14, 1, 45, 0, 0, ZoneOffset.of("-05:00"))},
            {"2015-12-14T02:45:00-00:00", OffsetDateTime.of(2015, 12, 14, 2, 45, 0, 0, ZoneOffset.of("-00:00"))},
            {"2015-12-14T03:45:00+00:00", OffsetDateTime.of(2015, 12, 14, 3, 45, 0, 0, ZoneOffset.of("+00:00"))},
            {"2015-12-14T04:45:00+03:30", OffsetDateTime.of(2015, 12, 14, 4, 45, 0, 0, ZoneOffset.of("+03:30"))},
            {"2015-12-14T05:45:00+10:00", OffsetDateTime.of(2015, 12, 14, 5, 45, 0, 0, ZoneOffset.of("+10:00"))}
        };
    }

    @Test(dataProvider="parseWithOffsetWithoutZone")
    public void testWithOffsetWithoutZone(String odtString, OffsetDateTime expectedOTD) {
        dtFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        odt1 = OffsetDateTime.parse(odtString, dtFormatter);
        assertEquals(expectedOTD, odt1);
    }
}
