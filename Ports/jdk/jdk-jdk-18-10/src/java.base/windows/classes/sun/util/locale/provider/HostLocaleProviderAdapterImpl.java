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
package sun.util.locale.provider;

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.text.spi.DateFormatProvider;
import java.text.spi.DateFormatSymbolsProvider;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.text.spi.NumberFormatProvider;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.spi.CalendarDataProvider;
import java.util.spi.CalendarNameProvider;
import java.util.spi.CurrencyNameProvider;
import java.util.spi.LocaleNameProvider;
import sun.text.spi.JavaTimeDateTimePatternProvider;
import sun.util.spi.CalendarProvider;

/**
 * LocaleProviderdapter implementation for the Windows locale data.
 *
 * @author Naoto Sato
 */
public class HostLocaleProviderAdapterImpl {

    // locale categories
    private static final int CAT_DISPLAY = 0;
    private static final int CAT_FORMAT  = 1;

    // NumberFormat styles
    private static final int NF_NUMBER   = 0;
    private static final int NF_CURRENCY = 1;
    private static final int NF_PERCENT  = 2;
    private static final int NF_INTEGER  = 3;
    private static final int NF_MAX = NF_INTEGER;

    // CalendarData value types
    private static final int CD_FIRSTDAYOFWEEK = 0;
    private static final int CD_FIRSTWEEKOFYEAR = 1;

    // Currency/Locale display name types
    private static final int DN_CURRENCY_NAME   = 0;
    private static final int DN_CURRENCY_SYMBOL = 1;
    private static final int DN_LOCALE_LANGUAGE = 2;
    private static final int DN_LOCALE_SCRIPT   = 3;
    private static final int DN_LOCALE_REGION   = 4;
    private static final int DN_LOCALE_VARIANT  = 5;

    // Windows Calendar IDs
    private static final int CAL_JAPAN  = 3;

    // Native Calendar ID to LDML calendar type map
    private static final String[] calIDToLDML = {
        "",
        "gregory",
        "gregory_en-US",
        "japanese",
        "roc",
        "",          // No appropriate type for CAL_KOREA
        "islamic",
        "buddhist",
        "hebrew",
        "gregory_fr",
        "gregory_ar",
        "gregory_en",
        "gregory_fr", "", "", "", "", "", "", "", "", "", "",
        "islamic-umalqura",
    };

    // Caches
    private static final ConcurrentMap<Locale, SoftReference<AtomicReferenceArray<String>>> dateFormatCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Locale, SoftReference<DateFormatSymbols>> dateFormatSymbolsCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Locale, SoftReference<AtomicReferenceArray<String>>> numberFormatCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Locale, SoftReference<DecimalFormatSymbols>> decimalFormatSymbolsCache = new ConcurrentHashMap<>();

    private static final Set<Locale> supportedLocaleSet;
    private static final String nativeDisplayLanguage;
    static {
        Set<Locale> tmpSet = new HashSet<>();
        if (initialize()) {
            // Assuming the default locales do not include any extensions, so
            // no stripping is needed here.
            Control c = Control.getNoFallbackControl(Control.FORMAT_DEFAULT);
            String displayLocale = getDefaultLocale(CAT_DISPLAY);
            Locale l = Locale.forLanguageTag(displayLocale.replace('_', '-'));
            tmpSet.addAll(c.getCandidateLocales("", l));
            nativeDisplayLanguage = l.getLanguage();

            String formatLocale = getDefaultLocale(CAT_FORMAT);
            if (!formatLocale.equals(displayLocale)) {
                l = Locale.forLanguageTag(formatLocale.replace('_', '-'));
                tmpSet.addAll(c.getCandidateLocales("", l));
            }
        } else {
            nativeDisplayLanguage = "";
        }
        supportedLocaleSet = Collections.unmodifiableSet(tmpSet);
    }
    private static final Locale[] supportedLocale = supportedLocaleSet.toArray(new Locale[0]);

