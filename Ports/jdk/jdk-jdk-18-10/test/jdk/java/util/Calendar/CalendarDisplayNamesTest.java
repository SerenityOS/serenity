/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * @test
 * @bug 8262108
 * @summary Verify the results returned by Calendar.getDisplayNames() API
 * @comment Locale providers: COMPAT,SPI
 * @run testng/othervm -Djava.locale.providers=COMPAT,SPI CalendarDisplayNamesTest
 * @comment Locale providers: CLDR
 * @run testng/othervm -Djava.locale.providers=CLDR CalendarDisplayNamesTest
 */
public class CalendarDisplayNamesTest {

    /**
     * Test that the {@link Calendar#getDisplayNames(int, int, Locale)} returns valid field values
     * for the {@link Calendar#AM_PM} field in various locales and styles
     */
    @Test
    public void testAM_PMDisplayNameValues() {
        final int[] styles = new int[]{Calendar.ALL_STYLES, Calendar.SHORT_FORMAT, Calendar.LONG_FORMAT,
                Calendar.SHORT_STANDALONE, Calendar.LONG_STANDALONE, Calendar.SHORT, Calendar.LONG};
        for (final Locale locale : Locale.getAvailableLocales()) {
            for (final int style : styles) {
                final Calendar cal = Calendar.getInstance();
                final Map<String, Integer> names = cal.getDisplayNames(Calendar.AM_PM, style, locale);
                if (names == null) {
                    continue;
                }
                for (final Integer fieldValue : names.values()) {
                    Assert.assertTrue(fieldValue == Calendar.AM || fieldValue == Calendar.PM,
                            "Invalid field value " + fieldValue + " for calendar field AM_PM, in locale "
                                    + locale + " with style " + style);
                }
            }
        }
    }
}
