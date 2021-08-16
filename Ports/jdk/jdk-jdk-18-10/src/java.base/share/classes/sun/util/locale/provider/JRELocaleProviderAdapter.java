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
import java.security.AccessControlException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.spi.BreakIteratorProvider;
import java.text.spi.CollatorProvider;
import java.text.spi.DateFormatProvider;
import java.text.spi.DateFormatSymbolsProvider;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.text.spi.NumberFormatProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.spi.CalendarDataProvider;
import java.util.spi.CalendarNameProvider;
import java.util.spi.CurrencyNameProvider;
import java.util.spi.LocaleNameProvider;
import java.util.spi.LocaleServiceProvider;
import java.util.spi.TimeZoneNameProvider;
import sun.text.spi.JavaTimeDateTimePatternProvider;
import sun.util.resources.LocaleData;
import sun.util.spi.CalendarProvider;

/**
 * LocaleProviderAdapter implementation for the legacy JRE locale data.
 *
 * @author Naoto Sato
 * @author Masayoshi Okutsu
 */
public class JRELocaleProviderAdapter extends LocaleProviderAdapter implements ResourceBundleBasedAdapter {

    private final ConcurrentMap<String, Set<String>> langtagSets
        = new ConcurrentHashMap<>();

    private final ConcurrentMap<Locale, LocaleResources> localeResourcesMap
        = new ConcurrentHashMap<>();

    // LocaleData specific to this LocaleProviderAdapter.
    private volatile LocaleData localeData;

    /**
     * Returns the type of this LocaleProviderAdapter
     */
    @Override
    public LocaleProviderAdapter.Type getAdapterType() {
        return Type.JRE;
    }

    /**
     * Getter method for Locale Service Providers
     */
    @Override
    @SuppressWarnings("unchecked")
    public <P extends LocaleServiceProvider> P getLocaleServiceProvider(Class<P> c) {
        switch (c.getSimpleName()) {
        case "BreakIteratorProvider":
            return (P) getBreakIteratorProvider();
        case "CollatorProvider":
            return (P) getCollatorProvider();
        case "DateFormatProvider":
            return (P) getDateFormatProvider();
        case "DateFormatSymbolsProvider":
            return (P) getDateFormatSymbolsProvider();
        case "DecimalFormatSymbolsProvider":
            return (P) getDecimalFormatSymbolsProvider();
        case "NumberFormatProvider":
            return (P) getNumberFormatProvider();
        case "CurrencyNameProvider":
            return (P) getCurrencyNameProvider();
        case "LocaleNameProvider":
            return (P) getLocaleNameProvider();
        case "TimeZoneNameProvider":
            return (P) getTimeZoneNameProvider();
        case "CalendarDataProvider":
            return (P) getCalendarDataProvider();
        case "CalendarNameProvider":
            return (P) getCalendarNameProvider();
        case "CalendarProvider":
            return (P) getCalendarProvider();
        case "JavaTimeDateTimePatternProvider":
            return (P) getJavaTimeDateTimePatternProvider();
        default:
            throw new InternalError("should not come down here");
        }
    }

    private volatile BreakIteratorProvider breakIteratorProvider;
    private volatile CollatorProvider collatorProvider;
    private volatile DateFormatProvider dateFormatProvider;
    private volatile DateFormatSymbolsProvider dateFormatSymbolsProvider;
    private volatile DecimalFormatSymbolsProvider decimalFormatSymbolsProvider;
    private volatile NumberFormatProvider numberFormatProvider;

    private volatile CurrencyNameProvider currencyNameProvider;
    private volatile LocaleNameProvider localeNameProvider;
    protected volatile TimeZoneNameProvider timeZoneNameProvider;
    protected volatile CalendarDataProvider calendarDataProvider;
    protected volatile CalendarNameProvider calendarNameProvider;

    private volatile CalendarProvider calendarProvider;
    private volatile JavaTimeDateTimePatternProvider javaTimeDateTimePatternProvider;