    public static DateFormatProvider getDateFormatProvider() {
        return new DateFormatProvider() {
            @Override
            public Locale[] getAvailableLocales() {
                return getSupportedCalendarLocales();
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return isSupportedCalendarLocale(locale);
            }

            @Override
            public DateFormat getDateInstance(int style, Locale locale) {
                AtomicReferenceArray<String> patterns = getDateTimePatterns(locale);
                return new SimpleDateFormat(patterns.get(style/2),
                                            getCalendarLocale(locale));
            }

            @Override
            public DateFormat getTimeInstance(int style, Locale locale) {
                AtomicReferenceArray<String> patterns = getDateTimePatterns(locale);
                return new SimpleDateFormat(patterns.get(style/2+2),
                                            getCalendarLocale(locale));
            }

            @Override
            public DateFormat getDateTimeInstance(int dateStyle,
                    int timeStyle, Locale locale) {
                AtomicReferenceArray<String> patterns = getDateTimePatterns(locale);
                String pattern = new StringBuilder(patterns.get(dateStyle/2))
                                       .append(" ")
                                       .append(patterns.get(timeStyle/2+2))
                                       .toString();
                return new SimpleDateFormat(pattern, getCalendarLocale(locale));
            }

            private AtomicReferenceArray<String> getDateTimePatterns(Locale locale) {
                AtomicReferenceArray<String> patterns;
                SoftReference<AtomicReferenceArray<String>> ref = dateFormatCache.get(locale);

                if (ref == null || (patterns = ref.get()) == null) {
                    String langtag = removeExtensions(locale).toLanguageTag();
                    patterns = new AtomicReferenceArray<>(4);
                    patterns.compareAndSet(0, null, convertDateTimePattern(
                        getDateTimePattern(DateFormat.LONG, -1, langtag)));
                    patterns.compareAndSet(1, null, convertDateTimePattern(
                        getDateTimePattern(DateFormat.SHORT, -1, langtag)));
                    patterns.compareAndSet(2, null, convertDateTimePattern(
                        getDateTimePattern(-1, DateFormat.LONG, langtag)));
                    patterns.compareAndSet(3, null, convertDateTimePattern(
                        getDateTimePattern(-1, DateFormat.SHORT, langtag)));
                    ref = new SoftReference<>(patterns);
                    dateFormatCache.put(locale, ref);
                }

                return patterns;
            }
        };
    }

    public static DateFormatSymbolsProvider getDateFormatSymbolsProvider() {
        return new DateFormatSymbolsProvider() {

            @Override
            public Locale[] getAvailableLocales() {
                return getSupportedCalendarLocales();
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return isSupportedCalendarLocale(locale);
            }

            @Override
            public DateFormatSymbols getInstance(Locale locale) {
                DateFormatSymbols dfs;
                SoftReference<DateFormatSymbols> ref =
                    dateFormatSymbolsCache.get(locale);

                if (ref == null || (dfs = ref.get()) == null) {
                    dfs = new DateFormatSymbols(locale);
                    String langTag = removeExtensions(locale).toLanguageTag();

                    dfs.setAmPmStrings(getAmPmStrings(langTag, dfs.getAmPmStrings()));
                    dfs.setEras(getEras(langTag, dfs.getEras()));
                    dfs.setMonths(getMonths(langTag, dfs.getMonths()));
                    dfs.setShortMonths(getShortMonths(langTag, dfs.getShortMonths()));
                    dfs.setWeekdays(getWeekdays(langTag, dfs.getWeekdays()));
                    dfs.setShortWeekdays(getShortWeekdays(langTag, dfs.getShortWeekdays()));
                    ref = new SoftReference<>(dfs);
                    dateFormatSymbolsCache.put(locale, ref);
                }
                return (DateFormatSymbols)dfs.clone();
            }
        };
    }

