/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 2002 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 */

package sun.util.locale.provider;

import java.text.CompactNumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.spi.NumberFormatProvider;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Concrete implementation of the  {@link java.text.spi.NumberFormatProvider
 * NumberFormatProvider} class for the JRE LocaleProviderAdapter.
 *
 * @author Naoto Sato
 * @author Masayoshi Okutsu
 */
public class NumberFormatProviderImpl extends NumberFormatProvider implements AvailableLanguageTags {

    // Constants used by factory methods to specify a style of format.
    private static final int NUMBERSTYLE = 0;
    private static final int CURRENCYSTYLE = 1;
    private static final int PERCENTSTYLE = 2;
    private static final int ACCOUNTINGSTYLE = 3;
    private static final int INTEGERSTYLE = 4;

    private final LocaleProviderAdapter.Type type;
    private final Set<String> langtags;

    public NumberFormatProviderImpl(LocaleProviderAdapter.Type type, Set<String> langtags) {
        this.type = type;
        this.langtags = langtags;
    }

    /**
     * Returns an array of all locales for which this locale service provider
     * can provide localized objects or names.
     *
     * @return An array of all locales for which this locale service provider
     * can provide localized objects or names.
     */
    @Override
    public Locale[] getAvailableLocales() {
        return LocaleProviderAdapter.forType(type).getAvailableLocales();
    }

    @Override
    public boolean isSupportedLocale(Locale locale) {
        return LocaleProviderAdapter.forType(type).isSupportedProviderLocale(locale, langtags);
    }

    /**
     * Returns a new <code>NumberFormat</code> instance which formats
     * monetary values for the specified locale.
     *
     * @param locale the desired locale.
     * @exception NullPointerException if <code>locale</code> is null
     * @exception IllegalArgumentException if <code>locale</code> isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a currency formatter
     * @see java.text.NumberFormat#getCurrencyInstance(java.util.Locale)
     */
    @Override
    public NumberFormat getCurrencyInstance(Locale locale) {
        return getInstance(locale, CURRENCYSTYLE);
    }

    /**
     * Returns a new <code>NumberFormat</code> instance which formats
     * integer values for the specified locale.
     * The returned number format is configured to
     * round floating point numbers to the nearest integer using
     * half-even rounding (see {@link java.math.RoundingMode#HALF_EVEN HALF_EVEN})
     * for formatting, and to parse only the integer part of
     * an input string (see {@link
     * java.text.NumberFormat#isParseIntegerOnly isParseIntegerOnly}).
     *
     * @param locale the desired locale
     * @exception NullPointerException if <code>locale</code> is null
     * @exception IllegalArgumentException if <code>locale</code> isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a number format for integer values
     * @see java.text.NumberFormat#getIntegerInstance(java.util.Locale)
     */
    @Override
    public NumberFormat getIntegerInstance(Locale locale) {
        return getInstance(locale, INTEGERSTYLE);
    }

    /**
     * Returns a new general-purpose <code>NumberFormat</code> instance for
     * the specified locale.
     *
     * @param locale the desired locale
     * @exception NullPointerException if <code>locale</code> is null
     * @exception IllegalArgumentException if <code>locale</code> isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a general-purpose number formatter
     * @see java.text.NumberFormat#getNumberInstance(java.util.Locale)
     */
    @Override
    public NumberFormat getNumberInstance(Locale locale) {
        return getInstance(locale, NUMBERSTYLE);
    }

    /**
     * Returns a new <code>NumberFormat</code> instance which formats
     * percentage values for the specified locale.
     *
     * @param locale the desired locale
     * @exception NullPointerException if <code>locale</code> is null
     * @exception IllegalArgumentException if <code>locale</code> isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a percent formatter
     * @see java.text.NumberFormat#getPercentInstance(java.util.Locale)
     */
    @Override
    public NumberFormat getPercentInstance(Locale locale) {
        return getInstance(locale, PERCENTSTYLE);
    }

