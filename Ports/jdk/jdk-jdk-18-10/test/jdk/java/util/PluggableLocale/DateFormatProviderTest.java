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
 * @summary DateFormatProvider tests
 * @library providersrc/foobarutils
 *          providersrc/fooprovider
 * @modules java.base/sun.util.locale.provider
 *          java.base/sun.util.resources
 * @build com.foobar.Utils
 *        com.foo.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI DateFormatProviderTest
 */

import java.text.DateFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.foo.DateFormatProviderImpl;

import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.ResourceBundleBasedAdapter;

public class DateFormatProviderTest extends ProviderTest {

    DateFormatProviderImpl dfp = new DateFormatProviderImpl();
    List<Locale> availloc = Arrays.asList(DateFormat.getAvailableLocales());
    List<Locale> providerloc = Arrays.asList(dfp.getAvailableLocales());
    List<Locale> jreloc = Arrays.asList(LocaleProviderAdapter.forJRE().getAvailableLocales());
    List<Locale> jreimplloc = Arrays.asList(LocaleProviderAdapter.forJRE().getDateFormatProvider().getAvailableLocales());

    public static void main(String[] s) {
        new DateFormatProviderTest();
    }

    DateFormatProviderTest() {
        availableLocalesTest();
        objectValidityTest();
        extendedVariantTest();
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
            // Get the key for the date/time patterns which is
            // specific to each calendar system.
            Calendar cal = Calendar.getInstance(target);
            String dkey = "DatePatterns";
            String tkey = "TimePatterns";
            String dtkey = "DateTimePatterns";
            switch (cal.getCalendarType()) {
                case "java.util.JapaneseImperialCalendar":
                    dkey = "japanese"+ "." + dkey;
                    tkey = "japanese"+ "." + tkey;
                    dtkey = "japanese"+ "." + dtkey;
                    break;
                case "sun.util.BuddhistCalendar":
                    dkey = "buddhist"+ "." + dkey;
                    tkey = "buddhist"+ "." + tkey;
                    dtkey = "buddhist"+ "." + dtkey;
                    break;
                case "java.util.GregorianCalendar":
                default:
                    break;
            }
            // pure JRE implementation
            ResourceBundle rb = ((ResourceBundleBasedAdapter)LocaleProviderAdapter.forJRE()).getLocaleData().getDateFormatData(target);
            boolean jreSupportsLocale = jreimplloc.contains(target);

            // JRE string arrays
            String[] jreDatePatterns = null;
            String[] jreTimePatterns = null;
            String[] jreDateTimePatterns = null;
            if (jreSupportsLocale) {
                try {
                    jreDatePatterns = (String[])rb.getObject(dkey);
                    jreTimePatterns = (String[])rb.getObject(tkey);
                    jreDateTimePatterns = (String[])rb.getObject(dtkey);
                } catch (MissingResourceException mre) {}
            }

            for (int style = DateFormat.FULL; style <= DateFormat.SHORT; style ++) {
                // result object
                DateFormat result = DateFormat.getDateTimeInstance(style, style, target);

                // provider's object (if any)
                DateFormat providersResult = null;
                if (providerloc.contains(target)) {
                    providersResult = dfp.getDateTimeInstance(style, style, target);
                }

                // JRE's object (if any)
                DateFormat jresResult = null;
                if (jreSupportsLocale) {
                    Object[] dateTimeArgs = {jreTimePatterns[style],
                                             jreDatePatterns[style]};
                    String pattern = MessageFormat.format(jreDateTimePatterns[0], dateTimeArgs);
                    jresResult = new SimpleDateFormat(pattern, target);
                }

                checkValidity(target, jresResult, providersResult, result, jreSupportsLocale);
            }
        }
    }

    // Check that fallback correctly occurs with locales with variant including '_'s
    // This test assumes that the provider supports the ja_JP_osaka locale, and JRE does not.
    void extendedVariantTest() {
        Locale[] testlocs = {new Locale("ja", "JP", "osaka_extended"),
                             new Locale("ja", "JP", "osaka_extended_further"),
                             new Locale("ja", "JP", "osaka_")};
        for (Locale test: testlocs) {
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, test);
            DateFormat provider = dfp.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, test);
            if (!df.equals(provider)) {
                throw new RuntimeException("variant fallback failed. test locale: "+test);
            }
        }
    }


    private static final String[] TYPES = {
        "date",
        "time"
    };
    private static final String[] MODIFIERS = {
        "",
        "short",
        "medium", // Same as DEFAULT
        "long",
        "full"
    };

    void messageFormatTest() {
        for (Locale target : providerloc) {
            for (String type : TYPES) {
                for (String modifier : MODIFIERS) {
                    String pattern, expected;
                    if (modifier.equals("")) {
                        pattern = String.format("%s={0,%s}", type, type);
                    } else {
                        pattern = String.format("%s={0,%s,%s}", type, type, modifier);
                    }
                    if (modifier.equals("medium")) {
                        // medium is default.
                        expected = String.format("%s={0,%s}", type, type);
                    } else {
                        expected = pattern;
                    }
                    MessageFormat mf = new MessageFormat(pattern, target);
                    Format[] fmts = mf.getFormats();
                    if (fmts[0] instanceof SimpleDateFormat) {
                        continue;
                    }
                    String toPattern = mf.toPattern();
                    if (!toPattern.equals(expected)) {
                        throw new RuntimeException("messageFormatTest: got '" + toPattern
                                                   + "', expected '" + expected + "'");
                    }
                }
            }
        }
    }
}