    public static NumberFormatProvider getNumberFormatProvider() {
        return new NumberFormatProvider() {

            @Override
            public Locale[] getAvailableLocales() {
                return getSupportedNativeDigitLocales();
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return isSupportedNativeDigitLocale(locale);
            }

            @Override
            public NumberFormat getCurrencyInstance(Locale locale) {
                AtomicReferenceArray<String> patterns = getNumberPatterns(locale);
                return new DecimalFormat(patterns.get(NF_CURRENCY),
                    DecimalFormatSymbols.getInstance(locale));
            }

            @Override
            public NumberFormat getIntegerInstance(Locale locale) {
                AtomicReferenceArray<String> patterns = getNumberPatterns(locale);
                DecimalFormat format = new DecimalFormat(patterns.get(NF_INTEGER),
                    DecimalFormatSymbols.getInstance(locale));
                return HostLocaleProviderAdapter.makeIntegerFormatter(format);
            }

            @Override
            public NumberFormat getNumberInstance(Locale locale) {
                AtomicReferenceArray<String> patterns = getNumberPatterns(locale);
                return new DecimalFormat(patterns.get(NF_NUMBER),
                    DecimalFormatSymbols.getInstance(locale));
            }

            @Override
            public NumberFormat getPercentInstance(Locale locale) {
                AtomicReferenceArray<String> patterns = getNumberPatterns(locale);
                return new DecimalFormat(patterns.get(NF_PERCENT),
                    DecimalFormatSymbols.getInstance(locale));
            }

            private AtomicReferenceArray<String> getNumberPatterns(Locale locale) {
                AtomicReferenceArray<String> patterns;
                SoftReference<AtomicReferenceArray<String>> ref = numberFormatCache.get(locale);

                if (ref == null || (patterns = ref.get()) == null) {
                    String langtag = locale.toLanguageTag();
                    patterns = new AtomicReferenceArray<>(NF_MAX+1);
                    for (int i = 0; i <= NF_MAX; i++) {
                        patterns.compareAndSet(i, null, getNumberPattern(i, langtag));
                    }
                    ref = new SoftReference<>(patterns);
                    numberFormatCache.put(locale, ref);
                }
                return patterns;
            }
        };
    }

    public static DecimalFormatSymbolsProvider getDecimalFormatSymbolsProvider() {
        return new DecimalFormatSymbolsProvider() {

            @Override
            public Locale[] getAvailableLocales() {
                return getSupportedNativeDigitLocales();
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return isSupportedNativeDigitLocale(locale);
            }

            @Override
            public DecimalFormatSymbols getInstance(Locale locale) {
                DecimalFormatSymbols dfs;
                SoftReference<DecimalFormatSymbols> ref =
                    decimalFormatSymbolsCache.get(locale);

                if (ref == null || (dfs = ref.get()) == null) {
                    dfs = new DecimalFormatSymbols(getNumberLocale(locale));
                    String langTag = removeExtensions(locale).toLanguageTag();

                    // DecimalFormatSymbols.setInternationalCurrencySymbol() has
                    // a side effect of setting the currency symbol as well. So
                    // the calling order is relevant here.
                    dfs.setInternationalCurrencySymbol(getInternationalCurrencySymbol(langTag, dfs.getInternationalCurrencySymbol()));
                    dfs.setCurrencySymbol(getCurrencySymbol(langTag, dfs.getCurrencySymbol()));
                    dfs.setDecimalSeparator(getDecimalSeparator(langTag, dfs.getDecimalSeparator()));
                    dfs.setGroupingSeparator(getGroupingSeparator(langTag, dfs.getGroupingSeparator()));
                    dfs.setInfinity(getInfinity(langTag, dfs.getInfinity()));
                    dfs.setMinusSign(getMinusSign(langTag, dfs.getMinusSign()));
                    dfs.setMonetaryDecimalSeparator(getMonetaryDecimalSeparator(langTag, dfs.getMonetaryDecimalSeparator()));
                    dfs.setNaN(getNaN(langTag, dfs.getNaN()));
                    dfs.setPercent(getPercent(langTag, dfs.getPercent()));
                    dfs.setPerMill(getPerMill(langTag, dfs.getPerMill()));
                    dfs.setZeroDigit(getZeroDigit(langTag, dfs.getZeroDigit()));
                    ref = new SoftReference<>(dfs);
                    decimalFormatSymbolsCache.put(locale, ref);
                }
                return (DecimalFormatSymbols)dfs.clone();
            }
        };
    }

    public static CalendarDataProvider getCalendarDataProvider() {
        return new CalendarDataProvider() {
            @Override
            public Locale[] getAvailableLocales() {
                return getSupportedCalendarLocales();
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return isSupportedCalendarLocale(locale);
            }

            @Override
            public int getFirstDayOfWeek(Locale locale) {
                int first = getCalendarDataValue(
                                 removeExtensions(locale).toLanguageTag(),
                                 CD_FIRSTDAYOFWEEK);
                if (first != -1) {
                    return (first + 1) % 7 + 1;
                } else {
                    return 0;
                }
            }

            @Override
            public int getMinimalDaysInFirstWeek(Locale locale) {
                int firstWeek = getCalendarDataValue(
                        removeExtensions(locale).toLanguageTag(),
                        CD_FIRSTWEEKOFYEAR);
                // Interpret the value from Windows LOCALE_IFIRSTWEEKOFYEAR setting
                return switch (firstWeek) {
                    case 1 -> 7; // First full week following 1/1 is the first week of the year.
                    case 2 -> 4; // First week containing at least four days is the first week of the year.
                    default -> 1; // First week can be a single day, if 1/1 falls on the last day of the week.
                };
            }
        };
    }

