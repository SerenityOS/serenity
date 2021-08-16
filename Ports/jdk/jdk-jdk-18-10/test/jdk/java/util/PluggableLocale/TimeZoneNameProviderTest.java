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
 * @bug 4052440 8003267 8062588 8210406
 * @summary TimeZoneNameProvider tests
 * @library providersrc/foobarutils
 *          providersrc/barprovider
 * @modules java.base/sun.util.locale.provider
 *          java.base/sun.util.resources
 * @build com.foobar.Utils
 *        com.bar.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI TimeZoneNameProviderTest
 */

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.TimeZone;

import com.bar.TimeZoneNameProviderImpl;

import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.ResourceBundleBasedAdapter;
import sun.util.resources.OpenListResourceBundle;

public class TimeZoneNameProviderTest extends ProviderTest {

    TimeZoneNameProviderImpl tznp = new TimeZoneNameProviderImpl();

    public static void main(String[] s) {
        new TimeZoneNameProviderTest();
    }

    TimeZoneNameProviderTest() {
        test1();
        test2();
        test3();
        aliasTest();
        genericFallbackTest();
    }

    void test1() {
        Locale[] available = Locale.getAvailableLocales();
        List<Locale> jreimplloc = Arrays.asList(LocaleProviderAdapter.forJRE().getTimeZoneNameProvider().getAvailableLocales());
        List<Locale> providerLocales = Arrays.asList(tznp.getAvailableLocales());
        String[] ids = TimeZone.getAvailableIDs();

        for (Locale target: available) {
            // pure JRE implementation
            OpenListResourceBundle rb = ((ResourceBundleBasedAdapter)LocaleProviderAdapter.forJRE()).getLocaleData().getTimeZoneNames(target);
            boolean jreSupportsTarget = jreimplloc.contains(target);

            for (String id: ids) {
                // the time zone
                TimeZone tz = TimeZone.getTimeZone(id);

                // JRE string array for the id
                String[] jrearray = null;
                if (jreSupportsTarget) {
                    try {
                        jrearray = rb.getStringArray(id);
                    } catch (MissingResourceException mre) {}
                }

                for (int i = 1; i <=(tz.useDaylightTime()?4:2); i++) {
                    // the localized name
                    String name = tz.getDisplayName(i>=3, i%2, target);

                    // provider's name (if any)
                    String providersname = null;
                    if (providerLocales.contains(target)) {
                        providersname = tznp.getDisplayName(id, i>=3, i%2, target);
                    }

                    // JRE's name
                    String jresname = null;
                    if (jrearray != null) {
                        jresname = jrearray[i];
                    }

                    checkValidity(target, jresname, providersname, name,
                        jreSupportsTarget && jresname != null);
                }
            }
        }
    }

    final String pattern = "z";
    final Locale OSAKA = new Locale("ja", "JP", "osaka");
    final Locale KYOTO = new Locale("ja", "JP", "kyoto");
    final Locale GENERIC = new Locale("ja", "JP", "generic");

    final String[] TIMEZONES = {
        "GMT", "America/Los_Angeles", "SystemV/PST8",
        "SystemV/PST8PDT", "PST8PDT",
    };
    final String[] DISPLAY_NAMES_OSAKA = {
        tznp.getDisplayName(TIMEZONES[0], false, TimeZone.SHORT, OSAKA),
        tznp.getDisplayName(TIMEZONES[1], false, TimeZone.SHORT, OSAKA),
        tznp.getDisplayName(TIMEZONES[2], false, TimeZone.SHORT, OSAKA),
        tznp.getDisplayName(TIMEZONES[3], false, TimeZone.SHORT, OSAKA),
        tznp.getDisplayName(TIMEZONES[4], false, TimeZone.SHORT, OSAKA)
    };
    final String[] DISPLAY_NAMES_KYOTO = {
        tznp.getDisplayName(TIMEZONES[0], false, TimeZone.SHORT, KYOTO),
        tznp.getDisplayName(TIMEZONES[1], false, TimeZone.SHORT, KYOTO),
        tznp.getDisplayName(TIMEZONES[2], false, TimeZone.SHORT, KYOTO),
        tznp.getDisplayName(TIMEZONES[3], false, TimeZone.SHORT, KYOTO),
        tznp.getDisplayName(TIMEZONES[4], false, TimeZone.SHORT, KYOTO)
    };

