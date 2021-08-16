/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.text.spi;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * An abstract class for service providers that
 * provide concrete implementations of the
 * {@link java.text.NumberFormat NumberFormat} class.
 *
 * @since        1.6
 */
public abstract class NumberFormatProvider extends LocaleServiceProvider {

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected NumberFormatProvider() {
    }

    /**
     * Returns a new {@code NumberFormat} instance which formats
     * monetary values for the specified locale.
     *
     * @param locale the desired locale.
     * @throws    NullPointerException if {@code locale} is null
     * @throws    IllegalArgumentException if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a currency formatter
     * @see java.text.NumberFormat#getCurrencyInstance(java.util.Locale)
     */
    public abstract NumberFormat getCurrencyInstance(Locale locale);

    /**
     * Returns a new {@code NumberFormat} instance which formats
     * integer values for the specified locale.
     * The returned number format is configured to
     * round floating point numbers to the nearest integer using
     * half-even rounding (see {@link java.math.RoundingMode#HALF_EVEN HALF_EVEN})
     * for formatting, and to parse only the integer part of
     * an input string (see {@link
     * java.text.NumberFormat#isParseIntegerOnly isParseIntegerOnly}).
     *
     * @param locale the desired locale
     * @throws    NullPointerException if {@code locale} is null
     * @throws    IllegalArgumentException if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a number format for integer values
     * @see java.text.NumberFormat#getIntegerInstance(java.util.Locale)
     */
    public abstract NumberFormat getIntegerInstance(Locale locale);

    /**
     * Returns a new general-purpose {@code NumberFormat} instance for
     * the specified locale.
     *
     * @param locale the desired locale
     * @throws    NullPointerException if {@code locale} is null
     * @throws    IllegalArgumentException if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a general-purpose number formatter
     * @see java.text.NumberFormat#getNumberInstance(java.util.Locale)
     */
    public abstract NumberFormat getNumberInstance(Locale locale);

    /**
     * Returns a new {@code NumberFormat} instance which formats
     * percentage values for the specified locale.
     *
     * @param locale the desired locale
     * @throws    NullPointerException if {@code locale} is null
     * @throws    IllegalArgumentException if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a percent formatter
     * @see java.text.NumberFormat#getPercentInstance(java.util.Locale)
     */
    public abstract NumberFormat getPercentInstance(Locale locale);

    /**
     * Returns a new {@code NumberFormat} instance which formats
     * a number in its compact form for the specified
     * {@code locale} and {@code formatStyle}.
     *
     * @implSpec The default implementation of this method throws
     * {@link java.lang.UnsupportedOperationException
     * UnsupportedOperationException}. Overriding the implementation
     * of this method returns the compact number formatter instance
     * of the given {@code locale} with specified {@code formatStyle}.
     *
     * @param locale the desired locale
     * @param formatStyle the style for formatting a number
     * @throws NullPointerException if {@code locale} or {@code formatStyle}
     *     is {@code null}
     * @throws IllegalArgumentException if {@code locale} is not
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @throws UnsupportedOperationException if the implementation does not
     *      support this method
     * @return a compact number formatter
     *
     * @see java.text.NumberFormat#getCompactNumberInstance(Locale,
     *                      NumberFormat.Style)
     * @since 12
     */
    public NumberFormat getCompactNumberInstance(Locale locale,
            NumberFormat.Style formatStyle) {
        throw new UnsupportedOperationException(
                "The " + this.getClass().getName() + " should override this"
                + " method to return compact number format instance of "
                + locale + " locale and " + formatStyle + " style.");
    }

}
