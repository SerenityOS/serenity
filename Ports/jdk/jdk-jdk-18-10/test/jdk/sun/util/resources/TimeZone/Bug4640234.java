/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4640234 4946057 4938151 4873691 5023181
 * @summary Verifies the translation of time zone names, this test will catch
 *          presence of country name for english and selected locales for all
 *          ISO country codes.
 *          The test program also displays which timezone, country and
 *          language names are not translated
 * @modules java.base/sun.util.resources
 */


/*
 * 4946057 Localization for ISO country & language data.
 * 4938151 Time zones not translated
 * 4873691 Changes in TimeZone mapping
 */

import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

import sun.util.resources.LocaleData;

public class Bug4640234  {
    static SimpleDateFormat sdfEn = new SimpleDateFormat("zzzz", Locale.US);
    static SimpleDateFormat sdfEnShort = new SimpleDateFormat("z", Locale.US);
    static Locale locEn = Locale.ENGLISH;

    static SimpleDateFormat sdfLoc;
    static SimpleDateFormat sdfLocShort;
    static Date date = new Date();

    // Define supported locales
    static Locale[] locales2Test = new Locale[] {
        new Locale("de"),
        new Locale("es"),
        new Locale("fr"),
        new Locale("it"),
        new Locale("ja"),
        new Locale("ko"),
        new Locale("sv"),
        new Locale("zh", "CN"),
        new Locale("zh", "TW")
    };

