/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.util.locale.provider;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.security.action.GetPropertyAction;
import sun.util.resources.LocaleData;
import sun.util.resources.OpenListResourceBundle;
import sun.util.resources.ParallelListResourceBundle;
import sun.util.resources.TimeZoneNamesBundle;

/**
 * Central accessor to locale-dependent resources for JRE/CLDR provider adapters.
 *
 * @author Masayoshi Okutsu
 * @author Naoto Sato
 */
public class LocaleResources {

    private final Locale locale;
    private final LocaleData localeData;
    private final LocaleProviderAdapter.Type type;

    // Resource cache
    private final ConcurrentMap<String, ResourceReference> cache = new ConcurrentHashMap<>();
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

    // cache key prefixes
    private static final String BREAK_ITERATOR_INFO = "BII.";
    private static final String CALENDAR_DATA = "CALD.";
    private static final String COLLATION_DATA_CACHEKEY = "COLD";
    private static final String DECIMAL_FORMAT_SYMBOLS_DATA_CACHEKEY = "DFSD";
    private static final String CURRENCY_NAMES = "CN.";
    private static final String LOCALE_NAMES = "LN.";
    private static final String TIME_ZONE_NAMES = "TZN.";
    private static final String ZONE_IDS_CACHEKEY = "ZID";
    private static final String CALENDAR_NAMES = "CALN.";
    private static final String NUMBER_PATTERNS_CACHEKEY = "NP";
    private static final String COMPACT_NUMBER_PATTERNS_CACHEKEY = "CNP";
    private static final String DATE_TIME_PATTERN = "DTP.";
    private static final String RULES_CACHEKEY = "RULE";

    // TimeZoneNamesBundle exemplar city prefix
    private static final String TZNB_EXCITY_PREFIX = "timezone.excity.";

    // null singleton cache value
    private static final Object NULLOBJECT = new Object();

    LocaleResources(ResourceBundleBasedAdapter adapter, Locale locale) {
        this.locale = locale;
        this.localeData = adapter.getLocaleData();
        type = ((LocaleProviderAdapter)adapter).getAdapterType();
    }

    private void removeEmptyReferences() {
        Object ref;
        while ((ref = referenceQueue.poll()) != null) {
            cache.remove(((ResourceReference)ref).getCacheKey());
        }
    }

    Object getBreakIteratorInfo(String key) {
        Object biInfo;
        String cacheKey = BREAK_ITERATOR_INFO + key;

        removeEmptyReferences();
        ResourceReference data = cache.get(cacheKey);
        if (data == null || ((biInfo = data.get()) == null)) {
           biInfo = localeData.getBreakIteratorInfo(locale).getObject(key);
           cache.put(cacheKey, new ResourceReference(cacheKey, biInfo, referenceQueue));
        }

       return biInfo;
    }

    byte[] getBreakIteratorResources(String key) {
        return (byte[]) localeData.getBreakIteratorResources(locale).getObject(key);
    }

    public String getCalendarData(String key) {
        String caldata = "";
        String cacheKey = CALENDAR_DATA  + key;

        removeEmptyReferences();

        ResourceReference data = cache.get(cacheKey);
        if (data == null || ((caldata = (String) data.get()) == null)) {
            ResourceBundle rb = localeData.getCalendarData(locale);
            if (rb.containsKey(key)) {
                caldata = rb.getString(key);
            }

            cache.put(cacheKey,
                      new ResourceReference(cacheKey, caldata, referenceQueue));
        }

        return caldata;
    }

    public String getCollationData() {
        String key = "Rule";
        String coldata = "";

        removeEmptyReferences();
        ResourceReference data = cache.get(COLLATION_DATA_CACHEKEY);
        if (data == null || ((coldata = (String) data.get()) == null)) {
            ResourceBundle rb = localeData.getCollationData(locale);
            if (rb.containsKey(key)) {
                coldata = rb.getString(key);
            }
            cache.put(COLLATION_DATA_CACHEKEY,
                      new ResourceReference(COLLATION_DATA_CACHEKEY, coldata, referenceQueue));
        }

        return coldata;
    }

