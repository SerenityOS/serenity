/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @test
 * @bug 8206120
 * @modules jdk.localedata
 */

package test.java.time.format;

import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DecimalStyle;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test DateTimeFormatter with non-ISO chronology.
 *
 * Strings in test data are all dependent on CLDR data which may change
 * in future CLDR releases.
 */
@Test
public class TestNonIsoFormatter {
    private static final Chronology ISO8601 = IsoChronology.INSTANCE;
    private static final Chronology JAPANESE = JapaneseChronology.INSTANCE;
    private static final Chronology HIJRAH = HijrahChronology.INSTANCE;
    private static final Chronology MINGUO = MinguoChronology.INSTANCE;
    private static final Chronology BUDDHIST = ThaiBuddhistChronology.INSTANCE;

    private static final LocalDate IsoDate = LocalDate.of(2013, 2, 11);

    private static final Locale ARABIC = new Locale("ar");
    private static final Locale thTH = new Locale("th", "TH");
    private static final Locale thTHTH = Locale.forLanguageTag("th-TH-u-nu-thai");
    private static final Locale jaJPJP = Locale.forLanguageTag("ja-JP-u-ca-japanese");

    @BeforeMethod
    public void setUp() {
    }

    @DataProvider(name="format_data")
    Object[][] formatData() {
        return new Object[][] {
            // Chronology, Format Locale, Numbering Locale, ChronoLocalDate, expected string
            { JAPANESE, Locale.JAPANESE, Locale.JAPANESE, JAPANESE.date(IsoDate),
              "\u5e73\u621025\u5e742\u670811\u65e5\u6708\u66dc\u65e5" }, // Japanese Heisei 25-02-11
            { HIJRAH, ARABIC, ARABIC, HIJRAH.date(IsoDate),
              "\u0627\u0644\u0627\u062b\u0646\u064a\u0646\u060c \u0661 \u0631\u0628\u064a\u0639 "
              + "\u0627\u0644\u0622\u062e\u0631 \u0661\u0664\u0663\u0664 \u0647\u0640" }, // Hijrah AH 1434-04-01 (Mon)
            { MINGUO, Locale.TAIWAN, Locale.TAIWAN, MINGUO.date(IsoDate),
              "\u6c11\u570b102\u5e742\u670811\u65e5 \u661f\u671f\u4e00" }, // Minguo ROC 102-02-11 (Mon)
            { BUDDHIST, thTH, thTH, BUDDHIST.date(IsoDate),
              "\u0e27\u0e31\u0e19\u0e08\u0e31\u0e19\u0e17\u0e23\u0e4c\u0e17\u0e35\u0e48"
              + " 11 \u0e01\u0e38\u0e21\u0e20\u0e32\u0e1e\u0e31\u0e19\u0e18\u0e4c"
              + " \u0e1e.\u0e28. 2556" }, // ThaiBuddhist BE 2556-02-11
            { BUDDHIST, thTH, thTHTH, BUDDHIST.date(IsoDate),
              "\u0e27\u0e31\u0e19\u0e08\u0e31\u0e19\u0e17\u0e23\u0e4c\u0e17\u0e35\u0e48 \u0e51\u0e51 "
              + "\u0e01\u0e38\u0e21\u0e20\u0e32\u0e1e\u0e31\u0e19\u0e18\u0e4c \u0e1e.\u0e28. "
              + "\u0e52\u0e55\u0e55\u0e56" }, // ThaiBuddhist BE 2556-02-11 (with Thai digits)
        };
    }

    @DataProvider(name="invalid_text")
    Object[][] invalidText() {
        return new Object[][] {
            // TODO: currently fixed Chronology and Locale.
            // line commented out, as S64.01.09 seems like a reasonable thing to parse
            // (era "S" ended on S64.01.07, but a little leniency is a good thing
//            { "\u662d\u548c64\u5e741\u67089\u65e5\u6708\u66dc\u65e5" }, // S64.01.09 (Mon)
            { "\u662d\u548c65\u5e741\u67081\u65e5\u6708\u66dc\u65e5" }, // S65.01.01 (Mon)
        };
    }

