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
 * @bug 4052440 7200341 8062588 8210406
 * @summary DateFormatSymbolsProvider tests
 * @library providersrc/foobarutils
 *          providersrc/fooprovider
 * @modules java.base/sun.util.locale.provider
 *          java.base/sun.util.resources
 * @build com.foobar.Utils
 *        com.foo.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI DateFormatSymbolsProviderTest
 */

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.foo.DateFormatSymbolsProviderImpl;

import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.ResourceBundleBasedAdapter;

public class DateFormatSymbolsProviderTest extends ProviderTest {

    DateFormatSymbolsProviderImpl dfsp = new DateFormatSymbolsProviderImpl();
    List<Locale> availloc = Arrays.asList(DateFormatSymbols.getAvailableLocales());
    List<Locale> providerloc = Arrays.asList(dfsp.getAvailableLocales());
    List<Locale> jreloc = Arrays.asList(LocaleProviderAdapter.forJRE().getAvailableLocales());
    List<Locale> jreimplloc = Arrays.asList(LocaleProviderAdapter.forJRE().getDateFormatSymbolsProvider().getAvailableLocales());

    public static void main(String[] s) {
        new DateFormatSymbolsProviderTest();
    }

    DateFormatSymbolsProviderTest() {
        availableLocalesTest();
        objectValidityTest();
        hashCodeTest();
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
            // pure JRE implementation
            ResourceBundle rb = ((ResourceBundleBasedAdapter)LocaleProviderAdapter.forJRE()).getLocaleData().getDateFormatData(target);
            boolean jreSupportsLocale = jreimplloc.contains(target);

            // JRE string arrays
            String[][] jres = new String[6][];
            if (jreSupportsLocale) {
                try {
                    jres[0] = (String[])rb.getObject("MonthNames");
                    jres[1] = (String[])rb.getObject("MonthAbbreviations");
                    jres[2] = (String[])rb.getObject("DayNames");
                    jres[3] = (String[])rb.getObject("DayAbbreviations");
                    jres[4] = (String[])rb.getObject("AmPmMarkers");
                    jres[5] = (String[])rb.getObject("Eras");
                } catch (MissingResourceException mre) {}
            }

            // result object
            DateFormatSymbols dfs = DateFormatSymbols.getInstance(target);
            String[][] result = new String[6][];
            result[0] = dfs.getMonths();
            result[1] = dfs.getShortMonths();
            // note that weekdays are 1-based
            String[] tmp = dfs.getWeekdays();
            result[2] = new String[7];
            System.arraycopy(tmp, 1, result[2], 0, result[2].length);
            tmp = dfs.getShortWeekdays();
            result[3] = new String[7];
            System.arraycopy(tmp, 1, result[3], 0, result[3].length);
            result[4] = dfs.getAmPmStrings();
            result[5] = dfs.getEras();

            // provider's object (if any)
            DateFormatSymbols providersDfs= null;
            String[][] providers = new String[6][];
            if (providerloc.contains(target)) {
                providersDfs = dfsp.getInstance(target);
                providers[0] = providersDfs.getMonths();
                providers[1] = providersDfs.getShortMonths();
                // note that weekdays are 1-based
                tmp = dfs.getWeekdays();
                providers[2] = new String[7];
                System.arraycopy(tmp, 1, providers[2], 0, providers[2].length);
                tmp = dfs.getShortWeekdays();
                providers[3] = new String[7];
                System.arraycopy(tmp, 1, providers[3], 0, providers[3].length);
                providers[4] = providersDfs.getAmPmStrings();
                providers[5] = providersDfs.getEras();
            }

            for (int i = 0; i < result.length; i ++) {
                for (int j = 0; j < result[i].length; j++) {
                    String jresStr =
                        (jres[i] != null ? jres[i][j] : null);
                    String providersStr =
                        (providers[i] != null ? providers[i][j] : null);
                    String resultStr =
                        (result[i] != null ? result[i][j] : null);
                    checkValidity(target, jresStr, providersStr, resultStr, jreSupportsLocale);
                }
            }
        }
    }

    // Bug 7200341.
    void hashCodeTest() {
        for (Locale target: availloc) {
            // look for provider's object
            DateFormatSymbols dfs = DateFormatSymbols.getInstance(target);
            if (dfs.getClass().getSimpleName().equals("FooDateFormatSymbols")) {
                // call its hashCode(). success if no ArrayIndexOutOfBoundsException is thrown.
                dfs.hashCode();
                break;
            }
        }
    }
}