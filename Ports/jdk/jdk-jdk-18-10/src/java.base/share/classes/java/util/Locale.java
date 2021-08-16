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

package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.spi.LocaleNameProvider;
import java.util.stream.Collectors;

import sun.security.action.GetPropertyAction;
import sun.util.locale.BaseLocale;
import sun.util.locale.InternalLocaleBuilder;
import sun.util.locale.LanguageTag;
import sun.util.locale.LocaleExtensions;
import sun.util.locale.LocaleMatcher;
import sun.util.locale.LocaleObjectCache;
import sun.util.locale.LocaleSyntaxException;
import sun.util.locale.LocaleUtils;
import sun.util.locale.ParseStatus;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;
import sun.util.locale.provider.LocaleServiceProviderPool;
import sun.util.locale.provider.TimeZoneNameUtility;

/**
 * A {@code Locale} object represents a specific geographical, political,
 * or cultural region. An operation that requires a {@code Locale} to perform
 * its task is called <em>locale-sensitive</em> and uses the {@code Locale}
 * to tailor information for the user. For example, displaying a number
 * is a locale-sensitive operation&mdash; the number should be formatted
 * according to the customs and conventions of the user's native country,
 * region, or culture.
 *
 * <p> The {@code Locale} class implements IETF BCP 47 which is composed of
 * <a href="http://tools.ietf.org/html/rfc4647">RFC 4647 "Matching of Language
 * Tags"</a> and <a href="http://tools.ietf.org/html/rfc5646">RFC 5646 "Tags
 * for Identifying Languages"</a> with support for the LDML (UTS#35, "Unicode
 * Locale Data Markup Language") BCP 47-compatible extensions for locale data
 * exchange.
 *
 * <p> A {@code Locale} object logically consists of the fields
 * described below.
 *
 * <dl>
 *   <dt><a id="def_language"><b>language</b></a></dt>
 *
 *   <dd>ISO 639 alpha-2 or alpha-3 language code, or registered
 *   language subtags up to 8 alpha letters (for future enhancements).
 *   When a language has both an alpha-2 code and an alpha-3 code, the
 *   alpha-2 code must be used.  You can find a full list of valid
 *   language codes in the IANA Language Subtag Registry (search for
 *   "Type: language").  The language field is case insensitive, but
 *   {@code Locale} always canonicalizes to lower case.</dd>
 *
 *   <dd>Well-formed language values have the form
 *   <code>[a-zA-Z]{2,8}</code>.  Note that this is not the full
 *   BCP47 language production, since it excludes extlang.  They are
 *   not needed since modern three-letter language codes replace
 *   them.</dd>
 *
 *   <dd>Example: "en" (English), "ja" (Japanese), "kok" (Konkani)</dd>
 *
 *   <dt><a id="def_script"><b>script</b></a></dt>
 *
 *   <dd>ISO 15924 alpha-4 script code.  You can find a full list of
 *   valid script codes in the IANA Language Subtag Registry (search
 *   for "Type: script").  The script field is case insensitive, but
 *   {@code Locale} always canonicalizes to title case (the first
 *   letter is upper case and the rest of the letters are lower
 *   case).</dd>
 *
 *   <dd>Well-formed script values have the form
 *   <code>[a-zA-Z]{4}</code></dd>
 *
 *   <dd>Example: "Latn" (Latin), "Cyrl" (Cyrillic)</dd>
 *
 *   <dt><a id="def_region"><b>country (region)</b></a></dt>
 *
 *   <dd>ISO 3166 alpha-2 country code or UN M.49 numeric-3 area code.
 *   You can find a full list of valid country and region codes in the
 *   IANA Language Subtag Registry (search for "Type: region").  The
 *   country (region) field is case insensitive, but
 *   {@code Locale} always canonicalizes to upper case.</dd>
 *
 *   <dd>Well-formed country/region values have
 *   the form <code>[a-zA-Z]{2} | [0-9]{3}</code></dd>
 *
 *   <dd>Example: "US" (United States), "FR" (France), "029"
 *   (Caribbean)</dd>
 *
 *   <dt><a id="def_variant"><b>variant</b></a></dt>
 *
 *   <dd>Any arbitrary value used to indicate a variation of a
 *   {@code Locale}.  Where there are two or more variant values
 *   each indicating its own semantics, these values should be ordered
 *   by importance, with most important first, separated by
 *   underscore('_').  The variant field is case sensitive.</dd>
 *
 *   <dd>Note: IETF BCP 47 places syntactic restrictions on variant
 *   subtags.  Also BCP 47 subtags are strictly used to indicate
 *   additional variations that define a language or its dialects that
 *   are not covered by any combinations of language, script and
 *   region subtags.  You can find a full list of valid variant codes
 *   in the IANA Language Subtag Registry (search for "Type: variant").
 *
 *   <p>However, the variant field in {@code Locale} has
 *   historically been used for any kind of variation, not just
 *   language variations.  For example, some supported variants
 *   available in Java SE Runtime Environments indicate alternative
 *   cultural behaviors such as calendar type or number script.  In
 *   BCP 47 this kind of information, which does not identify the
 *   language, is supported by extension subtags or private use
 *   subtags.</dd>
 *
 *   <dd>Well-formed variant values have the form <code>SUBTAG
 *   (('_'|'-') SUBTAG)*</code> where <code>SUBTAG =
 *   [0-9][0-9a-zA-Z]{3} | [0-9a-zA-Z]{5,8}</code>. (Note: BCP 47 only
 *   uses hyphen ('-') as a delimiter, this is more lenient).</dd>
 *
 *   <dd>Example: "polyton" (Polytonic Greek), "POSIX"</dd>
 *
 *   <dt><a id="def_extensions"><b>extensions</b></a></dt>
 *
 *   <dd>A map from single character keys to string values, indicating
 *   extensions apart from language identification.  The extensions in
 *   {@code Locale} implement the semantics and syntax of BCP 47
 *   extension subtags and private use subtags. The extensions are
 *   case insensitive, but {@code Locale} canonicalizes all
 *   extension keys and values to lower case. Note that extensions
 *   cannot have empty values.</dd>
 *
 *   <dd>Well-formed keys are single characters from the set
 *   {@code [0-9a-zA-Z]}.  Well-formed values have the form
 *   {@code SUBTAG ('-' SUBTAG)*} where for the key 'x'
 *   <code>SUBTAG = [0-9a-zA-Z]{1,8}</code> and for other keys
 *   <code>SUBTAG = [0-9a-zA-Z]{2,8}</code> (that is, 'x' allows
 *   single-character subtags).</dd>
 *
 *   <dd>Example: key="u"/value="ca-japanese" (Japanese Calendar),
 *   key="x"/value="java-1-7"</dd>
 * </dl>
 *
 * <b>Note:</b> Although BCP 47 requires field values to be registered
 * in the IANA Language Subtag Registry, the {@code Locale} class
 * does not provide any validation features.  The {@code Builder}
 * only checks if an individual field satisfies the syntactic
 * requirement (is well-formed), but does not validate the value
 * itself.  See {@link Builder} for details.
 *
 * <h2><a id="def_locale_extension">Unicode locale/language extension</a></h2>
 *
 * <p>UTS#35, "Unicode Locale Data Markup Language" defines optional
 * attributes and keywords to override or refine the default behavior
 * associated with a locale.  A keyword is represented by a pair of
 * key and type.  For example, "nu-thai" indicates that Thai local
 * digits (value:"thai") should be used for formatting numbers
 * (key:"nu").
 *
 * <p>The keywords are mapped to a BCP 47 extension value using the
 * extension key 'u' ({@link #UNICODE_LOCALE_EXTENSION}).  The above
 * example, "nu-thai", becomes the extension "u-nu-thai".
 *
 * <p>Thus, when a {@code Locale} object contains Unicode locale
 * attributes and keywords,
 * {@code getExtension(UNICODE_LOCALE_EXTENSION)} will return a
 * String representing this information, for example, "nu-thai".  The
 * {@code Locale} class also provides {@link
 * #getUnicodeLocaleAttributes}, {@link #getUnicodeLocaleKeys}, and
 * {@link #getUnicodeLocaleType} which allow you to access Unicode
 * locale attributes and key/type pairs directly.  When represented as
 * a string, the Unicode Locale Extension lists attributes
 * alphabetically, followed by key/type sequences with keys listed
 * alphabetically (the order of subtags comprising a key's type is
 * fixed when the type is defined)
 *
 * <p>A well-formed locale key has the form
 * <code>[0-9a-zA-Z]{2}</code>.  A well-formed locale type has the
 * form <code>"" | [0-9a-zA-Z]{3,8} ('-' [0-9a-zA-Z]{3,8})*</code> (it
 * can be empty, or a series of subtags 3-8 alphanums in length).  A
 * well-formed locale attribute has the form
 * <code>[0-9a-zA-Z]{3,8}</code> (it is a single subtag with the same
 * form as a locale type subtag).
 *
 * <p>The Unicode locale extension specifies optional behavior in
 * locale-sensitive services.  Although the LDML specification defines
 * various keys and values, actual locale-sensitive service
 * implementations in a Java Runtime Environment might not support any
 * particular Unicode locale attributes or key/type pairs.
 *
 * <h3>Creating a Locale</h3>
 *
 * <p>There are several different ways to create a {@code Locale}
 * object.
 *
 * <h4>Builder</h4>
 *
 * <p>Using {@link Builder} you can construct a {@code Locale} object
 * that conforms to BCP 47 syntax.
 *
 * <h4>Constructors</h4>
 *
 * <p>The {@code Locale} class provides three constructors:
 * <blockquote>
 * <pre>
 *     {@link #Locale(String language)}
 *     {@link #Locale(String language, String country)}
 *     {@link #Locale(String language, String country, String variant)}
 * </pre>
 * </blockquote>
 * These constructors allow you to create a {@code Locale} object
 * with language, country and variant, but you cannot specify
 * script or extensions.
 *
 * <h4>Factory Methods</h4>
 *
 * <p>The method {@link #forLanguageTag} creates a {@code Locale}
 * object for a well-formed BCP 47 language tag.
 *
 * <h4>Locale Constants</h4>
 *
 * <p>The {@code Locale} class provides a number of convenient constants
 * that you can use to create {@code Locale} objects for commonly used
 * locales. For example, the following creates a {@code Locale} object
 * for the United States:
 * <blockquote>
 * <pre>
 *     Locale.US
 * </pre>
 * </blockquote>
 *
 * <h3><a id="LocaleMatching">Locale Matching</a></h3>
 *
 * <p>If an application or a system is internationalized and provides localized
 * resources for multiple locales, it sometimes needs to find one or more
 * locales (or language tags) which meet each user's specific preferences. Note
 * that a term "language tag" is used interchangeably with "locale" in this
 * locale matching documentation.
 *
 * <p>In order to do matching a user's preferred locales to a set of language
 * tags, <a href="http://tools.ietf.org/html/rfc4647">RFC 4647 Matching of
 * Language Tags</a> defines two mechanisms: filtering and lookup.
 * <em>Filtering</em> is used to get all matching locales, whereas
 * <em>lookup</em> is to choose the best matching locale.
 * Matching is done case-insensitively. These matching mechanisms are described
 * in the following sections.
 *
 * <p>A user's preference is called a <em>Language Priority List</em> and is
 * expressed as a list of language ranges. There are syntactically two types of
 * language ranges: basic and extended. See
 * {@link Locale.LanguageRange Locale.LanguageRange} for details.
 *
 * <h4>Filtering</h4>
 *
 * <p>The filtering operation returns all matching language tags. It is defined
 * in RFC 4647 as follows:
 * "In filtering, each language range represents the least specific language
 * tag (that is, the language tag with fewest number of subtags) that is an
 * acceptable match. All of the language tags in the matching set of tags will
 * have an equal or greater number of subtags than the language range. Every
 * non-wildcard subtag in the language range will appear in every one of the
 * matching language tags."
 *
 * <p>There are two types of filtering: filtering for basic language ranges
 * (called "basic filtering") and filtering for extended language ranges
 * (called "extended filtering"). They may return different results by what
 * kind of language ranges are included in the given Language Priority List.
 * {@link Locale.FilteringMode} is a parameter to specify how filtering should
 * be done.
 *
 * <h4>Lookup</h4>
 *
 * <p>The lookup operation returns the best matching language tags. It is
 * defined in RFC 4647 as follows:
 * "By contrast with filtering, each language range represents the most
 * specific tag that is an acceptable match.  The first matching tag found,
 * according to the user's priority, is considered the closest match and is the
 * item returned."
 *
 * <p>For example, if a Language Priority List consists of two language ranges,
 * {@code "zh-Hant-TW"} and {@code "en-US"}, in prioritized order, lookup
 * method progressively searches the language tags below in order to find the
 * best matching language tag.
 * <blockquote>
 * <pre>
 *    1. zh-Hant-TW
 *    2. zh-Hant
 *    3. zh
 *    4. en-US
 *    5. en
 * </pre>
 * </blockquote>
 * If there is a language tag which matches completely to a language range
 * above, the language tag is returned.
 *
 * <p>{@code "*"} is the special language range, and it is ignored in lookup.
 *
 * <p>If multiple language tags match as a result of the subtag {@code '*'}
 * included in a language range, the first matching language tag returned by
 * an {@link Iterator} over a {@link Collection} of language tags is treated as
 * the best matching one.
 *
 * <h3>Use of Locale</h3>
 *
 * <p>Once you've created a {@code Locale} you can query it for information
 * about itself. Use {@code getCountry} to get the country (or region)
 * code and {@code getLanguage} to get the language code.
 * You can use {@code getDisplayCountry} to get the
 * name of the country suitable for displaying to the user. Similarly,
 * you can use {@code getDisplayLanguage} to get the name of
 * the language suitable for displaying to the user. Interestingly,
 * the {@code getDisplayXXX} methods are themselves locale-sensitive
 * and have two versions: one that uses the default
 * {@link Locale.Category#DISPLAY DISPLAY} locale and one
 * that uses the locale specified as an argument.
 *
 * <p>The Java Platform provides a number of classes that perform locale-sensitive
 * operations. For example, the {@code NumberFormat} class formats
 * numbers, currency, and percentages in a locale-sensitive manner. Classes
 * such as {@code NumberFormat} have several convenience methods
 * for creating a default object of that type. For example, the
 * {@code NumberFormat} class provides these three convenience methods
 * for creating a default {@code NumberFormat} object:
 * <blockquote>
 * <pre>
 *     NumberFormat.getInstance()
 *     NumberFormat.getCurrencyInstance()
 *     NumberFormat.getPercentInstance()
 * </pre>
 * </blockquote>
 * Each of these methods has two variants; one with an explicit locale
 * and one without; the latter uses the default
 * {@link Locale.Category#FORMAT FORMAT} locale:
 * <blockquote>
 * <pre>
 *     NumberFormat.getInstance(myLocale)
 *     NumberFormat.getCurrencyInstance(myLocale)
 *     NumberFormat.getPercentInstance(myLocale)
 * </pre>
 * </blockquote>
 * A {@code Locale} is the mechanism for identifying the kind of object
 * ({@code NumberFormat}) that you would like to get. The locale is
 * <STRONG>just</STRONG> a mechanism for identifying objects,
 * <STRONG>not</STRONG> a container for the objects themselves.
 *
 * <h3>Compatibility</h3>
 *
 * <p>In order to maintain compatibility with existing usage, Locale's
 * constructors retain their behavior prior to the Java Runtime
 * Environment version 1.7.  The same is largely true for the
 * {@code toString} method. Thus Locale objects can continue to
 * be used as they were. In particular, clients who parse the output
 * of toString into language, country, and variant fields can continue
 * to do so (although this is strongly discouraged), although the
 * variant field will have additional information in it if script or
 * extensions are present.
 *
 * <p>In addition, BCP 47 imposes syntax restrictions that are not
 * imposed by Locale's constructors. This means that conversions
 * between some Locales and BCP 47 language tags cannot be made without
 * losing information. Thus {@code toLanguageTag} cannot
 * represent the state of locales whose language, country, or variant
 * do not conform to BCP 47.
 *
 * <p>Because of these issues, it is recommended that clients migrate
 * away from constructing non-conforming locales and use the
 * {@code forLanguageTag} and {@code Locale.Builder} APIs instead.
 * Clients desiring a string representation of the complete locale can
 * then always rely on {@code toLanguageTag} for this purpose.
 *
 * <h4><a id="special_cases_constructor">Special cases</a></h4>
 *
 * <p>For compatibility reasons, two
 * non-conforming locales are treated as special cases.  These are
 * <b>{@code ja_JP_JP}</b> and <b>{@code th_TH_TH}</b>. These are ill-formed
 * in BCP 47 since the variants are too short. To ease migration to BCP 47,
 * these are treated specially during construction.  These two cases (and only
 * these) cause a constructor to generate an extension, all other values behave
 * exactly as they did prior to Java 7.
 *
 * <p>Java has used {@code ja_JP_JP} to represent Japanese as used in
 * Japan together with the Japanese Imperial calendar. This is now
 * representable using a Unicode locale extension, by specifying the
 * Unicode locale key {@code ca} (for "calendar") and type
 * {@code japanese}. When the Locale constructor is called with the
 * arguments "ja", "JP", "JP", the extension "u-ca-japanese" is
 * automatically added.
 *
 * <p>Java has used {@code th_TH_TH} to represent Thai as used in
 * Thailand together with Thai digits. This is also now representable using
 * a Unicode locale extension, by specifying the Unicode locale key
 * {@code nu} (for "number") and value {@code thai}. When the Locale
 * constructor is called with the arguments "th", "TH", "TH", the
 * extension "u-nu-thai" is automatically added.
 *
 * <h4>Serialization</h4>
 *
 * <p>During serialization, writeObject writes all fields to the output
 * stream, including extensions.
 *
 * <p>During deserialization, readResolve adds extensions as described
 * in <a href="#special_cases_constructor">Special Cases</a>, only
 * for the two cases th_TH_TH and ja_JP_JP.
 *
 * <h4><a id="legacy_language_codes">Legacy language codes</a></h4>
 *
 * <p>Locale's constructor has always converted three language codes to
 * their earlier, obsoleted forms: {@code he} maps to {@code iw},
 * {@code yi} maps to {@code ji}, and {@code id} maps to
 * {@code in}. Since Java SE 17, this is no longer the case. Each
 * language maps to its new form; {@code iw} maps to {@code he}, {@code ji}
 * maps to {@code yi}, and {@code in} maps to {@code id}.
 *
 * <p>For the backward compatible behavior, the system property
 * {@systemProperty java.locale.useOldISOCodes} reverts the behavior
 * back to that of before Java SE 17. If the system property is set to
 * {@code true}, those three current language codes are mapped to their
 * backward compatible forms. The property is only read at Java runtime
 * startup and subsequent calls to {@code System.setProperty()} will
 * have no effect.
 *
 * <p>The APIs added in 1.7 map between the old and new language codes,
 * maintaining the mapped codes internal to Locale (so that
 * {@code getLanguage} and {@code toString} reflect the mapped
 * code, which depends on the {@code java.locale.useOldISOCodes} system
 * property), but using the new codes in the BCP 47 language tag APIs (so
 * that {@code toLanguageTag} reflects the new one). This
 * preserves the equivalence between Locales no matter which code or
 * API is used to construct them. Java's default resource bundle
 * lookup mechanism also implements this mapping, so that resources
 * can be named using either convention, see {@link ResourceBundle.Control}.
 *
 * <h4>Three-letter language/country(region) codes</h4>
 *
 * <p>The Locale constructors have always specified that the language
 * and the country param be two characters in length, although in
 * practice they have accepted any length.  The specification has now
 * been relaxed to allow language codes of two to eight characters and
 * country (region) codes of two to three characters, and in
 * particular, three-letter language codes and three-digit region
 * codes as specified in the IANA Language Subtag Registry.  For
 * compatibility, the implementation still does not impose a length
 * constraint.
 *
 * @see Builder
 * @see ResourceBundle
 * @see java.text.Format
 * @see java.text.NumberFormat
 * @see java.text.Collator
 * @author Mark Davis
 * @since 1.1
 */