    public Object[] getDecimalFormatSymbolsData() {
        Object[] dfsdata;

        removeEmptyReferences();
        ResourceReference data = cache.get(DECIMAL_FORMAT_SYMBOLS_DATA_CACHEKEY);
        if (data == null || ((dfsdata = (Object[]) data.get()) == null)) {
            // Note that only dfsdata[0] is prepared here in this method. Other
            // elements are provided by the caller, yet they are cached here.
            ResourceBundle rb = localeData.getNumberFormatData(locale);
            dfsdata = new Object[3];
            dfsdata[0] = getNumberStrings(rb, "NumberElements");

            cache.put(DECIMAL_FORMAT_SYMBOLS_DATA_CACHEKEY,
                      new ResourceReference(DECIMAL_FORMAT_SYMBOLS_DATA_CACHEKEY, dfsdata, referenceQueue));
        }

        return dfsdata;
    }

    private String[] getNumberStrings(ResourceBundle rb, String type) {
        String[] ret = null;
        String key;
        String numSys;

        // Number strings look up. First, try the Unicode extension
        numSys = locale.getUnicodeLocaleType("nu");
        if (numSys != null) {
            key = numSys + "." + type;
            if (rb.containsKey(key)) {
                ret = rb.getStringArray(key);
            }
        }

        // Next, try DefaultNumberingSystem value
        if (ret == null && rb.containsKey("DefaultNumberingSystem")) {
            key = rb.getString("DefaultNumberingSystem") + "." + type;
            if (rb.containsKey(key)) {
                ret = rb.getStringArray(key);
            }
        }

        // Last resort. No need to check the availability.
        // Just let it throw MissingResourceException when needed.
        if (ret == null) {
            ret = rb.getStringArray(type);
        }

        return ret;
    }

    public String getCurrencyName(String key) {
        Object currencyName = null;
        String cacheKey = CURRENCY_NAMES + key;

        removeEmptyReferences();
        ResourceReference data = cache.get(cacheKey);

        if (data != null && ((currencyName = data.get()) != null)) {
            if (currencyName.equals(NULLOBJECT)) {
                currencyName = null;
            }

            return (String) currencyName;
        }

        OpenListResourceBundle olrb = localeData.getCurrencyNames(locale);

        if (olrb.containsKey(key)) {
            currencyName = olrb.getObject(key);
            cache.put(cacheKey,
                      new ResourceReference(cacheKey, currencyName, referenceQueue));
        }

        return (String) currencyName;
    }

    public String getLocaleName(String key) {
        Object localeName = null;
        String cacheKey = LOCALE_NAMES + key;

        removeEmptyReferences();
        ResourceReference data = cache.get(cacheKey);

        if (data != null && ((localeName = data.get()) != null)) {
            if (localeName.equals(NULLOBJECT)) {
                localeName = null;
            }

            return (String) localeName;
        }

        OpenListResourceBundle olrb = localeData.getLocaleNames(locale);

        if (olrb.containsKey(key)) {
            localeName = olrb.getObject(key);
            cache.put(cacheKey,
                      new ResourceReference(cacheKey, localeName, referenceQueue));
        }

        return (String) localeName;
    }