    /*
     * Getter methods for java.text.spi.* providers
     */
    @Override
    public BreakIteratorProvider getBreakIteratorProvider() {
        if (breakIteratorProvider == null) {
            @SuppressWarnings("removal")
            BreakIteratorProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<BreakIteratorProvider>) () ->
                    new BreakIteratorProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("FormatData")));

            synchronized (this) {
                if (breakIteratorProvider == null) {
                    breakIteratorProvider = provider;
                }
            }
        }
        return breakIteratorProvider;
    }

    @Override
    public CollatorProvider getCollatorProvider() {
        if (collatorProvider == null) {
            @SuppressWarnings("removal")
            CollatorProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<CollatorProvider>) () ->
                    new CollatorProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("CollationData")));

            synchronized (this) {
                if (collatorProvider == null) {
                    collatorProvider = provider;
                }
            }
        }
        return collatorProvider;
    }

    @Override
    public DateFormatProvider getDateFormatProvider() {
        if (dateFormatProvider == null) {
            @SuppressWarnings("removal")
            DateFormatProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<DateFormatProvider>) () ->
                    new DateFormatProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("FormatData")));

            synchronized (this) {
                if (dateFormatProvider == null) {
                    dateFormatProvider = provider;
                }
            }
        }
        return dateFormatProvider;
    }

    @Override
    public DateFormatSymbolsProvider getDateFormatSymbolsProvider() {
        if (dateFormatSymbolsProvider == null) {
            @SuppressWarnings("removal")
            DateFormatSymbolsProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<DateFormatSymbolsProvider>) () ->
                    new DateFormatSymbolsProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("FormatData")));

            synchronized (this) {
                if (dateFormatSymbolsProvider == null) {
                    dateFormatSymbolsProvider = provider;
                }
            }
        }
        return dateFormatSymbolsProvider;
    }

    @Override
    public DecimalFormatSymbolsProvider getDecimalFormatSymbolsProvider() {
        if (decimalFormatSymbolsProvider == null) {
            @SuppressWarnings("removal")
            DecimalFormatSymbolsProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<DecimalFormatSymbolsProvider>) () ->
                    new DecimalFormatSymbolsProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("FormatData")));

            synchronized (this) {
                if (decimalFormatSymbolsProvider == null) {
                    decimalFormatSymbolsProvider = provider;
                }
            }
        }
        return decimalFormatSymbolsProvider;
    }

    @Override
    public NumberFormatProvider getNumberFormatProvider() {
        if (numberFormatProvider == null) {
            @SuppressWarnings("removal")
            NumberFormatProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<NumberFormatProvider>) () ->
                    new NumberFormatProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("FormatData")));

            synchronized (this) {
                if (numberFormatProvider == null) {
                    numberFormatProvider = provider;
                }
            }
        }
        return numberFormatProvider;
    }

    /**
     * Getter methods for java.util.spi.* providers
     */
    @Override
    public CurrencyNameProvider getCurrencyNameProvider() {
        if (currencyNameProvider == null) {
            @SuppressWarnings("removal")
            CurrencyNameProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<CurrencyNameProvider>) () ->
                    new CurrencyNameProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("CurrencyNames")));

            synchronized (this) {
                if (currencyNameProvider == null) {
                    currencyNameProvider = provider;
                }
            }
        }
        return currencyNameProvider;
    }

    @Override
    public LocaleNameProvider getLocaleNameProvider() {
        if (localeNameProvider == null) {
            @SuppressWarnings("removal")
            LocaleNameProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<LocaleNameProvider>) () ->
                    new LocaleNameProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("LocaleNames")));

            synchronized (this) {
                if (localeNameProvider == null) {
                    localeNameProvider = provider;
                }
            }
        }
        return localeNameProvider;
    }

    @Override
    public TimeZoneNameProvider getTimeZoneNameProvider() {
        if (timeZoneNameProvider == null) {
            @SuppressWarnings("removal")
            TimeZoneNameProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<TimeZoneNameProvider>) () ->
                    new TimeZoneNameProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("TimeZoneNames")));

            synchronized (this) {
                if (timeZoneNameProvider == null) {
                    timeZoneNameProvider = provider;
                }
            }
        }
        return timeZoneNameProvider;
    }

    @Override
    public CalendarDataProvider getCalendarDataProvider() {
        if (calendarDataProvider == null) {
            @SuppressWarnings("removal")
            CalendarDataProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<CalendarDataProvider>) () ->
                    new CalendarDataProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("CalendarData")));

            synchronized (this) {
                if (calendarDataProvider == null) {
                    calendarDataProvider = provider;
                }
            }
        }
        return calendarDataProvider;
    }

    @Override
    public CalendarNameProvider getCalendarNameProvider() {
        if (calendarNameProvider == null) {
            @SuppressWarnings("removal")
            CalendarNameProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<CalendarNameProvider>) () ->
                    new CalendarNameProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("FormatData")));

            synchronized (this) {
                if (calendarNameProvider == null) {
                    calendarNameProvider = provider;
                }
            }
        }
        return calendarNameProvider;
    }

    /**
     * Getter methods for sun.util.spi.* providers
     */
    @Override
    public CalendarProvider getCalendarProvider() {
        if (calendarProvider == null) {
            @SuppressWarnings("removal")
            CalendarProvider provider = AccessController.doPrivileged(
                (PrivilegedAction<CalendarProvider>) () ->
                    new CalendarProviderImpl(
                        getAdapterType(),
                        getLanguageTagSet("CalendarData")));

            synchronized (this) {
                if (calendarProvider == null) {
                    calendarProvider = provider;
                }
            }
        }
        return calendarProvider;
    }

    /**
     * Getter methods for sun.text.spi.JavaTimeDateTimePatternProvider provider
     */
    @Override
    public JavaTimeDateTimePatternProvider getJavaTimeDateTimePatternProvider() {
        if (javaTimeDateTimePatternProvider == null) {
            @SuppressWarnings("removal")
            JavaTimeDateTimePatternProvider provider = AccessController.doPrivileged(
                    (PrivilegedAction<JavaTimeDateTimePatternProvider>) ()
                    -> new JavaTimeDateTimePatternImpl(
                            getAdapterType(),
                            getLanguageTagSet("FormatData")));

            synchronized (this) {
                if (javaTimeDateTimePatternProvider == null) {
                    javaTimeDateTimePatternProvider = provider;
                }
            }
        }
        return javaTimeDateTimePatternProvider;
    }

    @Override
    public LocaleResources getLocaleResources(Locale locale) {
        LocaleResources lr = localeResourcesMap.get(locale);
        if (lr == null) {
            lr = new LocaleResources(this, locale);
            LocaleResources lrc = localeResourcesMap.putIfAbsent(locale, lr);
            if (lrc != null) {
                lr = lrc;
            }
        }
        return lr;
    }

    // ResourceBundleBasedAdapter method implementation

    @Override
    public LocaleData getLocaleData() {
        if (localeData == null) {
            synchronized (this) {
                if (localeData == null) {
                    localeData = new LocaleData(getAdapterType());
                }
            }
        }
        return localeData;
    }

    @Override
    public List<Locale> getCandidateLocales(String baseName, Locale locale) {
        return ResourceBundle.Control
            .getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
            .getCandidateLocales(baseName, locale);
    }

    /**
     * Returns a list of the installed locales. Currently, this simply returns
     * the list of locales for which a sun.text.resources.FormatData bundle
     * exists. This bundle family happens to be the one with the broadest
     * locale coverage in the JRE.
     */
    @Override
    public Locale[] getAvailableLocales() {
        return AvailableJRELocales.localeList.clone();
    }

    public Set<String> getLanguageTagSet(String category) {
        Set<String> tagset = langtagSets.get(category);
        if (tagset == null) {
            tagset = createLanguageTagSet(category);
            Set<String> ts = langtagSets.putIfAbsent(category, tagset);
            if (ts != null) {
                tagset = ts;
            }
        }
        return tagset;
    }

    protected Set<String> createLanguageTagSet(String category) {
        String supportedLocaleString = createSupportedLocaleString(category);
        if (supportedLocaleString == null) {
            return Collections.emptySet();
        }
        StringTokenizer tokens = new StringTokenizer(supportedLocaleString);
        Set<String> tagset = new HashSet<>((tokens.countTokens() * 4 + 2) / 3);
        while (tokens.hasMoreTokens()) {
            tagset.add(tokens.nextToken());
        }

        return tagset;
    }

    private static String createSupportedLocaleString(String category) {
        // Directly call Base tags, as we know it's in the base module.
        String supportedLocaleString = BaseLocaleDataMetaInfo.getSupportedLocaleString(category);

        // Use ServiceLoader to dynamically acquire installed locales' tags.
        try {
            @SuppressWarnings("removal")
            String nonBaseTags = AccessController.doPrivileged((PrivilegedExceptionAction<String>) () -> {
                StringBuilder tags = new StringBuilder();
                for (LocaleDataMetaInfo ldmi :
                        ServiceLoader.loadInstalled(LocaleDataMetaInfo.class)) {
                    if (ldmi.getType() == LocaleProviderAdapter.Type.JRE) {
                        String t = ldmi.availableLanguageTags(category);
                        if (t != null) {
                            if (tags.length() > 0) {
                                tags.append(' ');
                            }
                            tags.append(t);
                        }
                    }
                }
                return tags.toString();
            });

            if (nonBaseTags != null) {
                supportedLocaleString += " " + nonBaseTags;
            }
        } catch (PrivilegedActionException pae) {
            throw new InternalError(pae.getCause());
        }

        return supportedLocaleString;
    }

    /**
     * Lazy load available locales.
     */
    private static class AvailableJRELocales {
        private static final Locale[] localeList = createAvailableLocales();
        private AvailableJRELocales() {}
    }

    private static Locale[] createAvailableLocales() {
        /*
         * Gets the locale string list from LocaleDataMetaInfo classes and then
         * contructs the Locale array and a set of language tags based on the
         * locale string returned above.
         */
        String supportedLocaleString = createSupportedLocaleString("AvailableLocales");

        if (supportedLocaleString.isEmpty()) {
            throw new InternalError("No available locales for JRE");
        }

        StringTokenizer localeStringTokenizer = new StringTokenizer(supportedLocaleString);

        int length = localeStringTokenizer.countTokens();
        Locale[] locales = new Locale[length + 1];
        locales[0] = Locale.ROOT;
        for (int i = 1; i <= length; i++) {
            String currentToken = localeStringTokenizer.nextToken();
            switch (currentToken) {
                case "ja-JP-JP":
                    locales[i] = JRELocaleConstants.JA_JP_JP;
                    break;
                case "no-NO-NY":
                    locales[i] = JRELocaleConstants.NO_NO_NY;
                    break;
                case "th-TH-TH":
                    locales[i] = JRELocaleConstants.TH_TH_TH;
                    break;
                default:
                    locales[i] = Locale.forLanguageTag(currentToken);
            }
        }
        return locales;
    }

    @Override
    public boolean isSupportedProviderLocale(Locale locale,  Set<String> langtags) {
        if (Locale.ROOT.equals(locale)) {
            return true;
        }

        locale = locale.stripExtensions();
        if (langtags.contains(locale.toLanguageTag())) {
            return true;
        }

        String oldname = locale.toString().replace('_', '-');
        return langtags.contains(oldname) ||
                   "ja-JP-JP".equals(oldname) ||
                   "th-TH-TH".equals(oldname) ||
                   "no-NO-NY".equals(oldname);
    }
}