public final class Locale implements Cloneable, Serializable {

    /** Useful constant for language.
     */
    public static final Locale ENGLISH;

    /** Useful constant for language.
     */
    public static final Locale FRENCH;

    /** Useful constant for language.
     */
    public static final Locale GERMAN;

    /** Useful constant for language.
     */
    public static final Locale ITALIAN;

    /** Useful constant for language.
     */
    public static final Locale JAPANESE;

    /** Useful constant for language.
     */
    public static final Locale KOREAN;

    /** Useful constant for language.
     */
    public static final Locale CHINESE;

    /** Useful constant for language.
     */
    public static final Locale SIMPLIFIED_CHINESE;

    /** Useful constant for language.
     */
    public static final Locale TRADITIONAL_CHINESE;

    /** Useful constant for country.
     */
    public static final Locale FRANCE;

    /** Useful constant for country.
     */
    public static final Locale GERMANY;

    /** Useful constant for country.
     */
    public static final Locale ITALY;

    /** Useful constant for country.
     */
    public static final Locale JAPAN;

    /** Useful constant for country.
     */
    public static final Locale KOREA;

    /** Useful constant for country.
     */
    public static final Locale UK;

    /** Useful constant for country.
     */
    public static final Locale US;

    /** Useful constant for country.
     */
    public static final Locale CANADA;

    /** Useful constant for country.
     */
    public static final Locale CANADA_FRENCH;

    /**
     * Useful constant for the root locale.  The root locale is the locale whose
     * language, country, and variant are empty ("") strings.  This is regarded
     * as the base locale of all locales, and is used as the language/country
     * neutral locale for the locale sensitive operations.
     *
     * @since 1.6
     */
    public static final Locale ROOT;

    private static final Map<BaseLocale, Locale> CONSTANT_LOCALES = new HashMap<>();

    static {
        ENGLISH = createConstant(BaseLocale.ENGLISH);
        FRENCH = createConstant(BaseLocale.FRENCH);
        GERMAN = createConstant(BaseLocale.GERMAN);
        ITALIAN = createConstant(BaseLocale.ITALIAN);
        JAPANESE = createConstant(BaseLocale.JAPANESE);
        KOREAN = createConstant(BaseLocale.KOREAN);
        CHINESE = createConstant(BaseLocale.CHINESE);
        SIMPLIFIED_CHINESE = createConstant(BaseLocale.SIMPLIFIED_CHINESE);
        TRADITIONAL_CHINESE = createConstant(BaseLocale.TRADITIONAL_CHINESE);
        FRANCE = createConstant(BaseLocale.FRANCE);
        GERMANY = createConstant(BaseLocale.GERMANY);
        ITALY = createConstant(BaseLocale.ITALY);
        JAPAN = createConstant(BaseLocale.JAPAN);
        KOREA = createConstant(BaseLocale.KOREA);
        UK = createConstant(BaseLocale.UK);
        US = createConstant(BaseLocale.US);
        CANADA = createConstant(BaseLocale.CANADA);
        CANADA_FRENCH = createConstant(BaseLocale.CANADA_FRENCH);
        ROOT = createConstant(BaseLocale.ROOT);
    }

    /** Useful constant for country.
     */
    public static final Locale CHINA = SIMPLIFIED_CHINESE;

    /** Useful constant for country.
     */
    public static final Locale PRC = SIMPLIFIED_CHINESE;

    /** Useful constant for country.
     */
    public static final Locale TAIWAN = TRADITIONAL_CHINESE;

    /**
     * This method must be called only for creating the Locale.*
     * constants due to making shortcuts.
     */
    private static Locale createConstant(byte baseType) {
        BaseLocale base = BaseLocale.constantBaseLocales[baseType];
        Locale locale = new Locale(base, null);
        CONSTANT_LOCALES.put(base, locale);
        return locale;
    }

    /**
     * The key for the private use extension ('x').
     *
     * @see #getExtension(char)
     * @see Builder#setExtension(char, String)
     * @since 1.7
     */
    public static final char PRIVATE_USE_EXTENSION = 'x';

    /**
     * The key for Unicode locale extension ('u').
     *
     * @see #getExtension(char)
     * @see Builder#setExtension(char, String)
     * @since 1.7
     */
    public static final char UNICODE_LOCALE_EXTENSION = 'u';

    /** serialization ID
     */
    @java.io.Serial
    static final long serialVersionUID = 9149081749638150636L;

    /**
     * Enum for specifying the type defined in ISO 3166. This enum is used to
     * retrieve the two-letter ISO3166-1 alpha-2, three-letter ISO3166-1
     * alpha-3, four-letter ISO3166-3 country codes.
     *
     * @see #getISOCountries(Locale.IsoCountryCode)
     * @since 9
     */
    public static enum IsoCountryCode {
        /**
         * PART1_ALPHA2 is used to represent the ISO3166-1 alpha-2 two letter
         * country codes.
         */
        PART1_ALPHA2 {
            @Override
            Set<String> createCountryCodeSet() {
                return Set.of(Locale.getISOCountries());
            }
        },

        /**
         *
         * PART1_ALPHA3 is used to represent the ISO3166-1 alpha-3 three letter
         * country codes.
         */
        PART1_ALPHA3 {
            @Override
            Set<String> createCountryCodeSet() {
                return LocaleISOData.computeISO3166_1Alpha3Countries();
            }
        },

        /**
         * PART3 is used to represent the ISO3166-3 four letter country codes.
         */
        PART3 {
            @Override
            Set<String> createCountryCodeSet() {
                return Set.of(LocaleISOData.ISO3166_3);
            }
        };

        /**
         * Concrete implementation of this method attempts to compute value
         * for iso3166CodesMap for each IsoCountryCode type key.
         */
        abstract Set<String> createCountryCodeSet();

        /**
         * Map to hold country codes for each ISO3166 part.
         */
        private static Map<IsoCountryCode, Set<String>> iso3166CodesMap = new ConcurrentHashMap<>();

        /**
         * This method is called from Locale class to retrieve country code set
         * for getISOCountries(type)
         */
        static Set<String> retrieveISOCountryCodes(IsoCountryCode type) {
            return iso3166CodesMap.computeIfAbsent(type, IsoCountryCode::createCountryCodeSet);
        }
    }

    /**
     * Display types for retrieving localized names from the name providers.
     */
    private static final int DISPLAY_LANGUAGE  = 0;
    private static final int DISPLAY_COUNTRY   = 1;
    private static final int DISPLAY_VARIANT   = 2;
    private static final int DISPLAY_SCRIPT    = 3;
    private static final int DISPLAY_UEXT_KEY  = 4;
    private static final int DISPLAY_UEXT_TYPE = 5;

    /**
     * Private constructor used by getInstance method
     */
    private Locale(BaseLocale baseLocale, LocaleExtensions extensions) {
        this.baseLocale = baseLocale;
        this.localeExtensions = extensions;
    }

    /**
     * Construct a locale from language, country and variant.
     * This constructor normalizes the language value to lowercase and
     * the country value to uppercase.
     * @implNote
     * <ul>
     * <li>Obsolete ISO 639 codes ("iw", "ji", and "in") are mapped to
     * their current forms. See <a href="#legacy_language_codes">Legacy language
     * codes</a> for more information.
     * <li>For backward compatibility reasons, this constructor does not make
     * any syntactic checks on the input.
     * <li>The two cases ("ja", "JP", "JP") and ("th", "TH", "TH") are handled specially,
     * see <a href="#special_cases_constructor">Special Cases</a> for more information.
     * </ul>
     *
     * @param language An ISO 639 alpha-2 or alpha-3 language code, or a language subtag
     * up to 8 characters in length.  See the {@code Locale} class description about
     * valid language values.
     * @param country An ISO 3166 alpha-2 country code or a UN M.49 numeric-3 area code.
     * See the {@code Locale} class description about valid country values.
     * @param variant Any arbitrary value used to indicate a variation of a {@code Locale}.
     * See the {@code Locale} class description for the details.
     * @throws    NullPointerException thrown if any argument is null.
     */
    public Locale(String language, String country, String variant) {
        if (language == null || country == null || variant == null) {
            throw new NullPointerException();
        }
        baseLocale = BaseLocale.getInstance(convertOldISOCodes(language), "", country, variant);
        localeExtensions = getCompatibilityExtensions(language, "", country, variant);
    }

    /**
     * Construct a locale from language and country.
     * This constructor normalizes the language value to lowercase and
     * the country value to uppercase.
     * @implNote
     * <ul>
     * <li>Obsolete ISO 639 codes ("iw", "ji", and "in") are mapped to
     * their current forms. See <a href="#legacy_language_codes">Legacy language
     * codes</a> for more information.
     * <li>For backward compatibility reasons, this constructor does not make
     * any syntactic checks on the input.
     * </ul>
     *
     * @param language An ISO 639 alpha-2 or alpha-3 language code, or a language subtag
     * up to 8 characters in length.  See the {@code Locale} class description about
     * valid language values.
     * @param country An ISO 3166 alpha-2 country code or a UN M.49 numeric-3 area code.
     * See the {@code Locale} class description about valid country values.
     * @throws    NullPointerException thrown if either argument is null.
     */
    public Locale(String language, String country) {
        this(language, country, "");
    }

    /**
     * Construct a locale from a language code.
     * This constructor normalizes the language value to lowercase.
     * @implNote
     * <ul>
     * <li>Obsolete ISO 639 codes ("iw", "ji", and "in") are mapped to
     * their current forms. See <a href="#legacy_language_codes">Legacy language
     * codes</a> for more information.
     * <li>For backward compatibility reasons, this constructor does not make
     * any syntactic checks on the input.
     * </ul>
     *
     * @param language An ISO 639 alpha-2 or alpha-3 language code, or a language subtag
     * up to 8 characters in length.  See the {@code Locale} class description about
     * valid language values.
     * @throws    NullPointerException thrown if argument is null.
     * @since 1.4
     */
    public Locale(String language) {
        this(language, "", "");
    }

    /**
     * Returns a {@code Locale} constructed from the given
     * {@code language}, {@code country} and
     * {@code variant}. If the same {@code Locale} instance
     * is available in the cache, then that instance is
     * returned. Otherwise, a new {@code Locale} instance is
     * created and cached.
     *
     * @param language lowercase 2 to 8 language code.
     * @param country uppercase two-letter ISO-3166 code and numeric-3 UN M.49 area code.
     * @param variant vendor and browser specific code. See class description.
     * @return the {@code Locale} instance requested
     * @throws    NullPointerException if any argument is null.
     */
    static Locale getInstance(String language, String country, String variant) {
        return getInstance(language, "", country, variant, null);
    }

