/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8248434
 * @modules jdk.localedata
 * @run testng/othervm CaseInsensitiveParseTest
 * @summary Checks format/parse round trip in case-insensitive manner.
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CaseInsensitiveParseTest {

    private final static String PATTERN = "GGGG/yyyy/MMMM/dddd/hhhh/mmmm/ss/aaaa";
    private final static Date EPOCH = new Date(0L);

    @DataProvider
    private Object[][] locales() {
        return (Object[][])Arrays.stream(DateFormat.getAvailableLocales())
            .map(Stream::of)
            .map(Stream::toArray)
            .toArray(Object[][]::new);
    }

    @Test(dataProvider = "locales")
    public void testUpperCase(Locale loc) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN, loc);
        String formatted = sdf.format(EPOCH);
        assertEquals(sdf.parse(formatted.toUpperCase(Locale.ROOT)), EPOCH,
                "roundtrip failed for string '" + formatted + "', locale: " + loc);
    }

    @Test(dataProvider = "locales")
    public void testLowerCase(Locale loc) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN, loc);
        String formatted = sdf.format(EPOCH);
        assertEquals(sdf.parse(formatted.toLowerCase(Locale.ROOT)), EPOCH,
                "roundtrip failed for string '" + formatted + "', locale: " + loc);
    }
}
