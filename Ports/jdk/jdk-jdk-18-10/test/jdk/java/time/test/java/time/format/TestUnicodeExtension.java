/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8176841 8202537 8244245
 * @summary Tests java.time classes deals with Unicode extensions
 *      correctly.
 * @modules jdk.localedata
 */
package test.java.time.format;

import static org.testng.Assert.assertEquals;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test JavaTime with BCP47 U extensions
 */
@Test
public class TestUnicodeExtension {
    private static TimeZone defaultTZ;

    private static final Chronology JAPANESE = JapaneseChronology.INSTANCE;
    private static final Chronology HIJRAH = HijrahChronology.INSTANCE;
    private static final Chronology ISO = IsoChronology.INSTANCE;

    private static final ZoneId ASIATOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId AMLA = ZoneId.of("America/Los_Angeles");

    private static final Locale JPTYO = Locale.forLanguageTag("en-u-tz-jptyo");
    private static final Locale JCAL = Locale.forLanguageTag("en-u-ca-japanese");
    private static final Locale HCAL = Locale.forLanguageTag("en-u-ca-islamic-umalqura");

    private static final Locale FW_SUN = Locale.forLanguageTag("en-US-u-fw-sun");
    private static final Locale FW_MON = Locale.forLanguageTag("en-US-u-fw-mon");
    private static final Locale FW_TUE = Locale.forLanguageTag("en-US-u-fw-tue");
    private static final Locale FW_WED = Locale.forLanguageTag("en-US-u-fw-wed");
    private static final Locale FW_THU = Locale.forLanguageTag("en-US-u-fw-thu");
    private static final Locale FW_FRI = Locale.forLanguageTag("en-US-u-fw-fri");
    private static final Locale FW_SAT = Locale.forLanguageTag("en-US-u-fw-sat");

    private static final Locale RG_GB = Locale.forLanguageTag("en-US-u-rg-gbzzzz");

    private static final ZonedDateTime ZDT = ZonedDateTime.of(2017, 8, 10, 15, 15, 0, 0, AMLA);

    private static final String PATTERN = "GGGG MMMM-dd-uu HH:mm:ss zzzz";