    static Locale getInstance(String language, String script, String country,
                                      String variant, LocaleExtensions extensions) {
        if (language== null || script == null || country == null || variant == null) {
            throw new NullPointerException();
        }

        if (extensions == null) {
            extensions = getCompatibilityExtensions(language, script, country, variant);
        }

        BaseLocale baseloc = BaseLocale.getInstance(convertOldISOCodes(language), script, country, variant);
        return getInstance(baseloc, extensions);
    }

    static Locale getInstance(BaseLocale baseloc, LocaleExtensions extensions) {
        if (extensions == null) {
            Locale locale = CONSTANT_LOCALES.get(baseloc);
            if (locale != null) {
                return locale;
            }
            return Cache.LOCALECACHE.get(baseloc);
        } else {
            LocaleKey key = new LocaleKey(baseloc, extensions);
            return Cache.LOCALECACHE.get(key);
        }
    }

    private static class Cache extends LocaleObjectCache<Object, Locale> {

        private static final Cache LOCALECACHE = new Cache();

        private Cache() {
        }

        @Override
        protected Locale createObject(Object key) {
            if (key instanceof BaseLocale) {
                return new Locale((BaseLocale)key, null);
            } else {
                LocaleKey lk = (LocaleKey)key;
                return new Locale(lk.base, lk.exts);
            }
        }
    }

    private static final class LocaleKey {
        private final BaseLocale base;
        private final LocaleExtensions exts;
        private final int hash;

        private LocaleKey(BaseLocale baseLocale, LocaleExtensions extensions) {
            base = baseLocale;
            exts = extensions;

            // Calculate the hash value here because it's always used.
            int h = base.hashCode();
            if (exts != null) {
                h ^= exts.hashCode();
            }
            hash = h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LocaleKey other)) {
                return false;
            }
            if (hash != other.hash || !base.equals(other.base)) {
                return false;
            }
            if (exts == null) {
                return other.exts == null;
            }
            return exts.equals(other.exts);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * Gets the current value of the default locale for this instance
     * of the Java Virtual Machine.
     * <p>
     * The Java Virtual Machine sets the default locale during startup
     * based on the host environment. It is used by many locale-sensitive
     * methods if no locale is explicitly specified.
     * It can be changed using the
     * {@link #setDefault(java.util.Locale) setDefault} method.
     *
     * @return the default locale for this instance of the Java Virtual Machine
     */
    public static Locale getDefault() {
        // do not synchronize this method - see 4071298
        return defaultLocale;
    }

    /**
     * Gets the current value of the default locale for the specified Category
     * for this instance of the Java Virtual Machine.
     * <p>
     * The Java Virtual Machine sets the default locale during startup based
     * on the host environment. It is used by many locale-sensitive methods
     * if no locale is explicitly specified. It can be changed using the
     * setDefault(Locale.Category, Locale) method.
     *
     * @param category the specified category to get the default locale
     * @throws NullPointerException if category is null
     * @return the default locale for the specified Category for this instance
     *     of the Java Virtual Machine
     * @see #setDefault(Locale.Category, Locale)
     * @since 1.7
     */
    public static Locale getDefault(Locale.Category category) {
        // do not synchronize this method - see 4071298
        Objects.requireNonNull(category);
        if (category == Category.DISPLAY) {
            Locale loc = defaultDisplayLocale; // volatile read
            if (loc == null) {
                loc = getDisplayLocale();
            }
            return loc;
        } else {
            assert category == Category.FORMAT : "Unknown category";
            Locale loc = defaultFormatLocale; // volatile read
            if (loc == null) {
                loc = getFormatLocale();
            }
            return loc;
        }
    }

    private static synchronized Locale getDisplayLocale() {
        Locale loc = defaultDisplayLocale;
        if (loc == null) {
            loc = defaultDisplayLocale = initDefault(Category.DISPLAY);
        }
        return loc;
    }


    private static synchronized Locale getFormatLocale() {
        Locale loc = defaultFormatLocale;
        if (loc == null) {
            loc = defaultFormatLocale = initDefault(Category.FORMAT);
        }
        return loc;
    }

    private static Locale initDefault() {
        String language, region, script, country, variant;
        Properties props = GetPropertyAction.privilegedGetProperties();
        language = props.getProperty("user.language", "en");
        // for compatibility, check for old user.region property
        region = props.getProperty("user.region");
        if (region != null) {
            // region can be of form country, country_variant, or _variant
            int i = region.indexOf('_');
            if (i >= 0) {
                country = region.substring(0, i);
                variant = region.substring(i + 1);
            } else {
                country = region;
                variant = "";
            }
            script = "";
        } else {
            script = props.getProperty("user.script", "");
            country = props.getProperty("user.country", "");
            variant = props.getProperty("user.variant", "");
        }

        return getInstance(language, script, country, variant,
                getDefaultExtensions(props.getProperty("user.extensions", ""))
                    .orElse(null));
    }

    private static Locale initDefault(Locale.Category category) {
        Properties props = GetPropertyAction.privilegedGetProperties();

        return getInstance(
            props.getProperty(category.languageKey,
                    defaultLocale.getLanguage()),
            props.getProperty(category.scriptKey,
                    defaultLocale.getScript()),
            props.getProperty(category.countryKey,
                    defaultLocale.getCountry()),
            props.getProperty(category.variantKey,
                    defaultLocale.getVariant()),
            getDefaultExtensions(props.getProperty(category.extensionsKey, ""))
                .orElse(defaultLocale.getLocaleExtensions()));
    }

    private static Optional<LocaleExtensions> getDefaultExtensions(String extensionsProp) {
        if (LocaleUtils.isEmpty(extensionsProp)) {
            return Optional.empty();
        }

        LocaleExtensions exts = null;
        try {
            exts = new InternalLocaleBuilder()
                .setExtensions(extensionsProp)
                .getLocaleExtensions();
        } catch (LocaleSyntaxException e) {
            // just ignore this incorrect property
        }

        return Optional.ofNullable(exts);
    }

    /**
     * Sets the default locale for this instance of the Java Virtual Machine.
     * This does not affect the host locale.
     * <p>
     * If there is a security manager, its {@code checkPermission}
     * method is called with a {@code PropertyPermission("user.language", "write")}
     * permission before the default locale is changed.
     * <p>
     * The Java Virtual Machine sets the default locale during startup
     * based on the host environment. It is used by many locale-sensitive
     * methods if no locale is explicitly specified.
     * <p>
     * Since changing the default locale may affect many different areas
     * of functionality, this method should only be used if the caller
     * is prepared to reinitialize locale-sensitive code running
     * within the same Java Virtual Machine.
     * <p>
     * By setting the default locale with this method, all of the default
     * locales for each Category are also set to the specified default locale.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow the operation.
     * @throws NullPointerException if {@code newLocale} is null
     * @param newLocale the new default locale
     * @see SecurityManager#checkPermission
     * @see java.util.PropertyPermission
     */
    public static synchronized void setDefault(Locale newLocale) {
        setDefault(Category.DISPLAY, newLocale);
        setDefault(Category.FORMAT, newLocale);
        defaultLocale = newLocale;
    }

    /**
     * Sets the default locale for the specified Category for this instance
     * of the Java Virtual Machine. This does not affect the host locale.
     * <p>
     * If there is a security manager, its checkPermission method is called
     * with a PropertyPermission("user.language", "write") permission before
     * the default locale is changed.
     * <p>
     * The Java Virtual Machine sets the default locale during startup based
     * on the host environment. It is used by many locale-sensitive methods
     * if no locale is explicitly specified.
     * <p>
     * Since changing the default locale may affect many different areas of
     * functionality, this method should only be used if the caller is
     * prepared to reinitialize locale-sensitive code running within the
     * same Java Virtual Machine.
     *
     * @param category the specified category to set the default locale
     * @param newLocale the new default locale
     * @throws SecurityException if a security manager exists and its
     *     checkPermission method doesn't allow the operation.
     * @throws NullPointerException if category and/or newLocale is null
     * @see SecurityManager#checkPermission(java.security.Permission)
     * @see PropertyPermission
     * @see #getDefault(Locale.Category)
     * @since 1.7
     */
    public static synchronized void setDefault(Locale.Category category,
        Locale newLocale) {
        if (category == null)
            throw new NullPointerException("Category cannot be NULL");
        if (newLocale == null)
            throw new NullPointerException("Can't set default locale to NULL");

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new PropertyPermission
                        ("user.language", "write"));
        switch (category) {
        case DISPLAY:
            defaultDisplayLocale = newLocale;
            break;
        case FORMAT:
            defaultFormatLocale = newLocale;
            break;
        default:
            assert false: "Unknown Category";
        }
    }

    /**
     * Returns an array of all installed locales.
     * The returned array represents the union of locales supported
     * by the Java runtime environment and by installed
     * {@link java.util.spi.LocaleServiceProvider LocaleServiceProvider}
     * implementations.  It must contain at least a {@code Locale}
     * instance equal to {@link java.util.Locale#US Locale.US}.
     *
     * @return An array of installed locales.
     */
    public static Locale[] getAvailableLocales() {
        return LocaleServiceProviderPool.getAllAvailableLocales();
    }

    /**
     * Returns a list of all 2-letter country codes defined in ISO 3166.
     * Can be used to create Locales.
     * This method is equivalent to {@link #getISOCountries(Locale.IsoCountryCode type)}
     * with {@code type}  {@link IsoCountryCode#PART1_ALPHA2}.
     * <p>
     * <b>Note:</b> The {@code Locale} class also supports other codes for
     * country (region), such as 3-letter numeric UN M.49 area codes.
     * Therefore, the list returned by this method does not contain ALL valid
     * codes that can be used to create Locales.
     * <p>
     * Note that this method does not return obsolete 2-letter country codes.
     * ISO3166-3 codes which designate country codes for those obsolete codes,
     * can be retrieved from {@link #getISOCountries(Locale.IsoCountryCode type)} with
     * {@code type}  {@link IsoCountryCode#PART3}.
     * @return An array of ISO 3166 two-letter country codes.
     */
    public static String[] getISOCountries() {
        if (isoCountries == null) {
            isoCountries = getISO2Table(LocaleISOData.isoCountryTable);
        }
        String[] result = new String[isoCountries.length];
        System.arraycopy(isoCountries, 0, result, 0, isoCountries.length);
        return result;
    }

    /**
     * Returns a {@code Set} of ISO3166 country codes for the specified type.
     *
     * @param type {@link Locale.IsoCountryCode} specified ISO code type.
     * @see java.util.Locale.IsoCountryCode
     * @throws NullPointerException if type is null
     * @return a {@code Set} of ISO country codes for the specified type.
     * @since 9
     */
    public static Set<String> getISOCountries(IsoCountryCode type) {
        Objects.requireNonNull(type);
        return IsoCountryCode.retrieveISOCountryCodes(type);
    }

    /**
     * Returns a list of all 2-letter language codes defined in ISO 639.
     * Can be used to create Locales.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>ISO 639 is not a stable standard&mdash; some languages' codes have changed.
     * The list this function returns includes both the new and the old codes for the
     * languages whose codes have changed.
     * <li>The {@code Locale} class also supports language codes up to
     * 8 characters in length.  Therefore, the list returned by this method does
     * not contain ALL valid codes that can be used to create Locales.
     * </ul>
     *
     * @return An array of ISO 639 two-letter language codes.
     */
    public static String[] getISOLanguages() {
        if (isoLanguages == null) {
            isoLanguages = getISO2Table(LocaleISOData.isoLanguageTable);
        }
        String[] result = new String[isoLanguages.length];
        System.arraycopy(isoLanguages, 0, result, 0, isoLanguages.length);
        return result;
    }

    private static String[] getISO2Table(String table) {
        int len = table.length() / 5;
        String[] isoTable = new String[len];
        for (int i = 0, j = 0; i < len; i++, j += 5) {
            isoTable[i] = table.substring(j, j + 2);
        }
        return isoTable;
    }

    /**
     * Returns the language code of this Locale.
     *
     * @implNote This method returns the new forms for the obsolete ISO 639
     * codes ("iw", "ji", and "in"). See <a href="#legacy_language_codes">
     * Legacy language codes</a> for more information.
     *
     * @return The language code, or the empty string if none is defined.
     * @see #getDisplayLanguage
     */
    public String getLanguage() {
        return baseLocale.getLanguage();
    }

    /**
     * Returns the script for this locale, which should
     * either be the empty string or an ISO 15924 4-letter script
     * code. The first letter is uppercase and the rest are
     * lowercase, for example, 'Latn', 'Cyrl'.
     *
     * @return The script code, or the empty string if none is defined.
     * @see #getDisplayScript
     * @since 1.7
     */
    public String getScript() {
        return baseLocale.getScript();
    }

    /**
     * Returns the country/region code for this locale, which should
     * either be the empty string, an uppercase ISO 3166 2-letter code,
     * or a UN M.49 3-digit code.
     *
     * @return The country/region code, or the empty string if none is defined.
     * @see #getDisplayCountry
     */
    public String getCountry() {
        return baseLocale.getRegion();
    }

    /**
     * Returns the variant code for this locale.
     *
     * @return The variant code, or the empty string if none is defined.
     * @see #getDisplayVariant
     */
    public String getVariant() {
        return baseLocale.getVariant();
    }

    /**
     * Returns {@code true} if this {@code Locale} has any <a href="#def_extensions">
     * extensions</a>.
     *
     * @return {@code true} if this {@code Locale} has any extensions
     * @since 1.8
     */
    public boolean hasExtensions() {
        return localeExtensions != null;
    }

    /**
     * Returns a copy of this {@code Locale} with no <a href="#def_extensions">
     * extensions</a>. If this {@code Locale} has no extensions, this {@code Locale}
     * is returned.
     *
     * @return a copy of this {@code Locale} with no extensions, or {@code this}
     *         if {@code this} has no extensions
     * @since 1.8
     */
    public Locale stripExtensions() {
        return hasExtensions() ? Locale.getInstance(baseLocale, null) : this;
    }

    /**
     * Returns the extension (or private use) value associated with
     * the specified key, or null if there is no extension
     * associated with the key. To be well-formed, the key must be one
     * of {@code [0-9A-Za-z]}. Keys are case-insensitive, so
     * for example 'z' and 'Z' represent the same extension.
     *
     * @param key the extension key
     * @return The extension, or null if this locale defines no
     * extension for the specified key.
     * @throws IllegalArgumentException if key is not well-formed
     * @see #PRIVATE_USE_EXTENSION
     * @see #UNICODE_LOCALE_EXTENSION
     * @since 1.7
     */
    public String getExtension(char key) {
        if (!LocaleExtensions.isValidKey(key)) {
            throw new IllegalArgumentException("Ill-formed extension key: " + key);
        }
        return hasExtensions() ? localeExtensions.getExtensionValue(key) : null;
    }

    /**
     * Returns the set of extension keys associated with this locale, or the
     * empty set if it has no extensions. The returned set is unmodifiable.
     * The keys will all be lower-case.
     *
     * @return The set of extension keys, or the empty set if this locale has
     * no extensions.
     * @since 1.7
     */
    public Set<Character> getExtensionKeys() {
        if (!hasExtensions()) {
            return Collections.emptySet();
        }
        return localeExtensions.getKeys();
    }

    /**
     * Returns the set of unicode locale attributes associated with
     * this locale, or the empty set if it has no attributes. The
     * returned set is unmodifiable.
     *
     * @return The set of attributes.
     * @since 1.7
     */
    public Set<String> getUnicodeLocaleAttributes() {
        if (!hasExtensions()) {
            return Collections.emptySet();
        }
        return localeExtensions.getUnicodeLocaleAttributes();
    }

