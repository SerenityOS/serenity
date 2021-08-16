/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2011-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java.time.format;

import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalField;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import sun.util.locale.provider.CalendarDataUtility;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;

/**
 * A provider to obtain the textual form of a date-time field.
 *
 * @implSpec
 * Implementations must be thread-safe.
 * Implementations should cache the textual information.
 *
 * @since 1.8
 */
class DateTimeTextProvider {

    /** Cache. */
    private static final ConcurrentMap<Entry<TemporalField, Locale>, Object> CACHE = new ConcurrentHashMap<>(16, 0.75f, 2);
    /** Comparator. */
    private static final Comparator<Entry<String, Long>> COMPARATOR = new Comparator<Entry<String, Long>>() {
        @Override
        public int compare(Entry<String, Long> obj1, Entry<String, Long> obj2) {
            return obj2.getKey().length() - obj1.getKey().length();  // longest to shortest
        }
    };

    // Singleton instance
    private static final DateTimeTextProvider INSTANCE = new DateTimeTextProvider();

    DateTimeTextProvider() {}

    /**
     * Gets the provider of text.
     *
     * @return the provider, not null
     */
    static DateTimeTextProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the text for the specified field, locale and style
     * for the purpose of formatting.
     * <p>
     * The text associated with the value is returned.
     * The null return value should be used if there is no applicable text, or
     * if the text would be a numeric representation of the value.
     *
     * @param field  the field to get text for, not null
     * @param value  the field value to get text for, not null
     * @param style  the style to get text for, not null
     * @param locale  the locale to get text for, not null
     * @return the text for the field value, null if no text found
     */
    public String getText(TemporalField field, long value, TextStyle style, Locale locale) {
        Object store = findStore(field, locale);
        if (store instanceof LocaleStore) {
            return ((LocaleStore) store).getText(value, style);
        }
        return null;
    }

    /**
     * Gets the text for the specified chrono, field, locale and style
     * for the purpose of formatting.
     * <p>
     * The text associated with the value is returned.
     * The null return value should be used if there is no applicable text, or
     * if the text would be a numeric representation of the value.
     *
     * @param chrono  the Chronology to get text for, not null
     * @param field  the field to get text for, not null
     * @param value  the field value to get text for, not null
     * @param style  the style to get text for, not null
     * @param locale  the locale to get text for, not null
     * @return the text for the field value, null if no text found
     */
    public String getText(Chronology chrono, TemporalField field, long value,
                                    TextStyle style, Locale locale) {
        if (chrono == IsoChronology.INSTANCE
                || !(field instanceof ChronoField)) {
            return getText(field, value, style, locale);
        }

        int fieldIndex;
        int fieldValue;
        if (field == ERA) {
            fieldIndex = Calendar.ERA;
            if (chrono == JapaneseChronology.INSTANCE) {
                if (value == -999) {
                    fieldValue = 0;
                } else {
                    fieldValue = (int) value + 2;
                }
            } else {
                fieldValue = (int) value;
            }
        } else if (field == MONTH_OF_YEAR) {
            fieldIndex = Calendar.MONTH;
            fieldValue = (int) value - 1;
        } else if (field == DAY_OF_WEEK) {
            fieldIndex = Calendar.DAY_OF_WEEK;
            fieldValue = (int) value + 1;
            if (fieldValue > 7) {
                fieldValue = Calendar.SUNDAY;
            }
        } else if (field == AMPM_OF_DAY) {
            fieldIndex = Calendar.AM_PM;
            fieldValue = (int) value;
        } else {
            return null;
        }
        return CalendarDataUtility.retrieveJavaTimeFieldValueName(
                chrono.getCalendarType(), fieldIndex, fieldValue, style.toCalendarStyle(), locale);
    }

    /**
     * Gets an iterator of text to field for the specified field, locale and style
     * for the purpose of parsing.
     * <p>
     * The iterator must be returned in order from the longest text to the shortest.
     * <p>
     * The null return value should be used if there is no applicable parsable text, or
     * if the text would be a numeric representation of the value.
     * Text can only be parsed if all the values for that field-style-locale combination are unique.
     *
     * @param field  the field to get text for, not null
     * @param style  the style to get text for, null for all parsable text
     * @param locale  the locale to get text for, not null
     * @return the iterator of text to field pairs, in order from longest text to shortest text,
     *  null if the field or style is not parsable
     */
    public Iterator<Entry<String, Long>> getTextIterator(TemporalField field, TextStyle style, Locale locale) {
        Object store = findStore(field, locale);
        if (store instanceof LocaleStore) {
            return ((LocaleStore) store).getTextIterator(style);
        }
        return null;
    }

