/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import sun.util.locale.provider.CalendarDataUtility;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleServiceProviderPool;
import sun.util.locale.provider.ResourceBundleBasedAdapter;

/**
 * This class represents the set of symbols (such as the decimal separator,
 * the grouping separator, and so on) needed by {@code DecimalFormat}
 * to format numbers. {@code DecimalFormat} creates for itself an instance of
 * {@code DecimalFormatSymbols} from its locale data.  If you need to change any
 * of these symbols, you can get the {@code DecimalFormatSymbols} object from
 * your {@code DecimalFormat} and modify it.
 *
 * <p>If the locale contains "rg" (region override)
 * <a href="../util/Locale.html#def_locale_extension">Unicode extension</a>,
 * the symbols are overridden for the designated region.
 *
 * @see          java.util.Locale
 * @see          DecimalFormat
 * @author       Mark Davis
 * @author       Alan Liu
 * @since 1.1
 */

public class DecimalFormatSymbols implements Cloneable, Serializable {

    /**
     * Create a DecimalFormatSymbols object for the default
     * {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * This constructor can only construct instances for the locales
     * supported by the Java runtime environment, not for those
     * supported by installed
     * {@link java.text.spi.DecimalFormatSymbolsProvider DecimalFormatSymbolsProvider}
     * implementations. For full locale coverage, use the
     * {@link #getInstance(Locale) getInstance} method.
     * <p>This is equivalent to calling
     * {@link #DecimalFormatSymbols(Locale)
     *     DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     */
    public DecimalFormatSymbols() {
        initialize( Locale.getDefault(Locale.Category.FORMAT) );
    }

    /**
     * Create a DecimalFormatSymbols object for the given locale.
     * This constructor can only construct instances for the locales
     * supported by the Java runtime environment, not for those
     * supported by installed
     * {@link java.text.spi.DecimalFormatSymbolsProvider DecimalFormatSymbolsProvider}
     * implementations. For full locale coverage, use the
     * {@link #getInstance(Locale) getInstance} method.
     * If the specified locale contains the {@link java.util.Locale#UNICODE_LOCALE_EXTENSION}
     * for the numbering system, the instance is initialized with the specified numbering
     * system if the JRE implementation supports it. For example,
     * <pre>
     * NumberFormat.getNumberInstance(Locale.forLanguageTag("th-TH-u-nu-thai"))
     * </pre>
     * This may return a {@code NumberFormat} instance with the Thai numbering system,
     * instead of the Latin numbering system.
     *
     * @param locale the desired locale
     * @throws    NullPointerException if {@code locale} is null
     */
    public DecimalFormatSymbols( Locale locale ) {
        initialize( locale );
    }

    /**
     * Returns an array of all locales for which the
     * {@code getInstance} methods of this class can return
     * localized instances.
     * The returned array represents the union of locales supported by the Java
     * runtime and by installed
     * {@link java.text.spi.DecimalFormatSymbolsProvider DecimalFormatSymbolsProvider}
     * implementations.  It must contain at least a {@code Locale}
     * instance equal to {@link java.util.Locale#US Locale.US}.
     *
     * @return an array of locales for which localized
     *         {@code DecimalFormatSymbols} instances are available.
     * @since 1.6
     */
    public static Locale[] getAvailableLocales() {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(DecimalFormatSymbolsProvider.class);
        return pool.getAvailableLocales();
    }