    /**
     * Returns the Unicode locale type associated with the specified Unicode locale key
     * for this locale. Returns the empty string for keys that are defined with no type.
     * Returns null if the key is not defined. Keys are case-insensitive. The key must
     * be two alphanumeric characters ([0-9a-zA-Z]), or an IllegalArgumentException is
     * thrown.
     *
     * @param key the Unicode locale key
     * @return The Unicode locale type associated with the key, or null if the
     * locale does not define the key.
     * @throws IllegalArgumentException if the key is not well-formed
     * @throws NullPointerException if {@code key} is null
     * @since 1.7
     */
    public String getUnicodeLocaleType(String key) {
        if (!isUnicodeExtensionKey(key)) {
            throw new IllegalArgumentException("Ill-formed Unicode locale key: " + key);
        }
        return hasExtensions() ? localeExtensions.getUnicodeLocaleType(key) : null;
    }

    /**
     * Returns the set of Unicode locale keys defined by this locale, or the empty set if
     * this locale has none.  The returned set is immutable.  Keys are all lower case.
     *
     * @return The set of Unicode locale keys, or the empty set if this locale has
     * no Unicode locale keywords.
     * @since 1.7
     */
    public Set<String> getUnicodeLocaleKeys() {
        if (localeExtensions == null) {
            return Collections.emptySet();
        }
        return localeExtensions.getUnicodeLocaleKeys();
    }

    /**
     * Package locale method returning the Locale's BaseLocale,
     * used by ResourceBundle
     * @return base locale of this Locale
     */
    BaseLocale getBaseLocale() {
        return baseLocale;
    }

    /**
     * Package private method returning the Locale's LocaleExtensions,
     * used by ResourceBundle.
     * @return locale extensions of this Locale,
     *         or {@code null} if no extensions are defined
     */
     LocaleExtensions getLocaleExtensions() {
         return localeExtensions;
     }

    /**
     * Returns a string representation of this {@code Locale}
     * object, consisting of language, country, variant, script,
     * and extensions as below:
     * <blockquote>
     * language + "_" + country + "_" + (variant + "_#" | "#") + script + "_" + extensions
     * </blockquote>
     *
     * Language is always lower case, country is always upper case, script is always title
     * case, and extensions are always lower case.  Extensions and private use subtags
     * will be in canonical order as explained in {@link #toLanguageTag}.
     *
     * <p>When the locale has neither script nor extensions, the result is the same as in
     * Java 6 and prior.
     *
     * <p>If both the language and country fields are missing, this function will return
     * the empty string, even if the variant, script, or extensions field is present (you
     * can't have a locale with just a variant, the variant must accompany a well-formed
     * language or country code).
     *
     * <p>If script or extensions are present and variant is missing, no underscore is
     * added before the "#".
     *
     * <p>This behavior is designed to support debugging and to be compatible with
     * previous uses of {@code toString} that expected language, country, and variant
     * fields only.  To represent a Locale as a String for interchange purposes, use
     * {@link #toLanguageTag}.
     *
     * <p>Examples: <ul>
     * <li>{@code en}</li>
     * <li>{@code de_DE}</li>
     * <li>{@code _GB}</li>
     * <li>{@code en_US_WIN}</li>
     * <li>{@code de__POSIX}</li>
     * <li>{@code zh_CN_#Hans}</li>
     * <li>{@code zh_TW_#Hant_x-java}</li>
     * <li>{@code th_TH_TH_#u-nu-thai}</li></ul>
     *
     * @return A string representation of the Locale, for debugging.
     * @see #getDisplayName
     * @see #toLanguageTag
     */
    @Override
    public final String toString() {
        boolean l = !baseLocale.getLanguage().isEmpty();
        boolean s = !baseLocale.getScript().isEmpty();
        boolean r = !baseLocale.getRegion().isEmpty();
        boolean v = !baseLocale.getVariant().isEmpty();
        boolean e = localeExtensions != null && !localeExtensions.getID().isEmpty();

        StringBuilder result = new StringBuilder(baseLocale.getLanguage());
        if (r || (l && (v || s || e))) {
            result.append('_')
                .append(baseLocale.getRegion()); // This may just append '_'
        }
        if (v && (l || r)) {
            result.append('_')
                .append(baseLocale.getVariant());
        }

        if (s && (l || r)) {
            result.append("_#")
                .append(baseLocale.getScript());
        }

        if (e && (l || r)) {
            result.append('_');
            if (!s) {
                result.append('#');
            }
            result.append(localeExtensions.getID());
        }

        return result.toString();
    }

    /**
     * Returns a well-formed IETF BCP 47 language tag representing
     * this locale.
     *
     * <p>If this {@code Locale} has a language, country, or
     * variant that does not satisfy the IETF BCP 47 language tag
     * syntax requirements, this method handles these fields as
     * described below:
     *
     * <p><b>Language:</b> If language is empty, or not <a
     * href="#def_language" >well-formed</a> (for example "a" or
     * "e2"), it will be emitted as "und" (Undetermined).
     *
     * <p><b>Country:</b> If country is not <a
     * href="#def_region">well-formed</a> (for example "12" or "USA"),
     * it will be omitted.
     *
     * <p><b>Variant:</b> If variant <b>is</b> <a
     * href="#def_variant">well-formed</a>, each sub-segment
     * (delimited by '-' or '_') is emitted as a subtag.  Otherwise:
     * <ul>
     *
     * <li>if all sub-segments match <code>[0-9a-zA-Z]{1,8}</code>
     * (for example "WIN" or "Oracle_JDK_Standard_Edition"), the first
     * ill-formed sub-segment and all following will be appended to
     * the private use subtag.  The first appended subtag will be
     * "lvariant", followed by the sub-segments in order, separated by
     * hyphen. For example, "x-lvariant-WIN",
     * "Oracle-x-lvariant-JDK-Standard-Edition".
     *
     * <li>if any sub-segment does not match
     * <code>[0-9a-zA-Z]{1,8}</code>, the variant will be truncated
     * and the problematic sub-segment and all following sub-segments
     * will be omitted.  If the remainder is non-empty, it will be
     * emitted as a private use subtag as above (even if the remainder
     * turns out to be well-formed).  For example,
     * "Solaris_isjustthecoolestthing" is emitted as
     * "x-lvariant-Solaris", not as "solaris".</li></ul>
     *
     * <p><b>Special Conversions:</b> Java supports some old locale
     * representations, including deprecated ISO language codes,
     * for compatibility. This method performs the following
     * conversions:
     * <ul>
     *
     * <li>Deprecated ISO language codes "iw", "ji", and "in" are
     * converted to "he", "yi", and "id", respectively.
     *
     * <li>A locale with language "no", country "NO", and variant
     * "NY", representing Norwegian Nynorsk (Norway), is converted
     * to a language tag "nn-NO".</li></ul>
     *
     * <p><b>Note:</b> Although the language tag created by this
     * method is well-formed (satisfies the syntax requirements
     * defined by the IETF BCP 47 specification), it is not
     * necessarily a valid BCP 47 language tag.  For example,
     * <pre>
     *   new Locale("xx", "YY").toLanguageTag();</pre>
     *
     * will return "xx-YY", but the language subtag "xx" and the
     * region subtag "YY" are invalid because they are not registered
     * in the IANA Language Subtag Registry.
     *
     * @return a BCP47 language tag representing the locale
     * @see #forLanguageTag(String)
     * @since 1.7
     */
    public String toLanguageTag() {
        if (languageTag != null) {
            return languageTag;
        }

        LanguageTag tag = LanguageTag.parseLocale(baseLocale, localeExtensions);
        StringBuilder buf = new StringBuilder();

        String subtag = tag.getLanguage();
        if (!subtag.isEmpty()) {
            buf.append(LanguageTag.canonicalizeLanguage(subtag));
        }

        subtag = tag.getScript();
        if (!subtag.isEmpty()) {
            buf.append(LanguageTag.SEP);
            buf.append(LanguageTag.canonicalizeScript(subtag));
        }

        subtag = tag.getRegion();
        if (!subtag.isEmpty()) {
            buf.append(LanguageTag.SEP);
            buf.append(LanguageTag.canonicalizeRegion(subtag));
        }

        List<String>subtags = tag.getVariants();
        for (String s : subtags) {
            buf.append(LanguageTag.SEP);
            // preserve casing
            buf.append(s);
        }

        subtags = tag.getExtensions();
        for (String s : subtags) {
            buf.append(LanguageTag.SEP);
            buf.append(LanguageTag.canonicalizeExtension(s));
        }

        subtag = tag.getPrivateuse();
        if (!subtag.isEmpty()) {
            if (buf.length() > 0) {
                buf.append(LanguageTag.SEP);
            }
            buf.append(LanguageTag.PRIVATEUSE).append(LanguageTag.SEP);
            // preserve casing
            buf.append(subtag);
        }

        String langTag = buf.toString();
        synchronized (this) {
            if (languageTag == null) {
                languageTag = langTag;
            }
        }
        return languageTag;
    }

    /**
     * Returns a locale for the specified IETF BCP 47 language tag string.
     *
     * <p>If the specified language tag contains any ill-formed subtags,
     * the first such subtag and all following subtags are ignored.  Compare
     * to {@link Locale.Builder#setLanguageTag} which throws an exception
     * in this case.
     *
     * <p>The following <b>conversions</b> are performed:<ul>
     *
     * <li>The language code "und" is mapped to language "".
     *
     * <li>The language codes "iw", "ji", and "in" are mapped to "he",
     * "yi", and "id" respectively. (This is the same canonicalization
     * that's done in Locale's constructors.) See
     * <a href="#legacy_language_codes">Legacy language codes</a>
     * for more information.
     *
     * <li>The portion of a private use subtag prefixed by "lvariant",
     * if any, is removed and appended to the variant field in the
     * result locale (without case normalization).  If it is then
     * empty, the private use subtag is discarded:
     *
     * <pre>
     *     Locale loc;
     *     loc = Locale.forLanguageTag("en-US-x-lvariant-POSIX");
     *     loc.getVariant(); // returns "POSIX"
     *     loc.getExtension('x'); // returns null
     *
     *     loc = Locale.forLanguageTag("de-POSIX-x-URP-lvariant-Abc-Def");
     *     loc.getVariant(); // returns "POSIX_Abc_Def"
     *     loc.getExtension('x'); // returns "urp"
     * </pre>
     *
     * <li>When the languageTag argument contains an extlang subtag,
     * the first such subtag is used as the language, and the primary
     * language subtag and other extlang subtags are ignored:
     *
     * <pre>
     *     Locale.forLanguageTag("ar-aao").getLanguage(); // returns "aao"
     *     Locale.forLanguageTag("en-abc-def-us").toString(); // returns "abc_US"
     * </pre>
     *
     * <li>Case is normalized except for variant tags, which are left
     * unchanged.  Language is normalized to lower case, script to
     * title case, country to upper case, and extensions to lower
     * case.
     *
     * <li>If, after processing, the locale would exactly match either
     * ja_JP_JP or th_TH_TH with no extensions, the appropriate
     * extensions are added as though the constructor had been called:
     *
     * <pre>
     *    Locale.forLanguageTag("ja-JP-x-lvariant-JP").toLanguageTag();
     *    // returns "ja-JP-u-ca-japanese-x-lvariant-JP"
     *    Locale.forLanguageTag("th-TH-x-lvariant-TH").toLanguageTag();
     *    // returns "th-TH-u-nu-thai-x-lvariant-TH"
     * </pre></ul>
     *
     * <p>This implements the 'Language-Tag' production of BCP47, and
     * so supports legacy (regular and irregular, referred to as
     * "Type: grandfathered" in BCP47) as well as
     * private use language tags.  Stand alone private use tags are
     * represented as empty language and extension 'x-whatever',
     * and legacy tags are converted to their canonical replacements
     * where they exist.
     *
     * <p>Legacy tags with canonical replacements are as follows:
     *
     * <table class="striped">
     * <caption style="display:none">Legacy tags with canonical replacements</caption>
     * <thead style="text-align:center">
     * <tr><th scope="col" style="padding: 0 2px">legacy tag</th><th scope="col" style="padding: 0 2px">modern replacement</th></tr>
     * </thead>
     * <tbody style="text-align:center">
     * <tr><th scope="row">art-lojban</th><td>jbo</td></tr>
     * <tr><th scope="row">i-ami</th><td>ami</td></tr>
     * <tr><th scope="row">i-bnn</th><td>bnn</td></tr>
     * <tr><th scope="row">i-hak</th><td>hak</td></tr>
     * <tr><th scope="row">i-klingon</th><td>tlh</td></tr>
     * <tr><th scope="row">i-lux</th><td>lb</td></tr>
     * <tr><th scope="row">i-navajo</th><td>nv</td></tr>
     * <tr><th scope="row">i-pwn</th><td>pwn</td></tr>
     * <tr><th scope="row">i-tao</th><td>tao</td></tr>
     * <tr><th scope="row">i-tay</th><td>tay</td></tr>
     * <tr><th scope="row">i-tsu</th><td>tsu</td></tr>
     * <tr><th scope="row">no-bok</th><td>nb</td></tr>
     * <tr><th scope="row">no-nyn</th><td>nn</td></tr>
     * <tr><th scope="row">sgn-BE-FR</th><td>sfb</td></tr>
     * <tr><th scope="row">sgn-BE-NL</th><td>vgt</td></tr>
     * <tr><th scope="row">sgn-CH-DE</th><td>sgg</td></tr>
     * <tr><th scope="row">zh-guoyu</th><td>cmn</td></tr>
     * <tr><th scope="row">zh-hakka</th><td>hak</td></tr>
     * <tr><th scope="row">zh-min-nan</th><td>nan</td></tr>
     * <tr><th scope="row">zh-xiang</th><td>hsn</td></tr>
     * </tbody>
     * </table>
     *
     * <p>Legacy tags with no modern replacement will be
     * converted as follows:
     *
     * <table class="striped">
     * <caption style="display:none">Legacy tags with no modern replacement</caption>
     * <thead style="text-align:center">
     * <tr><th scope="col" style="padding: 0 2px">legacy tag</th><th scope="col" style="padding: 0 2px">converts to</th></tr>
     * </thead>
     * <tbody style="text-align:center">
     * <tr><th scope="row">cel-gaulish</th><td>xtg-x-cel-gaulish</td></tr>
     * <tr><th scope="row">en-GB-oed</th><td>en-GB-x-oed</td></tr>
     * <tr><th scope="row">i-default</th><td>en-x-i-default</td></tr>
     * <tr><th scope="row">i-enochian</th><td>und-x-i-enochian</td></tr>
     * <tr><th scope="row">i-mingo</th><td>see-x-i-mingo</td></tr>
     * <tr><th scope="row">zh-min</th><td>nan-x-zh-min</td></tr>
     * </tbody>
     * </table>
     *
     * <p>For a list of all legacy tags, see the
     * IANA Language Subtag Registry (search for "Type: grandfathered").
     *
     * <p><b>Note</b>: there is no guarantee that {@code toLanguageTag}
     * and {@code forLanguageTag} will round-trip.
     *
     * @param languageTag the language tag
     * @return The locale that best represents the language tag.
     * @throws NullPointerException if {@code languageTag} is {@code null}
     * @see #toLanguageTag()
     * @see java.util.Locale.Builder#setLanguageTag(String)
     * @since 1.7
     */
    public static Locale forLanguageTag(String languageTag) {
        LanguageTag tag = LanguageTag.parse(languageTag, null);
        InternalLocaleBuilder bldr = new InternalLocaleBuilder();
        bldr.setLanguageTag(tag);
        BaseLocale base = bldr.getBaseLocale();
        LocaleExtensions exts = bldr.getLocaleExtensions();
        if (exts == null && !base.getVariant().isEmpty()) {
            exts = getCompatibilityExtensions(base.getLanguage(), base.getScript(),
                                              base.getRegion(), base.getVariant());
        }
        return getInstance(base, exts);
    }

