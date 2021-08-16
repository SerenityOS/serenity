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

package java.util.spi;

import java.util.Locale;
import java.util.Objects;

/**
 * An abstract class for service providers that
 * provide localized names for the
 * {@link java.util.Locale Locale} class.
 *
 * @since        1.6
 */
public abstract class LocaleNameProvider extends LocaleServiceProvider {

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected LocaleNameProvider() {
    }

    /**
     * Returns a localized name for the given <a href="http://www.rfc-editor.org/rfc/bcp/bcp47.txt">
     * IETF BCP47</a> language code and the given locale that is appropriate for
     * display to the user.
     * For example, if {@code languageCode} is "fr" and {@code locale}
     * is en_US, getDisplayLanguage() will return "French"; if {@code languageCode}
     * is "en" and {@code locale} is fr_FR, getDisplayLanguage() will return "anglais".
     * If the name returned cannot be localized according to {@code locale},
     * (say, the provider does not have a Japanese name for Croatian),
     * this method returns null.
     * @param languageCode the language code string in the form of two to eight
     *     lower-case letters between 'a' (U+0061) and 'z' (U+007A)
     * @param locale the desired locale
     * @return the name of the given language code for the specified locale, or null if it's not
     *     available.
     * @throws    NullPointerException if {@code languageCode} or {@code locale} is null
     * @throws    IllegalArgumentException if {@code languageCode} is not in the form of
     *     two or three lower-case letters, or {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @see java.util.Locale#getDisplayLanguage(java.util.Locale)
     */
    public abstract String getDisplayLanguage(String languageCode, Locale locale);

    /**
     * Returns a localized name for the given <a href="http://www.rfc-editor.org/rfc/bcp/bcp47.txt">
     * IETF BCP47</a> script code and the given locale that is appropriate for
     * display to the user.
     * For example, if {@code scriptCode} is "Latn" and {@code locale}
     * is en_US, getDisplayScript() will return "Latin"; if {@code scriptCode}
     * is "Cyrl" and {@code locale} is fr_FR, getDisplayScript() will return "cyrillique".
     * If the name returned cannot be localized according to {@code locale},
     * (say, the provider does not have a Japanese name for Cyrillic),
     * this method returns null. The default implementation returns null.
     * @param scriptCode the four letter script code string in the form of title-case
     *     letters (the first letter is upper-case character between 'A' (U+0041) and
     *     'Z' (U+005A) followed by three lower-case character between 'a' (U+0061)
     *     and 'z' (U+007A)).
     * @param locale the desired locale
     * @return the name of the given script code for the specified locale, or null if it's not
     *     available.
     * @throws    NullPointerException if {@code scriptCode} or {@code locale} is null
     * @throws    IllegalArgumentException if {@code scriptCode} is not in the form of
     *     four title case letters, or {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @see java.util.Locale#getDisplayScript(java.util.Locale)
     * @since 1.7
     */
    public String getDisplayScript(String scriptCode, Locale locale) {
        return null;
    }

    /**
     * Returns a localized name for the given <a href="http://www.rfc-editor.org/rfc/bcp/bcp47.txt">
     * IETF BCP47</a> region code (either ISO 3166 country code or UN M.49 area
     * codes) and the given locale that is appropriate for display to the user.
     * For example, if {@code countryCode} is "FR" and {@code locale}
     * is en_US, getDisplayCountry() will return "France"; if {@code countryCode}
     * is "US" and {@code locale} is fr_FR, getDisplayCountry() will return "Etats-Unis".
     * If the name returned cannot be localized according to {@code locale},
     * (say, the provider does not have a Japanese name for Croatia),
     * this method returns null.
     * @param countryCode the country(region) code string in the form of two
     *     upper-case letters between 'A' (U+0041) and 'Z' (U+005A) or the UN M.49 area code
     *     in the form of three digit letters between '0' (U+0030) and '9' (U+0039).
     * @param locale the desired locale
     * @return the name of the given country code for the specified locale, or null if it's not
     *     available.
     * @throws    NullPointerException if {@code countryCode} or {@code locale} is null
     * @throws    IllegalArgumentException if {@code countryCode} is not in the form of
     *     two upper-case letters or three digit letters, or {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @see java.util.Locale#getDisplayCountry(java.util.Locale)
     */
    public abstract String getDisplayCountry(String countryCode, Locale locale);

    /**
     * Returns a localized name for the given variant code and the given locale that
     * is appropriate for display to the user.
     * If the name returned cannot be localized according to {@code locale},
     * this method returns null.
     * @param variant the variant string
     * @param locale the desired locale
     * @return the name of the given variant string for the specified locale, or null if it's not
     *     available.
     * @throws    NullPointerException if {@code variant} or {@code locale} is null
     * @throws    IllegalArgumentException if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @see java.util.Locale#getDisplayVariant(java.util.Locale)
     */
    public abstract String getDisplayVariant(String variant, Locale locale);

    /**
     * Returns a localized name for the given
     * <a href="../Locale.html#def_locale_extension">Unicode extension</a> key,
     * and the given locale that is appropriate for display to the user.
     * If the name returned cannot be localized according to {@code locale},
     * this method returns null.
     * @implSpec the default implementation returns {@code null}.
     * @param key the Unicode Extension key, not null.
     * @param locale the desired locale, not null.
     * @return the name of the given key string for the specified locale,
     *  or null if it's not available.
     * @throws    NullPointerException if {@code key} or {@code locale} is null
     * @throws    IllegalArgumentException if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @since 10
     */
    public String getDisplayUnicodeExtensionKey(String key, Locale locale) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(locale);
        return null;
    }

    /**
     * Returns a localized name for the given
     * <a href="../Locale.html#def_locale_extension">Unicode extension</a> type,
     * and the given locale that is appropriate for display to the user.
     * If the name returned cannot be localized according to {@code locale},
     * this method returns null.
     * @implSpec the default implementation returns {@code null}.
     * @param type the Unicode Extension type, not null.
     * @param key the Unicode Extension key for this {@code type}, not null.
     * @param locale the desired locale, not null.
     * @return the name of the given type string for the specified locale,
     *  or null if it's not available.
     * @throws    NullPointerException if {@code key}, {@code type} or {@code locale} is null
     * @throws    IllegalArgumentException if {@code locale} isn't
     *     one of the locales returned from
     *     {@link java.util.spi.LocaleServiceProvider#getAvailableLocales()
     *     getAvailableLocales()}.
     * @since 10
     */
    public String getDisplayUnicodeExtensionType(String type, String key, Locale locale) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(key);
        Objects.requireNonNull(locale);
        return null;
    }
}
