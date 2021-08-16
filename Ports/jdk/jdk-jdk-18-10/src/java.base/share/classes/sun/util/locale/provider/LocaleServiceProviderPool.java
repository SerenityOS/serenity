/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Builder;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.spi.LocaleServiceProvider;

/**
 * An instance of this class holds a set of the third party implementations of a particular
 * locale sensitive service, such as {@link java.util.spi.LocaleNameProvider}.
 *
 * @author Naoto Sato
 * @author Masayoshi Okutsu
 */
public final class LocaleServiceProviderPool {

    /**
     * A Map that holds singleton instances of this class.  Each instance holds a
     * set of provider implementations of a particular locale sensitive service.
     */
    private static final ConcurrentMap<Class<? extends LocaleServiceProvider>, LocaleServiceProviderPool> poolOfPools =
        new ConcurrentHashMap<>();

    /**
     * A Map that retains Locale->provider mapping
     */
    private final ConcurrentMap<Locale, List<LocaleServiceProvider>> providersCache =
        new ConcurrentHashMap<>();

    /**
     * Available locales for this locale sensitive service.  This also contains
     * JRE's available locales
     */
    private Set<Locale> availableLocales = null;

    /**
     * Provider class
     */
    private final Class<? extends LocaleServiceProvider> providerClass;

    /**
     * Array of all Locale Sensitive SPI classes.
     *
     * We know "spiClasses" contains classes that extends LocaleServiceProvider,
     * but generic array creation is not allowed, thus the "unchecked" warning
     * is suppressed here.
     */
    @SuppressWarnings("unchecked")
    static final Class<LocaleServiceProvider>[] spiClasses =
                (Class<LocaleServiceProvider>[]) new Class<?>[] {
        java.text.spi.BreakIteratorProvider.class,
        java.text.spi.CollatorProvider.class,
        java.text.spi.DateFormatProvider.class,
        java.text.spi.DateFormatSymbolsProvider.class,
        java.text.spi.DecimalFormatSymbolsProvider.class,
        java.text.spi.NumberFormatProvider.class,
        java.util.spi.CurrencyNameProvider.class,
        java.util.spi.LocaleNameProvider.class,
        java.util.spi.TimeZoneNameProvider.class,
        java.util.spi.CalendarDataProvider.class
    };

    /**
     * A factory method that returns a singleton instance
     */
    public static LocaleServiceProviderPool getPool(Class<? extends LocaleServiceProvider> providerClass) {
        LocaleServiceProviderPool pool = poolOfPools.get(providerClass);
        if (pool == null) {
            LocaleServiceProviderPool newPool =
                new LocaleServiceProviderPool(providerClass);
            pool = poolOfPools.putIfAbsent(providerClass, newPool);
            if (pool == null) {
                pool = newPool;
            }
        }

        return pool;
    }

    /**
     * The sole constructor.
     *
     * @param c class of the locale sensitive service
     */
    private LocaleServiceProviderPool (final Class<? extends LocaleServiceProvider> c) {
        providerClass = c;
    }

    /**
     * Lazy loaded set of available locales.
     * Loading all locales is a very long operation.
     */
    private static class AllAvailableLocales {
        /**
         * Available locales for all locale sensitive services.
         * This also contains JRE's available locales
         */
        static final Locale[] allAvailableLocales;

        static {
            Set<Locale> all = new HashSet<>();
            for (Class<? extends LocaleServiceProvider> c : spiClasses) {
                LocaleServiceProviderPool pool =
                    LocaleServiceProviderPool.getPool(c);
                all.addAll(pool.getAvailableLocaleSet());
            }

            allAvailableLocales = all.toArray(new Locale[0]);
        }

        // No instantiation
        private AllAvailableLocales() {
        }
    }

    /**
     * Returns an array of available locales for all the provider classes.
     * This array is a merged array of all the locales that are provided by each
     * provider, including the JRE.
     *
     * @return an array of the available locales for all provider classes
     */
    public static Locale[] getAllAvailableLocales() {
        return AllAvailableLocales.allAvailableLocales.clone();
    }