    /**
     * Returns a three-letter abbreviation of this locale's language.
     * If the language matches an ISO 639-1 two-letter code, the
     * corresponding ISO 639-2/T three-letter lowercase code is
     * returned.  The ISO 639-2 language codes can be found on-line,
     * see "Codes for the Representation of Names of Languages Part 2:
     * Alpha-3 Code".  If the locale specifies a three-letter
     * language, the language is returned as is.  If the locale does
     * not specify a language the empty string is returned.
     *
     * @return A three-letter abbreviation of this locale's language.
     * @throws    MissingResourceException Throws MissingResourceException if
     * three-letter language abbreviation is not available for this locale.
     */
    public String getISO3Language() throws MissingResourceException {
        String lang = baseLocale.getLanguage();
        if (lang.length() == 3) {
            return lang;
        }

        String language3 = getISO3Code(lang, LocaleISOData.isoLanguageTable);
        if (language3 == null) {
            throw new MissingResourceException("Couldn't find 3-letter language code for "
                    + lang, "FormatData_" + toString(), "ShortLanguage");
        }
        return language3;
    }

    /**
     * Returns a three-letter abbreviation for this locale's country.
     * If the country matches an ISO 3166-1 alpha-2 code, the
     * corresponding ISO 3166-1 alpha-3 uppercase code is returned.
     * If the locale doesn't specify a country, this will be the empty
     * string.
     *
     * <p>The ISO 3166-1 codes can be found on-line.
     *
     * @return A three-letter abbreviation of this locale's country.
     * @throws    MissingResourceException Throws MissingResourceException if the
     * three-letter country abbreviation is not available for this locale.
     */
    public String getISO3Country() throws MissingResourceException {
        String country3 = getISO3Code(baseLocale.getRegion(), LocaleISOData.isoCountryTable);
        if (country3 == null) {
            throw new MissingResourceException("Couldn't find 3-letter country code for "
                    + baseLocale.getRegion(), "FormatData_" + toString(), "ShortCountry");
        }
        return country3;
    }

    private static String getISO3Code(String iso2Code, String table) {
        int codeLength = iso2Code.length();
        if (codeLength == 0) {
            return "";
        }

        int tableLength = table.length();
        int index = tableLength;
        if (codeLength == 2) {
            char c1 = iso2Code.charAt(0);
            char c2 = iso2Code.charAt(1);
            for (index = 0; index < tableLength; index += 5) {
                if (table.charAt(index) == c1
                    && table.charAt(index + 1) == c2) {
                    break;
                }
            }
        }
        return index < tableLength ? table.substring(index + 2, index + 5) : null;
    }

    /**
     * Returns a name for the locale's language that is appropriate for display to the
     * user.
     * If possible, the name returned will be localized for the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale.
     * For example, if the locale is fr_FR and the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale
     * is en_US, getDisplayLanguage() will return "French"; if the locale is en_US and
     * the default {@link Locale.Category#DISPLAY DISPLAY} locale is fr_FR,
     * getDisplayLanguage() will return "anglais".
     * If the name returned cannot be localized for the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale,
     * (say, we don't have a Japanese name for Croatian),
     * this function falls back on the English name, and uses the ISO code as a last-resort
     * value.  If the locale doesn't specify a language, this function returns the empty string.
     *
     * @return The name of the display language.
     */
    public final String getDisplayLanguage() {
        return getDisplayLanguage(getDefault(Category.DISPLAY));
    }

    /**
     * Returns a name for the locale's language that is appropriate for display to the
     * user.
     * If possible, the name returned will be localized according to inLocale.
     * For example, if the locale is fr_FR and inLocale
     * is en_US, getDisplayLanguage() will return "French"; if the locale is en_US and
     * inLocale is fr_FR, getDisplayLanguage() will return "anglais".
     * If the name returned cannot be localized according to inLocale,
     * (say, we don't have a Japanese name for Croatian),
     * this function falls back on the English name, and finally
     * on the ISO code as a last-resort value.  If the locale doesn't specify a language,
     * this function returns the empty string.
     *
     * @param inLocale The locale for which to retrieve the display language.
     * @return The name of the display language appropriate to the given locale.
     * @throws    NullPointerException if {@code inLocale} is {@code null}
     */
    public String getDisplayLanguage(Locale inLocale) {
        return getDisplayString(baseLocale.getLanguage(), null, inLocale, DISPLAY_LANGUAGE);
    }

    /**
     * Returns a name for the locale's script that is appropriate for display to
     * the user. If possible, the name will be localized for the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale.  Returns
     * the empty string if this locale doesn't specify a script code.
     *
     * @return the display name of the script code for the current default
     *     {@link Locale.Category#DISPLAY DISPLAY} locale
     * @since 1.7
     */
    public String getDisplayScript() {
        return getDisplayScript(getDefault(Category.DISPLAY));
    }

    /**
     * Returns a name for the locale's script that is appropriate
     * for display to the user. If possible, the name will be
     * localized for the given locale. Returns the empty string if
     * this locale doesn't specify a script code.
     *
     * @param inLocale The locale for which to retrieve the display script.
     * @return the display name of the script code for the current default
     * {@link Locale.Category#DISPLAY DISPLAY} locale
     * @throws NullPointerException if {@code inLocale} is {@code null}
     * @since 1.7
     */
    public String getDisplayScript(Locale inLocale) {
        return getDisplayString(baseLocale.getScript(), null, inLocale, DISPLAY_SCRIPT);
    }

    /**
     * Returns a name for the locale's country that is appropriate for display to the
     * user.
     * If possible, the name returned will be localized for the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale.
     * For example, if the locale is fr_FR and the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale
     * is en_US, getDisplayCountry() will return "France"; if the locale is en_US and
     * the default {@link Locale.Category#DISPLAY DISPLAY} locale is fr_FR,
     * getDisplayCountry() will return "Etats-Unis".
     * If the name returned cannot be localized for the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale,
     * (say, we don't have a Japanese name for Croatia),
     * this function falls back on the English name, and uses the ISO code as a last-resort
     * value.  If the locale doesn't specify a country, this function returns the empty string.
     *
     * @return The name of the country appropriate to the locale.
     */
    public final String getDisplayCountry() {
        return getDisplayCountry(getDefault(Category.DISPLAY));
    }

    /**
     * Returns a name for the locale's country that is appropriate for display to the
     * user.
     * If possible, the name returned will be localized according to inLocale.
     * For example, if the locale is fr_FR and inLocale
     * is en_US, getDisplayCountry() will return "France"; if the locale is en_US and
     * inLocale is fr_FR, getDisplayCountry() will return "Etats-Unis".
     * If the name returned cannot be localized according to inLocale.
     * (say, we don't have a Japanese name for Croatia),
     * this function falls back on the English name, and finally
     * on the ISO code as a last-resort value.  If the locale doesn't specify a country,
     * this function returns the empty string.
     *
     * @param inLocale The locale for which to retrieve the display country.
     * @return The name of the country appropriate to the given locale.
     * @throws    NullPointerException if {@code inLocale} is {@code null}
     */
    public String getDisplayCountry(Locale inLocale) {
        return getDisplayString(baseLocale.getRegion(), null, inLocale, DISPLAY_COUNTRY);
    }

    private String getDisplayString(String code, String cat, Locale inLocale, int type) {
        Objects.requireNonNull(inLocale);
        Objects.requireNonNull(code);

        if (code.isEmpty()) {
            return "";
        }

        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(LocaleNameProvider.class);
        String rbKey = (type == DISPLAY_VARIANT ? "%%"+code : code);
        String result = pool.getLocalizedObject(
                                LocaleNameGetter.INSTANCE,
                                inLocale, rbKey, type, code, cat);
        return result != null ? result : code;
    }

    /**
     * Returns a name for the locale's variant code that is appropriate for display to the
     * user.  If possible, the name will be localized for the default
     * {@link Locale.Category#DISPLAY DISPLAY} locale.  If the locale
     * doesn't specify a variant code, this function returns the empty string.
     *
     * @return The name of the display variant code appropriate to the locale.
     */
    public final String getDisplayVariant() {
        return getDisplayVariant(getDefault(Category.DISPLAY));
    }

    /**
     * Returns a name for the locale's variant code that is appropriate for display to the
     * user.  If possible, the name will be localized for inLocale.  If the locale
     * doesn't specify a variant code, this function returns the empty string.
     *
     * @param inLocale The locale for which to retrieve the display variant code.
     * @return The name of the display variant code appropriate to the given locale.
     * @throws    NullPointerException if {@code inLocale} is {@code null}
     */
    public String getDisplayVariant(Locale inLocale) {
        if (baseLocale.getVariant().isEmpty())
            return "";

        LocaleResources lr = LocaleProviderAdapter
            .getResourceBundleBased()
            .getLocaleResources(inLocale);

        String names[] = getDisplayVariantArray(inLocale);

        // Get the localized patterns for formatting a list, and use
        // them to format the list.
        return formatList(names,
                          lr.getLocaleName("ListCompositionPattern"));
    }

    /**
     * Returns a name for the locale that is appropriate for display to the
     * user. This will be the values returned by getDisplayLanguage(),
     * getDisplayScript(), getDisplayCountry(), getDisplayVariant() and
     * optional <a href="./Locale.html#def_locale_extension">Unicode extensions</a>
     * assembled into a single string. The non-empty values are used in order, with
     * the second and subsequent names in parentheses.  For example:
     * <blockquote>
     * language (script, country, variant(, extension)*)<br>
     * language (country(, extension)*)<br>
     * language (variant(, extension)*)<br>
     * script (country(, extension)*)<br>
     * country (extension)*<br>
     * </blockquote>
     * depending on which fields are specified in the locale. The field
     * separator in the above parentheses, denoted as a comma character, may
     * be localized depending on the locale. If the language, script, country,
     * and variant fields are all empty, this function returns the empty string.
     *
     * @return The name of the locale appropriate to display.
     */
    public final String getDisplayName() {
        return getDisplayName(getDefault(Category.DISPLAY));
    }

    /**
     * Returns a name for the locale that is appropriate for display
     * to the user.  This will be the values returned by
     * getDisplayLanguage(), getDisplayScript(),getDisplayCountry()
     * getDisplayVariant(), and optional <a href="./Locale.html#def_locale_extension">
     * Unicode extensions</a> assembled into a single string. The non-empty
     * values are used in order, with the second and subsequent names in
     * parentheses.  For example:
     * <blockquote>
     * language (script, country, variant(, extension)*)<br>
     * language (country(, extension)*)<br>
     * language (variant(, extension)*)<br>
     * script (country(, extension)*)<br>
     * country (extension)*<br>
     * </blockquote>
     * depending on which fields are specified in the locale. The field
     * separator in the above parentheses, denoted as a comma character, may
     * be localized depending on the locale. If the language, script, country,
     * and variant fields are all empty, this function returns the empty string.
     *
     * @param inLocale The locale for which to retrieve the display name.
     * @return The name of the locale appropriate to display.
     * @throws NullPointerException if {@code inLocale} is {@code null}
     */
    public String getDisplayName(Locale inLocale) {
        LocaleResources lr =  LocaleProviderAdapter
            .getResourceBundleBased()
            .getLocaleResources(inLocale);

        String languageName = getDisplayLanguage(inLocale);
        String scriptName = getDisplayScript(inLocale);
        String countryName = getDisplayCountry(inLocale);
        String[] variantNames = getDisplayVariantArray(inLocale);

        // Get the localized patterns for formatting a display name.
        String displayNamePattern = lr.getLocaleName("DisplayNamePattern");
        String listCompositionPattern = lr.getLocaleName("ListCompositionPattern");

        // The display name consists of a main name, followed by qualifiers.
        // Typically, the format is "MainName (Qualifier, Qualifier)" but this
        // depends on what pattern is stored in the display locale.
        String   mainName;
        String[] qualifierNames;

        // The main name is the language, or if there is no language, the script,
        // then if no script, the country. If there is no language/script/country
        // (an anomalous situation) then the display name is simply the variant's
        // display name.
        if (languageName.isEmpty() && scriptName.isEmpty() && countryName.isEmpty()) {
            if (variantNames.length == 0) {
                return "";
            } else {
                return formatList(variantNames, listCompositionPattern);
            }
        }
        ArrayList<String> names = new ArrayList<>(4);
        if (!languageName.isEmpty()) {
            names.add(languageName);
        }
        if (!scriptName.isEmpty()) {
            names.add(scriptName);
        }
        if (!countryName.isEmpty()) {
            names.add(countryName);
        }
        if (variantNames.length != 0) {
            names.addAll(Arrays.asList(variantNames));
        }

        // add Unicode extensions
        if (localeExtensions != null) {
            localeExtensions.getUnicodeLocaleAttributes().stream()
                .map(key -> getDisplayString(key, null, inLocale, DISPLAY_UEXT_KEY))
                .forEach(names::add);
            localeExtensions.getUnicodeLocaleKeys().stream()
                .map(key -> getDisplayKeyTypeExtensionString(key, lr, inLocale))
                .forEach(names::add);
        }

        // The first one in the main name
        mainName = names.get(0);

        // Others are qualifiers
        int numNames = names.size();
        qualifierNames = (numNames > 1) ?
                names.subList(1, numNames).toArray(new String[numNames - 1]) : new String[0];

        // Create an array whose first element is the number of remaining
        // elements.  This serves as a selector into a ChoiceFormat pattern from
        // the resource.  The second and third elements are the main name and
        // the qualifier; if there are no qualifiers, the third element is
        // unused by the format pattern.
        Object[] displayNames = {
            qualifierNames.length != 0 ? 2 : 1,
            mainName,
            // We could also just call formatList() and have it handle the empty
            // list case, but this is more efficient, and we want it to be
            // efficient since all the language-only locales will not have any
            // qualifiers.
            qualifierNames.length != 0 ? formatList(qualifierNames, listCompositionPattern) : null
        };

        if (displayNamePattern != null) {
            return new MessageFormat(displayNamePattern).format(displayNames);
        }
        else {
            // If we cannot get the message format pattern, then we use a simple
            // hard-coded pattern.  This should not occur in practice unless the
            // installation is missing some core files (FormatData etc.).
            StringBuilder result = new StringBuilder();
            result.append((String)displayNames[1]);
            if (displayNames.length > 2) {
                result.append(" (");
                result.append((String)displayNames[2]);
                result.append(')');
            }
            return result.toString();
        }
    }

