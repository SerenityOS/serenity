/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.text.*;
import java.text.spi.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.spi.*;
import java.util.stream.IntStream;
import sun.util.locale.provider.LocaleProviderAdapter;

import static java.util.logging.LogManager.*;

public class LocaleProviders {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
    private static final boolean IS_MAC = System.getProperty("os.name").startsWith("Mac");

    public static void main(String[] args) {
        String methodName = args[0];

        switch (methodName) {
            case "getPlatformLocale":
                if (args[1].equals("format")) {
                    getPlatformLocale(Locale.Category.FORMAT);
                } else {
                    getPlatformLocale(Locale.Category.DISPLAY);
                }
                break;

            case "adapterTest":
                adapterTest(args[1], args[2], (args.length >= 4 ? args[3] : ""));
                break;

            case "bug7198834Test":
                bug7198834Test();
                break;

            case "tzNameTest":
                tzNameTest(args[1]);
                break;

            case "bug8001440Test":
                bug8001440Test();
                break;

            case "bug8010666Test":
                bug8010666Test();
                break;

            case "bug8013086Test":
                bug8013086Test(args[1], args[2]);
                break;

            case "bug8013903Test":
                bug8013903Test();
                break;

            case "bug8027289Test":
                bug8027289Test(args[1]);
                break;

            case "bug8220227Test":
                bug8220227Test();
                break;

            case "bug8228465Test":
                bug8228465Test();
                break;

            case "bug8232871Test":
                bug8232871Test();
                break;

            case "bug8232860Test":
                bug8232860Test();
                break;

            case "bug8245241Test":
                bug8245241Test(args[1]);
                break;

            case "bug8248695Test":
                bug8248695Test();
                break;

            case "bug8257964Test":
                bug8257964Test();
                break;

            default:
                throw new RuntimeException("Test method '"+methodName+"' not found.");
        }
    }

    static void getPlatformLocale(Locale.Category cat) {
        Locale defloc = Locale.getDefault(cat);
        System.out.printf("%s,%s\n", defloc.getLanguage(), defloc.getCountry());
    }

    static void adapterTest(String expected, String lang, String ctry) {
        Locale testLocale = new Locale(lang, ctry);
        LocaleProviderAdapter ldaExpected =
            LocaleProviderAdapter.forType(LocaleProviderAdapter.Type.valueOf(expected));
        if (!ldaExpected.getDateFormatProvider().isSupportedLocale(testLocale)) {
            System.out.println("test locale: "+testLocale+" is not supported by the expected provider: "+ldaExpected+". Ignoring the test.");
            return;
        }
        String preference = System.getProperty("java.locale.providers", "");
        LocaleProviderAdapter lda = LocaleProviderAdapter.getAdapter(DateFormatProvider.class, testLocale);
        LocaleProviderAdapter.Type type = lda.getAdapterType();
        System.out.printf("testLocale: %s, got: %s, expected: %s\n", testLocale, type, expected);
        if (!type.toString().equals(expected)) {
            throw new RuntimeException("Returned locale data adapter is not correct.");
        }
    }

