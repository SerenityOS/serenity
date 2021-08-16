/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.bar;

import com.foobar.Utils;
import java.util.Arrays;
import static java.util.Calendar.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.spi.CalendarNameProvider;

public class CalendarNameProviderImpl extends CalendarNameProvider {
    static final char FULLWIDTH_ZERO = '\uff10';
    static final Locale[] avail = {
        new Locale("ja", "JP", "kids"),
    };

    @Override
    public String getDisplayName(String calendarType, int field, int value, int style, Locale locale) {
        if (calendarType == null || locale == null) {
            throw new NullPointerException();
        }
        if (!Utils.supportsLocale(Arrays.asList(avail), locale)) {
            throw new IllegalArgumentException("locale is not one of available locales: "+ locale);
        }
        if (field != MONTH) {
            return null;
        }
        return toMonthName(value + 1, style);
    }

    @Override
    public Map<String, Integer> getDisplayNames(String calendarType, int field, int style, Locale locale) {
        if (calendarType == null || locale == null) {
            throw new NullPointerException();
        }
        if (!Utils.supportsLocale(Arrays.asList(avail), locale)) {
            throw new IllegalArgumentException("locale is not one of available locales: " + locale);
        }
        if (field != MONTH) {
            return null;
        }
        Map<String, Integer> map = new HashMap<>();
        if (style == LONG_STANDALONE) {
            style = LONG;
        } else if (style == SHORT_STANDALONE) {
            style = SHORT;
        }
        for (int month = JANUARY; month <= DECEMBER; month++) {
            if (style == ALL_STYLES || style == LONG) {
                map.put(toMonthName(month + 1, LONG), month);
            }
            if (style == ALL_STYLES || style == SHORT) {
                map.put(toMonthName(month + 1, SHORT), month);
            }
        }
        return map;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return avail.clone();
    }

    // month is 1-based.
    public static String toMonthName(int month, int style) {
        StringBuilder sb = new StringBuilder();
        if (month >= 10) {
            sb.append((char)(FULLWIDTH_ZERO + 1));
            sb.appendCodePoint((char)(FULLWIDTH_ZERO + (month % 10)));
        } else {
            sb.appendCodePoint((char)(FULLWIDTH_ZERO + month));
        }
        if (style == SHORT || style == SHORT_STANDALONE) {
            return sb.toString(); // full-width digit(s)
        }
        sb.append("\u304c\u3064"); // + "gatsu" in Hiragana
        return sb.toString();
    }
}
