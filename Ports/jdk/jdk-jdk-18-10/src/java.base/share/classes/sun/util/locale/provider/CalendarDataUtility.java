/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.locale.provider;

import static java.util.Calendar.*;
import java.util.Locale;
import java.util.Map;
import java.util.spi.CalendarDataProvider;
import java.util.spi.CalendarNameProvider;

/**
 * {@code CalendarDataUtility} is a utility class for calling the
 * {@link CalendarDataProvider} methods.
 *
 * @author Masayoshi Okutsu
 * @author Naoto Sato
 */
public class CalendarDataUtility {
    public static final String FIRST_DAY_OF_WEEK = "firstDayOfWeek";
    public static final String MINIMAL_DAYS_IN_FIRST_WEEK = "minimalDaysInFirstWeek";
    private static final Locale.Builder OVERRIDE_BUILDER = new Locale.Builder();

    // No instantiation
    private CalendarDataUtility() {
    }

    public static int retrieveFirstDayOfWeek(Locale locale) {
        // Look for the Unicode Extension in the locale parameter
        if (locale.hasExtensions()) {
            String fw = locale.getUnicodeLocaleType("fw");
            if (fw != null) {
                switch (fw.toLowerCase(Locale.ROOT)) {
                    case "mon":
                        return MONDAY;
                    case "tue":
                        return TUESDAY;
                    case "wed":
                        return WEDNESDAY;
                    case "thu":
                        return THURSDAY;
                    case "fri":
                        return FRIDAY;
                    case "sat":
                        return SATURDAY;
                    case "sun":
                        return SUNDAY;
                }
            }
        }

        LocaleServiceProviderPool pool =
                LocaleServiceProviderPool.getPool(CalendarDataProvider.class);
        Integer value = pool.getLocalizedObject(CalendarWeekParameterGetter.INSTANCE,
                                                findRegionOverride(locale),
                                                true, FIRST_DAY_OF_WEEK);
        return (value != null && (value >= SUNDAY && value <= SATURDAY)) ? value : SUNDAY;
    }

    public static int retrieveMinimalDaysInFirstWeek(Locale locale) {
        LocaleServiceProviderPool pool =
                LocaleServiceProviderPool.getPool(CalendarDataProvider.class);
        Integer value = pool.getLocalizedObject(CalendarWeekParameterGetter.INSTANCE,
                                                findRegionOverride(locale),
                                                true, MINIMAL_DAYS_IN_FIRST_WEEK);
        return (value != null && (value >= 1 && value <= 7)) ? value : 1;
    }

    public static String retrieveFieldValueName(String id, int field, int value, int style, Locale locale) {
        LocaleServiceProviderPool pool =
                LocaleServiceProviderPool.getPool(CalendarNameProvider.class);
        return pool.getLocalizedObject(CalendarFieldValueNameGetter.INSTANCE, locale, normalizeCalendarType(id),
                                       field, value, style, false);
    }

    public static String retrieveJavaTimeFieldValueName(String id, int field, int value, int style, Locale locale) {
        LocaleServiceProviderPool pool =
                LocaleServiceProviderPool.getPool(CalendarNameProvider.class);
        String name;
        name = pool.getLocalizedObject(CalendarFieldValueNameGetter.INSTANCE, locale, normalizeCalendarType(id),
                                       field, value, style, true);
        if (name == null) {
            name = pool.getLocalizedObject(CalendarFieldValueNameGetter.INSTANCE, locale, normalizeCalendarType(id),
                                           field, value, style, false);
        }
        return name;
    }

    public static Map<String, Integer> retrieveFieldValueNames(String id, int field, int style, Locale locale) {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(CalendarNameProvider.class);
        return pool.getLocalizedObject(CalendarFieldValueNamesMapGetter.INSTANCE, locale,
                                       normalizeCalendarType(id), field, style, false);
    }

    public static Map<String, Integer> retrieveJavaTimeFieldValueNames(String id, int field, int style, Locale locale) {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(CalendarNameProvider.class);
        Map<String, Integer> map;
        map = pool.getLocalizedObject(CalendarFieldValueNamesMapGetter.INSTANCE, locale,
                                       normalizeCalendarType(id), field, style, true);
        if (map == null) {
            map = pool.getLocalizedObject(CalendarFieldValueNamesMapGetter.INSTANCE, locale,
                                           normalizeCalendarType(id), field, style, false);
        }
        return map;
    }