    /**
     * Returns an array of available locales.  This array is a
     * merged array of all the locales that are provided by each
     * provider, including the JRE.
     *
     * @return an array of the available locales
     */
    public Locale[] getAvailableLocales() {
        Set<Locale> locList = new HashSet<>();
        locList.addAll(getAvailableLocaleSet());
        // Make sure it all contains JRE's locales for compatibility.
        locList.addAll(Arrays.asList(LocaleProviderAdapter.forJRE().getAvailableLocales()));
        Locale[] tmp = new Locale[locList.size()];
        locList.toArray(tmp);
        return tmp;
    }

    /**
     * Returns the union of locale sets that are available from
     * each service provider. This method does NOT return the
     * defensive copy.
     *
     * @return a set of available locales
     */
    private synchronized Set<Locale> getAvailableLocaleSet() {
        if (availableLocales == null) {
            availableLocales = new HashSet<>();
            for (LocaleProviderAdapter.Type type : LocaleProviderAdapter.getAdapterPreference()) {
                LocaleProviderAdapter lda = LocaleProviderAdapter.forType(type);
                if (lda != null) {
                    LocaleServiceProvider lsp = lda.getLocaleServiceProvider(providerClass);
                    if (lsp != null) {
                        Locale[] locales = lsp.getAvailableLocales();
                        for (Locale locale: locales) {
                            availableLocales.add(getLookupLocale(locale));
                        }
                    }
                }
            }
        }

        return availableLocales;
    }

    /**
     * Returns the provider's localized object for the specified
     * locale.
     *
     * @param getter an object on which getObject() method
     *     is called to obtain the provider's instance.
     * @param locale the given locale that is used as the starting one
     * @param params provider specific parameters
     * @return provider's instance, or null.
     */
    public <P extends LocaleServiceProvider, S> S getLocalizedObject(LocalizedObjectGetter<P, S> getter,
                                     Locale locale,
                                     Object... params) {
        return getLocalizedObjectImpl(getter, locale, true, null, params);
    }

    /**
     * Returns the provider's localized name for the specified
     * locale.
     *
     * @param getter an object on which getObject() method
     *     is called to obtain the provider's instance.
     * @param locale the given locale that is used as the starting one
     * @param key the key string for name providers
     * @param params provider specific parameters
     * @return provider's instance, or null.
     */
    public <P extends LocaleServiceProvider, S> S getLocalizedObject(LocalizedObjectGetter<P, S> getter,
                                     Locale locale,
                                     String key,
                                     Object... params) {
        return getLocalizedObjectImpl(getter, locale, false, key, params);
    }

    /**
     * Returns the provider's localized name for the specified
     * locale.
     *
     * @param getter an object on which getObject() method
     *     is called to obtain the provider's instance.
     * @param locale the given locale that is used as the starting one
     * @param isObjectProvider flag designating object provder or not
     * @param key the key string for name providers
     * @param params provider specific parameters
     * @return provider's instance, or null.
     */
    public <P extends LocaleServiceProvider, S> S getLocalizedObject(LocalizedObjectGetter<P, S> getter,
                                     Locale locale,
                                     Boolean isObjectProvider,
                                     String key,
                                     Object... params) {
        return getLocalizedObjectImpl(getter, locale, isObjectProvider, key, params);
    }

    @SuppressWarnings("unchecked")
    private <P extends LocaleServiceProvider, S> S getLocalizedObjectImpl(LocalizedObjectGetter<P, S> getter,
                                     Locale locale,
                                     boolean isObjectProvider,
                                     String key,
                                     Object... params) {
        if (locale == null) {
            throw new NullPointerException();
        }

        List<Locale> lookupLocales = getLookupLocales(locale);

        for (Locale current : lookupLocales) {
            S providersObj;

            for (LocaleServiceProvider lsp: findProviders(current, isObjectProvider)) {
                providersObj = getter.getObject((P)lsp, locale, key, params);
                if (providersObj != null) {
                    return providersObj;
                } else if (isObjectProvider) {
                    System.getLogger(LocaleServiceProviderPool.class.getCanonicalName())
                            .log(System.Logger.Level.INFO,
                           "A locale sensitive service object provider returned null, " +
                                "which should not happen. Provider: " + lsp + " Locale: " + locale);
                }
            }
        }

        // not found.
        return null;
    }

