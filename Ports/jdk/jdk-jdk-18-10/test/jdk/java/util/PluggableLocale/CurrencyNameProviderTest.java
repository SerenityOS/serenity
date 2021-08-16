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
 * @bug 4052440 7199750 8000997 8062588 8210406
 * @summary CurrencyNameProvider tests
 * @library providersrc/foobarutils
 *          providersrc/barprovider
 * @modules java.base/sun.util.locale.provider
 *          java.base/sun.util.resources
 * @build com.foobar.Utils
 *        com.bar.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI CurrencyNameProviderTest
 */

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import com.bar.CurrencyNameProviderImpl;
import com.bar.CurrencyNameProviderImpl2;

import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.ResourceBundleBasedAdapter;
import sun.util.resources.OpenListResourceBundle;

public class CurrencyNameProviderTest extends ProviderTest {

    public static void main(String[] s) {
        Locale reservedLocale = Locale.getDefault();
        try {
            new CurrencyNameProviderTest();
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    CurrencyNameProviderTest() {
        test1();
        test2();
    }

    void test1() {
        CurrencyNameProviderImpl cnp = new CurrencyNameProviderImpl();
        CurrencyNameProviderImpl2 cnp2 = new CurrencyNameProviderImpl2();
        Locale[] availloc = Locale.getAvailableLocales();
        Locale[] testloc = availloc.clone();
        List<Locale> jreimplloc = Arrays.asList(LocaleProviderAdapter.forJRE().getCurrencyNameProvider().getAvailableLocales());
        List<Locale> providerloc = new ArrayList<Locale>();
        providerloc.addAll(Arrays.asList(cnp.getAvailableLocales()));
        providerloc.addAll(Arrays.asList(cnp2.getAvailableLocales()));

        for (Locale target: availloc) {
            // pure JRE implementation
            OpenListResourceBundle rb = ((ResourceBundleBasedAdapter)LocaleProviderAdapter.forJRE()).getLocaleData().getCurrencyNames(target);
            boolean jreSupportsTarget = jreimplloc.contains(target);

            for (Locale test: testloc) {
                // get a Currency instance
                Currency c = null;
                try {
                    c = Currency.getInstance(test);
                } catch (IllegalArgumentException iae) {}

                if (c == null) {
                    continue;
                }

                // the localized symbol for the target locale
                String currencyresult = c.getSymbol(target);

                // the localized name for the target locale
                String nameresult = c.getDisplayName(target);

                // provider's name (if any)
                String providerscurrency = null;
                String providersname = null;
                if (providerloc.contains(target)) {
                    if (cnp.isSupportedLocale(target)) {
                    providerscurrency = cnp.getSymbol(c.getCurrencyCode(), target);
                    providersname = cnp.getDisplayName(c.getCurrencyCode(), target);
                    } else {
                        providerscurrency = cnp2.getSymbol(c.getCurrencyCode(), target);
                        providersname = cnp2.getDisplayName(c.getCurrencyCode(), target);
                    }
                }

                // JRE's name
                String jrescurrency = null;
                String jresname = null;
                String key = c.getCurrencyCode();
                String nameKey = key.toLowerCase(Locale.ROOT);
                if (jreSupportsTarget) {
                    try {
                        jrescurrency = rb.getString(key);
                    } catch (MissingResourceException mre) {}
                    try {
                        jresname = rb.getString(nameKey);
                    } catch (MissingResourceException mre) {}
                }

                checkValidity(target, jrescurrency, providerscurrency, currencyresult,
                              jreSupportsTarget && jrescurrency != null);
                checkValidity(target, jresname, providersname, nameresult,
                              jreSupportsTarget && jresname != null);
            }
        }
    }


    final String pattern = "###,###\u00A4";
    final String YEN_IN_OSAKA = "100,000\u5186\u3084\u3002";
    final String YEN_IN_KYOTO = "100,000\u5186\u3069\u3059\u3002";
    final String YEN_IN_TOKYO= "100,000JPY-tokyo";
    final Locale OSAKA = new Locale("ja", "JP", "osaka");
    final Locale KYOTO = new Locale("ja", "JP", "kyoto");
    final Locale TOKYO = new Locale("ja", "JP", "tokyo");
    Integer i = new Integer(100000);
    String formatted;
    DecimalFormat df;

    void test2() {
        Locale defloc = Locale.getDefault();

        try {
            df = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(OSAKA));
            System.out.println(formatted = df.format(i));
            if(!formatted.equals(YEN_IN_OSAKA)) {
                throw new RuntimeException("formatted currency names mismatch. " +
                    "Should match with " + YEN_IN_OSAKA);
            }

            df.parse(YEN_IN_OSAKA);

            Locale.setDefault(KYOTO);
            df = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance());
            System.out.println(formatted = df.format(i));
            if(!formatted.equals(YEN_IN_KYOTO)) {
                throw new RuntimeException("formatted currency names mismatch. " +
                    "Should match with " + YEN_IN_KYOTO);
            }

            df.parse(YEN_IN_KYOTO);

            Locale.setDefault(TOKYO);
            df = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance());
            System.out.println(formatted = df.format(i));
            if(!formatted.equals(YEN_IN_TOKYO)) {
                throw new RuntimeException("formatted currency names mismatch. " +
                    "Should match with " + YEN_IN_TOKYO);
            }

            df.parse(YEN_IN_TOKYO);
        } catch (ParseException pe) {
            throw new RuntimeException("parse error occured" + pe);
        } finally {
            Locale.setDefault(defloc);
        }
    }
}