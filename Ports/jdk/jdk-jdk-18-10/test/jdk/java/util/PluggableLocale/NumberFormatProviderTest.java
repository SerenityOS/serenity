/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @bug 4052440 7003643 8062588 8210406
 * @summary NumberFormatProvider tests
 * @library providersrc/foobarutils
 *          providersrc/fooprovider
 * @modules java.base/sun.util.locale.provider
 * @build com.foobar.Utils
 *        com.foo.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI NumberFormatProviderTest
 */

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.foo.FooNumberFormat;
import com.foo.NumberFormatProviderImpl;

import sun.util.locale.provider.LocaleProviderAdapter;

public class NumberFormatProviderTest extends ProviderTest {

    NumberFormatProviderImpl nfp = new NumberFormatProviderImpl();
    List<Locale> availloc = Arrays.asList(NumberFormat.getAvailableLocales());
    List<Locale> providerloc = Arrays.asList(nfp.getAvailableLocales());
    List<Locale> jreloc = Arrays.asList(LocaleProviderAdapter.forJRE().getAvailableLocales());
    List<Locale> jreimplloc = Arrays.asList(LocaleProviderAdapter.forJRE().getNumberFormatProvider().getAvailableLocales());

    public static void main(String[] s) {
        new NumberFormatProviderTest();
    }

    NumberFormatProviderTest() {
        availableLocalesTest();
        objectValidityTest();
        messageFormatTest();
    }

    void availableLocalesTest() {
        Set<Locale> localesFromAPI = new HashSet<>(availloc);
        Set<Locale> localesExpected = new HashSet<>(jreloc);
        localesExpected.addAll(providerloc);
        if (localesFromAPI.equals(localesExpected)) {
            System.out.println("availableLocalesTest passed.");
        } else {
            throw new RuntimeException("availableLocalesTest failed");
        }
    }

    void objectValidityTest() {

        for (Locale target: availloc) {
            boolean jreSupportsLocale = jreimplloc.contains(target);

            // JRE string arrays
            String[] jreNumberPatterns = null;
            if (jreSupportsLocale) {
                jreNumberPatterns = LocaleProviderAdapter.forJRE().getLocaleResources(target).getNumberPatterns();
            }

            // result object
            String resultCur = getPattern(NumberFormat.getCurrencyInstance(target));
            String resultInt = getPattern(NumberFormat.getIntegerInstance(target));
            String resultNum = getPattern(NumberFormat.getNumberInstance(target));
            String resultPer = getPattern(NumberFormat.getPercentInstance(target));

            // provider's object (if any)
            String providersCur = null;
            String providersInt = null;
            String providersNum = null;
            String providersPer = null;
            if (providerloc.contains(target)) {
                NumberFormat dfCur = nfp.getCurrencyInstance(target);
                if (dfCur != null) {
                    providersCur = getPattern(dfCur);
                }
                NumberFormat dfInt = nfp.getIntegerInstance(target);
                if (dfInt != null) {
                    providersInt = getPattern(dfInt);
                }
                NumberFormat dfNum = nfp.getNumberInstance(target);
                if (dfNum != null) {
                    providersNum = getPattern(dfNum);
                }
                NumberFormat dfPer = nfp.getPercentInstance(target);
                if (dfPer != null) {
                    providersPer = getPattern(dfPer);
                }
            }

            // JRE's object (if any)
            // note that this totally depends on the current implementation
            String jresCur = null;
            String jresInt = null;
            String jresNum = null;
            String jresPer = null;
            if (jreSupportsLocale) {
                DecimalFormat dfCur = new DecimalFormat(jreNumberPatterns[1],
                    DecimalFormatSymbols.getInstance(target));
                if (dfCur != null) {
                    adjustForCurrencyDefaultFractionDigits(dfCur);
                    jresCur = dfCur.toPattern();
                }
                DecimalFormat dfInt = new DecimalFormat(jreNumberPatterns[0],
                    DecimalFormatSymbols.getInstance(target));
                if (dfInt != null) {
                    dfInt.setMaximumFractionDigits(0);
                    dfInt.setDecimalSeparatorAlwaysShown(false);
                    dfInt.setParseIntegerOnly(true);
                    jresInt = dfInt.toPattern();
                }
                DecimalFormat dfNum = new DecimalFormat(jreNumberPatterns[0],
                    DecimalFormatSymbols.getInstance(target));
                if (dfNum != null) {
                    jresNum = dfNum.toPattern();
                }
                DecimalFormat dfPer = new DecimalFormat(jreNumberPatterns[2],
                    DecimalFormatSymbols.getInstance(target));
                if (dfPer != null) {
                    jresPer = dfPer.toPattern();
                }
            }

            checkValidity(target, jresCur, providersCur, resultCur, jreSupportsLocale);
            checkValidity(target, jresInt, providersInt, resultInt, jreSupportsLocale);
            checkValidity(target, jresNum, providersNum, resultNum, jreSupportsLocale);
            checkValidity(target, jresPer, providersPer, resultPer, jreSupportsLocale);
        }
    }

    /**
     * Adjusts the minimum and maximum fraction digits to values that
     * are reasonable for the currency's default fraction digits.
     */
    void adjustForCurrencyDefaultFractionDigits(DecimalFormat df) {
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        Currency currency = dfs.getCurrency();
        if (currency == null) {
            try {
                currency = Currency.getInstance(dfs.getInternationalCurrencySymbol());
            } catch (IllegalArgumentException e) {
            }
        }
        if (currency != null) {
            int digits = currency.getDefaultFractionDigits();
            if (digits != -1) {
                int oldMinDigits = df.getMinimumFractionDigits();
                // Common patterns are "#.##", "#.00", "#".
                // Try to adjust all of them in a reasonable way.
                if (oldMinDigits == df.getMaximumFractionDigits()) {
                    df.setMinimumFractionDigits(digits);
                    df.setMaximumFractionDigits(digits);
                } else {
                    df.setMinimumFractionDigits(Math.min(digits, oldMinDigits));
                    df.setMaximumFractionDigits(digits);
                }
            }
        }
    }

    private static String getPattern(NumberFormat nf) {
        if (nf instanceof DecimalFormat) {
            return ((DecimalFormat)nf).toPattern();
        }
        if (nf instanceof FooNumberFormat) {
            return ((FooNumberFormat)nf).toPattern();
        }
        return null;
    }

    private static final String[] NUMBER_PATTERNS = {
        "num={0,number}",
        "num={0,number,currency}",
        "num={0,number,percent}",
        "num={0,number,integer}"
    };

    void messageFormatTest() {
        for (Locale target : providerloc) {
            for (String pattern : NUMBER_PATTERNS) {
                MessageFormat mf = new MessageFormat(pattern, target);
                String toPattern = mf.toPattern();
                if (!pattern.equals(toPattern)) {
                    throw new RuntimeException("MessageFormat.toPattern: got '"
                                               + toPattern
                                               + "', expected '" + pattern + "'");
                }
            }
        }
    }
}