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
 * @bug 4052440 8062588 8165804 8210406
 * @summary BreakIteratorProvider tests
 * @library providersrc/foobarutils
 *          providersrc/fooprovider
 * @modules java.base/sun.util.locale.provider
 *          java.base/sun.util.resources
 * @build com.foobar.Utils
 *        com.foo.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI BreakIteratorProviderTest
 */

import java.text.BreakIterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.foo.BreakIteratorProviderImpl;

import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.ResourceBundleBasedAdapter;

public class BreakIteratorProviderTest extends ProviderTest {

    BreakIteratorProviderImpl bip = new BreakIteratorProviderImpl();
    List<Locale> availloc = Arrays.asList(BreakIterator.getAvailableLocales());
    List<Locale> providerloc = Arrays.asList(bip.getAvailableLocales());
    List<Locale> jreloc = Arrays.asList(LocaleProviderAdapter.forJRE().getAvailableLocales());
    List<Locale> jreimplloc = Arrays.asList(LocaleProviderAdapter.forJRE().getBreakIteratorProvider().getAvailableLocales());

    public static void main(String[] s) {
        new BreakIteratorProviderTest();
    }

    BreakIteratorProviderTest() {
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
            ResourceBundle rb = ((ResourceBundleBasedAdapter)LocaleProviderAdapter.forJRE()).getLocaleData().getBreakIteratorInfo(target);
            String[] classNames = rb.getStringArray("BreakIteratorClasses");
            boolean jreSupportsLocale = jreimplloc.contains(target);

            // result object
            String[] result = new String[4];
            result[0] = BreakIterator.getCharacterInstance(target).getClass().getName();
            result[1] = BreakIterator.getWordInstance(target).getClass().getName();
            result[2] = BreakIterator.getLineInstance(target).getClass().getName();
            result[3] = BreakIterator.getSentenceInstance(target).getClass().getName();

            // provider's object (if any)
            String[] providersResult = new String[4];
            if (providerloc.contains(target)) {
                providersResult[0] = bip.getCharacterInstance(target).getClass().getName();
                providersResult[1] = bip.getWordInstance(target).getClass().getName();
                providersResult[2] = bip.getLineInstance(target).getClass().getName();
                providersResult[3] = bip.getSentenceInstance(target).getClass().getName();
            }

            // JRE
            String[] jresResult = new String[4];
            if (jreSupportsLocale) {
                for (int i = 0; i < 4; i++) {
                    jresResult[i] = "sun.text." + classNames[i];
                }
            }

            for (int i = 0; i < 4; i++) {
                checkValidity(target, jresResult[i], providersResult[i], result[i], jreSupportsLocale);
            }
        }
    }
}