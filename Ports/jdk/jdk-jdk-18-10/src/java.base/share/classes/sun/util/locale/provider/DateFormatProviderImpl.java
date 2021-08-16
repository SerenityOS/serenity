/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.locale.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.spi.DateFormatProvider;
import java.util.Calendar;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TimeZone;

/**
 * Concrete implementation of the  {@link java.text.spi.DateFormatProvider
 * DateFormatProvider} class for the JRE LocaleProviderAdapter.
 *
 * @author Naoto Sato
 * @author Masayoshi Okutsu
 */
public class DateFormatProviderImpl extends DateFormatProvider implements AvailableLanguageTags {
    private final LocaleProviderAdapter.Type type;
    private final Set<String> langtags;

    public DateFormatProviderImpl(LocaleProviderAdapter.Type type, Set<String> langtags) {
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
        return LocaleProviderAdapter.toLocaleArray(langtags);
    }

    @Override
    public boolean isSupportedLocale(Locale locale) {
        return LocaleProviderAdapter.forType(type).isSupportedProviderLocale(locale, langtags);
    }

    /**
     * Returns a new <code>DateFormat</code> instance which formats time
     * with the given formatting style for the specified locale.
     * @param style the given formatting style.  Either one of
     *     {@link java.text.DateFormat#SHORT DateFormat.SHORT},
     *     {@link java.text.DateFormat#MEDIUM DateFormat.MEDIUM},
     *     {@link java.text.DateFormat#LONG DateFormat.LONG}, or
     *     {@link java.text.DateFormat#FULL DateFormat.FULL}.
     * @param locale the desired locale.
     * @exception IllegalArgumentException if <code>style</code> is invalid,
     *     or if <code>locale</code> isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @exception NullPointerException if <code>locale</code> is null
     * @return a time formatter.
     * @see java.text.DateFormat#getTimeInstance(int, java.util.Locale)
     */
    @Override
    public DateFormat getTimeInstance(int style, Locale locale) {
        return getInstance(-1, style, locale);
    }

    /**
     * Returns a new <code>DateFormat</code> instance which formats date
     * with the given formatting style for the specified locale.
     * @param style the given formatting style.  Either one of
     *     {@link java.text.DateFormat#SHORT DateFormat.SHORT},
     *     {@link java.text.DateFormat#MEDIUM DateFormat.MEDIUM},
     *     {@link java.text.DateFormat#LONG DateFormat.LONG}, or
     *     {@link java.text.DateFormat#FULL DateFormat.FULL}.
     * @param locale the desired locale.
     * @exception IllegalArgumentException if <code>style</code> is invalid,
     *     or if <code>locale</code> isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @exception NullPointerException if <code>locale</code> is null
     * @return a date formatter.
     * @see java.text.DateFormat#getDateInstance(int, java.util.Locale)
     */
    @Override
    public DateFormat getDateInstance(int style, Locale locale) {
        return getInstance(style, -1, locale);
    }

    /**
     * Returns a new <code>DateFormat</code> instance which formats date and time
     * with the given formatting style for the specified locale.
     * @param dateStyle the given date formatting style.  Either one of
     *     {@link java.text.DateFormat#SHORT DateFormat.SHORT},
     *     {@link java.text.DateFormat#MEDIUM DateFormat.MEDIUM},
     *     {@link java.text.DateFormat#LONG DateFormat.LONG}, or
     *     {@link java.text.DateFormat#FULL DateFormat.FULL}.
     * @param timeStyle the given time formatting style.  Either one of
     *     {@link java.text.DateFormat#SHORT DateFormat.SHORT},
     *     {@link java.text.DateFormat#MEDIUM DateFormat.MEDIUM},
     *     {@link java.text.DateFormat#LONG DateFormat.LONG}, or
     *     {@link java.text.DateFormat#FULL DateFormat.FULL}.
     * @param locale the desired locale.
     * @exception IllegalArgumentException if <code>dateStyle</code> or
     *     <code>timeStyle</code> is invalid,
     *     or if <code>locale</code> isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @exception NullPointerException if <code>locale</code> is null
     * @return a date/time formatter.
     * @see java.text.DateFormat#getDateTimeInstance(int, int, java.util.Locale)
     */
    @Override
    public DateFormat getDateTimeInstance(int dateStyle, int timeStyle,
                                          Locale locale) {
        return getInstance(dateStyle, timeStyle, locale);
    }

    private DateFormat getInstance(int dateStyle, int timeStyle, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }

        // Check for region override
        Locale rg = CalendarDataUtility.findRegionOverride(locale);

        SimpleDateFormat sdf = new SimpleDateFormat("", rg);
        Calendar cal = sdf.getCalendar();
        try {
            String pattern = LocaleProviderAdapter.forType(type)
                .getLocaleResources(rg).getDateTimePattern(timeStyle, dateStyle,
                                                               cal);
            sdf.applyPattern(pattern);
        } catch (MissingResourceException mre) {
            // Specify the fallback pattern
            sdf.applyPattern("M/d/yy h:mm a");
        }

        // Check for timezone override
        String tz = locale.getUnicodeLocaleType("tz");
        if (tz != null) {
            sdf.setTimeZone(
                TimeZoneNameUtility.convertLDMLShortID(tz)
                    .map(TimeZone::getTimeZone)
                    .orElseGet(sdf::getTimeZone));
        }

        return sdf;
    }

    @Override
    public Set<String> getAvailableLanguageTags() {
        return langtags;
    }
}
