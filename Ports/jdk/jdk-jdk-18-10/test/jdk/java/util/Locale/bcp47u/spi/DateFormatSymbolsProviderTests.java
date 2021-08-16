/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8208080
 * @summary Tests DateFormatSymbols provider implementations
 * @library provider
 * @build provider/module-info provider/foo.DateFormatSymbolsProviderImpl
 * @run main/othervm -Djava.locale.providers=SPI,CLDR DateFormatSymbolsProviderTests
 */

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.Map;

/**
 * Test DateFormatSymbolsProvider SPI with BCP47 U extensions
 */
public class DateFormatSymbolsProviderTests {
    private static final Map<Locale, String> data = Map.of(
        Locale.forLanguageTag("en-AA"),                 "foo",
        Locale.forLanguageTag("en-US-u-rg-aazzzz"),     "foo",
        Locale.forLanguageTag("en-US-u-ca-japanese"),   "bar"
    );

    public static void main(String... args) {
        data.forEach((l, e) -> {
            DateFormatSymbols dfs = DateFormatSymbols.getInstance(l);
            String[] months = dfs.getMonths();
            System.out.printf("January string for locale %s is %s.%n", l.toString(), months[0]);
            if (!months[0].equals(e)) {
                throw new RuntimeException("DateFormatSymbols provider is not called for" + l);
            }
        });
    }
}
