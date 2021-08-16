/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.util.resources;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.spi.ResourceBundleProvider;
import sun.util.locale.provider.JRELocaleProviderAdapter;
import sun.util.locale.provider.LocaleProviderAdapter;
import static sun.util.locale.provider.LocaleProviderAdapter.Type.CLDR;
import static sun.util.locale.provider.LocaleProviderAdapter.Type.JRE;
import sun.util.locale.provider.ResourceBundleBasedAdapter;

/**
 * Provides information about and access to resource bundles in the
 * sun.text.resources and sun.util.resources packages or in their corresponding
 * packages for CLDR.
 *
 * @author Asmus Freytag
 * @author Mark Davis
 */

public class LocaleData {
    private static final ResourceBundle.Control defaultControl
        = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT);

    private static final String DOTCLDR      = ".cldr";

    // Map of key (base name + locale) to candidates
    private static final Map<String, List<Locale>> CANDIDATES_MAP = new ConcurrentHashMap<>();

    private final LocaleProviderAdapter.Type type;

    public LocaleData(LocaleProviderAdapter.Type type) {
        this.type = type;
    }

    /**
     * Gets a calendar data resource bundle, using privileges
     * to allow accessing a sun.* package.
     */
    public ResourceBundle getCalendarData(Locale locale) {
        return getBundle(type.getUtilResourcesPackage() + ".CalendarData", locale);
    }

    /**
     * Gets a currency names resource bundle, using privileges
     * to allow accessing a sun.* package.
     */
    public OpenListResourceBundle getCurrencyNames(Locale locale) {
        return (OpenListResourceBundle) getBundle(type.getUtilResourcesPackage() + ".CurrencyNames", locale);
    }

    /**
     * Gets a locale names resource bundle, using privileges
     * to allow accessing a sun.* package.
     */
    public OpenListResourceBundle getLocaleNames(Locale locale) {
        return (OpenListResourceBundle) getBundle(type.getUtilResourcesPackage() + ".LocaleNames", locale);
    }

    /**
     * Gets a time zone names resource bundle, using privileges
     * to allow accessing a sun.* package.
     */
    public TimeZoneNamesBundle getTimeZoneNames(Locale locale) {
        return (TimeZoneNamesBundle) getBundle(type.getUtilResourcesPackage() + ".TimeZoneNames", locale);
    }

    /**
     * Gets a break iterator info resource bundle, using privileges
     * to allow accessing a sun.* package.
     */
    public ResourceBundle getBreakIteratorInfo(Locale locale) {
        return getBundle(type.getTextResourcesPackage() + ".BreakIteratorInfo", locale);
    }

    /**
     * Gets a break iterator resources resource bundle, using
     * privileges to allow accessing a sun.* package.
     */
    public ResourceBundle getBreakIteratorResources(Locale locale) {
        return getBundle(type.getTextResourcesPackage() + ".BreakIteratorResources", locale);
    }

    /**
     * Gets a collation data resource bundle, using privileges
     * to allow accessing a sun.* package.
     */
    public ResourceBundle getCollationData(Locale locale) {
        return getBundle(type.getTextResourcesPackage() + ".CollationData", locale);
    }

    /**
     * Gets a date format data resource bundle, using privileges
     * to allow accessing a sun.* package.
     */
    public ResourceBundle getDateFormatData(Locale locale) {
        return getBundle(type.getTextResourcesPackage() + ".FormatData", locale);
    }

    public void setSupplementary(ParallelListResourceBundle formatData) {
        if (!formatData.areParallelContentsComplete()) {
            String suppName = type.getTextResourcesPackage() + ".JavaTimeSupplementary";
            setSupplementary(suppName, formatData);
        }
    }

    private boolean setSupplementary(String suppName, ParallelListResourceBundle formatData) {
        ParallelListResourceBundle parent = (ParallelListResourceBundle) formatData.getParent();
        boolean resetKeySet = false;
        if (parent != null) {
            resetKeySet = setSupplementary(suppName, parent);
        }
        OpenListResourceBundle supp = getSupplementary(suppName, formatData.getLocale());
        formatData.setParallelContents(supp);
        resetKeySet |= supp != null;
        // If any parents or this bundle has parallel data, reset keyset to create
        // a new keyset with the data.
        if (resetKeySet) {
            formatData.resetKeySet();
        }
        return resetKeySet;
    }

    /**
     * Gets a number format data resource bundle, using privileges
     * to allow accessing a sun.* package.
     */
    public ResourceBundle getNumberFormatData(Locale locale) {
        return getBundle(type.getTextResourcesPackage() + ".FormatData", locale);
    }

    @SuppressWarnings("removal")
    public static ResourceBundle getBundle(final String baseName, final Locale locale) {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public ResourceBundle run() {
                return Bundles.of(baseName, locale, LocaleDataStrategy.INSTANCE);
            }
        });
    }

    @SuppressWarnings("removal")
    private static OpenListResourceBundle getSupplementary(final String baseName, final Locale locale) {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
           @Override
           public OpenListResourceBundle run() {
               OpenListResourceBundle rb = null;
               try {
                   rb = (OpenListResourceBundle) Bundles.of(baseName, locale,
                                                            SupplementaryStrategy.INSTANCE);
               } catch (MissingResourceException e) {
                   // return null if no supplementary is available
               }
               return rb;
           }
        });
    }

    private static abstract class LocaleDataResourceBundleProvider
                                            implements ResourceBundleProvider {
        /**
         * Changes baseName to its module dependent package name and
         * calls the super class implementation. For example,
         * if the baseName is "sun.text.resources.FormatData" and locale is ja_JP,
         * the baseName is changed to "sun.text.resources.ext.FormatData". If
         * baseName contains ".cldr", such as "sun.text.resources.cldr.FormatData",
         * the name is changed to "sun.text.resources.cldr.ext.FormatData".
         */
        protected String toBundleName(String baseName, Locale locale) {
            return LocaleDataStrategy.INSTANCE.toBundleName(baseName, locale);
        }

        /**
         * Retrieves the other bundle name for legacy ISO 639 languages.
         */
        protected String toOtherBundleName(String baseName, String bundleName, Locale locale) {
            return Bundles.toOtherBundleName(baseName, bundleName, locale);
        }
    }

    /**
     * A ResourceBundleProvider implementation for loading locale data
     * resource bundles except for the java.time supplementary data.
     */
    public static abstract class CommonResourceBundleProvider extends LocaleDataResourceBundleProvider {
    }

    /**
     * A ResourceBundleProvider implementation for loading supplementary
     * resource bundles for java.time.
     */
    public static abstract class SupplementaryResourceBundleProvider extends LocaleDataResourceBundleProvider {
    }

    // Bundles.Strategy implementations

    private static class LocaleDataStrategy implements Bundles.Strategy {
        private static final LocaleDataStrategy INSTANCE = new LocaleDataStrategy();
        // TODO: avoid hard-coded Locales
        private static Set<Locale> JAVA_BASE_LOCALES
            = Set.of(Locale.ROOT, Locale.ENGLISH, Locale.US, new Locale("en", "US", "POSIX"));

        private LocaleDataStrategy() {
        }

        /*
         * This method overrides the default implementation to search
         * from a prebaked locale string list to determin the candidate
         * locale list.
         *
         * @param baseName the resource bundle base name.
         *        locale   the requested locale for the resource bundle.
         * @return a list of candidate locales to search from.
         * @exception NullPointerException if baseName or locale is null.
         */
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            String key = baseName + '-' + locale.toLanguageTag();
            List<Locale> candidates = CANDIDATES_MAP.get(key);
            if (candidates == null) {
                LocaleProviderAdapter.Type type = baseName.contains(DOTCLDR) ? CLDR : JRE;
                LocaleProviderAdapter adapter = LocaleProviderAdapter.forType(type);
                candidates = adapter instanceof ResourceBundleBasedAdapter ?
                    ((ResourceBundleBasedAdapter)adapter).getCandidateLocales(baseName, locale) :
                    defaultControl.getCandidateLocales(baseName, locale);

                // Weed out Locales which are known to have no resource bundles
                int lastDot = baseName.lastIndexOf('.');
                String category = (lastDot >= 0) ? baseName.substring(lastDot + 1) : baseName;
                Set<String> langtags = ((JRELocaleProviderAdapter)adapter).getLanguageTagSet(category);
                if (!langtags.isEmpty()) {
                    for (Iterator<Locale> itr = candidates.iterator(); itr.hasNext();) {
                        if (!adapter.isSupportedProviderLocale(itr.next(), langtags)) {
                            itr.remove();
                        }
                    }
                }
                CANDIDATES_MAP.putIfAbsent(key, candidates);
            }
            return candidates;
        }

        boolean inJavaBaseModule(String baseName, Locale locale) {
            return JAVA_BASE_LOCALES.contains(locale);
        }

        @Override
        public String toBundleName(String baseName, Locale locale) {
            String newBaseName = baseName;
            if (!inJavaBaseModule(baseName, locale)) {
                if (baseName.startsWith(JRE.getUtilResourcesPackage())
                        || baseName.startsWith(JRE.getTextResourcesPackage())) {
                    // Assume the lengths are the same.
                    assert JRE.getUtilResourcesPackage().length()
                        == JRE.getTextResourcesPackage().length();
                    int index = JRE.getUtilResourcesPackage().length();
                    if (baseName.indexOf(DOTCLDR, index) > 0) {
                        index += DOTCLDR.length();
                    }
                    newBaseName = baseName.substring(0, index + 1) + "ext"
                                      + baseName.substring(index);
                }
            }
            return defaultControl.toBundleName(newBaseName, locale);
        }

        @Override
        public Class<? extends ResourceBundleProvider> getResourceBundleProviderType(String baseName,
                                                                                     Locale locale) {
            return inJavaBaseModule(baseName, locale) ?
                        null : CommonResourceBundleProvider.class;
        }
    }

    private static class SupplementaryStrategy extends LocaleDataStrategy {
        private static final SupplementaryStrategy INSTANCE
                = new SupplementaryStrategy();
        // TODO: avoid hard-coded Locales
        private static Set<Locale> JAVA_BASE_LOCALES
            = Set.of(Locale.ROOT, Locale.ENGLISH, Locale.US);

        private SupplementaryStrategy() {
        }

        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            // Specifiy only the given locale
            return Arrays.asList(locale);
        }

        @Override
        public Class<? extends ResourceBundleProvider> getResourceBundleProviderType(String baseName,
                                                                                     Locale locale) {
            return inJavaBaseModule(baseName, locale) ?
                    null : SupplementaryResourceBundleProvider.class;
        }

        @Override
        boolean inJavaBaseModule(String baseName, Locale locale) {
            return JAVA_BASE_LOCALES.contains(locale);
        }
    }
}