    /**
     * Gets the {@code DecimalFormatSymbols} instance for the default
     * locale.  This method provides access to {@code DecimalFormatSymbols}
     * instances for locales supported by the Java runtime itself as well
     * as for those supported by installed
     * {@link java.text.spi.DecimalFormatSymbolsProvider
     * DecimalFormatSymbolsProvider} implementations.
     * <p>This is equivalent to calling
     * {@link #getInstance(Locale)
     *     getInstance(Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @return a {@code DecimalFormatSymbols} instance.
     * @since 1.6
     */
    public static final DecimalFormatSymbols getInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Gets the {@code DecimalFormatSymbols} instance for the specified
     * locale.  This method provides access to {@code DecimalFormatSymbols}
     * instances for locales supported by the Java runtime itself as well
     * as for those supported by installed
     * {@link java.text.spi.DecimalFormatSymbolsProvider
     * DecimalFormatSymbolsProvider} implementations.
     * If the specified locale contains the {@link java.util.Locale#UNICODE_LOCALE_EXTENSION}
     * for the numbering system, the instance is initialized with the specified numbering
     * system if the JRE implementation supports it. For example,
     * <pre>
     * NumberFormat.getNumberInstance(Locale.forLanguageTag("th-TH-u-nu-thai"))
     * </pre>
     * This may return a {@code NumberFormat} instance with the Thai numbering system,
     * instead of the Latin numbering system.
     *
     * @param locale the desired locale.
     * @return a {@code DecimalFormatSymbols} instance.
     * @throws    NullPointerException if {@code locale} is null
     * @since 1.6
     */
    public static final DecimalFormatSymbols getInstance(Locale locale) {
        LocaleProviderAdapter adapter;
        adapter = LocaleProviderAdapter.getAdapter(DecimalFormatSymbolsProvider.class, locale);
        DecimalFormatSymbolsProvider provider = adapter.getDecimalFormatSymbolsProvider();
        DecimalFormatSymbols dfsyms = provider.getInstance(locale);
        if (dfsyms == null) {
            provider = LocaleProviderAdapter.forJRE().getDecimalFormatSymbolsProvider();
            dfsyms = provider.getInstance(locale);
        }
        return dfsyms;
    }

    /**
     * Gets the character used for zero. Different for Arabic, etc.
     *
     * @return the character used for zero
     */
    public char getZeroDigit() {
        return zeroDigit;
    }

    /**
     * Sets the character used for zero. Different for Arabic, etc.
     *
     * @param zeroDigit the character used for zero
     */
    public void setZeroDigit(char zeroDigit) {
        hashCode = 0;
        this.zeroDigit = zeroDigit;
    }

    /**
     * Gets the character used for grouping separator. Different for French, etc.
     *
     * @return the grouping separator
     */
    public char getGroupingSeparator() {
        return groupingSeparator;
    }

    /**
     * Sets the character used for grouping separator. Different for French, etc.
     *
     * @param groupingSeparator the grouping separator
     */
    public void setGroupingSeparator(char groupingSeparator) {
        hashCode = 0;
        this.groupingSeparator = groupingSeparator;
    }

    /**
     * Gets the character used for decimal sign. Different for French, etc.
     *
     * @return the character used for decimal sign
     */
    public char getDecimalSeparator() {
        return decimalSeparator;
    }

    /**
     * Sets the character used for decimal sign. Different for French, etc.
     *
     * @param decimalSeparator the character used for decimal sign
     */
    public void setDecimalSeparator(char decimalSeparator) {
        hashCode = 0;
        this.decimalSeparator = decimalSeparator;
    }

    /**
     * Gets the character used for per mille sign. Different for Arabic, etc.
     *
     * @return the character used for per mille sign
     */
    public char getPerMill() {
        return perMill;
    }

    /**
     * Sets the character used for per mille sign. Different for Arabic, etc.
     *
     * @param perMill the character used for per mille sign
     */
    public void setPerMill(char perMill) {
        hashCode = 0;
        this.perMill = perMill;
        this.perMillText = Character.toString(perMill);
    }

    /**
     * Gets the character used for percent sign. Different for Arabic, etc.
     *
     * @return the character used for percent sign
     */
    public char getPercent() {
        return percent;
    }

    /**
     * Sets the character used for percent sign. Different for Arabic, etc.
     *
     * @param percent the character used for percent sign
     */
    public void setPercent(char percent) {
        hashCode = 0;
        this.percent = percent;
        this.percentText = Character.toString(percent);
    }

    /**
     * Gets the character used for a digit in a pattern.
     *
     * @return the character used for a digit in a pattern
     */
    public char getDigit() {
        return digit;
    }

    /**
     * Sets the character used for a digit in a pattern.
     *
     * @param digit the character used for a digit in a pattern
     */
    public void setDigit(char digit) {
        hashCode = 0;
        this.digit = digit;
    }

    /**
     * Gets the character used to separate positive and negative subpatterns
     * in a pattern.
     *
     * @return the pattern separator
     */
    public char getPatternSeparator() {
        return patternSeparator;
    }

    /**
     * Sets the character used to separate positive and negative subpatterns
     * in a pattern.
     *
     * @param patternSeparator the pattern separator
     */
    public void setPatternSeparator(char patternSeparator) {
        hashCode = 0;
        this.patternSeparator = patternSeparator;
    }

    /**
     * Gets the string used to represent infinity. Almost always left
     * unchanged.
     *
     * @return the string representing infinity
     */
    public String getInfinity() {
        return infinity;
    }

    /**
     * Sets the string used to represent infinity. Almost always left
     * unchanged.
     *
     * @param infinity the string representing infinity
     */
    public void setInfinity(String infinity) {
        hashCode = 0;
        this.infinity = infinity;
    }

    /**
     * Gets the string used to represent "not a number". Almost always left
     * unchanged.
     *
     * @return the string representing "not a number"
     */
    public String getNaN() {
        return NaN;
    }

    /**
     * Sets the string used to represent "not a number". Almost always left
     * unchanged.
     *
     * @param NaN the string representing "not a number"
     */
    public void setNaN(String NaN) {
        hashCode = 0;
        this.NaN = NaN;
    }

    /**
     * Gets the character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     *
     * @return the character representing minus sign
     */
    public char getMinusSign() {
        return minusSign;
    }

    /**
     * Sets the character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     *
     * @param minusSign the character representing minus sign
     */
    public void setMinusSign(char minusSign) {
        hashCode = 0;
        this.minusSign = minusSign;
        this.minusSignText = Character.toString(minusSign);
    }

    /**
     * Returns the currency symbol for the currency of these
     * DecimalFormatSymbols in their locale.
     *
     * @return the currency symbol
     * @since 1.2
     */
    public String getCurrencySymbol()
    {
        initializeCurrency(locale);
        return currencySymbol;
    }

    /**
     * Sets the currency symbol for the currency of these
     * DecimalFormatSymbols in their locale.
     *
     * @param currency the currency symbol
     * @since 1.2
     */
    public void setCurrencySymbol(String currency)
    {
        initializeCurrency(locale);
        hashCode = 0;
        currencySymbol = currency;
    }

    /**
     * Returns the ISO 4217 currency code of the currency of these
     * DecimalFormatSymbols.
     *
     * @return the currency code
     * @since 1.2
     */
    public String getInternationalCurrencySymbol()
    {
        initializeCurrency(locale);
        return intlCurrencySymbol;
    }

    /**
     * Sets the ISO 4217 currency code of the currency of these
     * DecimalFormatSymbols.
     * If the currency code is valid (as defined by
     * {@link java.util.Currency#getInstance(java.lang.String) Currency.getInstance}),
     * this also sets the currency attribute to the corresponding Currency
     * instance and the currency symbol attribute to the currency's symbol
     * in the DecimalFormatSymbols' locale. If the currency code is not valid,
     * then the currency attribute is set to null and the currency symbol
     * attribute is not modified.
     *
     * @param currencyCode the currency code
     * @see #setCurrency
     * @see #setCurrencySymbol
     * @since 1.2
     */
    public void setInternationalCurrencySymbol(String currencyCode)
    {
        initializeCurrency(locale);
        hashCode = 0;
        intlCurrencySymbol = currencyCode;
        currency = null;
        if (currencyCode != null) {
            try {
                currency = Currency.getInstance(currencyCode);
                currencySymbol = currency.getSymbol();
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Gets the currency of these DecimalFormatSymbols. May be null if the
     * currency symbol attribute was previously set to a value that's not
     * a valid ISO 4217 currency code.
     *
     * @return the currency used, or null
     * @since 1.4
     */
    public Currency getCurrency() {
        initializeCurrency(locale);
        return currency;
    }

    /**
     * Sets the currency of these DecimalFormatSymbols.
     * This also sets the currency symbol attribute to the currency's symbol
     * in the DecimalFormatSymbols' locale, and the international currency
     * symbol attribute to the currency's ISO 4217 currency code.
     *
     * @param currency the new currency to be used
     * @throws    NullPointerException if {@code currency} is null
     * @since 1.4
     * @see #setCurrencySymbol
     * @see #setInternationalCurrencySymbol
     */
    public void setCurrency(Currency currency) {
        if (currency == null) {
            throw new NullPointerException();
        }
        initializeCurrency(locale);
        hashCode = 0;
        this.currency = currency;
        intlCurrencySymbol = currency.getCurrencyCode();
        currencySymbol = currency.getSymbol(locale);
    }


    /**
     * Returns the monetary decimal separator.
     *
     * @return the monetary decimal separator
     * @since 1.2
     */
    public char getMonetaryDecimalSeparator()
    {
        return monetarySeparator;
    }

    /**
     * Sets the monetary decimal separator.
     *
     * @param sep the monetary decimal separator
     * @since 1.2
     */
    public void setMonetaryDecimalSeparator(char sep)
    {
        hashCode = 0;
        monetarySeparator = sep;
    }

    /**
     * Returns the string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     *
     * @return the exponent separator string
     * @see #setExponentSeparator(java.lang.String)
     * @since 1.6
     */
    public String getExponentSeparator()
    {
        return exponentialSeparator;
    }

    /**
     * Sets the string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     *
     * @param exp the exponent separator string
     * @throws    NullPointerException if {@code exp} is null
     * @see #getExponentSeparator()
     * @since 1.6
     */
    public void setExponentSeparator(String exp)
    {
        if (exp == null) {
            throw new NullPointerException();
        }
        hashCode = 0;
        exponentialSeparator = exp;
    }

    /**
     * Gets the character used for grouping separator for currencies.
     * May be different from {@code grouping separator} in some locales,
     * e.g, German in Austria.
     *
     * @return the monetary grouping separator
     * @since 15
     */
    public char getMonetaryGroupingSeparator() {
        return monetaryGroupingSeparator;
    }

    /**
     * Sets the character used for grouping separator for currencies.
     * Invocation of this method will not affect the normal
     * {@code grouping separator}.
     *
     * @param monetaryGroupingSeparator the monetary grouping separator
     * @see #setGroupingSeparator(char)
     * @since 15
     */
    public void setMonetaryGroupingSeparator(char monetaryGroupingSeparator)
    {
        hashCode = 0;
        this.monetaryGroupingSeparator = monetaryGroupingSeparator;
    }

    //------------------------------------------------------------
    // BEGIN   Package Private methods ... to be made public later
    //------------------------------------------------------------

    /**
     * Returns the character used to separate the mantissa from the exponent.
     */
    char getExponentialSymbol()
    {
        return exponential;
    }

    /**
     * Sets the character used to separate the mantissa from the exponent.
     */
    void setExponentialSymbol(char exp)
    {
        exponential = exp;
    }

    /**
     * Gets the string used for per mille sign. Different for Arabic, etc.
     *
     * @return the string used for per mille sign
     * @since 13
     */
    String getPerMillText() {
        return perMillText;
    }

    /**
     * Sets the string used for per mille sign. Different for Arabic, etc.
     *
     * Setting the {@code perMillText} affects the return value of
     * {@link #getPerMill()}, in which the first non-format character of
     * {@code perMillText} is returned.
     *
     * @param perMillText the string used for per mille sign
     * @throws NullPointerException if {@code perMillText} is null
     * @throws IllegalArgumentException if {@code perMillText} is an empty string
     * @see #getPerMill()
     * @see #getPerMillText()
     * @since 13
     */
    void setPerMillText(String perMillText) {
        Objects.requireNonNull(perMillText);
        if (perMillText.isEmpty()) {
            throw new IllegalArgumentException("Empty argument string");
        }

        hashCode = 0;
        this.perMillText = perMillText;
        this.perMill = findNonFormatChar(perMillText, '\u2030');
    }

    /**
     * Gets the string used for percent sign. Different for Arabic, etc.
     *
     * @return the string used for percent sign
     * @since 13
     */
    String getPercentText() {
        return percentText;
    }

    /**
     * Sets the string used for percent sign. Different for Arabic, etc.
     *
     * Setting the {@code percentText} affects the return value of
     * {@link #getPercent()}, in which the first non-format character of
     * {@code percentText} is returned.
     *
     * @param percentText the string used for percent sign
     * @throws NullPointerException if {@code percentText} is null
     * @throws IllegalArgumentException if {@code percentText} is an empty string
     * @see #getPercent()
     * @see #getPercentText()
     * @since 13
     */
    void setPercentText(String percentText) {
        Objects.requireNonNull(percentText);
        if (percentText.isEmpty()) {
            throw new IllegalArgumentException("Empty argument string");
        }

        hashCode = 0;
        this.percentText = percentText;
        this.percent = findNonFormatChar(percentText, '%');
    }

    /**
     * Gets the string used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSignText to the positive format.
     *
     * @return the string representing minus sign
     * @since 13
     */
    String getMinusSignText() {
        return minusSignText;
    }

    /**
     * Sets the string used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSignText to the positive format.
     *
     * Setting the {@code minusSignText} affects the return value of
     * {@link #getMinusSign()}, in which the first non-format character of
     * {@code minusSignText} is returned.
     *
     * @param minusSignText the character representing minus sign
     * @throws NullPointerException if {@code minusSignText} is null
     * @throws IllegalArgumentException if {@code minusSignText} is an
     *  empty string
     * @see #getMinusSign()
     * @see #getMinusSignText()
     * @since 13
     */
    void setMinusSignText(String minusSignText) {
        Objects.requireNonNull(minusSignText);
        if (minusSignText.isEmpty()) {
            throw new IllegalArgumentException("Empty argument string");
        }

        hashCode = 0;
        this.minusSignText = minusSignText;
        this.minusSign = findNonFormatChar(minusSignText, '-');
    }

    //------------------------------------------------------------
    // END     Package Private methods ... to be made public later
    //------------------------------------------------------------

    /**
     * Standard override.
     */
    @Override
    public Object clone() {
        try {
            return (DecimalFormatSymbols)super.clone();
            // other fields are bit-copied
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Override equals.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        DecimalFormatSymbols other = (DecimalFormatSymbols) obj;
        return (zeroDigit == other.zeroDigit &&
            groupingSeparator == other.groupingSeparator &&
            decimalSeparator == other.decimalSeparator &&
            percent == other.percent &&
            percentText.equals(other.percentText) &&
            perMill == other.perMill &&
            perMillText.equals(other.perMillText) &&
            digit == other.digit &&
            minusSign == other.minusSign &&
            minusSignText.equals(other.minusSignText) &&
            patternSeparator == other.patternSeparator &&
            infinity.equals(other.infinity) &&
            NaN.equals(other.NaN) &&
            getCurrencySymbol().equals(other.getCurrencySymbol()) && // possible currency init occurs here
            intlCurrencySymbol.equals(other.intlCurrencySymbol) &&
            currency == other.currency &&
            monetarySeparator == other.monetarySeparator &&
            monetaryGroupingSeparator == other.monetaryGroupingSeparator &&
            exponentialSeparator.equals(other.exponentialSeparator) &&
            locale.equals(other.locale));
    }

    /**
     * Override hashCode.
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(
                zeroDigit,
                groupingSeparator,
                decimalSeparator,
                percent,
                percentText,
                perMill,
                perMillText,
                digit,
                minusSign,
                minusSignText,
                patternSeparator,
                infinity,
                NaN,
                getCurrencySymbol(), // possible currency init occurs here
                intlCurrencySymbol,
                currency,
                monetarySeparator,
                monetaryGroupingSeparator,
                exponentialSeparator,
                locale);
        }
        return hashCode;
    }

    /**
     * Initializes the symbols from the FormatData resource bundle.
     */
    private void initialize( Locale locale ) {
        this.locale = locale;

        // check for region override
        Locale override = locale.getUnicodeLocaleType("nu") == null ?
            CalendarDataUtility.findRegionOverride(locale) :
            locale;

        // get resource bundle data
        LocaleProviderAdapter adapter = LocaleProviderAdapter.getAdapter(DecimalFormatSymbolsProvider.class, override);
        // Avoid potential recursions
        if (!(adapter instanceof ResourceBundleBasedAdapter)) {
            adapter = LocaleProviderAdapter.getResourceBundleBased();
        }
        Object[] data = adapter.getLocaleResources(override).getDecimalFormatSymbolsData();
        String[] numberElements = (String[]) data[0];

        decimalSeparator = numberElements[0].charAt(0);
        groupingSeparator = numberElements[1].charAt(0);
        patternSeparator = numberElements[2].charAt(0);
        percentText = numberElements[3];
        percent = findNonFormatChar(percentText, '%');
        zeroDigit = numberElements[4].charAt(0); //different for Arabic,etc.
        digit = numberElements[5].charAt(0);
        minusSignText = numberElements[6];
        minusSign = findNonFormatChar(minusSignText, '-');
        exponential = numberElements[7].charAt(0);
        exponentialSeparator = numberElements[7]; //string representation new since 1.6
        perMillText = numberElements[8];
        perMill = findNonFormatChar(perMillText, '\u2030');
        infinity  = numberElements[9];
        NaN = numberElements[10];

        // monetary decimal/grouping separators may be missing in resource bundles
        monetarySeparator = numberElements.length < 12 || numberElements[11].isEmpty() ?
            decimalSeparator : numberElements[11].charAt(0);
        monetaryGroupingSeparator = numberElements.length < 13 || numberElements[12].isEmpty() ?
            groupingSeparator : numberElements[12].charAt(0);

        // maybe filled with previously cached values, or null.
        intlCurrencySymbol = (String) data[1];
        currencySymbol = (String) data[2];
    }

    /**
     * Obtains non-format single character from String
     */
    private char findNonFormatChar(String src, char defChar) {
        return (char)src.chars()
            .filter(c -> Character.getType(c) != Character.FORMAT)
            .findFirst()
            .orElse(defChar);
    }

    /**
     * Lazy initialization for currency related fields
     */
    private void initializeCurrency(Locale locale) {
        if (currencyInitialized) {
            return;
        }

        // Try to obtain the currency used in the locale's country.
        // Check for empty country string separately because it's a valid
        // country ID for Locale (and used for the C locale), but not a valid
        // ISO 3166 country code, and exceptions are expensive.
        if (!locale.getCountry().isEmpty()) {
            try {
                currency = Currency.getInstance(locale);
            } catch (IllegalArgumentException e) {
                // use default values below for compatibility
            }
        }

        if (currency != null) {
            // get resource bundle data
            LocaleProviderAdapter adapter =
                LocaleProviderAdapter.getAdapter(DecimalFormatSymbolsProvider.class, locale);
            // Avoid potential recursions
            if (!(adapter instanceof ResourceBundleBasedAdapter)) {
                adapter = LocaleProviderAdapter.getResourceBundleBased();
            }
            Object[] data = adapter.getLocaleResources(locale).getDecimalFormatSymbolsData();
            intlCurrencySymbol = currency.getCurrencyCode();
            if (data[1] != null && data[1] == intlCurrencySymbol) {
                currencySymbol = (String) data[2];
            } else {
                currencySymbol = currency.getSymbol(locale);
                data[1] = intlCurrencySymbol;
                data[2] = currencySymbol;
            }
        } else {
            // default values
            intlCurrencySymbol = "XXX";
            try {
                currency = Currency.getInstance(intlCurrencySymbol);
            } catch (IllegalArgumentException e) {
            }
            currencySymbol = "\u00A4";
        }

        currencyInitialized = true;
    }

    /**
     * Reads the default serializable fields, provides default values for objects
     * in older serial versions, and initializes non-serializable fields.
     * If {@code serialVersionOnStream}
     * is less than 1, initializes {@code monetarySeparator} to be
     * the same as {@code decimalSeparator} and {@code exponential}
     * to be 'E'.
     * If {@code serialVersionOnStream} is less than 2,
     * initializes {@code locale} to the root locale, and initializes
     * If {@code serialVersionOnStream} is less than 3, it initializes
     * {@code exponentialSeparator} using {@code exponential}.
     * If {@code serialVersionOnStream} is less than 4, it initializes
     * {@code perMillText}, {@code percentText}, and
     * {@code minusSignText} using {@code perMill}, {@code percent}, and
     * {@code minusSign} respectively.
     * If {@code serialVersionOnStream} is less than 5, it initializes
     * {@code monetaryGroupingSeparator} using {@code groupingSeparator}.
     * Sets {@code serialVersionOnStream} back to the maximum allowed value so that
     * default serialization will work properly if this object is streamed out again.
     * Initializes the currency from the intlCurrencySymbol field.
     *
     * @throws InvalidObjectException if {@code char} and {@code String}
     *      representations of either percent, per mille, and/or minus sign disagree.
     * @since  1.1.6
     */
    @java.io.Serial
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (serialVersionOnStream < 1) {
            // Didn't have monetarySeparator or exponential field;
            // use defaults.
            monetarySeparator = decimalSeparator;
            exponential       = 'E';
        }
        if (serialVersionOnStream < 2) {
            // didn't have locale; use root locale
            locale = Locale.ROOT;
        }
        if (serialVersionOnStream < 3) {
            // didn't have exponentialSeparator. Create one using exponential
            exponentialSeparator = Character.toString(exponential);
        }
        if (serialVersionOnStream < 4) {
            // didn't have perMillText, percentText, and minusSignText.
            // Create one using corresponding char variations.
            perMillText = Character.toString(perMill);
            percentText = Character.toString(percent);
            minusSignText = Character.toString(minusSign);
        } else {
            // Check whether char and text fields agree
            if (findNonFormatChar(perMillText, '\uFFFF') != perMill ||
                findNonFormatChar(percentText, '\uFFFF') != percent ||
                findNonFormatChar(minusSignText, '\uFFFF') != minusSign) {
                throw new InvalidObjectException(
                    "'char' and 'String' representations of either percent, " +
                    "per mille, and/or minus sign disagree.");
            }
        }
        if (serialVersionOnStream < 5) {
            // didn't have monetaryGroupingSeparator. Create one using groupingSeparator
            monetaryGroupingSeparator = groupingSeparator;
        }

        serialVersionOnStream = currentSerialVersion;

        if (intlCurrencySymbol != null) {
            try {
                 currency = Currency.getInstance(intlCurrencySymbol);
            } catch (IllegalArgumentException e) {
            }
            currencyInitialized = true;
        }
    }

    /**
     * Character used for zero.
     *
     * @serial
     * @see #getZeroDigit
     */
    private  char    zeroDigit;

    /**
     * Character used for grouping separator.
     *
     * @serial
     * @see #getGroupingSeparator
     */
    private  char    groupingSeparator;

    /**
     * Character used for decimal sign.
     *
     * @serial
     * @see #getDecimalSeparator
     */
    private  char    decimalSeparator;

    /**
     * Character used for per mille sign.
     *
     * @serial
     * @see #getPerMill
     */
    private  char    perMill;

    /**
     * Character used for percent sign.
     * @serial
     * @see #getPercent
     */
    private  char    percent;

    /**
     * Character used for a digit in a pattern.
     *
     * @serial
     * @see #getDigit
     */
    private  char    digit;

    /**
     * Character used to separate positive and negative subpatterns
     * in a pattern.
     *
     * @serial
     * @see #getPatternSeparator
     */
    private  char    patternSeparator;

    /**
     * String used to represent infinity.
     * @serial
     * @see #getInfinity
     */
    private  String  infinity;

    /**
     * String used to represent "not a number".
     * @serial
     * @see #getNaN
     */
    private  String  NaN;

    /**
     * Character used to represent minus sign.
     * @serial
     * @see #getMinusSign
     */
    private  char    minusSign;

    /**
     * String denoting the local currency, e.g. "$".
     * @serial
     * @see #getCurrencySymbol
     */
    private  String  currencySymbol;

    /**
     * ISO 4217 currency code denoting the local currency, e.g. "USD".
     * @serial
     * @see #getInternationalCurrencySymbol
     */
    private  String  intlCurrencySymbol;

    /**
     * The decimal separator used when formatting currency values.
     * @serial
     * @since  1.1.6
     * @see #getMonetaryDecimalSeparator
     */
    private  char    monetarySeparator; // Field new in JDK 1.1.6

    /**
     * The character used to distinguish the exponent in a number formatted
     * in exponential notation, e.g. 'E' for a number such as "1.23E45".
     * <p>
     * Note that the public API provides no way to set this field,
     * even though it is supported by the implementation and the stream format.
     * The intent is that this will be added to the API in the future.
     *
     * @serial
     * @since  1.1.6
     */
    private  char    exponential;       // Field new in JDK 1.1.6

    /**
     * The string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     * <p>
     * If both {@code exponential} and {@code exponentialSeparator}
     * exist, this {@code exponentialSeparator} has the precedence.
     *
     * @serial
     * @since 1.6
     */
    private  String    exponentialSeparator;       // Field new in JDK 1.6

    /**
     * The locale of these currency format symbols.
     *
     * @serial
     * @since 1.4
     */
    private Locale locale;

    /**
     * String representation of per mille sign, which may include
     * formatting characters, such as BiDi control characters.
     * The first non-format character of this string is the same as
     * {@code perMill}.
     *
     * @serial
     * @since 13
     */
    private  String perMillText;

    /**
     * String representation of percent sign, which may include
     * formatting characters, such as BiDi control characters.
     * The first non-format character of this string is the same as
     * {@code percent}.
     *
     * @serial
     * @since 13
     */
    private  String percentText;

    /**
     * String representation of minus sign, which may include
     * formatting characters, such as BiDi control characters.
     * The first non-format character of this string is the same as
     * {@code minusSign}.
     *
     * @serial
     * @since 13
     */
    private  String minusSignText;

    /**
     * The grouping separator used when formatting currency values.
     *
     * @serial
     * @since 15
     */
    private  char    monetaryGroupingSeparator;

    // currency; only the ISO code is serialized.
    private transient Currency currency;
    private transient volatile boolean currencyInitialized;

    /**
     * Cached hash code.
     */
    private transient volatile int hashCode;

    // Proclaim JDK 1.1 FCS compatibility
    @java.io.Serial
    static final long serialVersionUID = 5772796243397350300L;

    // The internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.5
    // - 1 for version from JDK 1.1.6, which includes two new fields:
    //     monetarySeparator and exponential.
    // - 2 for version from J2SE 1.4, which includes locale field.
    // - 3 for version from J2SE 1.6, which includes exponentialSeparator field.
    // - 4 for version from Java SE 13, which includes perMillText, percentText,
    //      and minusSignText field.
    // - 5 for version from Java SE 15, which includes monetaryGroupingSeparator.
    private static final int currentSerialVersion = 5;

    /**
     * Describes the version of {@code DecimalFormatSymbols} present on the stream.
     * Possible values are:
     * <ul>
     * <li><b>0</b> (or uninitialized): versions prior to JDK 1.1.6.
     *
     * <li><b>1</b>: Versions written by JDK 1.1.6 or later, which include
     *      two new fields: {@code monetarySeparator} and {@code exponential}.
     * <li><b>2</b>: Versions written by J2SE 1.4 or later, which include a
     *      new {@code locale} field.
     * <li><b>3</b>: Versions written by J2SE 1.6 or later, which include a
     *      new {@code exponentialSeparator} field.
     * <li><b>4</b>: Versions written by Java SE 13 or later, which include
     *      new {@code perMillText}, {@code percentText}, and
     *      {@code minusSignText} field.
     * <li><b>5</b>: Versions written by Java SE 15 or later, which include
     *      new {@code monetaryGroupingSeparator} field.
     * * </ul>
     * When streaming out a {@code DecimalFormatSymbols}, the most recent format
     * (corresponding to the highest allowable {@code serialVersionOnStream})
     * is always written.
     *
     * @serial
     * @since  1.1.6
     */
    private int serialVersionOnStream = currentSerialVersion;
}
