/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.spi.TimeZoneNameProvider;

/**
 * Concrete implementation of the
 * {@link java.util.spi.TimeZoneNameProvider TimeZoneNameProvider} class
 * for the JRE LocaleProviderAdapter.
 *
 * @author Naoto Sato
 * @author Masayoshi Okutsu
 */
public class TimeZoneNameProviderImpl extends TimeZoneNameProvider {
    private final LocaleProviderAdapter.Type type;
    private final Set<String> langtags;

    protected TimeZoneNameProviderImpl(LocaleProviderAdapter.Type type, Set<String> langtags) {
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
     * Returns a name for the given time zone ID that's suitable for
     * presentation to the user in the specified locale. The given time
     * zone ID is "GMT" or one of the names defined using "Zone" entries
     * in the "tz database", a public domain time zone database at
     * <a href="ftp://elsie.nci.nih.gov/pub/">ftp://elsie.nci.nih.gov/pub/</a>.
     * The data of this database is contained in a file whose name starts with
     * "tzdata", and the specification of the data format is part of the zic.8
     * man page, which is contained in a file whose name starts with "tzcode".
     * <p>
     * If <code>daylight</code> is true, the method should return a name
     * appropriate for daylight saving time even if the specified time zone
     * has not observed daylight saving time in the past.
     *
     * @param id a time zone ID string
     * @param daylight if true, return the daylight saving name.
     * @param style either {@link java.util.TimeZone#LONG TimeZone.LONG} or
     *    {@link java.util.TimeZone#SHORT TimeZone.SHORT}
     * @param locale the desired locale
     * @return the human-readable name of the given time zone in the
     *     given locale, or null if it's not available.
     * @exception IllegalArgumentException if <code>style</code> is invalid,
     *     or <code>locale</code> isn't one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @exception NullPointerException if <code>ID</code> or <code>locale</code>
     *     is null
     * @see java.util.TimeZone#getDisplayName(boolean, int, java.util.Locale)
     */
    @Override
    public String getDisplayName(String id, boolean daylight, int style, Locale locale) {
        String[] names = getDisplayNameArray(id, locale);
        if (Objects.nonNull(names)) {
            assert names.length >= 7;
            int index = daylight ? 3 : 1;
            if (style == TimeZone.SHORT) {
                index++;
            }
            return names[index];
        }
        return null;
    }

    @Override
    public String getGenericDisplayName(String id, int style, Locale locale) {
        String[] names = getDisplayNameArray(id, locale);
        if (Objects.nonNull(names)) {
            assert names.length >= 7;
            return names[(style == TimeZone.LONG) ? 5 : 6];
        }
        return null;
    }

    protected String[] getDisplayNameArray(String id, Locale locale) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(locale);

        return (String []) LocaleProviderAdapter.forType(type)
            .getLocaleResources(locale)
            .getTimeZoneNames(id);
    }

    /**
     * Returns a String[][] as the DateFormatSymbols.getZoneStrings() value for
     * the given locale.
     *
     * @param locale a Locale for time zone names
     * @return an array of time zone names arrays
     */
    protected String[][] getZoneStrings(Locale locale) {
        return LocaleProviderAdapter.forType(type).getLocaleResources(locale).getZoneStrings();
    }
}
