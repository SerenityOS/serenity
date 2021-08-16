/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.spi.BreakIteratorProvider;
import java.text.spi.CollatorProvider;
import java.text.spi.DateFormatProvider;
import java.text.spi.DateFormatSymbolsProvider;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.text.spi.NumberFormatProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.spi.CalendarDataProvider;
import java.util.spi.CalendarNameProvider;
import java.util.spi.CurrencyNameProvider;
import java.util.spi.LocaleNameProvider;
import java.util.spi.LocaleServiceProvider;
import java.util.spi.TimeZoneNameProvider;
import sun.text.spi.JavaTimeDateTimePatternProvider;
import sun.util.spi.CalendarProvider;

/**
 * An abstract parent class for the
 * HostLocaleProviderAdapter/SPILocaleProviderAdapter.
 *
 * @author Naoto Sato
 * @author Masayoshi Okutsu
 */
public abstract class AuxLocaleProviderAdapter extends LocaleProviderAdapter {
    /**
     * SPI implementations map
     */
    private final ConcurrentMap<Class<? extends LocaleServiceProvider>, LocaleServiceProvider> providersMap =
            new ConcurrentHashMap<>();

    /**
     * Getter method for Locale Service Providers
     */
    @Override
    public <P extends LocaleServiceProvider> P getLocaleServiceProvider(Class<P> c) {
        @SuppressWarnings("unchecked")
        P lsp = (P) providersMap.get(c);
        if (lsp == null) {
            lsp = findInstalledProvider(c);
            providersMap.putIfAbsent(c, lsp == null ? NULL_PROVIDER : lsp);
        }

        return lsp;
    }

    /**
     * Real body to find an implementation for each SPI.
     *
     * @param <P>
     * @param c
     * @return
     */
    protected abstract <P extends LocaleServiceProvider> P findInstalledProvider(final Class<P> c);

    @Override
    public BreakIteratorProvider getBreakIteratorProvider() {
        return getLocaleServiceProvider(BreakIteratorProvider.class);
    }

    @Override
    public CollatorProvider getCollatorProvider() {
        return getLocaleServiceProvider(CollatorProvider.class);
    }

    @Override
    public DateFormatProvider getDateFormatProvider() {
        return getLocaleServiceProvider(DateFormatProvider.class);
    }

    @Override
    public DateFormatSymbolsProvider getDateFormatSymbolsProvider() {
        return getLocaleServiceProvider(DateFormatSymbolsProvider.class);
    }

    @Override
    public DecimalFormatSymbolsProvider getDecimalFormatSymbolsProvider() {
        return getLocaleServiceProvider(DecimalFormatSymbolsProvider.class);
    }

    @Override
    public NumberFormatProvider getNumberFormatProvider() {
        return getLocaleServiceProvider(NumberFormatProvider.class);
    }

    /**
     * Getter methods for java.util.spi.* providers
     */
    @Override
    public CurrencyNameProvider getCurrencyNameProvider() {
        return getLocaleServiceProvider(CurrencyNameProvider.class);
    }

    @Override
    public LocaleNameProvider getLocaleNameProvider() {
        return getLocaleServiceProvider(LocaleNameProvider.class);
    }

    @Override
    public TimeZoneNameProvider getTimeZoneNameProvider() {
        return getLocaleServiceProvider(TimeZoneNameProvider.class);
    }

    @Override
    public CalendarDataProvider getCalendarDataProvider() {
        return getLocaleServiceProvider(CalendarDataProvider.class);
    }

    @Override
    public CalendarNameProvider getCalendarNameProvider() {
        return getLocaleServiceProvider(CalendarNameProvider.class);
    }

    /**
     * Getter methods for sun.util.spi.* providers
     */
    @Override
    public CalendarProvider getCalendarProvider() {
        return getLocaleServiceProvider(CalendarProvider.class);
    }

    @Override
    public LocaleResources getLocaleResources(Locale locale) {
        return null;
    }

    @Override
    public JavaTimeDateTimePatternProvider getJavaTimeDateTimePatternProvider() {
        return getLocaleServiceProvider(JavaTimeDateTimePatternProvider.class);
    }

    private static Locale[] availableLocales = null;

    @Override
    public Locale[] getAvailableLocales() {
        if (availableLocales == null) {
            List<Locale> avail = new ArrayList<>();
            for (Class<? extends LocaleServiceProvider> c :
                    LocaleServiceProviderPool.spiClasses) {
                LocaleServiceProvider lsp = getLocaleServiceProvider(c);
                if (lsp != null) {
                    avail.addAll(Arrays.asList(lsp.getAvailableLocales()));
                }
            }
            availableLocales = avail.toArray(new Locale[0]);
        }

        // assuming caller won't mutate the array.
        return availableLocales;
    }

    /**
     * A dummy locale service provider that indicates there is no
     * provider available
     */
    @SuppressWarnings("removal")
    private static final NullProvider NULL_PROVIDER = AccessController.doPrivileged(
        (PrivilegedAction<NullProvider>) () -> new NullProvider());

    private static class NullProvider extends LocaleServiceProvider {
        @Override
        public Locale[] getAvailableLocales() {
            return new Locale[0];
        }
    }
}
