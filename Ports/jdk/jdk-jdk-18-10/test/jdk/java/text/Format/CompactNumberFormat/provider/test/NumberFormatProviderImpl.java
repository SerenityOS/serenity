/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package test;

import java.text.CompactNumberFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.spi.NumberFormatProvider;
import java.util.Locale;

public class NumberFormatProviderImpl extends NumberFormatProvider {
    private static final Locale QAA = Locale.forLanguageTag("qaa");
    private static final Locale QAB = Locale.forLanguageTag("qab");
    private static final Locale[] locales = {QAA, QAB};

    private static final String[] oldPattern = {
        // old US short compact format
        "",
        "",
        "",
        "0K",
        "00K",
        "000K",
        "0M",
        "00M",
        "000M",
        "0B",
        "00B",
        "000B",
        "0T",
        "00T",
        "000T"
    };

    private static final String[] newPattern = {
        "",
        "",
        "",
        "{one:0K;(0K) two:0KK few:0KKK other:0KKKK}",
        "",
        "",
        "{one:0' 'M;(0' 'M) two:0' 'MM;(0' 'MM) few:0' 'MMM other:0' 'MMMM}"
    };

    @Override
    public NumberFormat getCurrencyInstance(Locale locale) {
        return null;
    }

    @Override
    public NumberFormat getIntegerInstance(Locale locale) {
        return null;
    }

    @Override
    public NumberFormat getNumberInstance(Locale locale) {
        return null;
    }

    @Override
    public NumberFormat getPercentInstance(Locale locale) {
        return null;
    }

    @Override
    public NumberFormat getCompactNumberInstance(Locale locale,
                            NumberFormat.Style style) {
        if (locale.equals(QAB)) {
            return new CompactNumberFormat(
                "#",
                DecimalFormatSymbols.getInstance(locale),
                newPattern,
                "one:v = 0 and i % 100 = 1;" +
                "two:v = 0 and i % 100 = 2;" +
                "few:v = 0 and i % 100 = 3..4 or v != 0;" +
                "other:");
        } else if (locale.equals(QAA)) {
            return new CompactNumberFormat(
                "#",
                DecimalFormatSymbols.getInstance(locale),
                oldPattern);
        } else {
            throw new RuntimeException("unsupported locale");
        }
    }

    @Override
    public Locale[] getAvailableLocales() {
        return locales;
    }
}