    /**
     * Returns the list of locale service provider instances that support
     * the specified locale.
     *
     * @param locale the given locale
     * @return the list of locale data adapter types
     */
    private List<LocaleServiceProvider> findProviders(Locale locale, boolean isObjectProvider) {
        List<LocaleServiceProvider> providersList = providersCache.get(locale);
        if (providersList == null) {
            for (LocaleProviderAdapter.Type type : LocaleProviderAdapter.getAdapterPreference()) {
                LocaleProviderAdapter lda = LocaleProviderAdapter.forType(type);
                if (lda != null) {
                    LocaleServiceProvider lsp = lda.getLocaleServiceProvider(providerClass);
                    if (lsp != null) {
                        if (lsp.isSupportedLocale(locale)) {
                            if (providersList == null) {
                                providersList = new ArrayList<>(2);
                            }
                            providersList.add(lsp);
                            if (isObjectProvider) {
                                break;
                            }
                        }
                    }
                }
            }
            if (providersList == null) {
                providersList = NULL_LIST;
            }
            List<LocaleServiceProvider> val = providersCache.putIfAbsent(locale, providersList);
            if (val != null) {
                providersList = val;
            }
        }
        return providersList;
    }

    /**
     * Returns a list of candidate locales for service look up.
     * @param locale the input locale
     * @return the list of candidate locales for the given locale
     */
    static List<Locale> getLookupLocales(Locale locale) {
        // Note: We currently use the default implementation of
        // ResourceBundle.Control.getCandidateLocales. The result
        // returned by getCandidateLocales are already normalized
        // (no extensions) for service look up.
        return Control.getNoFallbackControl(Control.FORMAT_DEFAULT)
                                            .getCandidateLocales("", locale);
    }

    /**
     * Returns an instance of Locale used for service look up.
     * The result Locale has no extensions except for ja_JP_JP
     * and th_TH_TH
     *
     * @param locale the locale
     * @return the locale used for service look up
     */
    static Locale getLookupLocale(Locale locale) {
        Locale lookupLocale = locale;
        if (locale.hasExtensions()
                && !locale.equals(JRELocaleConstants.JA_JP_JP)
                && !locale.equals(JRELocaleConstants.TH_TH_TH)) {
            // remove extensions
            Builder locbld = new Builder();
            try {
                locbld.setLocale(locale);
                locbld.clearExtensions();
                lookupLocale = locbld.build();
            } catch (IllformedLocaleException e) {
                // A Locale with non-empty extensions
                // should have well-formed fields except
                // for ja_JP_JP and th_TH_TH. Therefore,
                // it should never enter in this catch clause.
                System.getLogger(LocaleServiceProviderPool.class.getCanonicalName())
                    .log(System.Logger.Level.INFO,
                        "A locale(" + locale + ") has non-empty extensions, but has illformed fields.");

                // Fallback - script field will be lost.
                lookupLocale = new Locale(locale.getLanguage(), locale.getCountry(), locale.getVariant());
            }
        }
        return lookupLocale;
    }

    /**
     * A dummy locale service provider list that indicates there is no
     * provider available
     */
    private static final List<LocaleServiceProvider> NULL_LIST =
        Collections.emptyList();

    /**
     * An interface to get a localized object for each locale sensitive
     * service class.
     */
    public interface LocalizedObjectGetter<P extends LocaleServiceProvider, S> {
        /**
         * Returns an object from the provider
         *
         * @param lsp the provider
         * @param locale the locale
         * @param key key string to localize, or null if the provider is not
         *     a name provider
         * @param params provider specific params
         * @return localized object from the provider
         */
        S getObject(P lsp,
                    Locale locale,
                    String key,
                    Object... params);
    }
}