    void test2() {
        Locale defaultLocale = Locale.getDefault();
        TimeZone reservedTimeZone = TimeZone.getDefault();
        Date d = new Date(2005-1900, Calendar.DECEMBER, 22);
        String formatted;

        TimeZone tz;
        SimpleDateFormat df;

        try {
            for (int i = 0; i < TIMEZONES.length; i++) {
                tz = TimeZone.getTimeZone(TIMEZONES[i]);
                TimeZone.setDefault(tz);
                df = new SimpleDateFormat(pattern, DateFormatSymbols.getInstance(OSAKA));
                Locale.setDefault(defaultLocale);
                System.out.println(formatted = df.format(d));
                if(!formatted.equals(DISPLAY_NAMES_OSAKA[i])) {
                    throw new RuntimeException("TimeZone " + TIMEZONES[i] +
                        ": formatted zone names mismatch. " +
                        formatted + " should match with " +
                        DISPLAY_NAMES_OSAKA[i]);
                }

                df.parse(DISPLAY_NAMES_OSAKA[i]);

                Locale.setDefault(KYOTO);
                df = new SimpleDateFormat(pattern, DateFormatSymbols.getInstance());
                System.out.println(formatted = df.format(d));
                if(!formatted.equals(DISPLAY_NAMES_KYOTO[i])) {
                    throw new RuntimeException("Timezone " + TIMEZONES[i] +
                        ": formatted zone names mismatch. " +
                        formatted + " should match with " +
                        DISPLAY_NAMES_KYOTO[i]);
                }
                df.parse(DISPLAY_NAMES_KYOTO[i]);
            }
        } catch (ParseException pe) {
            throw new RuntimeException("parse error occured" + pe);
        } finally {
            // restore the reserved locale and time zone
            Locale.setDefault(defaultLocale);
            TimeZone.setDefault(reservedTimeZone);
        }
    }

    void test3() {
        final String[] TZNAMES = {
            LATIME, PST, PST8PDT, US_PACIFIC,
            TOKYOTIME, JST, JAPAN,
        };
        for (String tzname : TZNAMES) {
            TimeZone tz = TimeZone.getTimeZone(tzname);
            for (int style : new int[] { TimeZone.LONG, TimeZone.SHORT }) {
                String osakaStd = tz.getDisplayName(false, style, OSAKA);
                if (osakaStd != null) {
                    String generic = tz.toZoneId().getDisplayName(
                            style == TimeZone.LONG ? TextStyle.FULL : TextStyle.SHORT,
                            GENERIC);
                    String expected = "Generic " + osakaStd;
                    if (!expected.equals(generic)) {
                        throw new RuntimeException("Wrong generic name: got=\"" + generic
                                                   + "\", expected=\"" + expected + "\"");
                    }
                }
            }
        }
    }

    final String LATIME = "America/Los_Angeles";
    final String PST = "PST";
    final String PST8PDT = "PST8PDT";
    final String US_PACIFIC = "US/Pacific";
    final String LATIME_IN_OSAKA =
        tznp.getDisplayName(LATIME, false, TimeZone.LONG, OSAKA);

    final String TOKYOTIME = "Asia/Tokyo";
    final String JST = "JST";
    final String JAPAN = "Japan";
    final String JST_IN_OSAKA =
        tznp.getDisplayName(JST, false, TimeZone.LONG, OSAKA);

    void aliasTest() {
        // Check that provider's name for a standard id (America/Los_Angeles) is
        // propagated to its aliases
        String latime = TimeZone.getTimeZone(LATIME).getDisplayName(OSAKA);
        if (!LATIME_IN_OSAKA.equals(latime)) {
            throw new RuntimeException("Could not get provider's localized name.  result: "+latime+" expected: "+LATIME_IN_OSAKA);
        }

        String pst = TimeZone.getTimeZone(PST).getDisplayName(OSAKA);
        if (!LATIME_IN_OSAKA.equals(pst)) {
            throw new RuntimeException("Provider's localized name is not available for an alias ID: "+PST+".  result: "+pst+" expected: "+LATIME_IN_OSAKA);
        }

        String us_pacific = TimeZone.getTimeZone(US_PACIFIC).getDisplayName(OSAKA);
        if (!LATIME_IN_OSAKA.equals(us_pacific)) {
            throw new RuntimeException("Provider's localized name is not available for an alias ID: "+US_PACIFIC+".  result: "+us_pacific+" expected: "+LATIME_IN_OSAKA);
        }

        // Check that provider's name for an alias id (JST) is
        // propagated to its standard id and alias ids.
        String jstime = TimeZone.getTimeZone(JST).getDisplayName(OSAKA);
        if (!JST_IN_OSAKA.equals(jstime)) {
            throw new RuntimeException("Could not get provider's localized name.  result: "+jstime+" expected: "+JST_IN_OSAKA);
        }

        String tokyotime = TimeZone.getTimeZone(TOKYOTIME).getDisplayName(OSAKA);
        if (!JST_IN_OSAKA.equals(tokyotime)) {
            throw new RuntimeException("Provider's localized name is not available for a standard ID: "+TOKYOTIME+".  result: "+tokyotime+" expected: "+JST_IN_OSAKA);
        }

        String japan = TimeZone.getTimeZone(JAPAN).getDisplayName(OSAKA);
        if (!JST_IN_OSAKA.equals(japan)) {
            throw new RuntimeException("Provider's localized name is not available for an alias ID: "+JAPAN+".  result: "+japan+" expected: "+JST_IN_OSAKA);
        }
    }

    /*
     * Tests whether generic names can be retrieved through fallback.
     * The test assumes the provider impl for OSAKA locale does NOT
     * provide generic names.
     */
    final String PT = "PT"; // SHORT generic name for "America/Los_Angeles"
    void genericFallbackTest() {
        String generic =
            TimeZone.getTimeZone(LATIME)
                .toZoneId()
                .getDisplayName(TextStyle.SHORT, OSAKA);
        if (!PT.equals(generic)) {
            throw new RuntimeException("Generic name fallback failed. got: "+generic);
        }
    }
}