    public static CalendarNameProvider getCalendarNameProvider() {
        return new CalendarNameProvider() {
            @Override
            public Locale[] getAvailableLocales() {
                return getSupportedCalendarLocales();
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return isSupportedCalendarLocale(locale);
            }

            @Override
            public String getDisplayName(String calendarType, int field,
                int value, int style, Locale locale) {
                String[] names = getCalendarDisplayStrings(removeExtensions(locale).toLanguageTag(),
                            getCalendarIDFromLDMLType(calendarType), field, style);
                if (field == Calendar.DAY_OF_WEEK) {
                    // Align value to array index
                    value--;
                }
                if (names != null && value >= 0 && value < names.length) {
                    return names[value];
                } else {
                    return null;
                }
            }

            @Override
            public Map<String, Integer> getDisplayNames(String calendarType,
                int field, int style, Locale locale) {
                Map<String, Integer> map = null;
                String[] names = getCalendarDisplayStrings(removeExtensions(locale).toLanguageTag(),
                            getCalendarIDFromLDMLType(calendarType), field, style);
                if (names != null) {
                    map = new HashMap<>();
                    for (int value = 0; value < names.length; value++) {
                        if (names[value] != null) {
                            map.put(names[value],
                                    // Align array index to field value
                                    field == Calendar.DAY_OF_WEEK ? value + 1 : value);
                        }
                    }
                    map = map.isEmpty() ? null : map;
                }
                return map;
            }
        };
    }

    public static CalendarProvider getCalendarProvider() {
        return new CalendarProvider() {
            @Override
            public Locale[] getAvailableLocales() {
                return getSupportedCalendarLocales();
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return isSupportedCalendarLocale(locale);
            }

            @Override
            public Calendar getInstance(TimeZone zone, Locale locale) {
                return new Calendar.Builder()
                             .setLocale(getCalendarLocale(locale))
                             .setTimeZone(zone)
                             .setInstant(System.currentTimeMillis())
                             .build();
            }
        };
    }

    public static CurrencyNameProvider getCurrencyNameProvider() {
        return new CurrencyNameProvider() {
            @Override
            public Locale[] getAvailableLocales() {
                return supportedLocale;
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                // Ignore the extensions for now
                return supportedLocaleSet.contains(locale.stripExtensions()) &&
                       locale.getLanguage().equals(nativeDisplayLanguage);
            }

            @Override
            public String getSymbol(String currencyCode, Locale locale) {
                // Retrieves the currency symbol by calling
                // GetLocaleInfoEx(LOCALE_SCURRENCY).
                // It only works with the "locale"'s currency in its native
                // language.
                try {
                    if (Currency.getInstance(locale).getCurrencyCode()
                        .equals(currencyCode)) {
                        return getDisplayString(locale.toLanguageTag(),
                                DN_CURRENCY_SYMBOL, currencyCode);
                    }
                } catch (IllegalArgumentException iae) {}
                return null;
            }

            @Override
            public String getDisplayName(String currencyCode, Locale locale) {
                // Retrieves the display name by calling
                // GetLocaleInfoEx(LOCALE_SNATIVECURRNAME).
                // It only works with the "locale"'s currency in its native
                // language.
                try {
                    if (Currency.getInstance(locale).getCurrencyCode()
                        .equals(currencyCode)) {
                        return getDisplayString(locale.toLanguageTag(),
                                DN_CURRENCY_NAME, currencyCode);
                    }
                } catch (IllegalArgumentException iae) {}
                return null;
            }
        };
    }