    static void bug7198834Test() {
        LocaleProviderAdapter lda = LocaleProviderAdapter.getAdapter(DateFormatProvider.class, Locale.US);
        LocaleProviderAdapter.Type type = lda.getAdapterType();
        if (type == LocaleProviderAdapter.Type.HOST && IS_WINDOWS) {
            DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, Locale.US);
            String date = df.format(new Date());
            if (date.charAt(date.length()-1) == ' ') {
                throw new RuntimeException("Windows Host Locale Provider returns a trailing space.");
            }
        } else {
            System.out.println("Windows HOST locale adapter not found. Ignoring this test.");
        }
    }

    static void tzNameTest(String id) {
        TimeZone tz = TimeZone.getTimeZone(id);
        String tzName = tz.getDisplayName(false, TimeZone.SHORT, Locale.US);
        if (tzName.startsWith("GMT")) {
            throw new RuntimeException("JRE's localized time zone name for "+id+" could not be retrieved. Returned name was: "+tzName);
        }
    }

    static void bug8001440Test() {
        Locale locale = Locale.forLanguageTag("th-TH-u-nu-hoge");
        NumberFormat nf = NumberFormat.getInstance(locale);
        String nu = nf.format(1234560);
    }

    // This test assumes Windows localized language/country display names.
    static void bug8010666Test() {
        if (IS_WINDOWS) {
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            try {
                double ver = nf.parse(System.getProperty("os.version"))
                               .doubleValue();
                System.out.printf("Windows version: %.1f\n", ver);
                if (ver >= 6.0) {
                    LocaleProviderAdapter lda =
                        LocaleProviderAdapter.getAdapter(
                            LocaleNameProvider.class, Locale.ENGLISH);
                    LocaleProviderAdapter.Type type = lda.getAdapterType();
                    if (type == LocaleProviderAdapter.Type.HOST) {
                        LocaleNameProvider lnp = lda.getLocaleNameProvider();
                        Locale mkmk = Locale.forLanguageTag("mk-MK");
                        String result = mkmk.getDisplayLanguage(Locale.ENGLISH);
                        String hostResult =
                            lnp.getDisplayLanguage(mkmk.getLanguage(),
                                                   Locale.ENGLISH);
                        System.out.printf("  Display language name for" +
                            " (mk_MK): result(HOST): \"%s\", returned: \"%s\"\n",
                            hostResult, result);
                        if (result == null ||
                            hostResult != null &&
                            !result.equals(hostResult)) {
                            throw new RuntimeException("Display language name" +
                                " mismatch for \"mk\". Returned name was" +
                                " \"" + result + "\", result(HOST): \"" +
                                hostResult + "\"");
                        }
                        result = Locale.US.getDisplayLanguage(Locale.ENGLISH);
                        hostResult =
                            lnp.getDisplayLanguage(Locale.US.getLanguage(),
                                                   Locale.ENGLISH);
                        System.out.printf("  Display language name for" +
                            " (en_US): result(HOST): \"%s\", returned: \"%s\"\n",
                            hostResult, result);
                        if (result == null ||
                            hostResult != null &&
                            !result.equals(hostResult)) {
                            throw new RuntimeException("Display language name" +
                                " mismatch for \"en\". Returned name was" +
                                " \"" + result + "\", result(HOST): \"" +
                                hostResult + "\"");
                        }
                        if (ver >= 6.1) {
                            result = Locale.US.getDisplayCountry(Locale.ENGLISH);
                            hostResult = lnp.getDisplayCountry(
                                Locale.US.getCountry(), Locale.ENGLISH);
                            System.out.printf("  Display country name for" +
                                " (en_US): result(HOST): \"%s\", returned: \"%s\"\n",
                                hostResult, result);
                            if (result == null ||
                                hostResult != null &&
                                !result.equals(hostResult)) {
                                throw new RuntimeException("Display country name" +
                                    " mismatch for \"US\". Returned name was" +
                                    " \"" + result + "\", result(HOST): \"" +
                                    hostResult + "\"");
                            }
                        }
                    } else {
                        throw new RuntimeException("Windows Host" +
                            " LocaleProviderAdapter was not selected for" +
                            " English locale.");
                    }
                }
            } catch (ParseException pe) {
                throw new RuntimeException("Parsing Windows version failed: "+pe.toString());
            }
        }
    }

    static void bug8013086Test(String lang, String ctry) {
        try {
            // Throws a NullPointerException if the test fails.
            System.out.println(new SimpleDateFormat("z", new Locale(lang, ctry)).parse("UTC"));
        } catch (ParseException pe) {
            // ParseException is fine in this test, as it's not "UTC"
        }
    }

    static void bug8013903Test() {
        if (IS_WINDOWS) {
            Date sampleDate = new Date(0x10000000000L);
            String expected = "\u5e73\u6210 16.11.03 (\u6c34) \u5348\u524d 11:53:47";
            Locale l = new Locale("ja", "JP", "JP");
            SimpleDateFormat sdf = new SimpleDateFormat("GGGG yyyy.MMM.dd '('E')' a hh:mm:ss", l);
            sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
            String result = sdf.format(sampleDate);
            System.out.println(result);
            if (LocaleProviderAdapter.getAdapterPreference()
                .contains(LocaleProviderAdapter.Type.JRE)) {
                if (!expected.equals(result)) {
                    throw new RuntimeException("Format failed. result: \"" +
                        result + "\", expected: \"" + expected);
                }
            } else {
                // Windows display names. Subject to change if Windows changes its format.
                if (!expected.equals(result)) {
                    throw new RuntimeException("Format failed. result: \"" +
                        result + "\", expected: \"" + expected);
                }
            }
        }
    }

    static void bug8027289Test(String expectedCodePoint) {
        if (IS_WINDOWS) {
            char[] expectedSymbol = Character.toChars(Integer.valueOf(expectedCodePoint, 16));
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.CHINA);
            char formatted = nf.format(7000).charAt(0);
            System.out.println("returned: " + formatted + ", expected: " + expectedSymbol[0]);
            if (formatted != expectedSymbol[0]) {
                throw new RuntimeException(
                        "Unexpected Chinese currency symbol. returned: "
                                + formatted + ", expected: " + expectedSymbol[0]);
            }
        }
    }

    static void bug8220227Test() {
        if (IS_WINDOWS) {
            Locale l = new Locale("xx","XX");
            String country = l.getDisplayCountry();
            if (country.endsWith("(XX)")) {
                throw new RuntimeException(
                        "Unexpected Region name: " + country);
            }
        }
    }

    static void bug8228465Test() {
        LocaleProviderAdapter lda = LocaleProviderAdapter.getAdapter(CalendarNameProvider.class, Locale.US);
        LocaleProviderAdapter.Type type = lda.getAdapterType();
        if (type == LocaleProviderAdapter.Type.HOST && IS_WINDOWS) {
            var names =  new GregorianCalendar()
                .getDisplayNames(Calendar.ERA, Calendar.SHORT_FORMAT, Locale.US);
            if (!names.keySet().contains("AD") ||
                names.get("AD").intValue() != 1) {
                    throw new RuntimeException(
                            "Short Era name for 'AD' is missing or incorrect");
            } else {
                System.out.println("bug8228465Test succeeded.");
            }
        }
    }

    static void bug8232871Test() {
        LocaleProviderAdapter lda = LocaleProviderAdapter.getAdapter(CalendarNameProvider.class, Locale.US);
        LocaleProviderAdapter.Type type = lda.getAdapterType();
        var lang = Locale.getDefault().getLanguage();
        var cal = Calendar.getInstance();
        var calType = cal.getCalendarType();
        var expected = "\u4ee4\u548c1\u5e745\u67081\u65e5 \u6c34\u66dc\u65e5 \u5348\u524d0:00:00 \u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u590f\u6642\u9593";

        if (type == LocaleProviderAdapter.Type.HOST &&
            IS_MAC &&
            lang.equals("ja") &&
            calType.equals("japanese")) {
            cal.set(1, 4, 1, 0, 0, 0);
            cal.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL,
                            Locale.JAPAN);
            df.setCalendar(cal);
            var result = df.format(cal.getTime());
            if (result.equals(expected)) {
                System.out.println("bug8232871Test succeeded.");
            } else {
                throw new RuntimeException(
                            "Japanese calendar names mismatch. result: " +
                            result +
                            ", expected: " +
                            expected);
            }
        } else {
            System.out.println("Test ignored. Either :-\n" +
                "OS is not macOS, or\n" +
                "provider is not HOST: " + type + ", or\n" +
                "Language is not Japanese: " + lang + ", or\n" +
                "native calendar is not JapaneseCalendar: " + calType);
        }
    }

    static void bug8232860Test() {
        var inputList = List.of(123, 123.4);
        var nfExpectedList = List.of("123", "123.4");
        var ifExpectedList = List.of("123", "123");

        var defLoc = Locale.getDefault(Locale.Category.FORMAT);
        var type = LocaleProviderAdapter.getAdapter(CalendarNameProvider.class, Locale.US)
                                        .getAdapterType();
        if (defLoc.equals(Locale.US) &&
            type == LocaleProviderAdapter.Type.HOST &&
            (IS_WINDOWS || IS_MAC)) {
            final var numf = NumberFormat.getNumberInstance(Locale.US);
            final var intf = NumberFormat.getIntegerInstance(Locale.US);

            IntStream.range(0, inputList.size())
                .forEach(i -> {
                    var input = inputList.get(i);
                    var nfExpected = nfExpectedList.get(i);
                    var result = numf.format(input);
                    if (!result.equals(nfExpected)) {
                        throw new RuntimeException("Incorrect number format. " +
                            "input: " + input + ", expected: " +
                            nfExpected + ", result: " + result);
                    }

                    var ifExpected = ifExpectedList.get(i);
                    result = intf.format(input);
                    if (!result.equals(ifExpected)) {
                        throw new RuntimeException("Incorrect integer format. " +
                            "input: " + input + ", expected: " +
                            ifExpected + ", result: " + result);
                    }
                });
            System.out.println("bug8232860Test succeeded.");
        } else {
            System.out.println("Test ignored. Either :-\n" +
                "Default format locale is not Locale.US: " + defLoc + ", or\n" +
                "OS is neither macOS/Windows, or\n" +
                "provider is not HOST: " + type);
        }
    }

    static void bug8245241Test(String expected) {
        // this will ensure LocaleProviderAdapter initialization
        DateFormat.getDateInstance();
        LogConfig.handler.flush();

        if (LogConfig.logRecordList.stream()
                .noneMatch(r -> r.getLevel() == Level.INFO &&
                                r.getMessage().equals(expected))) {
            throw new RuntimeException("Expected log was not emitted.");
        }
    }

    // Set the root logger on loading the logging class
    public static class LogConfig {
        final static CopyOnWriteArrayList<LogRecord> logRecordList = new CopyOnWriteArrayList<>();
        final static StreamHandler handler = new StreamHandler() {
            @Override
            public void publish(LogRecord record) {
                logRecordList.add(record);
                System.out.println("LogRecord: " + record.getMessage());
            }
        };
        static {
            getLogManager().getLogger("").addHandler(handler);
        }
    }

    static void bug8248695Test() {
        Locale l = Locale.getDefault(Locale.Category.FORMAT);
        LocaleProviderAdapter lda = LocaleProviderAdapter.getAdapter(DateFormatProvider.class, l);
        LocaleProviderAdapter.Type type = lda.getAdapterType();
        if (type == LocaleProviderAdapter.Type.HOST) {
            System.out.println("Locale: " + l);
            var ld = LocalDate.now();
            var zdt = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
            var df = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(l);
            var tf = DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL).withLocale(l);
            var dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(l);

            // Checks there's no "unsupported temporal field" exception thrown, such as HourOfDay
            System.out.println(df.format(ld));
            System.out.println(tf.format(zdt));

            // Checks there's no "Too many pattern letters: aa" exception thrown, if
            // underlying OS provides the "am/pm" pattern.
            System.out.println(dtf.format(zdt));
        }
    }

    // Run only if the platform locale is en-GB
    static void bug8257964Test() {
        var defLoc = Locale.getDefault(Locale.Category.FORMAT);
        var type = LocaleProviderAdapter.getAdapter(CalendarNameProvider.class, Locale.UK)
                .getAdapterType();
        if (defLoc.equals(Locale.UK) &&
                type == LocaleProviderAdapter.Type.HOST &&
                (IS_WINDOWS || IS_MAC)) {
            Calendar instance = Calendar.getInstance(Locale.UK);
            int result = instance.getMinimalDaysInFirstWeek();
            if (result != 4) {
                throw new RuntimeException("MinimalDaysInFirstWeek for Locale.UK is incorrect. " +
                        "returned: " + result);
            }

            LocalDate date = LocalDate.of(2020,12,31);
            result = date.get(WeekFields.of(Locale.UK).weekOfWeekBasedYear());
            if (result != 53) {
                throw new RuntimeException("weekNumber is incorrect. " +
                        "returned: " + result);
            }
            System.out.println("bug8257964Test succeeded.");
        } else {
            System.out.println("Test ignored. Either :-\n" +
                    "Default format locale is not Locale.UK: " + defLoc + ", or\n" +
                    "OS is neither macOS/Windows, or\n" +
                    "provider is not HOST: " + type);
        }
    }
}
