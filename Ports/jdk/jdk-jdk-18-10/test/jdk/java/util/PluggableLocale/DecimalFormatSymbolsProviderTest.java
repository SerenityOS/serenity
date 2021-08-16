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
 * @bug 4052440 8062588 8210406
 * @summary DecimalFormatSymbolsProvider tests
 * @library providersrc/foobarutils
 *          providersrc/fooprovider
 * @modules java.base/sun.util.locale.provider
 * @build com.foobar.Utils
 *        com.foo.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI DecimalFormatSymbolsProviderTest
 */

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.foo.DecimalFormatSymbolsProviderImpl;

import sun.util.locale.provider.LocaleProviderAdapter;

public class DecimalFormatSymbolsProviderTest extends ProviderTest {

    DecimalFormatSymbolsProviderImpl dfsp = new DecimalFormatSymbolsProviderImpl();
    List<Locale> availloc = Arrays.asList(DecimalFormatSymbols.getAvailableLocales());
    List<Locale> providerloc = Arrays.asList(dfsp.getAvailableLocales());
    List<Locale> jreloc = Arrays.asList(LocaleProviderAdapter.forJRE().getAvailableLocales());
    List<Locale> jreimplloc = Arrays.asList(LocaleProviderAdapter.forJRE().getDecimalFormatSymbolsProvider().getAvailableLocales());

    public static void main(String[] s) {
        new DecimalFormatSymbolsProviderTest();
    }

    DecimalFormatSymbolsProviderTest() {
        availableLocalesTest();
        objectValidityTest();
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
            Object[] data = LocaleProviderAdapter.forJRE().getLocaleResources(target).getDecimalFormatSymbolsData();
            boolean jreSupportsLocale = jreimplloc.contains(target);

            // JRE string arrays
            String[] jres = new String[2];
            if (jreSupportsLocale) {
                String[] tmp = (String[]) data[0];
                jres[0] = tmp[9]; // infinity
                jres[1] = tmp[10]; // NaN
            }

            // result object
            DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(target);
            String[] result = new String[2];
            result[0] = dfs.getInfinity();
            result[1] = dfs.getNaN();

            // provider's object (if any)
            DecimalFormatSymbols providersDfs= null;
            String[] providers = new String[2];
            if (providerloc.contains(target)) {
                providersDfs = dfsp.getInstance(target);
                providers[0] = providersDfs.getInfinity();
                providers[1] = providersDfs.getNaN();
            }

            for (int i = 0; i < result.length; i ++) {
                checkValidity(target, jres[i], providers[i], result[i], jreSupportsLocale);
            }
        }
    }
}