    public static LocaleNameProvider getLocaleNameProvider() {
        return new LocaleNameProvider() {
            @Override
            public Locale[] getAvailableLocales() {
                return supportedLocale;
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return supportedLocaleSet.contains(locale.stripExtensions()) &&
                       locale.getLanguage().equals(nativeDisplayLanguage);
            }

            @Override
            public String getDisplayLanguage(String languageCode, Locale locale) {
                // Retrieves the display language name by calling
                // GetLocaleInfoEx(LOCALE_SLOCALIZEDLANGUAGENAME).
                return getDisplayString(locale.toLanguageTag(),
                            DN_LOCALE_LANGUAGE, languageCode);
            }

            @Override
            public String getDisplayCountry(String countryCode, Locale locale) {
                // Retrieves the display country name by calling
                // GetLocaleInfoEx(LOCALE_SLOCALIZEDCOUNTRYNAME).
                String str = getDisplayString(locale.toLanguageTag(),
                                 DN_LOCALE_REGION,
                                 nativeDisplayLanguage+"-"+countryCode);
                // Hack: Windows 10 returns translated "Unknown Region (XX)"
                // for localized XX region name. Take that as not known.
                if (str != null && str.endsWith("("+countryCode+")")) {
                    return null;
                }
                return str;
            }

            @Override
            public String getDisplayScript(String scriptCode, Locale locale) {
                return null;
            }

            @Override
            public String getDisplayVariant(String variantCode, Locale locale) {
                return null;
            }
        };
    }