    private NumberFormat getInstance(Locale locale,
                                            int choice) {
        if (locale == null) {
            throw new NullPointerException();
        }

        // Check for region override
        Locale override = locale.getUnicodeLocaleType("nu") == null ?
            CalendarDataUtility.findRegionOverride(locale) :
            locale;

        LocaleProviderAdapter adapter = LocaleProviderAdapter.forType(type);
        String[] numberPatterns = adapter.getLocaleResources(override).getNumberPatterns();
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(override);
        int entry = (choice == INTEGERSTYLE) ? NUMBERSTYLE : choice;
        if (choice == CURRENCYSTYLE &&
            numberPatterns.length > ACCOUNTINGSTYLE &&
            !numberPatterns[ACCOUNTINGSTYLE].isEmpty() &&
            "account".equalsIgnoreCase(override.getUnicodeLocaleType("cf"))) {
            entry = ACCOUNTINGSTYLE;
        }
        DecimalFormat format = new DecimalFormat(numberPatterns[entry], symbols);

        if (choice == INTEGERSTYLE) {
            format.setMaximumFractionDigits(0);
            format.setDecimalSeparatorAlwaysShown(false);
            format.setParseIntegerOnly(true);
        } else if (choice == CURRENCYSTYLE) {
            adjustForCurrencyDefaultFractionDigits(format, symbols);
        }

        return format;
    }

    /**
     * Adjusts the minimum and maximum fraction digits to values that
     * are reasonable for the currency's default fraction digits.
     */
    private static void adjustForCurrencyDefaultFractionDigits(
            DecimalFormat format, DecimalFormatSymbols symbols) {
        Currency currency = symbols.getCurrency();
        if (currency == null) {
            try {
                currency = Currency.getInstance(symbols.getInternationalCurrencySymbol());
            } catch (IllegalArgumentException e) {
            }
        }
        if (currency != null) {
            int digits = currency.getDefaultFractionDigits();
            if (digits != -1) {
                int oldMinDigits = format.getMinimumFractionDigits();
                // Common patterns are "#.##", "#.00", "#".
                // Try to adjust all of them in a reasonable way.
                if (oldMinDigits == format.getMaximumFractionDigits()) {
                    format.setMinimumFractionDigits(digits);
                    format.setMaximumFractionDigits(digits);
                } else {
                    format.setMinimumFractionDigits(Math.min(digits, oldMinDigits));
                    format.setMaximumFractionDigits(digits);
                }
            }
        }
    }

    /**
     * Returns a new {@code NumberFormat} instance which formats
     * a number in its compact form for the specified
     * {@code locale} and {@code formatStyle}.
     *
     * @param locale the desired locale
     * @param formatStyle the style for formatting a number
     * @throws NullPointerException if {@code locale} or {@code formatStyle}
     *     is {@code null}
     * @throws IllegalArgumentException if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @return a compact number formatter
     *
     * @see java.text.NumberFormat#getCompactNumberInstance(Locale,
     *                      NumberFormat.Style)
     * @since 12
     */
    @Override
    public NumberFormat getCompactNumberInstance(Locale locale,
            NumberFormat.Style formatStyle) {

        Objects.requireNonNull(locale);
        Objects.requireNonNull(formatStyle);

        // Check for region override
        Locale override = locale.getUnicodeLocaleType("nu") == null
                ? CalendarDataUtility.findRegionOverride(locale)
                : locale;

        LocaleProviderAdapter adapter = LocaleProviderAdapter.forType(type);
        LocaleResources resource = adapter.getLocaleResources(override);

        String[] numberPatterns = resource.getNumberPatterns();
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(override);
        String[] cnPatterns = resource.getCNPatterns(formatStyle);

        // plural rules
        String[] rules = resource.getRules();

        return new CompactNumberFormat(numberPatterns[0],
                symbols, cnPatterns, rules[0]);
    }

    @Override
    public Set<String> getAvailableLanguageTags() {
        return langtags;
    }
}