    /**
     * Utility to look for a region override extension.
     * If no region override is found, returns the original locale.
     */
    public static Locale findRegionOverride(Locale l) {
        String rg = l.getUnicodeLocaleType("rg");
        Locale override = l;

        if (rg != null && rg.length() == 6) {
            // UN M.49 code should not be allowed here
            // cannot use regex here, as it could be a recursive call
            rg = rg.toUpperCase(Locale.ROOT);
            if (rg.charAt(0) >= 0x0041 &&
                rg.charAt(0) <= 0x005A &&
                rg.charAt(1) >= 0x0041 &&
                rg.charAt(1) <= 0x005A &&
                rg.substring(2).equals("ZZZZ")) {
                override = OVERRIDE_BUILDER
                    .clear()
                    .setLocale(l)
                    .setRegion(rg.substring(0, 2))
                    .build();
            }
        }

        return override;
    }

    static String normalizeCalendarType(String requestID) {
        String type;
        if (requestID.equals("gregorian") || requestID.equals("iso8601")) {
            type = "gregory";
        } else if (requestID.startsWith("islamic")) {
            type = "islamic";
        } else {
            type = requestID;
        }
        return type;
    }

    /**
     * Obtains a localized field value string from a CalendarDataProvider
     * implementation.
     */
    private static class CalendarFieldValueNameGetter
        implements LocaleServiceProviderPool.LocalizedObjectGetter<CalendarNameProvider,
                                                                   String> {
        private static final CalendarFieldValueNameGetter INSTANCE =
            new CalendarFieldValueNameGetter();

        @Override
        public String getObject(CalendarNameProvider calendarNameProvider,
                                Locale locale,
                                String requestID, // calendarType
                                Object... params) {
            assert params.length == 4;
            int field = (int) params[0];
            int value = (int) params[1];
            int style = (int) params[2];
            boolean javatime = (boolean) params[3];

            // If javatime is true, resources from CLDR have precedence over JRE
            // native resources.
            if (javatime && calendarNameProvider instanceof CalendarNameProviderImpl) {
                String name;
                name = ((CalendarNameProviderImpl)calendarNameProvider)
                        .getJavaTimeDisplayName(requestID, field, value, style, locale);
                return name;
            }
            return calendarNameProvider.getDisplayName(requestID, field, value, style, locale);
        }
    }

    /**
     * Obtains a localized field-value pairs from a CalendarDataProvider
     * implementation.
     */
    private static class CalendarFieldValueNamesMapGetter
        implements LocaleServiceProviderPool.LocalizedObjectGetter<CalendarNameProvider,
                                                                   Map<String, Integer>> {
        private static final CalendarFieldValueNamesMapGetter INSTANCE =
            new CalendarFieldValueNamesMapGetter();

        @Override
        public Map<String, Integer> getObject(CalendarNameProvider calendarNameProvider,
                                              Locale locale,
                                              String requestID, // calendarType
                                              Object... params) {
            assert params.length == 3;
            int field = (int) params[0];
            int style = (int) params[1];
            boolean javatime = (boolean) params[2];

            // If javatime is true, resources from CLDR have precedence over JRE
            // native resources.
            if (javatime && calendarNameProvider instanceof CalendarNameProviderImpl) {
                Map<String, Integer> map;
                map = ((CalendarNameProviderImpl)calendarNameProvider)
                        .getJavaTimeDisplayNames(requestID, field, style, locale);
                return map;
            }
            return calendarNameProvider.getDisplayNames(requestID, field, style, locale);
        }
    }

    private static class CalendarWeekParameterGetter
        implements LocaleServiceProviderPool.LocalizedObjectGetter<CalendarDataProvider,
                                                                   Integer> {
        private static final CalendarWeekParameterGetter INSTANCE =
            new CalendarWeekParameterGetter();

        @Override
        public Integer getObject(CalendarDataProvider calendarDataProvider,
                                 Locale locale,
                                 String requestID,    // resource key
                                 Object... params) {
            assert params.length == 0;
            int value;
            switch (requestID) {
            case FIRST_DAY_OF_WEEK:
                value = calendarDataProvider.getFirstDayOfWeek(locale);
                if (value == 0) {
                    value = MONDAY; // default for the world ("001")
                }
                break;
            case MINIMAL_DAYS_IN_FIRST_WEEK:
                value = calendarDataProvider.getMinimalDaysInFirstWeek(locale);
                if (value == 0) {
                    value = 1; // default for the world ("001")
                }
                break;
            default:
                throw new InternalError("invalid requestID: " + requestID);
            }

            assert value != 0;
            return value;
        }
    }
}