    /**
     * Overrides Cloneable.
     */
    @Override
    public Object clone()
    {
        try {
            Locale that = (Locale)super.clone();
            return that;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Override hashCode.
     * Since Locales are often used in hashtables, caches the value
     * for speed.
     */
    @Override
    public int hashCode() {
        int hc = hashCodeValue;
        if (hc == 0) {
            hc = baseLocale.hashCode();
            if (localeExtensions != null) {
                hc ^= localeExtensions.hashCode();
            }
            hashCodeValue = hc;
        }
        return hc;
    }

    // Overrides

    /**
     * Returns true if this Locale is equal to another object.  A Locale is
     * deemed equal to another Locale with identical language, script, country,
     * variant and extensions, and unequal to all other objects.
     *
     * @return true if this Locale is equal to the specified object.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)                      // quick check
            return true;
        if (!(obj instanceof Locale))
            return false;
        BaseLocale otherBase = ((Locale)obj).baseLocale;
        if (!baseLocale.equals(otherBase)) {
            return false;
        }
        if (localeExtensions == null) {
            return ((Locale)obj).localeExtensions == null;
        }
        return localeExtensions.equals(((Locale)obj).localeExtensions);
    }

    // ================= privates =====================================

    private transient BaseLocale baseLocale;
    private transient LocaleExtensions localeExtensions;

    /**
     * Calculated hashcode
     */
    private transient volatile int hashCodeValue;

    private static volatile Locale defaultLocale = initDefault();
    private static volatile Locale defaultDisplayLocale;
    private static volatile Locale defaultFormatLocale;

    private transient volatile String languageTag;

    /**
     * Return an array of the display names of the variant.
     * @param inLocale The locale for which to retrieve the display variant.
     * @return an array of display names, possible of zero length.
     */
    private String[] getDisplayVariantArray(Locale inLocale) {
        // Split the variant name into tokens separated by '_'.
        StringTokenizer tokenizer = new StringTokenizer(baseLocale.getVariant(), "_");
        String[] names = new String[tokenizer.countTokens()];

        // For each variant token, lookup the display name.  If
        // not found, use the variant name itself.
        for (int i=0; i<names.length; ++i) {
            names[i] = getDisplayString(tokenizer.nextToken(), null,
                                inLocale, DISPLAY_VARIANT);
        }

        return names;
    }

    private String getDisplayKeyTypeExtensionString(String key, LocaleResources lr, Locale inLocale) {
        String type = localeExtensions.getUnicodeLocaleType(key);
        String ret = getDisplayString(type, key, inLocale, DISPLAY_UEXT_TYPE);

        if (ret == null || ret.equals(type)) {
            // no localization for this type. try combining key/type separately
            String displayType = type;
            switch (key) {
            case "cu":
                displayType = lr.getCurrencyName(type.toLowerCase(Locale.ROOT));
                break;
            case "rg":
                if (type != null &&
                    // UN M.49 code should not be allowed here
                    type.matches("^[a-zA-Z]{2}[zZ]{4}$")) {
                        displayType = lr.getLocaleName(type.substring(0, 2).toUpperCase(Locale.ROOT));
                }
                break;
            case "tz":
                displayType = TimeZoneNameUtility.convertLDMLShortID(type)
                    .map(id -> TimeZoneNameUtility.retrieveGenericDisplayName(id, TimeZone.LONG, inLocale))
                    .orElse(type);
                break;
            }
            ret = MessageFormat.format(lr.getLocaleName("ListKeyTypePattern"),
                getDisplayString(key, null, inLocale, DISPLAY_UEXT_KEY),
                Optional.ofNullable(displayType).orElse(type));
        }

        return ret;
    }

    /**
     * Format a list using given pattern strings.
     * If either of the patterns is null, then a the list is
     * formatted by concatenation with the delimiter ','.
     * @param stringList the list of strings to be formatted.
     * and formatting them into a list.
     * @param pattern should take 2 arguments for reduction
     * @return a string representing the list.
     */
    private static String formatList(String[] stringList, String pattern) {
        // If we have no list patterns, compose the list in a simple,
        // non-localized way.
        if (pattern == null) {
            return Arrays.stream(stringList).collect(Collectors.joining(","));
        }

        return switch (stringList.length) {
            case 0 -> "";
            case 1 -> stringList[0];
            default -> Arrays.stream(stringList).reduce("",
                (s1, s2) -> {
                    if (s1.isEmpty()) {
                        return s2;
                    }
                    if (s2.isEmpty()) {
                        return s1;
                    }
                    return MessageFormat.format(pattern, s1, s2);
                });
        };
    }

    // Duplicate of sun.util.locale.UnicodeLocaleExtension.isKey in order to
    // avoid its class loading.
    private static boolean isUnicodeExtensionKey(String s) {
        // 2alphanum
        return (s.length() == 2) && LocaleUtils.isAlphaNumericString(s);
    }

    /**
     * @serialField language    String
     *      language subtag in lower case.
     *      (See <a href="java.base/java/util/Locale.html#getLanguage()">getLanguage()</a>)
     * @serialField country     String
     *      country subtag in upper case.
     *      (See <a href="java.base/java/util/Locale.html#getCountry()">getCountry()</a>)
     * @serialField variant     String
     *      variant subtags separated by LOWLINE characters.
     *      (See <a href="java.base/java/util/Locale.html#getVariant()">getVariant()</a>)
     * @serialField hashcode    int
     *      deprecated, for forward compatibility only
     * @serialField script      String
     *      script subtag in title case
     *      (See <a href="java.base/java/util/Locale.html#getScript()">getScript()</a>)
     * @serialField extensions  String
     *      canonical representation of extensions, that is,
     *      BCP47 extensions in alphabetical order followed by
     *      BCP47 private use subtags, all in lower case letters
     *      separated by HYPHEN-MINUS characters.
     *      (See <a href="java.base/java/util/Locale.html#getExtensionKeys()">getExtensionKeys()</a>,
     *      <a href="java.base/java/util/Locale.html#getExtension(char)">getExtension(char)</a>)
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("language", String.class),
        new ObjectStreamField("country", String.class),
        new ObjectStreamField("variant", String.class),
        new ObjectStreamField("hashcode", int.class),
        new ObjectStreamField("script", String.class),
        new ObjectStreamField("extensions", String.class),
    };

    /**
     * Serializes this {@code Locale} to the specified {@code ObjectOutputStream}.
     * @param out the {@code ObjectOutputStream} to write
     * @throws IOException
     * @since 1.7
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("language", baseLocale.getLanguage());
        fields.put("script", baseLocale.getScript());
        fields.put("country", baseLocale.getRegion());
        fields.put("variant", baseLocale.getVariant());
        fields.put("extensions", localeExtensions == null ? "" : localeExtensions.getID());
        fields.put("hashcode", -1); // place holder just for backward support
        out.writeFields();
    }

    /**
     * Deserializes this {@code Locale}.
     * @param in the {@code ObjectInputStream} to read
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws IllformedLocaleException
     * @since 1.7
     */
    @java.io.Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = in.readFields();
        String language = (String)fields.get("language", "");
        String script = (String)fields.get("script", "");
        String country = (String)fields.get("country", "");
        String variant = (String)fields.get("variant", "");
        String extStr = (String)fields.get("extensions", "");

        baseLocale = BaseLocale.getInstance(convertOldISOCodes(language), script, country, variant);
        if (!extStr.isEmpty()) {
            try {
                InternalLocaleBuilder bldr = new InternalLocaleBuilder();
                bldr.setExtensions(extStr);
                localeExtensions = bldr.getLocaleExtensions();
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage());
            }
        } else {
            localeExtensions = null;
        }
    }

    /**
     * Returns a cached {@code Locale} instance equivalent to
     * the deserialized {@code Locale}. When serialized
     * language, country and variant fields read from the object data stream
     * are exactly "ja", "JP", "JP" or "th", "TH", "TH" and script/extensions
     * fields are empty, this method supplies {@code UNICODE_LOCALE_EXTENSION}
     * "ca"/"japanese" (calendar type is "japanese") or "nu"/"thai" (number script
     * type is "thai"). See <a href="Locale.html#special_cases_constructor">Special Cases</a>
     * for more information.
     *
     * @return an instance of {@code Locale} equivalent to
     * the deserialized {@code Locale}.
     * @throws java.io.ObjectStreamException
     */
    @java.io.Serial
    private Object readResolve() throws java.io.ObjectStreamException {
        return getInstance(baseLocale.getLanguage(), baseLocale.getScript(),
                baseLocale.getRegion(), baseLocale.getVariant(), localeExtensions);
    }

    private static volatile String[] isoLanguages;

    private static volatile String[] isoCountries;

    private static String convertOldISOCodes(String language) {
        // we accept both the old and the new ISO codes for the languages whose ISO
        // codes have changed, but we always store the NEW code, unless the property
        // java.locale.useOldISOCodes is set to "true"
        return BaseLocale.convertOldISOCodes(LocaleUtils.toLowerString(language).intern());
    }

    private static LocaleExtensions getCompatibilityExtensions(String language,
                                                               String script,
                                                               String country,
                                                               String variant) {
        LocaleExtensions extensions = null;
        // Special cases for backward compatibility support
        if (LocaleUtils.caseIgnoreMatch(language, "ja")
                && script.isEmpty()
                && LocaleUtils.caseIgnoreMatch(country, "jp")
                && "JP".equals(variant)) {
            // ja_JP_JP -> u-ca-japanese (calendar = japanese)
            extensions = LocaleExtensions.CALENDAR_JAPANESE;
        } else if (LocaleUtils.caseIgnoreMatch(language, "th")
                && script.isEmpty()
                && LocaleUtils.caseIgnoreMatch(country, "th")
                && "TH".equals(variant)) {
            // th_TH_TH -> u-nu-thai (numbersystem = thai)
            extensions = LocaleExtensions.NUMBER_THAI;
        }
        return extensions;
    }

    /**
     * Obtains a localized locale names from a LocaleNameProvider
     * implementation.
     */
    private static class LocaleNameGetter
        implements LocaleServiceProviderPool.LocalizedObjectGetter<LocaleNameProvider, String> {
        private static final LocaleNameGetter INSTANCE = new LocaleNameGetter();

        @Override
        public String getObject(LocaleNameProvider localeNameProvider,
                                Locale locale,
                                String key,
                                Object... params) {
            assert params.length == 3;
            int type = (Integer)params[0];
            String code = (String)params[1];
            String cat = (String)params[2];

            switch(type) {
            case DISPLAY_LANGUAGE:
                return localeNameProvider.getDisplayLanguage(code, locale);
            case DISPLAY_COUNTRY:
                return localeNameProvider.getDisplayCountry(code, locale);
            case DISPLAY_VARIANT:
                return localeNameProvider.getDisplayVariant(code, locale);
            case DISPLAY_SCRIPT:
                return localeNameProvider.getDisplayScript(code, locale);
            case DISPLAY_UEXT_KEY:
                return localeNameProvider.getDisplayUnicodeExtensionKey(code, locale);
            case DISPLAY_UEXT_TYPE:
                return localeNameProvider.getDisplayUnicodeExtensionType(code, cat, locale);
            default:
                assert false; // shouldn't happen
            }

            return null;
        }
    }

    /**
     * Enum for locale categories.  These locale categories are used to get/set
     * the default locale for the specific functionality represented by the
     * category.
     *
     * @see #getDefault(Locale.Category)
     * @see #setDefault(Locale.Category, Locale)
     * @since 1.7
     */
    public enum Category {

        /**
         * Category used to represent the default locale for
         * displaying user interfaces.
         */
        DISPLAY("user.language.display",
                "user.script.display",
                "user.country.display",
                "user.variant.display",
                "user.extensions.display"),

        /**
         * Category used to represent the default locale for
         * formatting dates, numbers, and/or currencies.
         */
        FORMAT("user.language.format",
               "user.script.format",
               "user.country.format",
               "user.variant.format",
               "user.extensions.format");

        Category(String languageKey, String scriptKey, String countryKey,
                String variantKey, String extensionsKey) {
            this.languageKey = languageKey;
            this.scriptKey = scriptKey;
            this.countryKey = countryKey;
            this.variantKey = variantKey;
            this.extensionsKey = extensionsKey;
        }

        final String languageKey;
        final String scriptKey;
        final String countryKey;
        final String variantKey;
        final String extensionsKey;
    }

    /**
     * {@code Builder} is used to build instances of {@code Locale}
     * from values configured by the setters.  Unlike the {@code Locale}
     * constructors, the {@code Builder} checks if a value configured by a
     * setter satisfies the syntax requirements defined by the {@code Locale}
     * class.  A {@code Locale} object created by a {@code Builder} is
     * well-formed and can be transformed to a well-formed IETF BCP 47 language tag
     * without losing information.
     *
     * <p><b>Note:</b> The {@code Locale} class does not provide any
     * syntactic restrictions on variant, while BCP 47 requires each variant
     * subtag to be 5 to 8 alphanumerics or a single numeric followed by 3
     * alphanumerics.  The method {@code setVariant} throws
     * {@code IllformedLocaleException} for a variant that does not satisfy
     * this restriction. If it is necessary to support such a variant, use a
     * Locale constructor.  However, keep in mind that a {@code Locale}
     * object created this way might lose the variant information when
     * transformed to a BCP 47 language tag.
     *
     * <p>The following example shows how to create a {@code Locale} object
     * with the {@code Builder}.
     * <blockquote>
     * <pre>
     *     Locale aLocale = new Builder().setLanguage("sr").setScript("Latn").setRegion("RS").build();
     * </pre>
     * </blockquote>
     *
     * <p>Builders can be reused; {@code clear()} resets all
     * fields to their default values.
     *
     * @see Locale#forLanguageTag
     * @since 1.7
     */
    public static final class Builder {
        private final InternalLocaleBuilder localeBuilder;

        /**
         * Constructs an empty Builder. The default value of all
         * fields, extensions, and private use information is the
         * empty string.
         */
        public Builder() {
            localeBuilder = new InternalLocaleBuilder();
        }

