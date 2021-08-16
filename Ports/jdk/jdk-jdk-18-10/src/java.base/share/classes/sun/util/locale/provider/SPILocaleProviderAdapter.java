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
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.BreakIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.spi.BreakIteratorProvider;
import java.text.spi.CollatorProvider;
import java.text.spi.DateFormatProvider;
import java.text.spi.DateFormatSymbolsProvider;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.text.spi.NumberFormatProvider;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.spi.CalendarDataProvider;
import java.util.spi.CalendarNameProvider;
import java.util.spi.CurrencyNameProvider;
import java.util.spi.LocaleNameProvider;
import java.util.spi.LocaleServiceProvider;
import java.util.spi.TimeZoneNameProvider;

/**
 * LocaleProviderAdapter implementation for the installed SPI implementations.
 *
 * @author Naoto Sato
 * @author Masayoshi Okutsu
 */
public class SPILocaleProviderAdapter extends AuxLocaleProviderAdapter {

    /**
     * Returns the type of this LocaleProviderAdapter
     */
    @Override
    public LocaleProviderAdapter.Type getAdapterType() {
        return LocaleProviderAdapter.Type.SPI;
    }

    @SuppressWarnings("removal")
    @Override
    protected <P extends LocaleServiceProvider> P findInstalledProvider(final Class<P> c) {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                @Override
                @SuppressWarnings(value={"unchecked", "deprecation"})
                public P run() {
                    P delegate = null;

                    for (LocaleServiceProvider provider :
                             ServiceLoader.load(c, ClassLoader.getSystemClassLoader())) {
                        if (delegate == null) {
                            try {
                                delegate =
                                    (P) Class.forName(SPILocaleProviderAdapter.class.getCanonicalName() +
                                              "$" +
                                              c.getSimpleName() +
                                              "Delegate")
                                              .newInstance();
                            }  catch (ClassNotFoundException |
                                      InstantiationException |
                                      IllegalAccessException e) {
                                throw new ServiceConfigurationError(
                                    "SPI locale provider cannot be instantiated.", e);
                            }
                        }

                        ((Delegate)delegate).addImpl(provider);
                    }
                    return delegate;
                }
            });
        }  catch (PrivilegedActionException e) {
            throw new ServiceConfigurationError(
                "SPI locale provider cannot be instantiated.", e);
        }
    }

    /*
     * Delegate interface. All the implementations have to have the class name
     * following "<provider class name>Delegate" convention.
     */
    private interface Delegate<P extends LocaleServiceProvider> {
        default void addImpl(P impl) {
            for (Locale l : impl.getAvailableLocales()) {
                getDelegateMap().putIfAbsent(l, impl);
            }
        }

        /*
         * Obtain the real SPI implementation, using locale fallback
         */
        default P getImpl(Locale locale) {
            for (Locale l : LocaleServiceProviderPool.getLookupLocales(locale.stripExtensions())) {
                P ret = getDelegateMap().get(l);
                if (ret != null) {
                    return ret;
                }
            }
            return null;
        }

        Map<Locale, P> getDelegateMap();

        default Locale[] getAvailableLocalesDelegate() {
            return getDelegateMap().keySet().toArray(new Locale[0]);
        }

        default boolean isSupportedLocaleDelegate(Locale locale) {
            Map<Locale, P> map = getDelegateMap();
            Locale override = CalendarDataUtility.findRegionOverride(locale);

            // First, call the method with extensions (if any)
            P impl = map.get(override);
            if (impl != null) {
                return impl.isSupportedLocale(override);
            } else {
                // The default behavior
                Locale overrideNoExt = override.stripExtensions();
                impl = map.get(overrideNoExt);
                if (impl != null) {
                    return Arrays.stream(impl.getAvailableLocales())
                                .anyMatch(overrideNoExt::equals);
                }
            }

            return false;
        }
    }

    /*
     * Delegates for the actual SPI implementations.
     */
    static class BreakIteratorProviderDelegate extends BreakIteratorProvider
                                        implements Delegate<BreakIteratorProvider> {
        private final Map<Locale, BreakIteratorProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, BreakIteratorProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public BreakIterator getWordInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            BreakIteratorProvider bip = getImpl(locale);
            return bip.getWordInstance(locale);
        }

        @Override
        public BreakIterator getLineInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            BreakIteratorProvider bip = getImpl(locale);
            return bip.getLineInstance(locale);
        }

        @Override
        public BreakIterator getCharacterInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            BreakIteratorProvider bip = getImpl(locale);
            return bip.getCharacterInstance(locale);
        }

        @Override
        public BreakIterator getSentenceInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            BreakIteratorProvider bip = getImpl(locale);
            return bip.getSentenceInstance(locale);
        }

    }

    static class CollatorProviderDelegate extends CollatorProvider implements Delegate<CollatorProvider> {
        private final Map<Locale, CollatorProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, CollatorProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public Collator getInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            CollatorProvider cp = getImpl(locale);
            return cp.getInstance(locale);
        }
    }

    static class DateFormatProviderDelegate extends DateFormatProvider
                                     implements Delegate<DateFormatProvider> {
        private final Map<Locale, DateFormatProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, DateFormatProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public DateFormat getTimeInstance(int style, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            DateFormatProvider dfp = getImpl(locale);
            return dfp.getTimeInstance(style, locale);
        }

        @Override
        public DateFormat getDateInstance(int style, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            DateFormatProvider dfp = getImpl(locale);
            return dfp.getDateInstance(style, locale);
        }

        @Override
        public DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            DateFormatProvider dfp = getImpl(locale);
            return dfp.getDateTimeInstance(dateStyle, timeStyle, locale);
        }
    }

    static class DateFormatSymbolsProviderDelegate extends DateFormatSymbolsProvider
                                            implements Delegate<DateFormatSymbolsProvider> {
        private final Map<Locale, DateFormatSymbolsProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, DateFormatSymbolsProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public DateFormatSymbols getInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            DateFormatSymbolsProvider dfsp = getImpl(locale);
            return dfsp.getInstance(locale);
        }
    }

    static class DecimalFormatSymbolsProviderDelegate extends DecimalFormatSymbolsProvider
                                               implements Delegate<DecimalFormatSymbolsProvider> {
        private final Map<Locale, DecimalFormatSymbolsProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, DecimalFormatSymbolsProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public DecimalFormatSymbols getInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            DecimalFormatSymbolsProvider dfsp = getImpl(locale);
            return dfsp.getInstance(locale);
        }
    }

    static class NumberFormatProviderDelegate extends NumberFormatProvider
                                       implements Delegate<NumberFormatProvider> {
        private final Map<Locale, NumberFormatProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, NumberFormatProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public NumberFormat getCurrencyInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            NumberFormatProvider nfp = getImpl(locale);
            return nfp.getCurrencyInstance(locale);
        }

        @Override
        public NumberFormat getIntegerInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            NumberFormatProvider nfp = getImpl(locale);
            return nfp.getIntegerInstance(locale);
        }

        @Override
        public NumberFormat getNumberInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            NumberFormatProvider nfp = getImpl(locale);
            return nfp.getNumberInstance(locale);
        }

        @Override
        public NumberFormat getPercentInstance(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            NumberFormatProvider nfp = getImpl(locale);
            return nfp.getPercentInstance(locale);
        }

        @Override
        public NumberFormat getCompactNumberInstance(Locale locale,
                                NumberFormat.Style style) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            NumberFormatProvider nfp = getImpl(locale);
            return nfp.getCompactNumberInstance(locale, style);
        }
    }

    static class CalendarDataProviderDelegate extends CalendarDataProvider
                                       implements Delegate<CalendarDataProvider> {
        private final Map<Locale, CalendarDataProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, CalendarDataProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public int getFirstDayOfWeek(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            CalendarDataProvider cdp = getImpl(locale);
            return cdp.getFirstDayOfWeek(locale);
        }

        @Override
        public int getMinimalDaysInFirstWeek(Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            CalendarDataProvider cdp = getImpl(locale);
            return cdp.getMinimalDaysInFirstWeek(locale);
        }
    }

    static class CalendarNameProviderDelegate extends CalendarNameProvider
                                       implements Delegate<CalendarNameProvider> {
        private final Map<Locale, CalendarNameProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, CalendarNameProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public String getDisplayName(String calendarType,
                                              int field, int value,
                                              int style, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            CalendarNameProvider cdp = getImpl(locale);
            return cdp.getDisplayName(calendarType, field, value, style, locale);
        }

        @Override
        public Map<String, Integer> getDisplayNames(String calendarType,
                                                             int field, int style,
                                                             Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            CalendarNameProvider cdp = getImpl(locale);
            return cdp.getDisplayNames(calendarType, field, style, locale);
        }
    }

    static class CurrencyNameProviderDelegate extends CurrencyNameProvider
                                       implements Delegate<CurrencyNameProvider> {
        private final Map<Locale, CurrencyNameProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, CurrencyNameProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public String getSymbol(String currencyCode, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            CurrencyNameProvider cnp = getImpl(locale);
            return cnp.getSymbol(currencyCode, locale);
        }

        @Override
        public String getDisplayName(String currencyCode, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            CurrencyNameProvider cnp = getImpl(locale);
            return cnp.getDisplayName(currencyCode, locale);
        }
    }

    static class LocaleNameProviderDelegate extends LocaleNameProvider
                                     implements Delegate<LocaleNameProvider> {
        private final Map<Locale, LocaleNameProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, LocaleNameProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public String getDisplayLanguage(String languageCode, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            LocaleNameProvider lnp = getImpl(locale);
            return lnp.getDisplayLanguage(languageCode, locale);
        }

        @Override
        public String getDisplayScript(String scriptCode, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            LocaleNameProvider lnp = getImpl(locale);
            return lnp.getDisplayScript(scriptCode, locale);
        }

        @Override
        public String getDisplayCountry(String countryCode, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            LocaleNameProvider lnp = getImpl(locale);
            return lnp.getDisplayCountry(countryCode, locale);
        }

        @Override
        public String getDisplayVariant(String variant, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            LocaleNameProvider lnp = getImpl(locale);
            return lnp.getDisplayVariant(variant, locale);
        }

        @Override
        public String getDisplayUnicodeExtensionKey(String key, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            LocaleNameProvider lnp = getImpl(locale);
            return lnp.getDisplayUnicodeExtensionKey(key, locale);
        }

        @Override
        public String getDisplayUnicodeExtensionType(String extType, String key, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            LocaleNameProvider lnp = getImpl(locale);
            return lnp.getDisplayUnicodeExtensionType(extType, key, locale);
        }
    }

    static class TimeZoneNameProviderDelegate extends TimeZoneNameProvider
                                     implements Delegate<TimeZoneNameProvider> {
        private final Map<Locale, TimeZoneNameProvider> map = new ConcurrentHashMap<>();

        @Override
        public Map<Locale, TimeZoneNameProvider> getDelegateMap() {
            return map;
        }

        @Override
        public Locale[] getAvailableLocales() {
            return getAvailableLocalesDelegate();
        }

        @Override
        public boolean isSupportedLocale(Locale locale) {
            return isSupportedLocaleDelegate(locale);
        }

        @Override
        public String getDisplayName(String ID, boolean daylight, int style, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            TimeZoneNameProvider tznp = getImpl(locale);
            return tznp.getDisplayName(ID, daylight, style, locale);
        }

        @Override
        public String getGenericDisplayName(String ID, int style, Locale locale) {
            locale = CalendarDataUtility.findRegionOverride(locale);
            TimeZoneNameProvider tznp = getImpl(locale);
            return tznp.getGenericDisplayName(ID, style, locale);
        }
    }
}
