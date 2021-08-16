/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7168528
 * @summary Test the default implementation of LocaleServiceProvider.isSupportedLocale.
 */

import java.util.*;
import java.util.spi.LocaleServiceProvider;

public class SupportedLocalesTest {
    private static final Locale[] GOOD_ONES = {
        Locale.forLanguageTag("ja-JP-x-lvariant-JP"),
        Locale.forLanguageTag("th-TH-x-lvariant-TH"),
        Locale.US,
    };
    private static final Locale[] BAD_ONES = {
        Locale.GERMAN,
        Locale.GERMANY,
        Locale.CANADA,
        Locale.TAIWAN,
    };

    public static void main(String[] args) {
        LocaleServiceProvider provider = new TestLocaleServiceProvider();
        List<Locale> locs = new ArrayList<>();
        locs.addAll(Arrays.asList(GOOD_ONES));
        locs.addAll(Arrays.asList(provider.getAvailableLocales()));
        for (Locale locale : locs) {
            if (!provider.isSupportedLocale(locale)) {
                throw new RuntimeException(locale + " is NOT supported.");
            }
        }

        for (Locale locale : BAD_ONES) {
            if (provider.isSupportedLocale(locale)) {
                throw new RuntimeException(locale + " should NOT be supported.");
            }
        }
    }

    private static class TestLocaleServiceProvider extends LocaleServiceProvider {
        private static final Locale[] locales = {
            new Locale("ja", "JP", "JP"),
            new Locale("th", "TH", "TH"),
            Locale.forLanguageTag("en-US-u-ca-buddhist"),
        };

        @Override
        public Locale[] getAvailableLocales() {
            return locales.clone();
        }
    }
}