    public Object getTimeZoneNames(String key) {
        Object val = null;
        String cacheKey = TIME_ZONE_NAMES + key;

        removeEmptyReferences();
        ResourceReference data = cache.get(cacheKey);

        if (Objects.isNull(data) || Objects.isNull(val = data.get())) {
            TimeZoneNamesBundle tznb = localeData.getTimeZoneNames(locale);
            if (key.startsWith(TZNB_EXCITY_PREFIX)) {
                if (tznb.containsKey(key)) {
                    val = tznb.getString(key);
                    assert val instanceof String;
                    trace("tznb: %s key: %s, val: %s\n", tznb, key, val);
                }
            } else {
                String[] names = null;
                if (tznb.containsKey(key)) {
                    names = tznb.getStringArray(key);
                } else {
                    var tz = TimeZoneNameUtility.canonicalTZID(key).orElse(key);
                    if (tznb.containsKey(tz)) {
                        names = tznb.getStringArray(tz);
                    }
                }

                if (names != null) {
                    names[0] = key;
                    trace("tznb: %s key: %s, names: %s, %s, %s, %s, %s, %s, %s\n", tznb, key,
                        names[0], names[1], names[2], names[3], names[4], names[5], names[6]);
                    val = names;
                }
            }
            if (val != null) {
                cache.put(cacheKey,
                          new ResourceReference(cacheKey, val, referenceQueue));
            }
        }

        return val;
    }

    @SuppressWarnings("unchecked")
    Set<String> getZoneIDs() {
        Set<String> zoneIDs;

        removeEmptyReferences();
        ResourceReference data = cache.get(ZONE_IDS_CACHEKEY);
        if (data == null || ((zoneIDs = (Set<String>) data.get()) == null)) {
            TimeZoneNamesBundle rb = localeData.getTimeZoneNames(locale);
            zoneIDs = rb.keySet();
            cache.put(ZONE_IDS_CACHEKEY,
                      new ResourceReference(ZONE_IDS_CACHEKEY, zoneIDs, referenceQueue));
        }

        return zoneIDs;
    }

