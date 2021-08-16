/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004489 8006509 8008577 8145136 8202537
 * @summary Unit test for CLDR FormatData resources
 * @modules java.base/sun.util.locale.provider
 *          jdk.localedata
 * @compile -XDignore.symbol.file CldrFormatNamesTest.java
 * @run main/othervm -Djava.locale.providers=CLDR CldrFormatNamesTest
 */

import java.util.*;
import static java.util.Calendar.*;
import sun.util.locale.provider.*;

public class CldrFormatNamesTest {
    private static final Locale ARABIC = new Locale("ar");
    private static final Locale ZH_HANT = Locale.forLanguageTag("zh-Hant");

    /*
     * The first element is a Locale followed by key-value pairs
     * in a FormatData resource bundle. The value type is either
     * String or String[].
     */
    static final Object[][] CLDR_DATA = {
        {
            Locale.JAPAN,
            "field.zone", "\u30bf\u30a4\u30e0\u30be\u30fc\u30f3",
            "java.time.japanese.DatePatterns", new String[] {
                "Gy\u5e74M\u6708d\u65e5EEEE",
                "Gy\u5e74M\u6708d\u65e5",
                "Gy\u5e74M\u6708d\u65e5",
                "GGGGGy/M/d",
            },
            "java.time.roc.DatePatterns", new String[] {
                "Gy\u5e74M\u6708d\u65e5EEEE",
                "Gy\u5e74M\u6708d\u65e5",
                "Gy/MM/dd",
                "Gy/MM/dd",
            },
            "calendarname.buddhist", "\u4ecf\u66a6",
        },
        {
            Locale.PRC,
            "field.zone", "\u65f6\u533a",
            "java.time.islamic.DatePatterns", new String[] {
                "Gy\u5e74M\u6708d\u65e5EEEE",
                "Gy\u5e74M\u6708d\u65e5",
                "Gy\u5e74M\u6708d\u65e5",
                "Gy/M/d",
            },
            "calendarname.islamic", "\u4f0a\u65af\u5170\u5386",
        },
        {
            Locale.GERMANY,
            "field.dayperiod", "Tagesh\u00e4lfte",
            "java.time.islamic.DatePatterns", new String[] {
                "EEEE, d. MMMM y G",
                "d. MMMM y G",
                "dd.MM.y G",
                "dd.MM.yy GGGGG",
            },
            "calendarname.islamic", "Islamischer Kalender",
        },
        {
            Locale.FRANCE,
            "field.dayperiod", "cadran",
            "java.time.islamic.DatePatterns", new String[] {
                "EEEE d MMMM y G",
                "d MMMM y G",
                "d MMM y G",
                "dd/MM/y GGGGG",
            },
            "calendarname.islamic", "calendrier musulman",
        },
    };

    // Islamic calendar symbol names in ar
    private static final String[] ISLAMIC_MONTH_NAMES = {
        "\u0645\u062d\u0631\u0645",
        "\u0635\u0641\u0631",
        "\u0631\u0628\u064a\u0639 \u0627\u0644\u0623\u0648\u0644",
        "\u0631\u0628\u064a\u0639 \u0627\u0644\u0622\u062e\u0631",
        "\u062c\u0645\u0627\u062f\u0649 \u0627\u0644\u0623\u0648\u0644\u0649",
        "\u062c\u0645\u0627\u062f\u0649 \u0627\u0644\u0622\u062e\u0631\u0629",
        "\u0631\u062c\u0628",
        "\u0634\u0639\u0628\u0627\u0646",
        "\u0631\u0645\u0636\u0627\u0646",
        "\u0634\u0648\u0627\u0644",
        "\u0630\u0648 \u0627\u0644\u0642\u0639\u062f\u0629",
        "\u0630\u0648 \u0627\u0644\u062d\u062c\u0629",
    };
    private static final String[] ISLAMIC_ERA_NAMES = {
        "",
        "\u0647\u0640",
    };

    // Minguo calendar symbol names in zh_Hant
    private static final String[] ROC_ERA_NAMES = {
        "\u6c11\u570b\u524d",
        "\u6c11\u570b",
    };

    private static int errors = 0;

    // This test is CLDR data dependent.
    public static void main(String[] args) {
        for (Object[] data : CLDR_DATA) {
            Locale locale = (Locale) data[0];
            ResourceBundle rb = LocaleProviderAdapter.getResourceBundleBased()
                                    .getLocaleResources(locale).getJavaTimeFormatData();
            for (int i = 1; i < data.length; ) {
                String key = (String) data[i++];
                Object expected = data[i++];
                if (rb.containsKey(key)) {
                    Object value = rb.getObject(key);
                    if (expected instanceof String) {
                        if (!expected.equals(value)) {
                            errors++;
                            System.err.printf("error: key='%s', got '%s' expected '%s', rb: %s%n",
                                              key, value, expected, rb);
                        }
                    } else if (expected instanceof String[]) {
                        try {
                            if (!Arrays.equals((Object[]) value, (Object[]) expected)) {
                                errors++;
                                System.err.printf("error: key='%s', got '%s' expected '%s', rb: %s%n",
                                                  key, Arrays.asList((Object[])value),
                                                  Arrays.asList((Object[])expected), rb);
                            }
                        } catch (Exception e) {
                            errors++;
                            e.printStackTrace();
                        }
                    }
                } else {
                    errors++;
                    System.err.println("No resource for " + key+", rb: "+rb);
                }
            }
        }

        // test Islamic calendar names in Arabic
        testSymbolNames(ARABIC, "islamic", ISLAMIC_MONTH_NAMES, MONTH, LONG, "month");
        testSymbolNames(ARABIC, "islamic", ISLAMIC_ERA_NAMES, ERA, SHORT, "era");

        // test ROC (Minguo) calendar names in zh-Hant
        testSymbolNames(ZH_HANT, "roc", ROC_ERA_NAMES, ERA, SHORT, "era");

        if (errors > 0) {
            throw new RuntimeException("test failed");
        }
    }

    private static void testSymbolNames(Locale locale, String calType, String[] expected,
                                        int field, int style, String fieldName) {
        for (int i = 0; i < expected.length; i++) {
            String expt = expected[i];
            String name = CalendarDataUtility.retrieveJavaTimeFieldValueName(calType, field, i, style, locale);
            if (!expt.equals(name)) {
                errors++;
                System.err.printf("error: wrong %s %s name in %s: value=%d, got='%s', expected='%s'%n",
                                  calType, fieldName, locale, i, name, expt);
            }
        }
    }
}