    public static void main(String[] args) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);

            StringBuffer errors = new StringBuffer("");
            StringBuffer warnings = new StringBuffer("");

            String[] timezones = TimeZone.getAvailableIDs();
            String[] countries = locEn.getISOCountries();
            String[] languages = locEn.getISOLanguages();

            ResourceBundle resEn = LocaleData.getBundle("sun.util.resources.LocaleNames",
                                                        locEn);
            Map<String, String> countryMapEn = getList(resEn, true);
            Map<String, String> languageMapEn = getList(resEn, false);

            ResourceBundle resLoc;
            Map<String, String> countryMap;
            Map<String, String> languageMap;

            for (Locale locale : locales2Test) {
                resLoc = LocaleData.getBundle("sun.util.resources.LocaleNames",
                                              locale);

                sdfLoc = new SimpleDateFormat("zzzz", locale);
                sdfLocShort = new SimpleDateFormat("z", locale);

                for (String timezone : timezones) {
                    if (isTZIgnored(timezone)) {
                        continue;
                    }
                    warnings.append(testTZ(timezone, locale));
                }

                countryMap = getList(resLoc, true);

                for (String country : countries) {
                    String[] result = testEntry(country,
                        countryMapEn,
                        countryMap,
                        locale,
                        "ERROR: {0} country name for country code: {1} not found!\n",
                        "WARNING: {0} country name for country code: {1} not localized!\n"
                    );
                    if (warnings.indexOf(result[0]) == -1) {
                        warnings.append(result[0]);
                    }
                    if (errors.indexOf(result[1]) == -1) {
                        errors.append(result[1]);
                    }
                }

                languageMap = getList(resLoc, false);
                for (String language : languages) {
                    String[] result = testEntry(language,
                        languageMapEn,
                        languageMap,
                        locale,
                        "ERROR: {0} language name for language code: {1} not found!\n",
                        "WARNING: {0} language name for language code: {1} not localized!\n");
                    if (warnings.indexOf(result[0]) == -1) {
                        warnings.append(result[0]);
                    }
                    if (errors.indexOf(result[1]) == -1) {
                        errors.append(result[1]);
                    }
                }
            }

            StringBuffer message = new StringBuffer("");
            if (!"".equals(errors.toString())) {
                message.append("Test failed! ");
                message.append("ERROR: some keys are missing! ");
            }

            if ("".equals(message.toString())) {
                System.out.println("\nTest passed");
                System.out.println(warnings.toString());
            } else {
                System.out.println("\nTest failed!");
                System.out.println(errors.toString());
                System.out.println(warnings.toString());
                throw new Exception("\n" + message);
            }
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    /**
    * Compares the english timezone name and timezone name in specified locale
    * @param timeZoneName - name of the timezone to compare
    * @param locale - locale to test against english
    * @return empty string when passed, descriptive error message in other cases
    */
    private static String testTZ(String timeZoneName, Locale locale) {
        StringBuffer timeZoneResult = new StringBuffer("");
        TimeZone tz = TimeZone.getTimeZone(timeZoneName);
        sdfEn.setTimeZone(tz);
        sdfEnShort.setTimeZone(tz);
        sdfLoc.setTimeZone(tz);
        sdfLocShort.setTimeZone(tz);

        String en, enShort, loc, locShort;
        en = sdfEn.format(date);
        enShort = sdfEnShort.format(date);
        loc = sdfLoc.format(date);
        locShort = sdfLocShort.format(date);

        String displayLanguage = locale.getDisplayLanguage();
        String displayCountry = locale.getDisplayCountry();

        if (loc.equals(en)) {
            timeZoneResult.append("[");
            timeZoneResult.append(displayLanguage);
            if (!"".equals(displayCountry)) {
                timeZoneResult.append(" ");
                timeZoneResult.append(displayCountry);
            }
            timeZoneResult.append("] timezone \"");
            timeZoneResult.append(timeZoneName);
            timeZoneResult.append("\" long name \"" + en);
            timeZoneResult.append("\" not localized!\n");
        }

        if (!locShort.equals(enShort)) {
            timeZoneResult.append("[");
            timeZoneResult.append(displayLanguage);
            if (!"".equals(displayCountry)) {
                timeZoneResult.append(" ");
                timeZoneResult.append(displayCountry);
            }
            timeZoneResult.append("] timezone \"");
            timeZoneResult.append(timeZoneName);
            timeZoneResult.append("\" short name \"" + enShort);
            timeZoneResult.append("\" is localized \"");
            timeZoneResult.append(locShort);
            timeZoneResult.append("\"!\n");
        }
        return timeZoneResult.toString();
    }

    /**
    * Verifies whether the name for ISOCode is localized.
    * @param ISOCode - ISO country/language code for country/language name
    *      to test
    * @param entriesEn - array of english country/language names
    * @param entriesLoc - array of localized country/language names for
    *      specified locale
    * @param locale - locale to test against english
    * @param notFoundMessage - message in form ready for MessageFormat,
    *      {0} will be human readable language name, {1} will be ISOCode.
    * @param notLocalizedMessage - message in for ready for MessageFormat,
    *      same formatting like for notFountMessage
    * @return array of two empty strings when passed, descriptive error message
    *      in other cases, [0] - warnings for not localized, [1] - errors for
    *      missing keys.
    */
    private static String[] testEntry(String ISOCode,
                Map<String, String> entriesEn,
                Map<String, String> entriesLoc,
                Locale locale,
                String notFoundMessage,
                String notLocalizedMessage) {
        String nameEn = null;
        String nameLoc = null;

        for (String key: entriesEn.keySet()) {
            if (ISOCode.equalsIgnoreCase(key)) {
                nameEn = entriesEn.get(key);
                break;
            }
        }

        for (String key: entriesLoc.keySet()) {
            if (ISOCode.equalsIgnoreCase(key)) {
                nameLoc = entriesLoc.get(key);
                break;
            }
        }

        if (nameEn == null) {
            // We should not get here but test is a MUST have
            return new String[] {"",
                                 MessageFormat.format(notFoundMessage, "English", ISOCode)};
        }

        if (nameLoc == null) {
            return new String[] {"",
                                 MessageFormat.format(notFoundMessage,
                                                      locale.getDisplayName(), ISOCode)};
        }

        if (nameEn.equals(nameLoc)) {
            return new String[] {MessageFormat.format(notLocalizedMessage,
                                                      locale.getDisplayName(), ISOCode),
                                 ""};
        }

        return new String[] {"", ""};
    }

    private static boolean isTZIgnored(String TZName) {
        if (TZName.startsWith("Etc/GMT") ||
                TZName.indexOf("Riyadh8") != -1 ||
                TZName.equals("GMT0") ||
                TZName.equals("MET")
                ) {
            return true;
        }
        return false;
    }

    private static Map<String, String> getList(
            ResourceBundle rs, Boolean getCountryList) {
        char beginChar = 'a';
        char endChar = 'z';
        if (getCountryList) {
            beginChar = 'A';
            endChar = 'Z';
        }

        Map<String, String> hm = new HashMap<String, String>();
        Enumeration<String> keys = rs.getKeys();
        while (keys.hasMoreElements()) {
            String s = keys.nextElement();
            if (s.length() == 2 &&
                s.charAt(0) >= beginChar && s.charAt(0) <= endChar) {
                hm.put(s, rs.getString(s));
            }
        }
        return hm;
    }
}
