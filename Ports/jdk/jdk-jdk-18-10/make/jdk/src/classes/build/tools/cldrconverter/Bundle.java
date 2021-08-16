/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.cldrconverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

class Bundle {
    static enum Type {
        LOCALENAMES, CURRENCYNAMES, TIMEZONENAMES, CALENDARDATA, FORMATDATA;

        static EnumSet<Type> ALL_TYPES = EnumSet.of(LOCALENAMES,
                                                    CURRENCYNAMES,
                                                    TIMEZONENAMES,
                                                    CALENDARDATA,
                                                    FORMATDATA);
    }

    private final static Map<String, Bundle> bundles = new HashMap<>();

    private final static String[] NUMBER_PATTERN_KEYS = {
        "NumberPatterns/decimal",
        "NumberPatterns/currency",
        "NumberPatterns/percent",
        "NumberPatterns/accounting"
    };

    private final static String[] COMPACT_NUMBER_PATTERN_KEYS = {
            "short.CompactNumberPatterns",
            "long.CompactNumberPatterns"
    };

    private final static String[] NUMBER_ELEMENT_KEYS = {
        "NumberElements/decimal",
        "NumberElements/group",
        "NumberElements/list",
        "NumberElements/percent",
        "NumberElements/zero",
        "NumberElements/pattern",
        "NumberElements/minus",
        "NumberElements/exponential",
        "NumberElements/permille",
        "NumberElements/infinity",
        "NumberElements/nan",
        "NumberElements/currencyDecimal",
        "NumberElements/currencyGroup",
    };

    private final static String[] TIME_PATTERN_KEYS = {
        "DateTimePatterns/full-time",
        "DateTimePatterns/long-time",
        "DateTimePatterns/medium-time",
        "DateTimePatterns/short-time",
    };

    private final static String[] DATE_PATTERN_KEYS = {
        "DateTimePatterns/full-date",
        "DateTimePatterns/long-date",
        "DateTimePatterns/medium-date",
        "DateTimePatterns/short-date",
    };

    private final static String[] DATETIME_PATTERN_KEYS = {
        "DateTimePatterns/full-dateTime",
        "DateTimePatterns/long-dateTime",
        "DateTimePatterns/medium-dateTime",
        "DateTimePatterns/short-dateTime",
    };

    private final static String[] ERA_KEYS = {
        "long.Eras",
        "Eras",
        "narrow.Eras"
    };

    // Keys for individual time zone names
    private final static String TZ_GEN_LONG_KEY = "timezone.displayname.generic.long";
    private final static String TZ_GEN_SHORT_KEY = "timezone.displayname.generic.short";
    private final static String TZ_STD_LONG_KEY = "timezone.displayname.standard.long";
    private final static String TZ_STD_SHORT_KEY = "timezone.displayname.standard.short";
    private final static String TZ_DST_LONG_KEY = "timezone.displayname.daylight.long";
    private final static String TZ_DST_SHORT_KEY = "timezone.displayname.daylight.short";
    private final static String[] ZONE_NAME_KEYS = {
        TZ_STD_LONG_KEY,
        TZ_STD_SHORT_KEY,
        TZ_DST_LONG_KEY,
        TZ_DST_SHORT_KEY,
        TZ_GEN_LONG_KEY,
        TZ_GEN_SHORT_KEY
    };

    private final String id;
    private final String cldrPath;
    private final EnumSet<Type> bundleTypes;
    private final String currencies;
    private Map<String, Object> targetMap;

    static Bundle getBundle(String id) {
        return bundles.get(id);
    }

    @SuppressWarnings("ConvertToStringSwitch")
    Bundle(String id, String cldrPath, String bundles, String currencies) {
        this.id = id;
        this.cldrPath = cldrPath;
        if ("localenames".equals(bundles)) {
            bundleTypes = EnumSet.of(Type.LOCALENAMES);
        } else if ("currencynames".equals(bundles)) {
            bundleTypes = EnumSet.of(Type.CURRENCYNAMES);
        } else {
            bundleTypes = Type.ALL_TYPES;
        }
        if (currencies == null) {
            currencies = "local";
        }
        this.currencies = currencies;
        addBundle();
    }