        /**
         * Resets the {@code Builder} to match the provided
         * {@code locale}.  Existing state is discarded.
         *
         * <p>All fields of the locale must be well-formed, see {@link Locale}.
         *
         * <p>Locales with any ill-formed fields cause
         * {@code IllformedLocaleException} to be thrown, except for the
         * following three cases which are accepted for compatibility
         * reasons:<ul>
         * <li>Locale("ja", "JP", "JP") is treated as "ja-JP-u-ca-japanese"
         * <li>Locale("th", "TH", "TH") is treated as "th-TH-u-nu-thai"
         * <li>Locale("no", "NO", "NY") is treated as "nn-NO"</ul>
         *
         * @param locale the locale
         * @return This builder.
         * @throws IllformedLocaleException if {@code locale} has
         * any ill-formed fields.
         * @throws NullPointerException if {@code locale} is null.
         */
        public Builder setLocale(Locale locale) {
            try {
                localeBuilder.setLocale(locale.baseLocale, locale.localeExtensions);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Resets the Builder to match the provided IETF BCP 47
         * language tag.  Discards the existing state.  Null and the
         * empty string cause the builder to be reset, like {@link
         * #clear}.  Legacy tags (see {@link
         * Locale#forLanguageTag}) are converted to their canonical
         * form before being processed.  Otherwise, the language tag
         * must be well-formed (see {@link Locale}) or an exception is
         * thrown (unlike {@code Locale.forLanguageTag}, which
         * just discards ill-formed and following portions of the
         * tag).
         *
         * @param languageTag the language tag
         * @return This builder.
         * @throws IllformedLocaleException if {@code languageTag} is ill-formed
         * @see Locale#forLanguageTag(String)
         */
        public Builder setLanguageTag(String languageTag) {
            ParseStatus sts = new ParseStatus();
            LanguageTag tag = LanguageTag.parse(languageTag, sts);
            if (sts.isError()) {
                throw new IllformedLocaleException(sts.getErrorMessage(), sts.getErrorIndex());
            }
            localeBuilder.setLanguageTag(tag);
            return this;
        }

        /**
         * Sets the language.  If {@code language} is the empty string or
         * null, the language in this {@code Builder} is removed.  Otherwise,
         * the language must be <a href="./Locale.html#def_language">well-formed</a>
         * or an exception is thrown.
         *
         * <p>The typical language value is a two or three-letter language
         * code as defined in ISO639.
         *
         * @param language the language
         * @return This builder.
         * @throws IllformedLocaleException if {@code language} is ill-formed
         */
        public Builder setLanguage(String language) {
            try {
                localeBuilder.setLanguage(language);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the script. If {@code script} is null or the empty string,
         * the script in this {@code Builder} is removed.
         * Otherwise, the script must be <a href="./Locale.html#def_script">well-formed</a> or an
         * exception is thrown.
         *
         * <p>The typical script value is a four-letter script code as defined by ISO 15924.
         *
         * @param script the script
         * @return This builder.
         * @throws IllformedLocaleException if {@code script} is ill-formed
         */
        public Builder setScript(String script) {
            try {
                localeBuilder.setScript(script);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the region.  If region is null or the empty string, the region
         * in this {@code Builder} is removed.  Otherwise,
         * the region must be <a href="./Locale.html#def_region">well-formed</a> or an
         * exception is thrown.
         *
         * <p>The typical region value is a two-letter ISO 3166 code or a
         * three-digit UN M.49 area code.
         *
         * <p>The country value in the {@code Locale} created by the
         * {@code Builder} is always normalized to upper case.
         *
         * @param region the region
         * @return This builder.
         * @throws IllformedLocaleException if {@code region} is ill-formed
         */
        public Builder setRegion(String region) {
            try {
                localeBuilder.setRegion(region);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the variant.  If variant is null or the empty string, the
         * variant in this {@code Builder} is removed.  Otherwise, it
         * must consist of one or more <a href="./Locale.html#def_variant">well-formed</a>
         * subtags, or an exception is thrown.
         *
         * <p><b>Note:</b> This method checks if {@code variant}
         * satisfies the IETF BCP 47 variant subtag's syntax requirements,
         * and normalizes the value to lowercase letters.  However,
         * the {@code Locale} class does not impose any syntactic
         * restriction on variant, and the variant value in
         * {@code Locale} is case sensitive.  To set such a variant,
         * use a Locale constructor.
         *
         * @param variant the variant
         * @return This builder.
         * @throws IllformedLocaleException if {@code variant} is ill-formed
         */
        public Builder setVariant(String variant) {
            try {
                localeBuilder.setVariant(variant);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the extension for the given key. If the value is null or the
         * empty string, the extension is removed.  Otherwise, the extension
         * must be <a href="./Locale.html#def_extensions">well-formed</a> or an exception
         * is thrown.
         *
         * <p><b>Note:</b> The key {@link Locale#UNICODE_LOCALE_EXTENSION
         * UNICODE_LOCALE_EXTENSION} ('u') is used for the Unicode locale extension.
         * Setting a value for this key replaces any existing Unicode locale key/type
         * pairs with those defined in the extension.
         *
         * <p><b>Note:</b> The key {@link Locale#PRIVATE_USE_EXTENSION
         * PRIVATE_USE_EXTENSION} ('x') is used for the private use code. To be
         * well-formed, the value for this key needs only to have subtags of one to
         * eight alphanumeric characters, not two to eight as in the general case.
         *
         * @param key the extension key
         * @param value the extension value
         * @return This builder.
         * @throws IllformedLocaleException if {@code key} is illegal
         * or {@code value} is ill-formed
         * @see #setUnicodeLocaleKeyword(String, String)
         */
        public Builder setExtension(char key, String value) {
            try {
                localeBuilder.setExtension(key, value);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the Unicode locale keyword type for the given key.  If the type
         * is null, the Unicode keyword is removed.  Otherwise, the key must be
         * non-null and both key and type must be <a
         * href="./Locale.html#def_locale_extension">well-formed</a> or an exception
         * is thrown.
         *
         * <p>Keys and types are converted to lower case.
         *
         * <p><b>Note</b>:Setting the 'u' extension via {@link #setExtension}
         * replaces all Unicode locale keywords with those defined in the
         * extension.
         *
         * @param key the Unicode locale key
         * @param type the Unicode locale type
         * @return This builder.
         * @throws IllformedLocaleException if {@code key} or {@code type}
         * is ill-formed
         * @throws NullPointerException if {@code key} is null
         * @see #setExtension(char, String)
         */
        public Builder setUnicodeLocaleKeyword(String key, String type) {
            try {
                localeBuilder.setUnicodeLocaleKeyword(key, type);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Adds a unicode locale attribute, if not already present, otherwise
         * has no effect.  The attribute must not be null and must be <a
         * href="./Locale.html#def_locale_extension">well-formed</a> or an exception
         * is thrown.
         *
         * @param attribute the attribute
         * @return This builder.
         * @throws NullPointerException if {@code attribute} is null
         * @throws IllformedLocaleException if {@code attribute} is ill-formed
         * @see #setExtension(char, String)
         */
        public Builder addUnicodeLocaleAttribute(String attribute) {
            try {
                localeBuilder.addUnicodeLocaleAttribute(attribute);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Removes a unicode locale attribute, if present, otherwise has no
         * effect.  The attribute must not be null and must be <a
         * href="./Locale.html#def_locale_extension">well-formed</a> or an exception
         * is thrown.
         *
         * <p>Attribute comparison for removal is case-insensitive.
         *
         * @param attribute the attribute
         * @return This builder.
         * @throws NullPointerException if {@code attribute} is null
         * @throws IllformedLocaleException if {@code attribute} is ill-formed
         * @see #setExtension(char, String)
         */
        public Builder removeUnicodeLocaleAttribute(String attribute) {
            Objects.requireNonNull(attribute);
            try {
                localeBuilder.removeUnicodeLocaleAttribute(attribute);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Resets the builder to its initial, empty state.
         *
         * @return This builder.
         */
        public Builder clear() {
            localeBuilder.clear();
            return this;
        }

        /**
         * Resets the extensions to their initial, empty state.
         * Language, script, region and variant are unchanged.
         *
         * @return This builder.
         * @see #setExtension(char, String)
         */
        public Builder clearExtensions() {
            localeBuilder.clearExtensions();
            return this;
        }

        /**
         * Returns an instance of {@code Locale} created from the fields set
         * on this builder.
         *
         * <p>This applies the conversions listed in {@link Locale#forLanguageTag}
         * when constructing a Locale. (Legacy tags are handled in
         * {@link #setLanguageTag}.)
         *
         * @return A Locale.
         */
        public Locale build() {
            BaseLocale baseloc = localeBuilder.getBaseLocale();
            LocaleExtensions extensions = localeBuilder.getLocaleExtensions();
            if (extensions == null && !baseloc.getVariant().isEmpty()) {
                extensions = getCompatibilityExtensions(baseloc.getLanguage(), baseloc.getScript(),
                        baseloc.getRegion(), baseloc.getVariant());
            }
            return Locale.getInstance(baseloc, extensions);
        }
    }

    /**
     * This enum provides constants to select a filtering mode for locale
     * matching. Refer to <a href="http://tools.ietf.org/html/rfc4647">RFC 4647
     * Matching of Language Tags</a> for details.
     *
     * <p>As an example, think of two Language Priority Lists each of which
     * includes only one language range and a set of following language tags:
     *
     * <pre>
     *    de (German)
     *    de-DE (German, Germany)
     *    de-Deva (German, in Devanagari script)
     *    de-Deva-DE (German, in Devanagari script, Germany)
     *    de-DE-1996 (German, Germany, orthography of 1996)
     *    de-Latn-DE (German, in Latin script, Germany)
     *    de-Latn-DE-1996 (German, in Latin script, Germany, orthography of 1996)
     * </pre>
     *
     * The filtering method will behave as follows:
     *
     * <table class="striped">
     * <caption>Filtering method behavior</caption>
     * <thead>
     * <tr>
     * <th scope="col">Filtering Mode</th>
     * <th scope="col">Language Priority List: {@code "de-DE"}</th>
     * <th scope="col">Language Priority List: {@code "de-*-DE"}</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row" style="vertical-align:top">
     * {@link FilteringMode#AUTOSELECT_FILTERING AUTOSELECT_FILTERING}
     * </th>
     * <td style="vertical-align:top">
     * Performs <em>basic</em> filtering and returns {@code "de-DE"} and
     * {@code "de-DE-1996"}.
     * </td>
     * <td style="vertical-align:top">
     * Performs <em>extended</em> filtering and returns {@code "de-DE"},
     * {@code "de-Deva-DE"}, {@code "de-DE-1996"}, {@code "de-Latn-DE"}, and
     * {@code "de-Latn-DE-1996"}.
     * </td>
     * </tr>
     * <tr>
     * <th scope="row" style="vertical-align:top">
     * {@link FilteringMode#EXTENDED_FILTERING EXTENDED_FILTERING}
     * </th>
     * <td style="vertical-align:top">
     * Performs <em>extended</em> filtering and returns {@code "de-DE"},
     * {@code "de-Deva-DE"}, {@code "de-DE-1996"}, {@code "de-Latn-DE"}, and
     * {@code "de-Latn-DE-1996"}.
     * </td>
     * <td style="vertical-align:top">Same as above.</td>
     * </tr>
     * <tr>
     * <th scope="row" style="vertical-align:top">
     * {@link FilteringMode#IGNORE_EXTENDED_RANGES IGNORE_EXTENDED_RANGES}
     * </th>
     * <td style="vertical-align:top">
     * Performs <em>basic</em> filtering and returns {@code "de-DE"} and
     * {@code "de-DE-1996"}.
     * </td>
     * <td style="vertical-align:top">
     * Performs <em>basic</em> filtering and returns {@code null} because
     * nothing matches.
     * </td>
     * </tr>
     * <tr>
     * <th scope="row" style="vertical-align:top">
     * {@link FilteringMode#MAP_EXTENDED_RANGES MAP_EXTENDED_RANGES}
     * </th>
     * <td style="vertical-align:top">Same as above.</td>
     * <td style="vertical-align:top">
     * Performs <em>basic</em> filtering and returns {@code "de-DE"} and
     * {@code "de-DE-1996"} because {@code "de-*-DE"} is mapped to
     * {@code "de-DE"}.
     * </td>
     * </tr>
     * <tr>
     * <th scope="row" style="vertical-align:top">
     * {@link FilteringMode#REJECT_EXTENDED_RANGES REJECT_EXTENDED_RANGES}
     * </th>
     * <td style="vertical-align:top">Same as above.</td>
     * <td style="vertical-align:top">
     * Throws {@link IllegalArgumentException} because {@code "de-*-DE"} is
     * not a valid basic language range.
     * </td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @see #filter(List, Collection, FilteringMode)
     * @see #filterTags(List, Collection, FilteringMode)
     *
     * @since 1.8
     */
    public static enum FilteringMode {
        /**
         * Specifies automatic filtering mode based on the given Language
         * Priority List consisting of language ranges. If all of the ranges
         * are basic, basic filtering is selected. Otherwise, extended
         * filtering is selected.
         */
        AUTOSELECT_FILTERING,

        /**
         * Specifies extended filtering.
         */
        EXTENDED_FILTERING,

        /**
         * Specifies basic filtering: Note that any extended language ranges
         * included in the given Language Priority List are ignored.
         */
        IGNORE_EXTENDED_RANGES,

        /**
         * Specifies basic filtering: If any extended language ranges are
         * included in the given Language Priority List, they are mapped to the
         * basic language range. Specifically, a language range starting with a
         * subtag {@code "*"} is treated as a language range {@code "*"}. For
         * example, {@code "*-US"} is treated as {@code "*"}. If {@code "*"} is
         * not the first subtag, {@code "*"} and extra {@code "-"} are removed.
         * For example, {@code "ja-*-JP"} is mapped to {@code "ja-JP"}.
         */
        MAP_EXTENDED_RANGES,

        /**
         * Specifies basic filtering: If any extended language ranges are
         * included in the given Language Priority List, the list is rejected
         * and the filtering method throws {@link IllegalArgumentException}.
         */
        REJECT_EXTENDED_RANGES
    };

    /**
     * This class expresses a <em>Language Range</em> defined in
     * <a href="http://tools.ietf.org/html/rfc4647">RFC 4647 Matching of
     * Language Tags</a>. A language range is an identifier which is used to
     * select language tag(s) meeting specific requirements by using the
     * mechanisms described in <a href="Locale.html#LocaleMatching">Locale
     * Matching</a>. A list which represents a user's preferences and consists
     * of language ranges is called a <em>Language Priority List</em>.
     *
     * <p>There are two types of language ranges: basic and extended. In RFC
     * 4647, the syntax of language ranges is expressed in
     * <a href="http://tools.ietf.org/html/rfc4234">ABNF</a> as follows:
     * <blockquote>
     * <pre>
     *     basic-language-range    = (1*8ALPHA *("-" 1*8alphanum)) / "*"
     *     extended-language-range = (1*8ALPHA / "*")
     *                               *("-" (1*8alphanum / "*"))
     *     alphanum                = ALPHA / DIGIT
     * </pre>
     * </blockquote>
     * For example, {@code "en"} (English), {@code "ja-JP"} (Japanese, Japan),
     * {@code "*"} (special language range which matches any language tag) are
     * basic language ranges, whereas {@code "*-CH"} (any languages,
     * Switzerland), {@code "es-*"} (Spanish, any regions), and
     * {@code "zh-Hant-*"} (Traditional Chinese, any regions) are extended
     * language ranges.
     *
     * @see #filter
     * @see #filterTags
     * @see #lookup
     * @see #lookupTag
     *
     * @since 1.8
     */
    public static final class LanguageRange {

       /**
        * A constant holding the maximum value of weight, 1.0, which indicates
        * that the language range is a good fit for the user.
        */
        public static final double MAX_WEIGHT = 1.0;

       /**
        * A constant holding the minimum value of weight, 0.0, which indicates
        * that the language range is not a good fit for the user.
        */
        public static final double MIN_WEIGHT = 0.0;

        private final String range;
        private final double weight;

        private volatile int hash;

        /**
         * Constructs a {@code LanguageRange} using the given {@code range}.
         * Note that no validation is done against the IANA Language Subtag
         * Registry at time of construction.
         *
         * <p>This is equivalent to {@code LanguageRange(range, MAX_WEIGHT)}.
         *
         * @param range a language range
         * @throws NullPointerException if the given {@code range} is
         *     {@code null}
         * @throws IllegalArgumentException if the given {@code range} does not
         * comply with the syntax of the language range mentioned in RFC 4647
         */
        public LanguageRange(String range) {
            this(range, MAX_WEIGHT);
        }

        /**
         * Constructs a {@code LanguageRange} using the given {@code range} and
         * {@code weight}. Note that no validation is done against the IANA
         * Language Subtag Registry at time of construction.
         *
         * @param range  a language range
         * @param weight a weight value between {@code MIN_WEIGHT} and
         *     {@code MAX_WEIGHT}
         * @throws NullPointerException if the given {@code range} is
         *     {@code null}
         * @throws IllegalArgumentException if the given {@code range} does not
         * comply with the syntax of the language range mentioned in RFC 4647
         * or if the given {@code weight} is less than {@code MIN_WEIGHT}
         * or greater than {@code MAX_WEIGHT}
         */
        public LanguageRange(String range, double weight) {
            if (range == null) {
                throw new NullPointerException();
            }
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                throw new IllegalArgumentException("weight=" + weight);
            }

            range = range.toLowerCase(Locale.ROOT);

            // Do syntax check.
            boolean isIllFormed = false;
            String[] subtags = range.split("-");
            if (isSubtagIllFormed(subtags[0], true)
                || range.endsWith("-")) {
                isIllFormed = true;
            } else {
                for (int i = 1; i < subtags.length; i++) {
                    if (isSubtagIllFormed(subtags[i], false)) {
                        isIllFormed = true;
                        break;
                    }
                }
            }
            if (isIllFormed) {
                throw new IllegalArgumentException("range=" + range);
            }

            this.range = range;
            this.weight = weight;
        }

        private static boolean isSubtagIllFormed(String subtag,
                                                 boolean isFirstSubtag) {
            if (subtag.isEmpty() || subtag.length() > 8) {
                return true;
            } else if (subtag.equals("*")) {
                return false;
            }
            char[] charArray = subtag.toCharArray();
            if (isFirstSubtag) { // ALPHA
                for (char c : charArray) {
                    if (c < 'a' || c > 'z') {
                        return true;
                    }
                }
            } else { // ALPHA / DIGIT
                for (char c : charArray) {
                    if (c < '0' || (c > '9' && c < 'a') || c > 'z') {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Returns the language range of this {@code LanguageRange}.
         *
         * @return the language range.
         */
        public String getRange() {
            return range;
        }

        /**
         * Returns the weight of this {@code LanguageRange}.
         *
         * @return the weight value.
         */
        public double getWeight() {
            return weight;
        }

        /**
         * Parses the given {@code ranges} to generate a Language Priority List.
         *
         * <p>This method performs a syntactic check for each language range in
         * the given {@code ranges} but doesn't do validation using the IANA
         * Language Subtag Registry.
         *
         * <p>The {@code ranges} to be given can take one of the following
         * forms:
         *
         * <pre>
         *   "Accept-Language: ja,en;q=0.4"  (weighted list with Accept-Language prefix)
         *   "ja,en;q=0.4"                   (weighted list)
         *   "ja,en"                         (prioritized list)
         * </pre>
         *
         * In a weighted list, each language range is given a weight value.
         * The weight value is identical to the "quality value" in
         * <a href="http://tools.ietf.org/html/rfc2616">RFC 2616</a>, and it
         * expresses how much the user prefers  the language. A weight value is
         * specified after a corresponding language range followed by
         * {@code ";q="}, and the default weight value is {@code MAX_WEIGHT}
         * when it is omitted.
         *
         * <p>Unlike a weighted list, language ranges in a prioritized list
         * are sorted in the descending order based on its priority. The first
         * language range has the highest priority and meets the user's
         * preference most.
         *
         * <p>In either case, language ranges are sorted in descending order in
         * the Language Priority List based on priority or weight. If a
         * language range appears in the given {@code ranges} more than once,
         * only the first one is included on the Language Priority List.
         *
         * <p>The returned list consists of language ranges from the given
         * {@code ranges} and their equivalents found in the IANA Language
         * Subtag Registry. For example, if the given {@code ranges} is
         * {@code "Accept-Language: iw,en-us;q=0.7,en;q=0.3"}, the elements in
         * the list to be returned are:
         *
         * <pre>
         *  <b>Range</b>                                   <b>Weight</b>
         *    "iw" (older tag for Hebrew)             1.0
         *    "he" (new preferred code for Hebrew)    1.0
         *    "en-us" (English, United States)        0.7
         *    "en" (English)                          0.3
         * </pre>
         *
         * Two language ranges, {@code "iw"} and {@code "he"}, have the same
         * highest priority in the list. By adding {@code "he"} to the user's
         * Language Priority List, locale-matching method can find Hebrew as a
         * matching locale (or language tag) even if the application or system
         * offers only {@code "he"} as a supported locale (or language tag).
         *
         * @param ranges a list of comma-separated language ranges or a list of
         *     language ranges in the form of the "Accept-Language" header
         *     defined in <a href="http://tools.ietf.org/html/rfc2616">RFC
         *     2616</a>
         * @return a Language Priority List consisting of language ranges
         *     included in the given {@code ranges} and their equivalent
         *     language ranges if available. The list is modifiable.
         * @throws NullPointerException if {@code ranges} is null
         * @throws IllegalArgumentException if a language range or a weight
         *     found in the given {@code ranges} is ill-formed
         */
        public static List<LanguageRange> parse(String ranges) {
            return LocaleMatcher.parse(ranges);
        }

        /**
         * Parses the given {@code ranges} to generate a Language Priority
         * List, and then customizes the list using the given {@code map}.
         * This method is equivalent to
         * {@code mapEquivalents(parse(ranges), map)}.
         *
         * @param ranges a list of comma-separated language ranges or a list
         *     of language ranges in the form of the "Accept-Language" header
         *     defined in <a href="http://tools.ietf.org/html/rfc2616">RFC
         *     2616</a>
         * @param map a map containing information to customize language ranges
         * @return a Language Priority List with customization. The list is
         *     modifiable.
         * @throws NullPointerException if {@code ranges} is null
         * @throws IllegalArgumentException if a language range or a weight
         *     found in the given {@code ranges} is ill-formed
         * @see #parse(String)
         * @see #mapEquivalents
         */
        public static List<LanguageRange> parse(String ranges,
                                                Map<String, List<String>> map) {
            return mapEquivalents(parse(ranges), map);
        }

        /**
         * Generates a new customized Language Priority List using the given
         * {@code priorityList} and {@code map}. If the given {@code map} is
         * empty, this method returns a copy of the given {@code priorityList}.
         *
         * <p>In the map, a key represents a language range whereas a value is
         * a list of equivalents of it. {@code '*'} cannot be used in the map.
         * Each equivalent language range has the same weight value as its
         * original language range.
         *
         * <pre>
         *  An example of map:
         *    <b>Key</b>                            <b>Value</b>
         *      "zh" (Chinese)                 "zh",
         *                                     "zh-Hans"(Simplified Chinese)
         *      "zh-HK" (Chinese, Hong Kong)   "zh-HK"
         *      "zh-TW" (Chinese, Taiwan)      "zh-TW"
         * </pre>
         *
         * The customization is performed after modification using the IANA
         * Language Subtag Registry.
         *
         * <p>For example, if a user's Language Priority List consists of five
         * language ranges ({@code "zh"}, {@code "zh-CN"}, {@code "en"},
         * {@code "zh-TW"}, and {@code "zh-HK"}), the newly generated Language
         * Priority List which is customized using the above map example will
         * consists of {@code "zh"}, {@code "zh-Hans"}, {@code "zh-CN"},
         * {@code "zh-Hans-CN"}, {@code "en"}, {@code "zh-TW"}, and
         * {@code "zh-HK"}.
         *
         * <p>{@code "zh-HK"} and {@code "zh-TW"} aren't converted to
         * {@code "zh-Hans-HK"} nor {@code "zh-Hans-TW"} even if they are
         * included in the Language Priority List. In this example, mapping
         * is used to clearly distinguish Simplified Chinese and Traditional
         * Chinese.
         *
         * <p>If the {@code "zh"}-to-{@code "zh"} mapping isn't included in the
         * map, a simple replacement will be performed and the customized list
         * won't include {@code "zh"} and {@code "zh-CN"}.
         *
         * @param priorityList user's Language Priority List
         * @param map a map containing information to customize language ranges
         * @return a new Language Priority List with customization. The list is
         *     modifiable.
         * @throws NullPointerException if {@code priorityList} is {@code null}
         * @see #parse(String, Map)
         */
        public static List<LanguageRange> mapEquivalents(
                                              List<LanguageRange>priorityList,
                                              Map<String, List<String>> map) {
            return LocaleMatcher.mapEquivalents(priorityList, map);
        }

        /**
         * Returns a hash code value for the object.
         *
         * @return  a hash code value for this object.
         */
        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                h = 17;
                h = 37*h + range.hashCode();
                long bitsWeight = Double.doubleToLongBits(weight);
                h = 37*h + (int)(bitsWeight ^ (bitsWeight >>> 32));
                if (h != 0) {
                    hash = h;
                }
            }
            return h;
        }

        /**
         * Compares this object to the specified object. The result is true if
         * and only if the argument is not {@code null} and is a
         * {@code LanguageRange} object that contains the same {@code range}
         * and {@code weight} values as this object.
         *
         * @param obj the object to compare with
         * @return  {@code true} if this object's {@code range} and
         *     {@code weight} are the same as the {@code obj}'s; {@code false}
         *     otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof LanguageRange other
                    && range.equals(other.range)
                    && weight == other.weight;
        }

        /**
         * Returns an informative string representation of this {@code LanguageRange}
         * object, consisting of language range and weight if the range is
         * weighted and the weight is less than the max weight.
         *
         * @return a string representation of this {@code LanguageRange} object.
         */
        @Override
        public String toString() {
            return (weight == MAX_WEIGHT) ? range : range + ";q=" + weight;
        }
    }

    /**
     * Returns a list of matching {@code Locale} instances using the filtering
     * mechanism defined in RFC 4647.
     *
     * This filter operation on the given {@code locales} ensures that only
     * unique matching locale(s) are returned.
     *
     * @param priorityList user's Language Priority List in which each language
     *     tag is sorted in descending order based on priority or weight
     * @param locales {@code Locale} instances used for matching
     * @param mode filtering mode
     * @return a list of {@code Locale} instances for matching language tags
     *     sorted in descending order based on priority or weight, or an empty
     *     list if nothing matches. The list is modifiable.
     * @throws NullPointerException if {@code priorityList} or {@code locales}
     *     is {@code null}
     * @throws IllegalArgumentException if one or more extended language ranges
     *     are included in the given list when
     *     {@link FilteringMode#REJECT_EXTENDED_RANGES} is specified
     *
     * @since 1.8
     */
    public static List<Locale> filter(List<LanguageRange> priorityList,
                                      Collection<Locale> locales,
                                      FilteringMode mode) {
        return LocaleMatcher.filter(priorityList, locales, mode);
    }

    /**
     * Returns a list of matching {@code Locale} instances using the filtering
     * mechanism defined in RFC 4647. This is equivalent to
     * {@link #filter(List, Collection, FilteringMode)} when {@code mode} is
     * {@link FilteringMode#AUTOSELECT_FILTERING}.
     *
     * This filter operation on the given {@code locales} ensures that only
     * unique matching locale(s) are returned.
     *
     * @param priorityList user's Language Priority List in which each language
     *     tag is sorted in descending order based on priority or weight
     * @param locales {@code Locale} instances used for matching
     * @return a list of {@code Locale} instances for matching language tags
     *     sorted in descending order based on priority or weight, or an empty
     *     list if nothing matches. The list is modifiable.
     * @throws NullPointerException if {@code priorityList} or {@code locales}
     *     is {@code null}
     *
     * @since 1.8
     */
    public static List<Locale> filter(List<LanguageRange> priorityList,
                                      Collection<Locale> locales) {
        return filter(priorityList, locales, FilteringMode.AUTOSELECT_FILTERING);
    }

    /**
     * Returns a list of matching languages tags using the basic filtering
     * mechanism defined in RFC 4647.
     *
     * This filter operation on the given {@code tags} ensures that only
     * unique matching tag(s) are returned with preserved case. In case of
     * duplicate matching tags with the case difference, the first matching
     * tag with preserved case is returned.
     * For example, "de-ch" is returned out of the duplicate matching tags
     * "de-ch" and "de-CH", if "de-ch" is checked first for matching in the
     * given {@code tags}. Note that if the given {@code tags} is an unordered
     * {@code Collection}, the returned matching tag out of duplicate tags is
     * subject to change, depending on the implementation of the
     * {@code Collection}.
     *
     * @param priorityList user's Language Priority List in which each language
     *     tag is sorted in descending order based on priority or weight
     * @param tags language tags
     * @param mode filtering mode
     * @return a list of matching language tags sorted in descending order
     *     based on priority or weight, or an empty list if nothing matches.
     *     The list is modifiable.
     * @throws NullPointerException if {@code priorityList} or {@code tags} is
     *     {@code null}
     * @throws IllegalArgumentException if one or more extended language ranges
     *     are included in the given list when
     *     {@link FilteringMode#REJECT_EXTENDED_RANGES} is specified
     *
     * @since 1.8
     */
    public static List<String> filterTags(List<LanguageRange> priorityList,
                                          Collection<String> tags,
                                          FilteringMode mode) {
        return LocaleMatcher.filterTags(priorityList, tags, mode);
    }

    /**
     * Returns a list of matching languages tags using the basic filtering
     * mechanism defined in RFC 4647. This is equivalent to
     * {@link #filterTags(List, Collection, FilteringMode)} when {@code mode}
     * is {@link FilteringMode#AUTOSELECT_FILTERING}.
     *
     * This filter operation on the given {@code tags} ensures that only
     * unique matching tag(s) are returned with preserved case. In case of
     * duplicate matching tags with the case difference, the first matching
     * tag with preserved case is returned.
     * For example, "de-ch" is returned out of the duplicate matching tags
     * "de-ch" and "de-CH", if "de-ch" is checked first for matching in the
     * given {@code tags}. Note that if the given {@code tags} is an unordered
     * {@code Collection}, the returned matching tag out of duplicate tags is
     * subject to change, depending on the implementation of the
     * {@code Collection}.
     *
     * @param priorityList user's Language Priority List in which each language
     *     tag is sorted in descending order based on priority or weight
     * @param tags language tags
     * @return a list of matching language tags sorted in descending order
     *     based on priority or weight, or an empty list if nothing matches.
     *     The list is modifiable.
     * @throws NullPointerException if {@code priorityList} or {@code tags} is
     *     {@code null}
     *
     * @since 1.8
     */
    public static List<String> filterTags(List<LanguageRange> priorityList,
                                          Collection<String> tags) {
        return filterTags(priorityList, tags, FilteringMode.AUTOSELECT_FILTERING);
    }

    /**
     * Returns a {@code Locale} instance for the best-matching language
     * tag using the lookup mechanism defined in RFC 4647.
     *
     * @param priorityList user's Language Priority List in which each language
     *     tag is sorted in descending order based on priority or weight
     * @param locales {@code Locale} instances used for matching
     * @return the best matching {@code Locale} instance chosen based on
     *     priority or weight, or {@code null} if nothing matches.
     * @throws NullPointerException if {@code priorityList} or {@code tags} is
     *     {@code null}
     *
     * @since 1.8
     */
    public static Locale lookup(List<LanguageRange> priorityList,
                                Collection<Locale> locales) {
        return LocaleMatcher.lookup(priorityList, locales);
    }

    /**
     * Returns the best-matching language tag using the lookup mechanism
     * defined in RFC 4647.
     *
     * This lookup operation on the given {@code tags} ensures that the
     * first matching tag with preserved case is returned.
     *
     * @param priorityList user's Language Priority List in which each language
     *     tag is sorted in descending order based on priority or weight
     * @param tags language tangs used for matching
     * @return the best matching language tag chosen based on priority or
     *     weight, or {@code null} if nothing matches.
     * @throws NullPointerException if {@code priorityList} or {@code tags} is
     *     {@code null}
     *
     * @since 1.8
     */
    public static String lookupTag(List<LanguageRange> priorityList,
                                   Collection<String> tags) {
        return LocaleMatcher.lookupTag(priorityList, tags);
    }

}
