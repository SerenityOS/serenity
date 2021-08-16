/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.cldr;

import static sun.util.locale.provider.LocaleProviderAdapter.Type;

import java.util.Arrays;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;
import sun.util.locale.provider.CalendarDataProviderImpl;
import sun.util.locale.provider.CalendarDataUtility;

/**
 * Concrete implementation of the
 * {@link java.util.spi.CalendarDataProvider CalendarDataProvider} class
 * for the CLDR LocaleProviderAdapter.
 *
 * @author Naoto Sato
 */
public class CLDRCalendarDataProviderImpl extends CalendarDataProviderImpl {

    private static Map<String, Integer> firstDay = new ConcurrentHashMap<>();
    private static Map<String, Integer> minDays = new ConcurrentHashMap<>();

    public CLDRCalendarDataProviderImpl(Type type, Set<String> langtags) {
        super(type, langtags);
    }

    @Override
    public int getFirstDayOfWeek(Locale locale) {
        return findValue(CalendarDataUtility.FIRST_DAY_OF_WEEK, locale);
    }

    @Override
    public int getMinimalDaysInFirstWeek(Locale locale) {
        return findValue(CalendarDataUtility.MINIMAL_DAYS_IN_FIRST_WEEK, locale);
    }

    /**
     * Finds the requested integer value for the locale.
     * Each resource consists of the following:
     *
     *    (n: cc1 cc2 ... ccx;)*
     *
     * where 'n' is the integer for the following region codes, terminated by
     * a ';'.
     *
     */
    private static int findValue(String key, Locale locale) {
        Map<String, Integer> map = CalendarDataUtility.FIRST_DAY_OF_WEEK.equals(key) ?
            firstDay : minDays;
        String region = locale.getCountry();

        if (region.isEmpty()) {
            // Use "US" as default
            region = "US";
        }

        Integer val = map.get(region);
        if (val == null) {
            String valStr =
                LocaleProviderAdapter.forType(Type.CLDR).getLocaleResources(Locale.ROOT)
                   .getCalendarData(key);
            val = retrieveInteger(valStr, region)
                .orElse(retrieveInteger(valStr, "001").orElse(0));
            map.putIfAbsent(region, val);
        }

        return val;
    }

    private static Optional<Integer> retrieveInteger(String src, String region) {
        int regionIndex = src.indexOf(region);
        if (regionIndex >= 0) {
            int start = src.lastIndexOf(';', regionIndex) + 1;
            return Optional.of(Integer.parseInt(src, start, src.indexOf(':', start), 10));
        }
        return Optional.empty();
    }
}