    public static JavaTimeDateTimePatternProvider getJavaTimeDateTimePatternProvider() {
        return new JavaTimeDateTimePatternProvider() {
            @Override
            public Locale[] getAvailableLocales() {
                return getSupportedCalendarLocales();
            }

            @Override
            public boolean isSupportedLocale(Locale locale) {
                return isSupportedCalendarLocale(locale);
            }

            @Override
            public String getJavaTimeDateTimePattern(int timeStyle, int dateStyle, String calType, Locale locale) {
                AtomicReferenceArray<String> patterns = getDateTimePatterns(locale);
                String datePattern = dateStyle != - 1 ? patterns.get(dateStyle / 2) : "";
                String timePattern = timeStyle != - 1 ? patterns.get(timeStyle / 2 + 2) : "";
                String delim = dateStyle != -1 && timeStyle != - 1 ? " " : "";
                return toJavaTimeDateTimePattern(calType, datePattern + delim + timePattern);
            }

            private AtomicReferenceArray<String> getDateTimePatterns(Locale locale) {
                AtomicReferenceArray<String> patterns;
                SoftReference<AtomicReferenceArray<String>> ref = dateFormatCache.get(locale);

                if (ref == null || (patterns = ref.get()) == null) {
                    String langtag = removeExtensions(locale).toLanguageTag();
                    patterns = new AtomicReferenceArray<>(4);
                    patterns.compareAndSet(0, null, convertDateTimePattern(
                            getDateTimePattern(DateFormat.LONG, -1, langtag)));
                    patterns.compareAndSet(1, null, convertDateTimePattern(
                            getDateTimePattern(DateFormat.SHORT, -1, langtag)));
                    patterns.compareAndSet(2, null, convertDateTimePattern(
                            getDateTimePattern(-1, DateFormat.LONG, langtag)));
                    patterns.compareAndSet(3, null, convertDateTimePattern(
                            getDateTimePattern(-1, DateFormat.SHORT, langtag)));
                    ref = new SoftReference<>(patterns);
                    dateFormatCache.put(locale, ref);
                }
                return patterns;
            }
            /**
             * This method will convert JRE Date/time Pattern String to JSR310
             * type Date/Time Pattern
             */
            private String toJavaTimeDateTimePattern(String calendarType, String jrePattern) {
                int length = jrePattern.length();
                StringBuilder sb = new StringBuilder(length);
                boolean inQuote = false;
                int count = 0;
                char lastLetter = 0;
                for (int i = 0; i < length; i++) {
                    char c = jrePattern.charAt(i);
                    if (c == '\'') {
                        // '' is treated as a single quote regardless of being
                        // in a quoted section.
                        if ((i + 1) < length) {
                            char nextc = jrePattern.charAt(i + 1);
                            if (nextc == '\'') {
                                i++;
                                if (count != 0) {
                                    convert(calendarType, lastLetter, count, sb);
                                    lastLetter = 0;
                                    count = 0;
                                }
                                sb.append("''");
                                continue;
                            }
                        }
                        if (!inQuote) {
                            if (count != 0) {
                                convert(calendarType, lastLetter, count, sb);
                                lastLetter = 0;
                                count = 0;
                            }
                            inQuote = true;
                        } else {
                            inQuote = false;
                        }
                        sb.append(c);
                        continue;
                    }
                    if (inQuote) {
                        sb.append(c);
                        continue;
                    }
                    if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z')) {
                        if (count != 0) {
                            convert(calendarType, lastLetter, count, sb);
                            lastLetter = 0;
                            count = 0;
                        }
                        sb.append(c);
                        continue;
                    }
                    if (lastLetter == 0 || lastLetter == c) {
                        lastLetter = c;
                        count++;
                        continue;
                    }
                    convert(calendarType, lastLetter, count, sb);
                    lastLetter = c;
                    count = 1;
                }
                if (inQuote) {
                    // should not come here.
                    // returning null so that FALLBACK provider will kick in.
                    return null;
                }
                if (count != 0) {
                    convert(calendarType, lastLetter, count, sb);
                }
                return sb.toString();
            }

            private void convert(String calendarType, char letter, int count, StringBuilder sb) {
                switch (letter) {
                    case 'G':
                        if (calendarType.equals("japanese")) {
                            if (count >= 4) {
                                count = 1;
                            } else {
                                count = 5;
                            }
                        } else if (!calendarType.equals("iso8601")) {
                            // Adjust the number of 'G's
                            // Gregorian calendar is iso8601 for java.time
                            if (count >= 4) {
                                // JRE full -> JavaTime full
                                count = 4;
                            } else {
                                // JRE short -> JavaTime short
                                count = 1;
                            }
                        }
                        break;
                    case 'y':
                        if (calendarType.equals("japanese") && count >= 4) {
                            // JRE specific "gan-nen" support
                            count = 1;
                        }
                        break;
                    default:
                        // JSR 310 and CLDR define 5-letter patterns for narrow text.
                        if (count > 4) {
                            count = 4;
                        }
                        break;
                }
                appendN(letter, count, sb);
            }

            private void appendN(char c, int n, StringBuilder sb) {
                for (int i = 0; i < n; i++) {
                    sb.append(c);
                }
            }
        };
    }

    private static String convertDateTimePattern(String winPattern) {
        String ret = winPattern.replaceAll("dddd", "EEEE");
        ret = ret.replaceAll("ddd", "EEE");
        ret = ret.replaceAll("tt", "a");
        ret = ret.replaceAll("g", "GG");
        return ret;
    }

    private static Locale[] getSupportedCalendarLocales() {
        if (supportedLocale.length != 0 &&
            supportedLocaleSet.contains(Locale.JAPAN) &&
            isJapaneseCalendar()) {
            Locale[] sup = new Locale[supportedLocale.length+1];
            sup[0] = JRELocaleConstants.JA_JP_JP;
            System.arraycopy(supportedLocale, 0, sup, 1, supportedLocale.length);
            return sup;
        }
        return supportedLocale;
    }

    private static boolean isSupportedCalendarLocale(Locale locale) {
        Locale base = stripVariantAndExtensions(locale);

        if (!supportedLocaleSet.contains(base)) {
            return false;
        }

        int calid = getCalendarID(base.toLanguageTag());
        if (calid <= 0 || calid >= calIDToLDML.length) {
            return false;
        }

        String requestedCalType = locale.getUnicodeLocaleType("ca");
        String nativeCalType = calIDToLDML[calid]
                .replaceFirst("_.*", ""); // remove locale part.

        if (requestedCalType == null) {
            return Calendar.getAvailableCalendarTypes().contains(nativeCalType);
        } else {
            return requestedCalType.equals(nativeCalType);
        }
    }

    private static Locale[] getSupportedNativeDigitLocales() {
        if (supportedLocale.length != 0 &&
            supportedLocaleSet.contains(JRELocaleConstants.TH_TH) &&
            isNativeDigit("th-TH")) {
            Locale[] sup = new Locale[supportedLocale.length+1];
            sup[0] = JRELocaleConstants.TH_TH_TH;
            System.arraycopy(supportedLocale, 0, sup, 1, supportedLocale.length);
            return sup;
        }
        return supportedLocale;
    }

    private static boolean isSupportedNativeDigitLocale(Locale locale) {
        // special case for th_TH_TH
        if (JRELocaleConstants.TH_TH_TH.equals(locale)) {
            return isNativeDigit("th-TH");
        }

        String numtype = null;
        Locale base = locale;
        if (locale.hasExtensions()) {
            numtype = locale.getUnicodeLocaleType("nu");
            base = locale.stripExtensions();
        }

        if (supportedLocaleSet.contains(base)) {
            // Only supports Latin or Thai (in thai locales) digits.
            if (numtype == null || numtype.equals("latn")) {
                return true;
            } else if (locale.getLanguage().equals("th")) {
                return "thai".equals(numtype) &&
                       isNativeDigit(locale.toLanguageTag());
            }
        }

        return false;
    }

    private static Locale removeExtensions(Locale src) {
        return new Locale.Builder().setLocale(src).clearExtensions().build();
    }

    private static boolean isJapaneseCalendar() {
        return getCalendarID("ja-JP") == CAL_JAPAN;
    }

    private static Locale stripVariantAndExtensions(Locale locale) {
        if (locale.hasExtensions() || locale.getVariant() != "") {
            // strip off extensions and variant.
            locale = new Locale.Builder()
                            .setLocale(locale)
                            .clearExtensions()
                            .build();
        }

        return locale;
    }

    private static Locale getCalendarLocale(Locale locale) {
        int calid = getCalendarID(stripVariantAndExtensions(locale).toLanguageTag());
        if (calid > 0 && calid < calIDToLDML.length) {
            Locale.Builder lb = new Locale.Builder();
            String[] caltype = calIDToLDML[calid].split("_");
            if (caltype.length > 1) {
                lb.setLocale(Locale.forLanguageTag(caltype[1]));
            } else {
                lb.setLocale(locale);
            }
            lb.setUnicodeLocaleKeyword("ca", caltype[0]);
            return lb.build();
        }

        return locale;
    }

    private static int getCalendarIDFromLDMLType(String ldmlType) {
        for (int i = 0; i < calIDToLDML.length; i++) {
            if (calIDToLDML[i].startsWith(ldmlType)) {
                return i;
            }
        }
        return -1;
    }

    private static Locale getNumberLocale(Locale src) {
        if (JRELocaleConstants.TH_TH.equals(src)) {
            if (isNativeDigit("th-TH")) {
                Locale.Builder lb = new Locale.Builder().setLocale(src);
                lb.setUnicodeLocaleKeyword("nu", "thai");
                return lb.build();
            }
        }

        return src;
    }

    // native methods

    // initialize
    private static native boolean initialize();
    private static native String getDefaultLocale(int cat);

    // For DateFormatProvider
    private static native String getDateTimePattern(int dateStyle, int timeStyle, String langTag);
    private static native int getCalendarID(String langTag);

    // For DateFormatSymbolsProvider
    private static native String[] getAmPmStrings(String langTag, String[] ampm);
    private static native String[] getEras(String langTag, String[] eras);
    private static native String[] getMonths(String langTag, String[] months);
    private static native String[] getShortMonths(String langTag, String[] smonths);
    private static native String[] getWeekdays(String langTag, String[] wdays);
    private static native String[] getShortWeekdays(String langTag, String[] swdays);

    // For NumberFormatProvider
    private static native String getNumberPattern(int numberStyle, String langTag);
    private static native boolean isNativeDigit(String langTag);

    // For DecimalFormatSymbolsProvider
    private static native String getCurrencySymbol(String langTag, String currencySymbol);
    private static native char getDecimalSeparator(String langTag, char decimalSeparator);
    private static native char getGroupingSeparator(String langTag, char groupingSeparator);
    private static native String getInfinity(String langTag, String infinity);
    private static native String getInternationalCurrencySymbol(String langTag, String internationalCurrencySymbol);
    private static native char getMinusSign(String langTag, char minusSign);
    private static native char getMonetaryDecimalSeparator(String langTag, char monetaryDecimalSeparator);
    private static native String getNaN(String langTag, String nan);
    private static native char getPercent(String langTag, char percent);
    private static native char getPerMill(String langTag, char perMill);
    private static native char getZeroDigit(String langTag, char zeroDigit);

    // For CalendarDataProvider
    private static native int getCalendarDataValue(String langTag, int type);

    // For CalendarNameProvider
    private static native String[] getCalendarDisplayStrings(String langTag, int calid, int field, int style);

    // For Locale/CurrencyNameProvider
    private static native String getDisplayString(String langTag, int key, String value);
}