    /**
     * Gets an iterator of text to field for the specified chrono, field, locale and style
     * for the purpose of parsing.
     * <p>
     * The iterator must be returned in order from the longest text to the shortest.
     * <p>
     * The null return value should be used if there is no applicable parsable text, or
     * if the text would be a numeric representation of the value.
     * Text can only be parsed if all the values for that field-style-locale combination are unique.
     *
     * @param chrono  the Chronology to get text for, not null
     * @param field  the field to get text for, not null
     * @param style  the style to get text for, null for all parsable text
     * @param locale  the locale to get text for, not null
     * @return the iterator of text to field pairs, in order from longest text to shortest text,
     *  null if the field or style is not parsable
     */
    public Iterator<Entry<String, Long>> getTextIterator(Chronology chrono, TemporalField field,
                                                         TextStyle style, Locale locale) {
        if (chrono == IsoChronology.INSTANCE
                || !(field instanceof ChronoField)) {
            return getTextIterator(field, style, locale);
        }

        int fieldIndex;
        switch ((ChronoField)field) {
        case ERA:
            fieldIndex = Calendar.ERA;
            break;
        case MONTH_OF_YEAR:
            fieldIndex = Calendar.MONTH;
            break;
        case DAY_OF_WEEK:
            fieldIndex = Calendar.DAY_OF_WEEK;
            break;
        case AMPM_OF_DAY:
            fieldIndex = Calendar.AM_PM;
            break;
        default:
            return null;
        }

        int calendarStyle = (style == null) ? Calendar.ALL_STYLES : style.toCalendarStyle();
        Map<String, Integer> map = CalendarDataUtility.retrieveJavaTimeFieldValueNames(
                chrono.getCalendarType(), fieldIndex, calendarStyle, locale);
        if (map == null) {
            return null;
        }
        List<Entry<String, Long>> list = new ArrayList<>(map.size());
        switch (fieldIndex) {
        case Calendar.ERA:
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                int era = entry.getValue();
                if (chrono == JapaneseChronology.INSTANCE) {
                    if (era == 0) {
                        era = -999;
                    } else {
                        era -= 2;
                    }
                }
                list.add(createEntry(entry.getKey(), (long)era));
            }
            break;
        case Calendar.MONTH:
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                list.add(createEntry(entry.getKey(), (long)(entry.getValue() + 1)));
            }
            break;
        case Calendar.DAY_OF_WEEK:
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                list.add(createEntry(entry.getKey(), (long)toWeekDay(entry.getValue())));
            }
            break;
        default:
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                list.add(createEntry(entry.getKey(), (long)entry.getValue()));
            }
            break;
        }
        return list.iterator();
    }

    private Object findStore(TemporalField field, Locale locale) {
        Entry<TemporalField, Locale> key = createEntry(field, locale);
        Object store = CACHE.get(key);
        if (store == null) {
            store = createStore(field, locale);
            CACHE.putIfAbsent(key, store);
            store = CACHE.get(key);
        }
        return store;
    }

    private static int toWeekDay(int calWeekDay) {
        if (calWeekDay == Calendar.SUNDAY) {
            return 7;
        } else {
            return calWeekDay - 1;
        }
    }

    private Object createStore(TemporalField field, Locale locale) {
        Map<TextStyle, Map<Long, String>> styleMap = new HashMap<>();
        if (field == ERA) {
            for (TextStyle textStyle : TextStyle.values()) {
                if (textStyle.isStandalone()) {
                    // Stand-alone isn't applicable to era names.
                    continue;
                }
                Map<String, Integer> displayNames = CalendarDataUtility.retrieveJavaTimeFieldValueNames(
                        "gregory", Calendar.ERA, textStyle.toCalendarStyle(), locale);
                if (displayNames != null) {
                    Map<Long, String> map = new HashMap<>();
                    for (Entry<String, Integer> entry : displayNames.entrySet()) {
                        map.put((long) entry.getValue(), entry.getKey());
                    }
                    if (!map.isEmpty()) {
                        styleMap.put(textStyle, map);
                    }
                }
            }
            return new LocaleStore(styleMap);
        }

        if (field == MONTH_OF_YEAR) {
            for (TextStyle textStyle : TextStyle.values()) {
                Map<Long, String> map = new HashMap<>();
                // Narrow names may have duplicated names, such as "J" for January, June, July.
                // Get names one by one in that case.
                if ((textStyle.equals(TextStyle.NARROW) ||
                        textStyle.equals(TextStyle.NARROW_STANDALONE))) {
                    for (int month = Calendar.JANUARY; month <= Calendar.DECEMBER; month++) {
                        String name;
                        name = CalendarDataUtility.retrieveJavaTimeFieldValueName(
                                "gregory", Calendar.MONTH,
                                month, textStyle.toCalendarStyle(), locale);
                        if (name == null) {
                            break;
                        }
                        map.put((month + 1L), name);
                    }
                } else {
                    Map<String, Integer> displayNames = CalendarDataUtility.retrieveJavaTimeFieldValueNames(
                            "gregory", Calendar.MONTH, textStyle.toCalendarStyle(), locale);
                    if (displayNames != null) {
                        for (Entry<String, Integer> entry : displayNames.entrySet()) {
                            map.put((long)(entry.getValue() + 1), entry.getKey());
                        }
                    } else {
                        // Although probability is very less, but if other styles have duplicate names.
                        // Get names one by one in that case.
                        for (int month = Calendar.JANUARY; month <= Calendar.DECEMBER; month++) {
                            String name;
                            name = CalendarDataUtility.retrieveJavaTimeFieldValueName(
                                    "gregory", Calendar.MONTH, month, textStyle.toCalendarStyle(), locale);
                            if (name == null) {
                                break;
                            }
                            map.put((month + 1L), name);
                        }
                    }
                }
                if (!map.isEmpty()) {
                    styleMap.put(textStyle, map);
                }
            }
            return new LocaleStore(styleMap);
        }

        if (field == DAY_OF_WEEK) {
            for (TextStyle textStyle : TextStyle.values()) {
                Map<Long, String> map = new HashMap<>();
                // Narrow names may have duplicated names, such as "S" for Sunday and Saturday.
                // Get names one by one in that case.
                if ((textStyle.equals(TextStyle.NARROW) ||
                        textStyle.equals(TextStyle.NARROW_STANDALONE))) {
                    for (int wday = Calendar.SUNDAY; wday <= Calendar.SATURDAY; wday++) {
                        String name;
                        name = CalendarDataUtility.retrieveJavaTimeFieldValueName(
                                "gregory", Calendar.DAY_OF_WEEK,
                                wday, textStyle.toCalendarStyle(), locale);
                        if (name == null) {
                            break;
                        }
                        map.put((long)toWeekDay(wday), name);
                    }
                } else {
                    Map<String, Integer> displayNames = CalendarDataUtility.retrieveJavaTimeFieldValueNames(
                            "gregory", Calendar.DAY_OF_WEEK, textStyle.toCalendarStyle(), locale);
                    if (displayNames != null) {
                        for (Entry<String, Integer> entry : displayNames.entrySet()) {
                            map.put((long)toWeekDay(entry.getValue()), entry.getKey());
                        }
                    } else {
                        // Although probability is very less, but if other styles have duplicate names.
                        // Get names one by one in that case.
                        for (int wday = Calendar.SUNDAY; wday <= Calendar.SATURDAY; wday++) {
                            String name;
                            name = CalendarDataUtility.retrieveJavaTimeFieldValueName(
                                    "gregory", Calendar.DAY_OF_WEEK, wday, textStyle.toCalendarStyle(), locale);
                            if (name == null) {
                                break;
                            }
                            map.put((long)toWeekDay(wday), name);
                        }
                    }
                }
                if (!map.isEmpty()) {
                    styleMap.put(textStyle, map);
                }
            }
            return new LocaleStore(styleMap);
        }

        if (field == AMPM_OF_DAY) {
            for (TextStyle textStyle : TextStyle.values()) {
                if (textStyle.isStandalone()) {
                    // Stand-alone isn't applicable to AM/PM.
                    continue;
                }
                Map<String, Integer> displayNames = CalendarDataUtility.retrieveJavaTimeFieldValueNames(
                        "gregory", Calendar.AM_PM, textStyle.toCalendarStyle(), locale);
                if (displayNames != null) {
                    Map<Long, String> map = new HashMap<>();
                    for (Entry<String, Integer> entry : displayNames.entrySet()) {
                        map.put((long) entry.getValue(), entry.getKey());
                    }
                    if (!map.isEmpty()) {
                        styleMap.put(textStyle, map);
                    }
                }
            }
            return new LocaleStore(styleMap);
        }

        if (field == IsoFields.QUARTER_OF_YEAR) {
            // The order of keys must correspond to the TextStyle.values() order.
            final String[] keys = {
                "QuarterNames",
                "standalone.QuarterNames",
                "QuarterAbbreviations",
                "standalone.QuarterAbbreviations",
                "QuarterNarrows",
                "standalone.QuarterNarrows",
            };
            for (int i = 0; i < keys.length; i++) {
                String[] names = getLocalizedResource(keys[i], locale);
                if (names != null) {
                    Map<Long, String> map = new HashMap<>();
                    for (int q = 0; q < names.length; q++) {
                        map.put((long) (q + 1), names[q]);
                    }
                    styleMap.put(TextStyle.values()[i], map);
                }
            }
            return new LocaleStore(styleMap);
        }

        return "";  // null marker for map
    }

    /**
     * Helper method to create an immutable entry.
     *
     * @param text  the text, not null
     * @param field  the field, not null
     * @return the entry, not null
     */
    private static <A, B> Entry<A, B> createEntry(A text, B field) {
        return new SimpleImmutableEntry<>(text, field);
    }

    /**
     * Returns the localized resource of the given key and locale, or null
     * if no localized resource is available.
     *
     * @param key  the key of the localized resource, not null
     * @param locale  the locale, not null
     * @return the localized resource, or null if not available
     * @throws NullPointerException if key or locale is null
     */
    @SuppressWarnings("unchecked")
    static <T> T getLocalizedResource(String key, Locale locale) {
        LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased()
                                    .getLocaleResources(
                                        CalendarDataUtility.findRegionOverride(locale));
        ResourceBundle rb = lr.getJavaTimeFormatData();
        return rb.containsKey(key) ? (T) rb.getObject(key) : null;
    }

    /**
     * Stores the text for a single locale.
     * <p>
     * Some fields have a textual representation, such as day-of-week or month-of-year.
     * These textual representations can be captured in this class for printing
     * and parsing.
     * <p>
     * This class is immutable and thread-safe.
     */
    static final class LocaleStore {
        /**
         * Map of value to text.
         */
        private final Map<TextStyle, Map<Long, String>> valueTextMap;
        /**
         * Parsable data.
         */
        private final Map<TextStyle, List<Entry<String, Long>>> parsable;

        /**
         * Constructor.
         *
         * @param valueTextMap  the map of values to text to store, assigned and not altered, not null
         */
        LocaleStore(Map<TextStyle, Map<Long, String>> valueTextMap) {
            this.valueTextMap = valueTextMap;
            Map<TextStyle, List<Entry<String, Long>>> map = new HashMap<>();
            List<Entry<String, Long>> allList = new ArrayList<>();
            for (Map.Entry<TextStyle, Map<Long, String>> vtmEntry : valueTextMap.entrySet()) {
                Map<String, Entry<String, Long>> reverse = new HashMap<>();
                for (Map.Entry<Long, String> entry : vtmEntry.getValue().entrySet()) {
                    if (reverse.put(entry.getValue(), createEntry(entry.getValue(), entry.getKey())) != null) {
                        // TODO: BUG: this has no effect
                        continue;  // not parsable, try next style
                    }
                }
                List<Entry<String, Long>> list = new ArrayList<>(reverse.values());
                Collections.sort(list, COMPARATOR);
                map.put(vtmEntry.getKey(), list);
                allList.addAll(list);
                map.put(null, allList);
            }
            Collections.sort(allList, COMPARATOR);
            this.parsable = map;
        }

        /**
         * Gets the text for the specified field value, locale and style
         * for the purpose of printing.
         *
         * @param value  the value to get text for, not null
         * @param style  the style to get text for, not null
         * @return the text for the field value, null if no text found
         */
        String getText(long value, TextStyle style) {
            Map<Long, String> map = valueTextMap.get(style);
            return map != null ? map.get(value) : null;
        }

        /**
         * Gets an iterator of text to field for the specified style for the purpose of parsing.
         * <p>
         * The iterator must be returned in order from the longest text to the shortest.
         *
         * @param style  the style to get text for, null for all parsable text
         * @return the iterator of text to field pairs, in order from longest text to shortest text,
         *  null if the style is not parsable
         */
        Iterator<Entry<String, Long>> getTextIterator(TextStyle style) {
            List<Entry<String, Long>> list = parsable.get(style);
            return list != null ? list.iterator() : null;
        }
    }
}