    @BeforeTest
    public void beforeTest() {
        defaultTZ = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(AMLA));
    }

    @AfterTest
    public void afterTest() {
        TimeZone.setDefault(defaultTZ);
    }

    @DataProvider(name="localizedBy")
    Object[][] localizedBy() {
        return new Object[][] {
            // Locale, Chrono override, Zone override, Expected Chrono, Expected Zone,
            // Expected formatted string
            {Locale.JAPAN, null, null, ISO, null,
            "2017\u5e748\u670810\u65e5\u6728\u66dc\u65e5 15\u664215\u520600\u79d2 " +
            "\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u590f\u6642\u9593"
            },
            {Locale.JAPAN, JAPANESE, null, ISO, null,
            "2017\u5e748\u670810\u65e5\u6728\u66dc\u65e5 15\u664215\u520600\u79d2 " +
            "\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u590f\u6642\u9593"
            },
            {Locale.JAPAN, JAPANESE, ASIATOKYO, ISO, ASIATOKYO,
            "2017\u5e748\u670811\u65e5\u91d1\u66dc\u65e5 7\u664215\u520600\u79d2 " +
            "\u65e5\u672c\u6a19\u6e96\u6642"
            },

            {JCAL, null, null, JAPANESE, null,
            "Thursday, August 10, 29 Heisei at 3:15:00 PM Pacific Daylight Time"
            },
            {JCAL, HIJRAH, null, JAPANESE, null,
            "Thursday, August 10, 29 Heisei at 3:15:00 PM Pacific Daylight Time"
            },
            {HCAL, JAPANESE, null, HIJRAH, null,
            "Thursday, Dhu\u02bbl-Qi\u02bbdah 18, 1438 AH at 3:15:00 PM Pacific Daylight Time"
            },


            {JPTYO, null, null, ISO, ASIATOKYO,
            "Friday, August 11, 2017 at 7:15:00 AM Japan Standard Time"
            },
            {JPTYO, null, AMLA, ISO, ASIATOKYO,
            "Friday, August 11, 2017 at 7:15:00 AM Japan Standard Time"
            },
            // invalid tz
            {Locale.forLanguageTag("en-US-u-tz-jpzzz"), null, null, ISO, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            {Locale.forLanguageTag("en-US-u-tz-jpzzz"), null, AMLA, ISO, AMLA,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },

            {RG_GB, null, null, ISO, null,
            "Thursday, 10 August 2017 at 15:15:00 Pacific Daylight Time"
            },

            // DecimalStyle
            {Locale.forLanguageTag("en-US-u-nu-thai"), null, null, ISO, null,
            "Thursday, August \u0e51\u0e50, \u0e52\u0e50\u0e51\u0e57 at \u0e53:\u0e51\u0e55:" +
            "\u0e50\u0e50 PM Pacific Daylight Time"
            },
            // DecimalStyle, "nu" vs "rg"
            {Locale.forLanguageTag("en-US-u-nu-thai-rg-uszzzz"), null, null, ISO, null,
            "Thursday, August \u0e51\u0e50, \u0e52\u0e50\u0e51\u0e57 at \u0e53:\u0e51\u0e55:" +
            "\u0e50\u0e50 PM Pacific Daylight Time"
            },
            // DecimalStyle, invalid
            {Locale.forLanguageTag("en-US-u-nu-foo"), null, null, ISO, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            // DecimalStyle, locale default
            // Farsi uses Extended Arabic-Indic numbering system
            {Locale.forLanguageTag("fa"), null, null, ISO, null,
            "\u067e\u0646\u062c\u0634\u0646\u0628\u0647 \u06f1\u06f0 \u0627\u0648\u062a " +
            "\u06f2\u06f0\u06f1\u06f7\u060c \u0633\u0627\u0639\u062a \u06f1\u06f5:\u06f1\u06f5:" +
            "\u06f0\u06f0 (\u0648\u0642\u062a \u062a\u0627\u0628\u0633\u062a\u0627\u0646\u06cc " +
            "\u063a\u0631\u0628 \u0627\u0645\u0631\u06cc\u06a9\u0627)"
            },
            // Farsi uses Extended Arabic-Indic numbering system
            // (should not be overridden with it, as "latn" is explicitly specified)
            {Locale.forLanguageTag("fa-u-nu-latn"), null, null, ISO, null,
            "\u067e\u0646\u062c\u0634\u0646\u0628\u0647 10 \u0627\u0648\u062a 2017\u060c " +
            "\u0633\u0627\u0639\u062a 15:15:00 (\u0648\u0642\u062a \u062a\u0627\u0628\u0633" +
            "\u062a\u0627\u0646\u06cc \u063a\u0631\u0628 \u0627\u0645\u0631\u06cc\u06a9\u0627)"
            },
            // Dzongkha uses Tibetan numbering system
            {Locale.forLanguageTag("dz"), null, null, ISO, null,
            "\u0f42\u0f5f\u0f60\u0f0b\u0f54\u0f0b\u0f66\u0f44\u0f66\u0f0b, \u0f66\u0fa4\u0fb1" +
            "\u0f72\u0f0b\u0f63\u0f7c\u0f0b\u0f22\u0f20\u0f21\u0f27 \u0f5f\u0fb3\u0f0b\u0f56" +
            "\u0f62\u0f92\u0fb1\u0f51\u0f0b\u0f54\u0f0b \u0f5a\u0f7a\u0f66\u0f0b\u0f21\u0f20 " +
            "\u0f46\u0f74\u0f0b\u0f5a\u0f7c\u0f51\u0f0b \u0f23 \u0f66\u0f90\u0f62\u0f0b\u0f58" +
            "\u0f0b \u0f21\u0f25:\u0f20\u0f20 \u0f55\u0fb1\u0f72\u0f0b\u0f46\u0f0b \u0f56\u0fb1" +
            "\u0f44\u0f0b\u0f68\u0f0b\u0f58\u0f72\u0f0b\u0f62\u0f72\u0f0b\u0f40\u0f0b\u0f54\u0f7a" +
            "\u0f0b\u0f66\u0f72\u0f0b\u0f55\u0f72\u0f42\u0f0b\u0f49\u0f72\u0f53\u0f0b\u0f66\u0fb2" +
            "\u0f74\u0f44\u0f0b\u0f46\u0f74\u0f0b\u0f5a\u0f7c\u0f51"
            },
        };
    }

    @DataProvider(name="withLocale")
    Object[][] withLocale() {
        return new Object[][] {
            // Locale, Chrono override, Zone override, Expected Chrono, Expected Zone,
            // Expected formatted string
            {Locale.JAPAN, null, null, null, null,
            "2017\u5e748\u670810\u65e5\u6728\u66dc\u65e5 15\u664215\u520600\u79d2 " +
            "\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u590f\u6642\u9593"
            },
            {Locale.JAPAN, JAPANESE, null, JAPANESE, null,
            "\u5e73\u621029\u5e748\u670810\u65e5\u6728\u66dc\u65e5 15\u664215\u520600\u79d2 " +
            "\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u590f\u6642\u9593"
            },
            {Locale.JAPAN, JAPANESE, ASIATOKYO, JAPANESE, ASIATOKYO,
            "\u5e73\u621029\u5e748\u670811\u65e5\u91d1\u66dc\u65e5 7\u664215\u520600\u79d2 " +
            "\u65e5\u672c\u6a19\u6e96\u6642"
            },

            {JCAL, null, null, null, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            {JCAL, HIJRAH, null, HIJRAH, null,
            "Thursday, Dhu\u02bbl-Qi\u02bbdah 18, 1438 AH at 3:15:00 PM Pacific Daylight Time"
            },
            {HCAL, JAPANESE, null, JAPANESE, null,
            "Thursday, August 10, 29 Heisei at 3:15:00 PM Pacific Daylight Time"
            },


            {JPTYO, null, null, null, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            {JPTYO, null, AMLA, null, AMLA,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            // invalid tz
            {Locale.forLanguageTag("en-US-u-tz-jpzzz"), null, null, null, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            {Locale.forLanguageTag("en-US-u-tz-jpzzz"), null, null, null, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },

            {RG_GB, null, null, null, null,
            "Thursday, 10 August 2017 at 15:15:00 Pacific Daylight Time"
            },

            // DecimalStyle
            {Locale.forLanguageTag("en-US-u-nu-thai"), null, null, null, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            // DecimalStyle, "nu" vs "rg"
            {Locale.forLanguageTag("en-US-u-nu-thai-rg-uszzzz"), null, null, null, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            // DecimalStyle, invalid
            {Locale.forLanguageTag("en-US-u-nu-foo"), null, null, null, null,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },
            // DecimalStyle, locale default
            // Farsi uses Extended Arabic-Indic numbering system
            // (should not be overridden with it)
            {Locale.forLanguageTag("fa"), null, null, null, null,
            "\u067e\u0646\u062c\u0634\u0646\u0628\u0647 10 \u0627\u0648\u062a 2017\u060c " +
            "\u0633\u0627\u0639\u062a 15:15:00 (\u0648\u0642\u062a \u062a\u0627\u0628\u0633" +
            "\u062a\u0627\u0646\u06cc \u063a\u0631\u0628 \u0627\u0645\u0631\u06cc\u06a9\u0627)"
            },
            // Farsi uses Extended Arabic-Indic numbering system
            // (should not be overridden with it)
            {Locale.forLanguageTag("fa-u-nu-latn"), null, null, null, null,
            "\u067e\u0646\u062c\u0634\u0646\u0628\u0647 10 \u0627\u0648\u062a 2017\u060c " +
            "\u0633\u0627\u0639\u062a 15:15:00 (\u0648\u0642\u062a \u062a\u0627\u0628\u0633" +
            "\u062a\u0627\u0646\u06cc \u063a\u0631\u0628 \u0627\u0645\u0631\u06cc\u06a9\u0627)"
            },
            // Dzongkha uses Tibetan numbering system
            // (should not be overridden with it)
            {Locale.forLanguageTag("dz"), null, null, null, null,
            "\u0f42\u0f5f\u0f60\u0f0b\u0f54\u0f0b\u0f66\u0f44\u0f66\u0f0b, \u0f66\u0fa4\u0fb1" +
            "\u0f72\u0f0b\u0f63\u0f7c\u0f0b2017 \u0f5f\u0fb3\u0f0b\u0f56\u0f62\u0f92\u0fb1" +
            "\u0f51\u0f0b\u0f54\u0f0b \u0f5a\u0f7a\u0f66\u0f0b10 \u0f46\u0f74\u0f0b\u0f5a" +
            "\u0f7c\u0f51\u0f0b 3 \u0f66\u0f90\u0f62\u0f0b\u0f58\u0f0b 15:00 \u0f55\u0fb1" +
            "\u0f72\u0f0b\u0f46\u0f0b \u0f56\u0fb1\u0f44\u0f0b\u0f68\u0f0b\u0f58\u0f72\u0f0b" +
            "\u0f62\u0f72\u0f0b\u0f40\u0f0b\u0f54\u0f7a\u0f0b\u0f66\u0f72\u0f0b\u0f55\u0f72" +
            "\u0f42\u0f0b\u0f49\u0f72\u0f53\u0f0b\u0f66\u0fb2\u0f74\u0f44\u0f0b\u0f46\u0f74" +
            "\u0f0b\u0f5a\u0f7c\u0f51"
            },
        };
    }

    @DataProvider(name="firstDayOfWeek")
    Object[][] firstDayOfWeek () {
        return new Object[][] {
            // Locale, Expected DayOfWeek,
            {Locale.US, DayOfWeek.SUNDAY},
            {FW_SUN, DayOfWeek.SUNDAY},
            {FW_MON, DayOfWeek.MONDAY},
            {FW_TUE, DayOfWeek.TUESDAY},
            {FW_WED, DayOfWeek.WEDNESDAY},
            {FW_THU, DayOfWeek.THURSDAY},
            {FW_FRI, DayOfWeek.FRIDAY},
            {FW_SAT, DayOfWeek.SATURDAY},

            // invalid case
            {Locale.forLanguageTag("en-US-u-fw-xxx"), DayOfWeek.SUNDAY},

            // region override
            {RG_GB, DayOfWeek.MONDAY},
            {Locale.forLanguageTag("zh-CN-u-rg-eszzzz"), DayOfWeek.MONDAY},

            // "fw" and "rg".
            {Locale.forLanguageTag("en-US-u-fw-wed-rg-gbzzzz"), DayOfWeek.WEDNESDAY},
            {Locale.forLanguageTag("en-US-u-fw-xxx-rg-gbzzzz"), DayOfWeek.MONDAY},
            {Locale.forLanguageTag("en-US-u-fw-xxx-rg-zzzz"), DayOfWeek.SUNDAY},
        };
    }

    @DataProvider(name="minDaysInFirstWeek")
    Object[][] minDaysInFrstWeek () {
        return new Object[][] {
            // Locale, Expected minDay,
            {Locale.US, 1},

            // region override
            {RG_GB, 4},
            {Locale.forLanguageTag("zh-CN-u-rg-eszzzz"), 4},
        };
    }

    @DataProvider(name="ofPattern")
    Object[][] ofPattern() {
        return new Object[][] {
            // Locale, Expected Chrono, Expected Zone,
            // Expected formatted string
            {JCAL, null, null,
            "Anno Domini August-10-17 15:15:00 Pacific Daylight Time"
            },
            {HCAL, null, null,
            "Anno Domini August-10-17 15:15:00 Pacific Daylight Time"
            },

            {JPTYO, null, null,
            "Anno Domini August-10-17 15:15:00 Pacific Daylight Time"
            },
            {Locale.forLanguageTag("en-US-u-tz-jpzzz"), null, null,
            "Anno Domini August-10-17 15:15:00 Pacific Daylight Time"
            },

            {RG_GB, null, null,
            "Anno Domini August-10-17 15:15:00 Pacific Daylight Time"
            },

        };
    }

    @DataProvider(name="shortTZID")
    Object[][] shortTZID() {
        return new Object[][] {
            // LDML's short ID, Expected Zone,
            {"adalv", "Europe/Andorra"},
            {"aedxb", "Asia/Dubai"},
            {"afkbl", "Asia/Kabul"},
            {"aganu", "America/Antigua"},
            {"aiaxa", "America/Anguilla"},
            {"altia", "Europe/Tirane"},
            {"amevn", "Asia/Yerevan"},
            {"ancur", "America/Curacao"},
            {"aolad", "Africa/Luanda"},
            {"aqcas", "Antarctica/Casey"},
            {"aqdav", "Antarctica/Davis"},
            {"aqddu", "Antarctica/DumontDUrville"},
            {"aqmaw", "Antarctica/Mawson"},
            {"aqmcm", "Antarctica/McMurdo"},
            {"aqplm", "Antarctica/Palmer"},
            {"aqrot", "Antarctica/Rothera"},
            {"aqsyw", "Antarctica/Syowa"},
            {"aqtrl", "Antarctica/Troll"},
            {"aqvos", "Antarctica/Vostok"},
            {"arbue", "America/Buenos_Aires"},
            {"arcor", "America/Cordoba"},
            {"arctc", "America/Catamarca"},
            {"arirj", "America/Argentina/La_Rioja"},
            {"arjuj", "America/Jujuy"},
            {"arluq", "America/Argentina/San_Luis"},
            {"armdz", "America/Mendoza"},
            {"arrgl", "America/Argentina/Rio_Gallegos"},
            {"arsla", "America/Argentina/Salta"},
            {"artuc", "America/Argentina/Tucuman"},
            {"aruaq", "America/Argentina/San_Juan"},
            {"arush", "America/Argentina/Ushuaia"},
            {"asppg", "Pacific/Pago_Pago"},
            {"atvie", "Europe/Vienna"},
            {"auadl", "Australia/Adelaide"},
            {"aubhq", "Australia/Broken_Hill"},
            {"aubne", "Australia/Brisbane"},
            {"audrw", "Australia/Darwin"},
            {"aueuc", "Australia/Eucla"},
            {"auhba", "Australia/Hobart"},
            {"aukns", "Australia/Currie"},
            {"auldc", "Australia/Lindeman"},
            {"auldh", "Australia/Lord_Howe"},
            {"aumel", "Australia/Melbourne"},
            {"aumqi", "Antarctica/Macquarie"},
            {"auper", "Australia/Perth"},
            {"ausyd", "Australia/Sydney"},
            {"awaua", "America/Aruba"},
            {"azbak", "Asia/Baku"},
            {"basjj", "Europe/Sarajevo"},
            {"bbbgi", "America/Barbados"},
            {"bddac", "Asia/Dhaka"},
            {"bebru", "Europe/Brussels"},
            {"bfoua", "Africa/Ouagadougou"},
            {"bgsof", "Europe/Sofia"},
            {"bhbah", "Asia/Bahrain"},
            {"bibjm", "Africa/Bujumbura"},
            {"bjptn", "Africa/Porto-Novo"},
            {"bmbda", "Atlantic/Bermuda"},
            {"bnbwn", "Asia/Brunei"},
            {"bolpb", "America/La_Paz"},
            {"bqkra", "America/Kralendijk"},
            {"braux", "America/Araguaina"},
            {"brbel", "America/Belem"},
            {"brbvb", "America/Boa_Vista"},
            {"brcgb", "America/Cuiaba"},
            {"brcgr", "America/Campo_Grande"},
            {"brern", "America/Eirunepe"},
            {"brfen", "America/Noronha"},
            {"brfor", "America/Fortaleza"},
            {"brmao", "America/Manaus"},
            {"brmcz", "America/Maceio"},
            {"brpvh", "America/Porto_Velho"},
            {"brrbr", "America/Rio_Branco"},
            {"brrec", "America/Recife"},
            {"brsao", "America/Sao_Paulo"},
            {"brssa", "America/Bahia"},
            {"brstm", "America/Santarem"},
            {"bsnas", "America/Nassau"},
            {"btthi", "Asia/Thimphu"},
            {"bwgbe", "Africa/Gaborone"},
            {"bymsq", "Europe/Minsk"},
            {"bzbze", "America/Belize"},
            {"cacfq", "America/Creston"},
            {"caedm", "America/Edmonton"},
            {"caffs", "America/Rainy_River"},
            {"cafne", "America/Fort_Nelson"},
            {"caglb", "America/Glace_Bay"},
            {"cagoo", "America/Goose_Bay"},
            {"cahal", "America/Halifax"},
            {"caiql", "America/Iqaluit"},
            {"camon", "America/Moncton"},
            {"capnt", "America/Pangnirtung"},
            {"careb", "America/Resolute"},
            {"careg", "America/Regina"},
            {"casjf", "America/St_Johns"},
            {"canpg", "America/Nipigon"},
            {"cathu", "America/Thunder_Bay"},
            {"cator", "America/Toronto"},
            {"cavan", "America/Vancouver"},
            {"cawnp", "America/Winnipeg"},
            {"caybx", "America/Blanc-Sablon"},
            {"caycb", "America/Cambridge_Bay"},
            {"cayda", "America/Dawson"},
            {"caydq", "America/Dawson_Creek"},
            {"cayek", "America/Rankin_Inlet"},
            {"cayev", "America/Inuvik"},
            {"cayxy", "America/Whitehorse"},
            {"cayyn", "America/Swift_Current"},
            {"cayzf", "America/Yellowknife"},
            {"cayzs", "America/Coral_Harbour"},
            {"cccck", "Indian/Cocos"},
            {"cdfbm", "Africa/Lubumbashi"},
            {"cdfih", "Africa/Kinshasa"},
            {"cfbgf", "Africa/Bangui"},
            {"cgbzv", "Africa/Brazzaville"},
            {"chzrh", "Europe/Zurich"},
            {"ciabj", "Africa/Abidjan"},
            {"ckrar", "Pacific/Rarotonga"},
            {"clipc", "Pacific/Easter"},
            {"clscl", "America/Santiago"},
            {"cmdla", "Africa/Douala"},
            {"cnsha", "Asia/Shanghai"},
            {"cnurc", "Asia/Urumqi"},
            {"cobog", "America/Bogota"},
            {"crsjo", "America/Costa_Rica"},
            {"cst6cdt", "CST6CDT"},
            {"cuhav", "America/Havana"},
            {"cvrai", "Atlantic/Cape_Verde"},
            {"cxxch", "Indian/Christmas"},
            {"cynic", "Asia/Nicosia"},
            {"czprg", "Europe/Prague"},
            {"deber", "Europe/Berlin"},
            {"debsngn", "Europe/Busingen"},
            {"djjib", "Africa/Djibouti"},
            {"dkcph", "Europe/Copenhagen"},
            {"dmdom", "America/Dominica"},
            {"dosdq", "America/Santo_Domingo"},
            {"dzalg", "Africa/Algiers"},
            {"ecgps", "Pacific/Galapagos"},
            {"ecgye", "America/Guayaquil"},
            {"eetll", "Europe/Tallinn"},
            {"egcai", "Africa/Cairo"},
            {"eheai", "Africa/El_Aaiun"},
            {"erasm", "Africa/Asmera"},
            {"esceu", "Africa/Ceuta"},
            {"eslpa", "Atlantic/Canary"},
            {"esmad", "Europe/Madrid"},
            {"est5edt", "EST5EDT"},
            {"etadd", "Africa/Addis_Ababa"},
            {"fihel", "Europe/Helsinki"},
            {"fimhq", "Europe/Mariehamn"},
            {"fjsuv", "Pacific/Fiji"},
            {"fkpsy", "Atlantic/Stanley"},
            {"fmksa", "Pacific/Kosrae"},
            {"fmpni", "Pacific/Ponape"},
            {"fmtkk", "Pacific/Truk"},
            {"fotho", "Atlantic/Faeroe"},
            {"frpar", "Europe/Paris"},
            {"galbv", "Africa/Libreville"},
            {"gaza", "Asia/Gaza"},
            {"gblon", "Europe/London"},
            {"gdgnd", "America/Grenada"},
            {"getbs", "Asia/Tbilisi"},
            {"gfcay", "America/Cayenne"},
            {"gggci", "Europe/Guernsey"},
            {"ghacc", "Africa/Accra"},
            {"gigib", "Europe/Gibraltar"},
            {"gldkshvn", "America/Danmarkshavn"},
            {"glgoh", "America/Godthab"},
            {"globy", "America/Scoresbysund"},
            {"glthu", "America/Thule"},
            {"gmbjl", "Africa/Banjul"},
            {"gncky", "Africa/Conakry"},
            {"gpbbr", "America/Guadeloupe"},
            {"gpmsb", "America/Marigot"},
            {"gpsbh", "America/St_Barthelemy"},
            {"gqssg", "Africa/Malabo"},
            {"grath", "Europe/Athens"},
            {"gsgrv", "Atlantic/South_Georgia"},
            {"gtgua", "America/Guatemala"},
            {"gugum", "Pacific/Guam"},
            {"gwoxb", "Africa/Bissau"},
            {"gygeo", "America/Guyana"},
            {"hebron", "Asia/Hebron"},
            {"hkhkg", "Asia/Hong_Kong"},
            {"hntgu", "America/Tegucigalpa"},
            {"hrzag", "Europe/Zagreb"},
            {"htpap", "America/Port-au-Prince"},
            {"hubud", "Europe/Budapest"},
            {"iddjj", "Asia/Jayapura"},
            {"idjkt", "Asia/Jakarta"},
            {"idmak", "Asia/Makassar"},
            {"idpnk", "Asia/Pontianak"},
            {"iedub", "Europe/Dublin"},
            {"imdgs", "Europe/Isle_of_Man"},
            {"inccu", "Asia/Calcutta"},
            {"iodga", "Indian/Chagos"},
            {"iqbgw", "Asia/Baghdad"},
            {"irthr", "Asia/Tehran"},
            {"isrey", "Atlantic/Reykjavik"},
            {"itrom", "Europe/Rome"},
            {"jeruslm", "Asia/Jerusalem"},
            {"jesth", "Europe/Jersey"},
            {"jmkin", "America/Jamaica"},
            {"joamm", "Asia/Amman"},
            {"jptyo", "Asia/Tokyo"},
            {"kenbo", "Africa/Nairobi"},
            {"kgfru", "Asia/Bishkek"},
            {"khpnh", "Asia/Phnom_Penh"},
            {"kicxi", "Pacific/Kiritimati"},
            {"kipho", "Pacific/Enderbury"},
            {"kitrw", "Pacific/Tarawa"},
            {"kmyva", "Indian/Comoro"},
            {"knbas", "America/St_Kitts"},
            {"kpfnj", "Asia/Pyongyang"},
            {"krsel", "Asia/Seoul"},
            {"kwkwi", "Asia/Kuwait"},
            {"kygec", "America/Cayman"},
            {"kzaau", "Asia/Aqtau"},
            {"kzakx", "Asia/Aqtobe"},
            {"kzala", "Asia/Almaty"},
            {"kzkzo", "Asia/Qyzylorda"},
            {"kzura", "Asia/Oral"},
            {"lavte", "Asia/Vientiane"},
            {"lbbey", "Asia/Beirut"},
            {"lccas", "America/St_Lucia"},
            {"livdz", "Europe/Vaduz"},
            {"lkcmb", "Asia/Colombo"},
            {"lrmlw", "Africa/Monrovia"},
            {"lsmsu", "Africa/Maseru"},
            {"ltvno", "Europe/Vilnius"},
            {"lulux", "Europe/Luxembourg"},
            {"lvrix", "Europe/Riga"},
            {"lytip", "Africa/Tripoli"},
            {"macas", "Africa/Casablanca"},
            {"mcmon", "Europe/Monaco"},
            {"mdkiv", "Europe/Chisinau"},
            {"metgd", "Europe/Podgorica"},
            {"mgtnr", "Indian/Antananarivo"},
            {"mhkwa", "Pacific/Kwajalein"},
            {"mhmaj", "Pacific/Majuro"},
            {"mkskp", "Europe/Skopje"},
            {"mlbko", "Africa/Bamako"},
            {"mmrgn", "Asia/Rangoon"},
            {"mncoq", "Asia/Choibalsan"},
            {"mnhvd", "Asia/Hovd"},
            {"mnuln", "Asia/Ulaanbaatar"},
            {"momfm", "Asia/Macau"},
            {"mpspn", "Pacific/Saipan"},
            {"mqfdf", "America/Martinique"},
            {"mrnkc", "Africa/Nouakchott"},
            {"msmni", "America/Montserrat"},
            {"mst7mdt", "MST7MDT"},
            {"mtmla", "Europe/Malta"},
            {"muplu", "Indian/Mauritius"},
            {"mvmle", "Indian/Maldives"},
            {"mwblz", "Africa/Blantyre"},
            {"mxchi", "America/Chihuahua"},
            {"mxcun", "America/Cancun"},
            {"mxhmo", "America/Hermosillo"},
            {"mxmam", "America/Matamoros"},
            {"mxmex", "America/Mexico_City"},
            {"mxmid", "America/Merida"},
            {"mxmty", "America/Monterrey"},
            {"mxmzt", "America/Mazatlan"},
            {"mxoji", "America/Ojinaga"},
            {"mxpvr", "America/Bahia_Banderas"},
            {"mxstis", "America/Santa_Isabel"},
            {"mxtij", "America/Tijuana"},
            {"mykch", "Asia/Kuching"},
            {"mykul", "Asia/Kuala_Lumpur"},
            {"mzmpm", "Africa/Maputo"},
            {"nawdh", "Africa/Windhoek"},
            {"ncnou", "Pacific/Noumea"},
            {"nenim", "Africa/Niamey"},
            {"nfnlk", "Pacific/Norfolk"},
            {"nglos", "Africa/Lagos"},
            {"nimga", "America/Managua"},
            {"nlams", "Europe/Amsterdam"},
            {"noosl", "Europe/Oslo"},
            {"npktm", "Asia/Katmandu"},
            {"nrinu", "Pacific/Nauru"},
            {"nuiue", "Pacific/Niue"},
            {"nzakl", "Pacific/Auckland"},
            {"nzcht", "Pacific/Chatham"},
            {"ommct", "Asia/Muscat"},
            {"papty", "America/Panama"},
            {"pelim", "America/Lima"},
            {"pfgmr", "Pacific/Gambier"},
            {"pfnhv", "Pacific/Marquesas"},
            {"pfppt", "Pacific/Tahiti"},
            {"pgpom", "Pacific/Port_Moresby"},
            {"pgraw", "Pacific/Bougainville"},
            {"phmnl", "Asia/Manila"},
            {"pkkhi", "Asia/Karachi"},
            {"plwaw", "Europe/Warsaw"},
            {"pmmqc", "America/Miquelon"},
            {"pnpcn", "Pacific/Pitcairn"},
            {"prsju", "America/Puerto_Rico"},
            {"pst8pdt", "PST8PDT"},
            {"ptfnc", "Atlantic/Madeira"},
            {"ptlis", "Europe/Lisbon"},
            {"ptpdl", "Atlantic/Azores"},
            {"pwror", "Pacific/Palau"},
            {"pyasu", "America/Asuncion"},
            {"qadoh", "Asia/Qatar"},
            {"rereu", "Indian/Reunion"},
            {"robuh", "Europe/Bucharest"},
            {"rsbeg", "Europe/Belgrade"},
            {"ruchita", "Asia/Chita"},
            {"rudyr", "Asia/Anadyr"},
            {"rugdx", "Asia/Magadan"},
            {"ruikt", "Asia/Irkutsk"},
            {"rukgd", "Europe/Kaliningrad"},
            {"rukhndg", "Asia/Khandyga"},
            {"rukra", "Asia/Krasnoyarsk"},
            {"rukuf", "Europe/Samara"},
            {"rumow", "Europe/Moscow"},
            {"runoz", "Asia/Novokuznetsk"},
            {"ruoms", "Asia/Omsk"},
            {"ruovb", "Asia/Novosibirsk"},
            {"rupkc", "Asia/Kamchatka"},
            {"rusred", "Asia/Srednekolymsk"},
            {"ruunera", "Asia/Ust-Nera"},
            {"ruuus", "Asia/Sakhalin"},
            {"ruvog", "Europe/Volgograd"},
            {"ruvvo", "Asia/Vladivostok"},
            {"ruyek", "Asia/Yekaterinburg"},
            {"ruyks", "Asia/Yakutsk"},
            {"rwkgl", "Africa/Kigali"},
            {"saruh", "Asia/Riyadh"},
            {"sbhir", "Pacific/Guadalcanal"},
            {"scmaw", "Indian/Mahe"},
            {"sdkrt", "Africa/Khartoum"},
            {"sesto", "Europe/Stockholm"},
            {"sgsin", "Asia/Singapore"},
            {"shshn", "Atlantic/St_Helena"},
            {"silju", "Europe/Ljubljana"},
            {"sjlyr", "Arctic/Longyearbyen"},
            {"skbts", "Europe/Bratislava"},
            {"slfna", "Africa/Freetown"},
            {"smsai", "Europe/San_Marino"},
            {"sndkr", "Africa/Dakar"},
            {"somgq", "Africa/Mogadishu"},
            {"srpbm", "America/Paramaribo"},
            {"ssjub", "Africa/Juba"},
            {"sttms", "Africa/Sao_Tome"},
            {"svsal", "America/El_Salvador"},
            {"sxphi", "America/Lower_Princes"},
            {"sydam", "Asia/Damascus"},
            {"szqmn", "Africa/Mbabane"},
            {"tcgdt", "America/Grand_Turk"},
            {"tdndj", "Africa/Ndjamena"},
            {"tfpfr", "Indian/Kerguelen"},
            {"tglfw", "Africa/Lome"},
            {"thbkk", "Asia/Bangkok"},
            {"tjdyu", "Asia/Dushanbe"},
            {"tkfko", "Pacific/Fakaofo"},
            {"tldil", "Asia/Dili"},
            {"tmasb", "Asia/Ashgabat"},
            {"tntun", "Africa/Tunis"},
            {"totbu", "Pacific/Tongatapu"},
            {"trist", "Europe/Istanbul"},
            {"ttpos", "America/Port_of_Spain"},
            {"tvfun", "Pacific/Funafuti"},
            {"twtpe", "Asia/Taipei"},
            {"tzdar", "Africa/Dar_es_Salaam"},
            {"uaiev", "Europe/Kiev"},
            {"uaozh", "Europe/Zaporozhye"},
            {"uasip", "Europe/Simferopol"},
            {"uauzh", "Europe/Uzhgorod"},
            {"ugkla", "Africa/Kampala"},
            {"umawk", "Pacific/Wake"},
            {"umjon", "Pacific/Johnston"},
            {"ummdy", "Pacific/Midway"},
//            {"unk", "Etc/Unknown"},
            {"usadk", "America/Adak"},
            {"usaeg", "America/Indiana/Marengo"},
            {"usanc", "America/Anchorage"},
            {"usboi", "America/Boise"},
            {"uschi", "America/Chicago"},
            {"usden", "America/Denver"},
            {"usdet", "America/Detroit"},
            {"ushnl", "Pacific/Honolulu"},
            {"usind", "America/Indianapolis"},
            {"usinvev", "America/Indiana/Vevay"},
            {"usjnu", "America/Juneau"},
            {"usknx", "America/Indiana/Knox"},
            {"uslax", "America/Los_Angeles"},
            {"uslui", "America/Louisville"},
            {"usmnm", "America/Menominee"},
            {"usmtm", "America/Metlakatla"},
            {"usmoc", "America/Kentucky/Monticello"},
            {"usndcnt", "America/North_Dakota/Center"},
            {"usndnsl", "America/North_Dakota/New_Salem"},
            {"usnyc", "America/New_York"},
            {"usoea", "America/Indiana/Vincennes"},
            {"usome", "America/Nome"},
            {"usphx", "America/Phoenix"},
            {"ussit", "America/Sitka"},
            {"ustel", "America/Indiana/Tell_City"},
            {"uswlz", "America/Indiana/Winamac"},
            {"uswsq", "America/Indiana/Petersburg"},
            {"usxul", "America/North_Dakota/Beulah"},
            {"usyak", "America/Yakutat"},
            {"utc", "Etc/UTC"},
            {"utce01", "Etc/GMT-1"},
            {"utce02", "Etc/GMT-2"},
            {"utce03", "Etc/GMT-3"},
            {"utce04", "Etc/GMT-4"},
            {"utce05", "Etc/GMT-5"},
            {"utce06", "Etc/GMT-6"},
            {"utce07", "Etc/GMT-7"},
            {"utce08", "Etc/GMT-8"},
            {"utce09", "Etc/GMT-9"},
            {"utce10", "Etc/GMT-10"},
            {"utce11", "Etc/GMT-11"},
            {"utce12", "Etc/GMT-12"},
            {"utce13", "Etc/GMT-13"},
            {"utce14", "Etc/GMT-14"},
            {"utcw01", "Etc/GMT+1"},
            {"utcw02", "Etc/GMT+2"},
            {"utcw03", "Etc/GMT+3"},
            {"utcw04", "Etc/GMT+4"},
            {"utcw05", "Etc/GMT+5"},
            {"utcw06", "Etc/GMT+6"},
            {"utcw07", "Etc/GMT+7"},
            {"utcw08", "Etc/GMT+8"},
            {"utcw09", "Etc/GMT+9"},
            {"utcw10", "Etc/GMT+10"},
            {"utcw11", "Etc/GMT+11"},
            {"utcw12", "Etc/GMT+12"},
            {"uymvd", "America/Montevideo"},
            {"uzskd", "Asia/Samarkand"},
            {"uztas", "Asia/Tashkent"},
            {"vavat", "Europe/Vatican"},
            {"vcsvd", "America/St_Vincent"},
            {"veccs", "America/Caracas"},
            {"vgtov", "America/Tortola"},
            {"vistt", "America/St_Thomas"},
            {"vnsgn", "Asia/Saigon"},
            {"vuvli", "Pacific/Efate"},
            {"wfmau", "Pacific/Wallis"},
            {"wsapw", "Pacific/Apia"},
            {"yeade", "Asia/Aden"},
            {"ytmam", "Indian/Mayotte"},
            {"zajnb", "Africa/Johannesburg"},
            {"zmlun", "Africa/Lusaka"},
            {"zwhre", "Africa/Harare"},

        };
    }

    @DataProvider(name="getLocalizedDateTimePattern")
    Object[][] getLocalizedDateTimePattern() {
        return new Object[][] {
            // Locale, Expected pattern,
            {Locale.US, FormatStyle.FULL, "EEEE, MMMM d, y 'at' h:mm:ss a zzzz"},
            {Locale.US, FormatStyle.LONG, "MMMM d, y 'at' h:mm:ss a z"},
            {Locale.US, FormatStyle.MEDIUM, "MMM d, y, h:mm:ss a"},
            {Locale.US, FormatStyle.SHORT, "M/d/yy, h:mm a"},
            {RG_GB, FormatStyle.FULL, "EEEE, d MMMM y 'at' HH:mm:ss zzzz"},
            {RG_GB, FormatStyle.LONG, "d MMMM y 'at' HH:mm:ss z"},
            {RG_GB, FormatStyle.MEDIUM, "d MMM y, HH:mm:ss"},
            {RG_GB, FormatStyle.SHORT, "dd/MM/y, HH:mm"},
        };
    }

    @DataProvider(name="getDisplayName")
    Object[][] getDisplayName() {
        return new Object[][] {
            // Locale, field, Expected name,
            {Locale.US, ChronoField.AMPM_OF_DAY, "AM/PM"},
            {RG_GB, ChronoField.AMPM_OF_DAY, "am/pm"},
        };
    }

    @Test(dataProvider="localizedBy")
    public void test_localizedBy(Locale locale, Chronology chrono, ZoneId zone,
                                Chronology chronoExpected, ZoneId zoneExpected,
                                String formatExpected) {
        // try this test both with the implicit default locale, and explicit default locale ja-JP
        Locale def = Locale.getDefault();
        try {
            Stream.of(def, Locale.JAPAN).forEach(l -> {
                System.out.println("    Testing with the default locale: " + l);
                Locale.setDefault(l);

                DateTimeFormatter dtf =
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)
                                .withChronology(chrono).withZone(zone).localizedBy(locale);
                assertEquals(dtf.getChronology(), chronoExpected);
                assertEquals(dtf.getZone(), zoneExpected);
                String formatted = dtf.format(ZDT);
                assertEquals(formatted, formatExpected);
                assertEquals(dtf.parse(formatted, ZonedDateTime::from),
                        zoneExpected != null ? ZDT.withZoneSameInstant(zoneExpected) : ZDT);
            });
        } finally {
            Locale.setDefault(def);
        }
    }

    @Test(dataProvider="withLocale")
    public void test_withLocale(Locale locale, Chronology chrono, ZoneId zone,
                                Chronology chronoExpected, ZoneId zoneExpected,
                                String formatExpected) {
        DateTimeFormatter dtf =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)
                .withChronology(chrono).withZone(zone).withLocale(locale);
        assertEquals(dtf.getChronology(), chronoExpected);
        assertEquals(dtf.getZone(), zoneExpected);
        String formatted = dtf.format(ZDT);
        assertEquals(formatted, formatExpected);
        assertEquals(dtf.parse(formatted, ZonedDateTime::from),
            zoneExpected != null ? ZDT.withZoneSameInstant(zoneExpected) : ZDT);
    }

    @Test(dataProvider="firstDayOfWeek")
    public void test_firstDayOfWeek(Locale locale, DayOfWeek dowExpected) {
        DayOfWeek dow = WeekFields.of(locale).getFirstDayOfWeek();
        assertEquals(dow, dowExpected);
    }

    @Test(dataProvider="minDaysInFirstWeek")
    public void test_minDaysInFirstWeek(Locale locale, int minDaysExpected) {
        int minDays = WeekFields.of(locale).getMinimalDaysInFirstWeek();
        assertEquals(minDays, minDaysExpected);
    }

    @Test(dataProvider="ofPattern")
    public void test_ofPattern(Locale locale,
                                Chronology chronoExpected, ZoneId zoneExpected,
                                String formatExpected) {
        DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern(PATTERN, locale);
        assertEquals(dtf.getChronology(), chronoExpected);
        assertEquals(dtf.getZone(), zoneExpected);
        String formatted = dtf.format(ZDT);
        assertEquals(formatted, formatExpected);
        assertEquals(dtf.parse(formatted, ZonedDateTime::from),
            zoneExpected != null ? ZDT.withZoneSameInstant(zoneExpected) : ZDT);
    }

    @Test(dataProvider="ofPattern")
    public void test_toFormatter(Locale locale,
                                Chronology chronoExpected, ZoneId zoneExpected,
                                String formatExpected) {
        DateTimeFormatter dtf =
            new DateTimeFormatterBuilder().appendPattern(PATTERN).toFormatter(locale);
        assertEquals(dtf.getChronology(), chronoExpected);
        assertEquals(dtf.getZone(), zoneExpected);
        String formatted = dtf.format(ZDT);
        assertEquals(formatted, formatExpected);
        assertEquals(dtf.parse(formatted, ZonedDateTime::from),
            zoneExpected != null ? ZDT.withZoneSameInstant(zoneExpected) : ZDT);
    }

    @Test(dataProvider="shortTZID")
    public void test_shortTZID(String shortID, String expectedZone) {
        Locale l = Locale.forLanguageTag("en-US-u-tz-" + shortID);
        DateTimeFormatter dtf =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)
                .localizedBy(l);
        assertEquals(dtf.getZone(), ZoneId.of(expectedZone));
    }

    @Test(dataProvider="getLocalizedDateTimePattern")
    public void test_getLocalizedDateTimePattern(Locale l, FormatStyle s, String expectedPattern) {
        DateTimeFormatterBuilder dtfb = new DateTimeFormatterBuilder();
        assertEquals(dtfb.getLocalizedDateTimePattern(s, s, IsoChronology.INSTANCE, l),
            expectedPattern);
    }

    @Test(dataProvider="getDisplayName")
    public void test_getDisplayName(Locale l, TemporalField f, String expectedName) {
        assertEquals(f.getDisplayName(l), expectedName);
    }
}