    private void addBundle() {
        Bundle.bundles.put(id, this);
    }

    String getID() {
        return id;
    }

    boolean isRoot() {
        return "root".equals(id);
    }

    String getCLDRPath() {
        return cldrPath;
    }

    EnumSet<Type> getBundleTypes() {
        return bundleTypes;
    }

    String getCurrencies() {
        return currencies;
    }

    /**
     * Generate a map that contains all the data that should be
     * visible for the bundle's locale
     */
    Map<String, Object> getTargetMap() throws Exception {
        if (targetMap != null) {
            return targetMap;
        }

        String[] cldrBundles = getCLDRPath().split(",");

        // myMap contains resources for id.
        @SuppressWarnings("unchecked")
        Map<String, Object> myMap = new HashMap<>();
        int index;
        for (index = 0; index < cldrBundles.length; index++) {
            if (cldrBundles[index].equals(id)) {
                myMap.putAll(CLDRConverter.getCLDRBundle(cldrBundles[index]));
                break;
            }
        }

        // parentsMap contains resources from id's parents.
        Map<String, Object> parentsMap = new HashMap<>();
        for (int i = cldrBundles.length - 1; i > index; i--) {
            if (!("no".equals(cldrBundles[i]) || cldrBundles[i].startsWith("no_"))) {
                parentsMap.putAll(CLDRConverter.getCLDRBundle(cldrBundles[i]));
            }
        }
        // Duplicate myMap as parentsMap for "root" so that the
        // fallback works. This is a hack, though.
        if ("root".equals(cldrBundles[0])) {
            assert parentsMap.isEmpty();
            parentsMap.putAll(myMap);
        }

        // merge individual strings into arrays

        // if myMap has any of the NumberPatterns/NumberElements members, create a
        // complete array of patterns/elements.
        @SuppressWarnings("unchecked")
        List<String> scripts = (List<String>) myMap.get("numberingScripts");
        if (scripts != null) {
            for (String script : scripts) {
                myMap.put(script + ".NumberPatterns",
                        createNumberArray(myMap, parentsMap, NUMBER_PATTERN_KEYS, script));
                myMap.put(script + ".NumberElements",
                        createNumberArray(myMap, parentsMap, NUMBER_ELEMENT_KEYS, script));
            }
        }

        for (String k : COMPACT_NUMBER_PATTERN_KEYS) {
            @SuppressWarnings("unchecked")
            List<String> patterns = (List<String>) myMap.remove(k);
            if (patterns != null) {
                // Convert the map value from List<String> to String[], replacing any missing
                // entry from the parents map, if any.
                @SuppressWarnings("unchecked")
                final List<String> pList = (List<String>)parentsMap.get(k);
                int size = patterns.size();
                int psize = pList != null ? pList.size() : 0;
                String[] arrPatterns = IntStream.range(0, Math.max(size, psize))
                    .mapToObj(i -> {
                        String pattern;
                        // first try itself.
                        if (i < size) {
                            pattern = patterns.get(i);
                            if (!pattern.isEmpty()) {
                                return "{" + pattern + "}";
                            }
                        }
                        // if not found, try parent
                        if (i < psize) {
                            pattern = pList.get(i);
                            if (!pattern.isEmpty()) {
                                return "{" + pattern + "}";
                            }
                        }
                        // bail out with empty string
                        return "";
                    })
                    .toArray(String[]::new);
                myMap.put(k, arrPatterns);
            }
        }

        // Processes aliases here
        CLDRConverter.handleAliases(myMap);

        // another hack: parentsMap is not used for date-time resources.
        if ("root".equals(id)) {
            parentsMap = null;
        }

        for (CalendarType calendarType : CalendarType.values()) {
            String calendarPrefix = calendarType.keyElementName();
            // handle multiple inheritance for month and day names
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "MonthNames");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "MonthAbbreviations");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "MonthNarrows");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "DayNames");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "DayAbbreviations");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "DayNarrows");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "AmPmMarkers");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "narrow.AmPmMarkers");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "abbreviated.AmPmMarkers");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "QuarterNames");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "QuarterAbbreviations");
            handleMultipleInheritance(myMap, parentsMap, calendarPrefix + "QuarterNarrows");

            adjustEraNames(myMap, parentsMap, calendarType);

            handleDateTimeFormatPatterns(TIME_PATTERN_KEYS, myMap, parentsMap, calendarType, "TimePatterns");
            handleDateTimeFormatPatterns(DATE_PATTERN_KEYS, myMap, parentsMap, calendarType, "DatePatterns");
            handleDateTimeFormatPatterns(DATETIME_PATTERN_KEYS, myMap, parentsMap, calendarType, "DateTimePatterns");
        }

        // First, weed out any empty timezone or metazone names from myMap.
        for (Iterator<String> it = myMap.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            if (key.startsWith(CLDRConverter.TIMEZONE_ID_PREFIX)
                    || key.startsWith(CLDRConverter.METAZONE_ID_PREFIX)) {
                @SuppressWarnings("unchecked")
                Map<String, String> nameMap = (Map<String, String>) myMap.get(key);
                if (nameMap.isEmpty()) {
                    // Some zones have only exemplarCity, which become empty.
                    // Remove those from the map.
                    it.remove();
                    continue;
                }
            }
        }
        for (Iterator<String> it = myMap.keySet().iterator(); it.hasNext();) {
            String key = it.next();
                if (key.startsWith(CLDRConverter.TIMEZONE_ID_PREFIX)
                    || key.startsWith(CLDRConverter.METAZONE_ID_PREFIX)) {
                @SuppressWarnings("unchecked")
                Map<String, String> nameMap = (Map<String, String>) myMap.get(key);

                // Convert key/value pairs to an array.
                String[] names = new String[ZONE_NAME_KEYS.length];
                int ix = 0;
                for (String nameKey : ZONE_NAME_KEYS) {
                    String name = nameMap.get(nameKey);
                    if (name == null && parentsMap != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> parentNames = (Map<String, String>) parentsMap.get(key);
                        if (parentNames != null) {
                            name = parentNames.get(nameKey);
                        }
                    }
                    names[ix++] = name;
                }
                if (hasNulls(names)) {
                    String metaKey = toMetaZoneKey(key);
                    if (metaKey != null) {
                        Object obj = myMap.get(metaKey);
                        if (obj instanceof String[]) {
                            String[] metaNames = (String[]) obj;
                            for (int i = 0; i < names.length; i++) {
                                if (names[i] == null) {
                                    names[i] = metaNames[i];
                                }
                            }
                        } else if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, String> m = (Map<String, String>) obj;
                            for (int i = 0; i < names.length; i++) {
                                if (names[i] == null) {
                                    names[i] = m.get(ZONE_NAME_KEYS[i]);
                                }
                            }
                        }
                    }
                }
                // replace the Map with the array
                if (names != null) {
                    myMap.put(key, names);
                } else {
                    it.remove();
                }
            }
        }
        // replace empty era names with parentMap era names
        for (String key : ERA_KEYS) {
            Object value = myMap.get(key);
            if (value != null && value instanceof String[]) {
                String[] eraStrings = (String[]) value;
                for (String eraString : eraStrings) {
                    if (eraString == null || eraString.isEmpty()) {
                        fillInElements(parentsMap, key, value);
                    }
                }
            }
        }

        // rules
        String rule = CLDRConverter.pluralRules.get(id);
        if (rule != null) {
            myMap.put("PluralRules", rule);
        }
        rule = CLDRConverter.dayPeriodRules.get(id);
        if (rule != null) {
            myMap.put("DayPeriodRules", rule);
        }

        // Remove all duplicates
        if (Objects.nonNull(parentsMap)) {
            for (Iterator<String> it = myMap.keySet().iterator(); it.hasNext();) {
                String key = it.next();
                if (!key.equals("numberingScripts") && // real body "NumberElements" may differ
                    Objects.deepEquals(parentsMap.get(key), myMap.get(key))) {
                    it.remove();
                }
            }
        }

        targetMap = myMap;
        return myMap;
    }

    private void handleMultipleInheritance(Map<String, Object> map, Map<String, Object> parents, String key) {
        String formatMapKey = key + "/format";
        Object format = map.get(formatMapKey);
        if (format != null) {
            map.remove(formatMapKey);
            map.put(key, format);
            if (fillInElements(parents, formatMapKey, format)) {
                map.remove(key);
            }
        }
        String standaloneMapKey = key + "/stand-alone";
        Object standalone = map.get(standaloneMapKey);
        if (standalone != null) {
            map.remove(standaloneMapKey);
            String standaloneResourceKey = "standalone." + key;
            map.put(standaloneResourceKey, standalone);
            if (fillInElements(parents, standaloneMapKey, standalone)) {
                map.remove(standaloneResourceKey);
            }
        }
    }

    /**
     * Fills in any empty elements with its parent element, falling back to
     * aliased one if parent element is not found. Returns true if the resulting
     * array is identical to its parent array.
     *
     * @param parents
     * @param key
     * @param value
     * @return true if the resulting array is identical to its parent array.
     */
    private boolean fillInElements(Map<String, Object> parents, String key, Object value) {
        if (parents == null) {
            return false;
        }
        if (value instanceof String[]) {
            Object pvalue = parents.getOrDefault(key, parents.get(CLDRConverter.aliases.get(key)));
            if (pvalue != null && pvalue instanceof String[]) {
                String[] strings = (String[]) value;
                String[] pstrings = (String[]) pvalue;
                for (int i = 0; i < strings.length; i++) {
                    if (strings[i] == null || strings[i].length() == 0) {
                        strings[i] = pstrings[i];
                    }
                }
                return Arrays.equals(strings, pstrings);
            }
        }
        return false;
    }

    /*
     * Adjusts String[] for era names because JRE's Calendars use different
     * ERA value indexes in the Buddhist, Japanese Imperial, and Islamic calendars.
     */
    private void adjustEraNames(Map<String, Object> map, Map<String, Object> pMap, CalendarType type) {
        String[][] eraNames = new String[ERA_KEYS.length][];
        String[] realKeys = new String[ERA_KEYS.length];
        int index = 0;
        for (String key : ERA_KEYS) {
            String realKey = type.keyElementName() + key;
            String[] value = (String[]) map.get(realKey);
            if (value != null) {
                // first fill in missing elements from parents map.
                fillInElements(pMap, realKey, value);

                switch (type) {
                case GREGORIAN:
                    break;

                case JAPANESE:
                    {
                        String[] newValue = new String[value.length + 1];
                        String[] julianEras = (String[]) map.get(key);
                        if (julianEras != null && julianEras.length >= 2) {
                            newValue[0] = julianEras[1];
                        } else {
                            newValue[0] = "";
                        }
                        System.arraycopy(value, 0, newValue, 1, value.length);
                        value = newValue;

                        // fix up 'Reiwa' era, which can be missing in some locales
                        if (value[value.length - 1] == null) {
                            value[value.length - 1] = (key.startsWith("narrow.") ? "R" : "Reiwa");
                        }
                    }
                    break;

                case BUDDHIST:
                    // Replace the value
                    value = new String[] {"BC", value[0]};
                    break;

                case ISLAMIC:
                    // Replace the value
                    value = new String[] {"", value[0]};
                    break;
                }
                if (!key.equals(realKey)) {
                    map.put(realKey, value);
                    map.put("java.time." + realKey, value);
                }
            }
            realKeys[index] = realKey;
            eraNames[index++] = value;
        }
        for (int i = 0; i < eraNames.length; i++) {
            if (eraNames[i] == null) {
                map.put(realKeys[i], null);
            }
        }
    }

    private void handleDateTimeFormatPatterns(String[] patternKeys, Map<String, Object> myMap, Map<String, Object> parentsMap,
                                              CalendarType calendarType, String name) {
        String calendarPrefix = calendarType.keyElementName();
        for (String k : patternKeys) {
            if (myMap.containsKey(calendarPrefix + k)) {
                int len = patternKeys.length;
                List<String> dateTimePatterns = new ArrayList<>(len);
                List<String> sdfPatterns = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    String key = calendarPrefix + patternKeys[i];
                    String pattern = (String) myMap.remove(key);
                    if (pattern == null) {
                        pattern = (String) parentsMap.remove(key);
                    }
                    if (pattern != null) {
                        // Perform date-time format pattern conversion which is
                        // applicable to both SimpleDateFormat and j.t.f.DateTimeFormatter.
                        String transPattern = translateDateFormatLetters(calendarType, pattern, this::convertDateTimePatternLetter);
                        dateTimePatterns.add(i, transPattern);
                        // Additionally, perform SDF specific date-time format pattern conversion
                        sdfPatterns.add(i, translateDateFormatLetters(calendarType, transPattern, this::convertSDFLetter));
                    } else {
                        dateTimePatterns.add(i, null);
                        sdfPatterns.add(i, null);
                    }
                }
                // If empty, discard patterns
                if (sdfPatterns.isEmpty()) {
                    return;
                }
                String key = calendarPrefix + name;

                // If additional changes are made in the SDF specific conversion,
                // keep the commonly converted patterns as java.time patterns
                if (!dateTimePatterns.equals(sdfPatterns)) {
                    myMap.put("java.time." + key, dateTimePatterns.toArray(String[]::new));
                }
                myMap.put(key, sdfPatterns.toArray(new String[len]));
                break;
            }
        }
    }

    private String translateDateFormatLetters(CalendarType calendarType, String cldrFormat, ConvertDateTimeLetters converter) {
        String pattern = cldrFormat;
        int length = pattern.length();
        boolean inQuote = false;
        StringBuilder jrePattern = new StringBuilder(length);
        int count = 0;
        char lastLetter = 0;

        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);

            if (c == '\'') {
                // '' is treated as a single quote regardless of being
                // in a quoted section.
                if ((i + 1) < length) {
                    char nextc = pattern.charAt(i + 1);
                    if (nextc == '\'') {
                        i++;
                        if (count != 0) {
                            converter.convert(calendarType, lastLetter, count, jrePattern);
                            lastLetter = 0;
                            count = 0;
                        }
                        jrePattern.append("''");
                        continue;
                    }
                }
                if (!inQuote) {
                    if (count != 0) {
                        converter.convert(calendarType, lastLetter, count, jrePattern);
                        lastLetter = 0;
                        count = 0;
                    }
                    inQuote = true;
                } else {
                    inQuote = false;
                }
                jrePattern.append(c);
                continue;
            }
            if (inQuote) {
                jrePattern.append(c);
                continue;
            }
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z')) {
                if (count != 0) {
                    converter.convert(calendarType, lastLetter, count, jrePattern);
                    lastLetter = 0;
                    count = 0;
                }
                jrePattern.append(c);
                continue;
            }

            if (lastLetter == 0 || lastLetter == c) {
                lastLetter = c;
                count++;
                continue;
            }
            converter.convert(calendarType, lastLetter, count, jrePattern);
            lastLetter = c;
            count = 1;
        }

        if (inQuote) {
            throw new InternalError("Unterminated quote in date-time pattern: " + cldrFormat);
        }

        if (count != 0) {
            converter.convert(calendarType, lastLetter, count, jrePattern);
        }
        if (cldrFormat.contentEquals(jrePattern)) {
            return cldrFormat;
        }
        return jrePattern.toString();
    }

    private String toMetaZoneKey(String tzKey) {
        if (tzKey.startsWith(CLDRConverter.TIMEZONE_ID_PREFIX)) {
            String tz = tzKey.substring(CLDRConverter.TIMEZONE_ID_PREFIX.length());
            String meta = CLDRConverter.handlerMetaZones.get(tz);
            if (meta != null) {
                return CLDRConverter.METAZONE_ID_PREFIX + meta;
            }
        }
        return null;
    }

    /**
     * Perform a generic conversion of CLDR date-time format pattern letter based
     * on the support given by the SimpleDateFormat and the j.t.f.DateTimeFormatter
     * for date-time formatting.
     */
    private void convertDateTimePatternLetter(CalendarType calendarType, char cldrLetter, int count, StringBuilder sb) {
        switch (cldrLetter) {
            case 'u':
                // Change cldr letter 'u' to 'y', as 'u' is interpreted as
                // "Extended year (numeric)" in CLDR/LDML,
                // which is not supported in SimpleDateFormat and
                // j.t.f.DateTimeFormatter, so it is replaced with 'y'
                // as the best approximation
                appendN('y', count, sb);
                break;
            default:
                appendN(cldrLetter, count, sb);
                break;

        }
    }

    /**
     * Perform a conversion of CLDR date-time format pattern letter which is
     * specific to the SimpleDateFormat.
     */
    private void convertSDFLetter(CalendarType calendarType, char cldrLetter, int count, StringBuilder sb) {
        switch (cldrLetter) {
            case 'G':
                if (calendarType != CalendarType.GREGORIAN) {
                    // Adjust the number of 'G's for JRE SimpleDateFormat
                    if (count == 5) {
                        // CLDR narrow -> JRE short
                        count = 1;
                    } else if (count == 1) {
                        // CLDR abbr -> JRE long
                        count = 4;
                    }
                }
                appendN(cldrLetter, count, sb);
                break;

            // TODO: support 'c' and 'e' in JRE SimpleDateFormat
            // Use 'u' and 'E' for now.
            case 'c':
            case 'e':
                switch (count) {
                    case 1:
                        sb.append('u');
                        break;
                    case 3:
                    case 4:
                        appendN('E', count, sb);
                        break;
                    case 5:
                        appendN('E', 3, sb);
                        break;
                }
                break;

            case 'v':
            case 'V':
                appendN('z', count, sb);
                break;

            case 'Z':
                if (count == 4 || count == 5) {
                    sb.append("XXX");
                }
                break;

            case 'B':
                // 'B' character (day period) is not supported by SimpleDateFormat,
                // this is a workaround in which 'B' character
                // appearing in CLDR date-time pattern is replaced
                // with 'a' character and hence resolved with am/pm strings.
                // This workaround is based on the the fallback mechanism
                // specified in LDML spec for 'B' character, when a locale
                // does not have data for day period ('B')
                appendN('a', count, sb);
                break;

            default:
                appendN(cldrLetter, count, sb);
                break;
        }
    }

    private void appendN(char c, int n, StringBuilder sb) {
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
    }

    private static boolean hasNulls(Object[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface ConvertDateTimeLetters {
        void convert(CalendarType calendarType, char cldrLetter, int count, StringBuilder sb);
    }

    /**
     * Returns a complete string array for NumberElements or NumberPatterns. If any
     * array element is missing, it will fall back to parents map, as well as
     * numbering script fallback.
     */
    private String[] createNumberArray(Map<String, Object> myMap, Map<String, Object>parentsMap,
                                        String[] keys, String script) {
        String[] numArray = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            String key = script + "." + keys[i];
            final int idx = i;
            Optional.ofNullable(
                myMap.getOrDefault(key,
                    // if value not found in myMap, search for parentsMap
                    parentsMap.getOrDefault(key,
                        parentsMap.getOrDefault(keys[i],
                            // the last resort is "latn"
                            parentsMap.get("latn." + keys[i])))))
                .ifPresentOrElse(v -> numArray[idx] = (String)v, () -> {
                    if (keys == NUMBER_PATTERN_KEYS) {
                        // NumberPatterns
                        if (!key.endsWith("accounting")) {
                            // throw error unless it is for "accounting",
                            // which may be missing.
                            throw new InternalError("NumberPatterns: null for " +
                                                    key + ", id: " + id);
                        }
                    } else {
                        // NumberElements
                        assert keys == NUMBER_ELEMENT_KEYS;
                        if (key.endsWith("/pattern")) {
                            numArray[idx] = "#";
                        } else if (!key.endsWith("currencyDecimal") &&
                                   !key.endsWith("currencyGroup")) {
                            // throw error unless it is for "currencyDecimal/Group",
                            // which may be missing.
                            throw new InternalError("NumberElements: null for " +
                                                    key + ", id: " + id);
                        }
                    }});
        }
        return numArray;
    }
}