    @DataProvider(name="chrono_names")
    Object[][] chronoNamesData() {
        return new Object[][] {
            // Chronology, Locale, Chronology Name
            { ISO8601,  Locale.ENGLISH, "ISO" },    // No data in CLDR; Use Id.
            { BUDDHIST, Locale.ENGLISH, "Buddhist Calendar" },
            { HIJRAH,   Locale.ENGLISH, "Islamic Calendar (Umm al-Qura)" },
            { JAPANESE, Locale.ENGLISH, "Japanese Calendar" },
            { MINGUO,   Locale.ENGLISH, "Minguo Calendar" },

            { ISO8601,  Locale.JAPANESE, "ISO" },    // No data in CLDR; Use Id.
            { JAPANESE, Locale.JAPANESE, "\u548c\u66a6" },
            { BUDDHIST, Locale.JAPANESE, "\u4ecf\u66a6" },

            { ISO8601,  thTH, "ISO" },    // No data in CLDR; Use Id.
            { JAPANESE, thTH, "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e0d\u0e35\u0e48\u0e1b\u0e38\u0e48\u0e19" },
            { BUDDHIST, thTH, "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e1e\u0e38\u0e17\u0e18" },

            { HIJRAH,   ARABIC, "\u0627\u0644\u062a\u0642\u0648\u064a\u0645 "
                                + "\u0627\u0644\u0625\u0633\u0644\u0627\u0645\u064a "
                                + "(\u0623\u0645 \u0627\u0644\u0642\u0631\u0649)" },
        };
    }

    @DataProvider(name="lenient_eraYear")
    Object[][] lenientEraYear() {
        return new Object[][] {
            // Chronology, lenient era/year, strict era/year
            { JAPANESE, "Meiji 123", "Heisei 2" },
            { JAPANESE, "Sh\u014dwa 65", "Heisei 2" },
            { JAPANESE, "Heisei 32", "Reiwa 2" },
        };
    }

    @Test(dataProvider="format_data")
    public void test_formatLocalizedDate(Chronology chrono, Locale formatLocale, Locale numberingLocale,
                                         ChronoLocalDate date, String expected) {
        DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withChronology(chrono).withLocale(formatLocale)
            .withDecimalStyle(DecimalStyle.of(numberingLocale));
        String text = dtf.format(date);
        assertEquals(text, expected);
    }

    @Test(dataProvider="format_data")
    public void test_parseLocalizedText(Chronology chrono, Locale formatLocale, Locale numberingLocale,
                                        ChronoLocalDate expected, String text) {
        DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withChronology(chrono).withLocale(formatLocale)
            .withDecimalStyle(DecimalStyle.of(numberingLocale));
        TemporalAccessor temporal = dtf.parse(text);
        ChronoLocalDate date = chrono.date(temporal);
        assertEquals(date, expected);
    }

    @Test(dataProvider="invalid_text", expectedExceptions=DateTimeParseException.class)
    public void test_parseInvalidText(String text) {
        DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withChronology(JAPANESE).withLocale(Locale.JAPANESE);
        dtf.parse(text);
    }

    @Test(dataProvider="chrono_names")
    public void test_chronoNames(Chronology chrono, Locale locale, String expected) {
        DateTimeFormatter dtf = new DateTimeFormatterBuilder().appendChronologyText(TextStyle.SHORT)
            .toFormatter(locale);
        String text = dtf.format(chrono.dateNow());
        assertEquals(text, expected);
        TemporalAccessor ta = dtf.parse(text);
        Chronology cal = ta.query(TemporalQueries.chronology());
        assertEquals(cal, chrono);
    }

    @Test(dataProvider="lenient_eraYear")
    public void test_lenientEraYear(Chronology chrono, String lenient, String strict) {
        String mdStr = "-01-01";
        DateTimeFormatter dtf = new DateTimeFormatterBuilder()
            .appendPattern("GGGG y-M-d")
            .toFormatter(Locale.ROOT)
            .withChronology(chrono);
        DateTimeFormatter dtfLenient = dtf.withResolverStyle(ResolverStyle.LENIENT);
        assertEquals(LocalDate.parse(lenient+mdStr, dtfLenient), LocalDate.parse(strict+mdStr, dtf));
    }
}
