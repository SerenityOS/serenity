/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import java.util.Locale.Builder;

/**
 * TestUtils provides utility methods to get a locale-dependent attribute.
 * For example,
 *   - whether or not a non-Gregorian calendar is used
 *   - whether or not non-ASCII digits are used
 *
 * This class was developed to help testing for internationalization &
 * localization and is not versatile.
 */
public class TestUtils {

    /**
     * Returns true if the give locale uses Gregorian calendar.
     */
    public static boolean usesGregorianCalendar(Locale locale) {
        return Calendar.getInstance(locale).getClass() == GregorianCalendar.class;
    }

    /**
     * Returns true if the given locale uses ASCII digits.
     */
    public static boolean usesAsciiDigits(Locale locale) {
        return DecimalFormatSymbols.getInstance(locale).getZeroDigit() == '0';
    }

    /**
     * Returns true if the given locale has a special variant which is treated
     * as ill-formed in BCP 47.
     *
     * BCP 47 requires a variant subtag to be 5 to 8 alphanumerics or a
     * single numeric followed by 3 alphanumerics.
     * However, note that this methods doesn't check a variant so rigorously
     * because this is a utility method for testing. Intended special variants
     * are: JP, NY, and TH, which can be commonly provided by
     * Locale.getAvailableLocales().
     *
     */
    public static boolean hasSpecialVariant(Locale locale) {
        String variant = locale.getVariant();
        return !variant.isEmpty()
                   && "JP".equals(variant) || "NY".equals(variant) || "TH".equals(variant);
    }

}