    // zoneStrings are cached separately in TimeZoneNameUtility.
    String[][] getZoneStrings() {
        TimeZoneNamesBundle rb = localeData.getTimeZoneNames(locale);
        Set<String> keyset = getZoneIDs();
        // Use a LinkedHashSet to preseve the order
        Set<String[]> value = new LinkedHashSet<>();
        Set<String> tzIds = new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));
        for (String key : keyset) {
            if (!key.startsWith(TZNB_EXCITY_PREFIX)) {
                value.add(rb.getStringArray(key));
                tzIds.remove(key);
            }
        }

        if (type == LocaleProviderAdapter.Type.CLDR) {
            // Note: TimeZoneNamesBundle creates a String[] on each getStringArray call.

            // Add timezones which are not present in this keyset,
            // so that their fallback names will be generated at runtime.
            tzIds.stream().filter(i -> (!i.startsWith("Etc/GMT")
                    && !i.startsWith("GMT")
                    && !i.startsWith("SystemV")))
                    .forEach(tzid -> {
                        String[] val = new String[7];
                        if (keyset.contains(tzid)) {
                            val = rb.getStringArray(tzid);
                        } else {
                            var canonID = TimeZoneNameUtility.canonicalTZID(tzid)
                                            .orElse(tzid);
                            if (keyset.contains(canonID)) {
                                val = rb.getStringArray(canonID);
                            }
                        }
                        val[0] = tzid;
                        value.add(val);
                    });
        }
        return value.toArray(new String[0][]);
    }

    String[] getCalendarNames(String key) {
        String[] names = null;
        String cacheKey = CALENDAR_NAMES + key;

        removeEmptyReferences();
        ResourceReference data = cache.get(cacheKey);

        if (data == null || ((names = (String[]) data.get()) == null)) {
            ResourceBundle rb = localeData.getDateFormatData(locale);
            if (rb.containsKey(key)) {
                names = rb.getStringArray(key);
                cache.put(cacheKey,
                          new ResourceReference(cacheKey, names, referenceQueue));
            }
        }

        return names;
    }

    String[] getJavaTimeNames(String key) {
        String[] names = null;
        String cacheKey = CALENDAR_NAMES + key;

        removeEmptyReferences();
        ResourceReference data = cache.get(cacheKey);

        if (data == null || ((names = (String[]) data.get()) == null)) {
            ResourceBundle rb = getJavaTimeFormatData();
            if (rb.containsKey(key)) {
                names = rb.getStringArray(key);
                cache.put(cacheKey,
                          new ResourceReference(cacheKey, names, referenceQueue));
            }
        }

        return names;
    }

    public String getDateTimePattern(int timeStyle, int dateStyle, Calendar cal) {
        if (cal == null) {
            cal = Calendar.getInstance(locale);
        }
        return getDateTimePattern(null, timeStyle, dateStyle, cal.getCalendarType());
    }

    /**
     * Returns a date-time format pattern
     * @param timeStyle style of time; one of FULL, LONG, MEDIUM, SHORT in DateFormat,
     *                  or -1 if not required
     * @param dateStyle style of time; one of FULL, LONG, MEDIUM, SHORT in DateFormat,
     *                  or -1 if not required
     * @param calType   the calendar type for the pattern
     * @return the pattern string
     */
    public String getJavaTimeDateTimePattern(int timeStyle, int dateStyle, String calType) {
        calType = CalendarDataUtility.normalizeCalendarType(calType);
        String pattern;
        pattern = getDateTimePattern("java.time.", timeStyle, dateStyle, calType);
        if (pattern == null) {
            pattern = getDateTimePattern(null, timeStyle, dateStyle, calType);
        }
        return pattern;
    }

    private String getDateTimePattern(String prefix, int timeStyle, int dateStyle, String calType) {
        String pattern;
        String timePattern = null;
        String datePattern = null;

        if (timeStyle >= 0) {
            if (prefix != null) {
                timePattern = getDateTimePattern(prefix, "TimePatterns", timeStyle, calType);
            }
            if (timePattern == null) {
                timePattern = getDateTimePattern(null, "TimePatterns", timeStyle, calType);
            }
        }
        if (dateStyle >= 0) {
            if (prefix != null) {
                datePattern = getDateTimePattern(prefix, "DatePatterns", dateStyle, calType);
            }
            if (datePattern == null) {
                datePattern = getDateTimePattern(null, "DatePatterns", dateStyle, calType);
            }
        }
        if (timeStyle >= 0) {
            if (dateStyle >= 0) {
                String dateTimePattern = null;
                int dateTimeStyle = Math.max(dateStyle, timeStyle);
                if (prefix != null) {
                    dateTimePattern = getDateTimePattern(prefix, "DateTimePatterns", dateTimeStyle, calType);
                }
                if (dateTimePattern == null) {
                    dateTimePattern = getDateTimePattern(null, "DateTimePatterns", dateTimeStyle, calType);
                }
                pattern = switch (Objects.requireNonNull(dateTimePattern)) {
                    case "{1} {0}" -> datePattern + " " + timePattern;
                    case "{0} {1}" -> timePattern + " " + datePattern;
                    default -> MessageFormat.format(dateTimePattern.replaceAll("'", "''"), timePattern, datePattern);
                };
            } else {
                pattern = timePattern;
            }
        } else if (dateStyle >= 0) {
            pattern = datePattern;
        } else {
            throw new IllegalArgumentException("No date or time style specified");
        }
        return pattern;
    }

    public String[] getNumberPatterns() {
        String[] numberPatterns;

        removeEmptyReferences();
        ResourceReference data = cache.get(NUMBER_PATTERNS_CACHEKEY);

        if (data == null || ((numberPatterns = (String[]) data.get()) == null)) {
            ResourceBundle resource = localeData.getNumberFormatData(locale);
            numberPatterns = getNumberStrings(resource, "NumberPatterns");
            cache.put(NUMBER_PATTERNS_CACHEKEY,
                      new ResourceReference(NUMBER_PATTERNS_CACHEKEY, numberPatterns, referenceQueue));
        }

        return numberPatterns;
    }

    /**
     * Returns the compact number format patterns.
     * @param formatStyle the style for formatting a number
     * @return an array of compact number patterns
     */
    public String[] getCNPatterns(NumberFormat.Style formatStyle) {

        Objects.requireNonNull(formatStyle);
        String[] compactNumberPatterns;
        removeEmptyReferences();
        String width = (formatStyle == NumberFormat.Style.LONG) ? "long" : "short";
        String cacheKey = width + "." + COMPACT_NUMBER_PATTERNS_CACHEKEY;
        ResourceReference data = cache.get(cacheKey);
        if (data == null || ((compactNumberPatterns
                = (String[]) data.get()) == null)) {
            ResourceBundle resource = localeData.getNumberFormatData(locale);
            compactNumberPatterns = (String[]) resource
                    .getObject(width + ".CompactNumberPatterns");
            cache.put(cacheKey, new ResourceReference(cacheKey, compactNumberPatterns, referenceQueue));
        }
        return compactNumberPatterns;
    }


    /**
     * Returns the FormatData resource bundle of this LocaleResources.
     * The FormatData should be used only for accessing extra
     * resources required by JSR 310.
     */
    public ResourceBundle getJavaTimeFormatData() {
        ResourceBundle rb = localeData.getDateFormatData(locale);
        if (rb instanceof ParallelListResourceBundle) {
            localeData.setSupplementary((ParallelListResourceBundle) rb);
        }
        return rb;
    }

    private String getDateTimePattern(String prefix, String key, int styleIndex, String calendarType) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix);
        }
        if (!"gregory".equals(calendarType)) {
            sb.append(calendarType).append('.');
        }
        sb.append(key);
        String resourceKey = sb.toString();
        String cacheKey = sb.insert(0, DATE_TIME_PATTERN).toString();

        removeEmptyReferences();
        ResourceReference data = cache.get(cacheKey);
        Object value = NULLOBJECT;

        if (data == null || ((value = data.get()) == null)) {
            ResourceBundle r = (prefix != null) ? getJavaTimeFormatData() : localeData.getDateFormatData(locale);
            if (r.containsKey(resourceKey)) {
                value = r.getStringArray(resourceKey);
            } else {
                assert !resourceKey.equals(key);
                if (r.containsKey(key)) {
                    value = r.getStringArray(key);
                }
            }
            cache.put(cacheKey,
                      new ResourceReference(cacheKey, value, referenceQueue));
        }
        if (value == NULLOBJECT) {
            assert prefix != null;
            return null;
        }

        // for DateTimePatterns. CLDR has multiple styles, while JRE has one.
        String[] styles = (String[])value;
        return (styles.length > 1 ? styles[styleIndex] : styles[0]);
    }

    public String[] getRules() {
        String[] rules;

        removeEmptyReferences();
        ResourceReference data = cache.get(RULES_CACHEKEY);

        if (data == null || ((rules = (String[]) data.get()) == null)) {
            ResourceBundle rb = localeData.getDateFormatData(locale);
            rules = new String[2];
            rules[0] = rules[1] = "";
            if (rb.containsKey("PluralRules")) {
                rules[0] = rb.getString("PluralRules");
            }
            if (rb.containsKey("DayPeriodRules")) {
                rules[1] = rb.getString("DayPeriodRules");
            }
            cache.put(RULES_CACHEKEY, new ResourceReference(RULES_CACHEKEY, rules, referenceQueue));
        }

        return rules;
    }

    private static class ResourceReference extends SoftReference<Object> {
        private final String cacheKey;

        ResourceReference(String cacheKey, Object o, ReferenceQueue<Object> q) {
            super(o, q);
            this.cacheKey = cacheKey;
        }

        String getCacheKey() {
            return cacheKey;
        }
    }

    private static final boolean TRACE_ON = Boolean.valueOf(
        GetPropertyAction.privilegedGetProperty("locale.resources.debug", "false"));

    public static void trace(String format, Object... params) {
        if (TRACE_ON) {
            System.out.format(format, params);
        }
    }
}
