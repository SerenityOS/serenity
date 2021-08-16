/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4449637 8008577
 * @summary Basic acceptance test for international J2RE. Verifies that the
 * most important locale data and character converters exist and are
 * minimally functional.
 * @modules jdk.localedata
 *          jdk.charsets
 * @run main/othervm -Djava.locale.providers=JRE,SPI InternationalBAT
 */

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class InternationalBAT {

    public static void main(String[] args) {
        boolean pass = true;

        TimeZone tz = TimeZone.getDefault();
        try {
            pass &= testRequiredLocales();
            pass &= testRequiredEncodings();
        } finally {
            TimeZone.setDefault(tz);
        }

        if (!pass) {
            System.out.println("\nSome tests failed.\n"
                    + "If you installed the US-only J2RE for Windows, "
                    + "failures are expected and OK.\n"
                    + "If you installed the international J2RE, or any J2SDK, "
                    + "or if this occurs on any platform other than Windows, "
                    + "please file a bug report.\n"
                    + "Unfortunately, this test cannot determine whether you "
                    + "installed a US-only J2RE, an international J2RE, or "
                    + "a J2SDK.\n");
            throw new RuntimeException();
        }
    }

    // We require the "fully supported locales" for java.util and java.text:
    // http://webwork.eng/j2se/1.4/docs/guide/intl/locale.doc.html#util-text

    private static Locale[] requiredLocales = {
        new Locale("ar", "SA"),
        new Locale("zh", "CN"),
        new Locale("zh", "TW"),
        new Locale("nl", "NL"),
        new Locale("en", "AU"),
        new Locale("en", "CA"),
        new Locale("en", "GB"),
        new Locale("en", "US"),
        new Locale("fr", "CA"),
        new Locale("fr", "FR"),
        new Locale("de", "DE"),
        new Locale("iw", "IL"),
        new Locale("hi", "IN"),
        new Locale("it", "IT"),
        new Locale("ja", "JP"),
        new Locale("ko", "KR"),
        new Locale("pt", "BR"),
        new Locale("es", "ES"),
        new Locale("sv", "SE"),
        new Locale("th", "TH"),
    };

    // Date strings for May 10, 2001, for the required locales
    private static String[] requiredLocaleDates = {
        "10 \u0645\u0627\u064A\u0648, 2001",
        "2001\u5E745\u670810\u65E5 \u661F\u671F\u56DB",
        "2001\u5E745\u670810\u65E5 \u661F\u671F\u56DB",
        "donderdag 10 mei 2001",
        "Thursday, 10 May 2001",
        "Thursday, May 10, 2001",
        "Thursday, 10 May 2001",
        "Thursday, May 10, 2001",
        "jeudi 10 mai 2001",
        "jeudi 10 mai 2001",
        "Donnerstag, 10. Mai 2001",
        "\u05D9\u05D5\u05DD \u05D7\u05DE\u05D9\u05E9\u05D9 10 \u05DE\u05D0\u05D9 2001",
        "\u0917\u0941\u0930\u0941\u0935\u093E\u0930, \u0967\u0966 \u092E\u0908, \u0968\u0966\u0966\u0967",
        "gioved\u00EC 10 maggio 2001",
        "2001\u5E745\u670810\u65E5", // ja_JP
        "2001\uB144 5\uC6D4 10\uC77C \uBAA9\uC694\uC77C",
        "Quinta-feira, 10 de Maio de 2001",
        "jueves 10 de mayo de 2001",
        "den 10 maj 2001",
        "\u0E27\u0E31\u0E19\u0E1E\u0E24\u0E2B\u0E31\u0E2A\u0E1A\u0E14\u0E35\u0E17\u0E35\u0E48 10 \u0E1E\u0E24\u0E29\u0E20\u0E32\u0E04\u0E21 \u0E1E.\u0E28. 2544",
    };

    private static boolean testRequiredLocales() {
        boolean pass = true;

        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.clear();
        calendar.set(2001, 4, 10, 12, 0, 0);
        Date date = calendar.getTime();

        Locale[] available = Locale.getAvailableLocales();
        for (int i = 0; i < requiredLocales.length; i++) {
            Locale locale = requiredLocales[i];
            boolean found = false;
            for (int j = 0; j < available.length; j++) {
                if (available[j].equals(locale)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Locale not available: " + locale);
                pass = false;
            } else {
                DateFormat format =
                        DateFormat.getDateInstance(DateFormat.FULL, locale);
                String dateString = format.format(date);
                if (!dateString.equals(requiredLocaleDates[i])) {
                    System.out.println("Incorrect date string for locale "
                            + locale + ". Expected: " + requiredLocaleDates[i]
                            + ", got: " + dateString);
                    pass = false;
                }
            }
        }
        return pass;
    }

    // We require the encodings of the fully supported writing systems:
    // http://webwork.eng/j2se/1.4/docs/guide/intl/locale.doc.html#jfc

    private static String[] requiredEncodings = {
        "Cp1256",
        "MS936",
        "MS950",
        "Cp1255",
        "MS932",
        "MS949",
        "Cp1252",
        "MS874",
        "ISO8859_6",
        "EUC_CN",
        "UTF8",
        "GBK",
        "EUC_TW",
        "ISO8859_8",
        "EUC_JP",
        "PCK",
        "EUC_KR",
        "ISO8859_1",
        "ISO8859_15",
        "TIS620",
    };

    // one sample locale each for the required encodings

    private static Locale[] sampleLocales = {
        new Locale("ar", "SA"),
        new Locale("zh", "CN"),
        new Locale("zh", "TW"),
        new Locale("iw", "IL"),
        new Locale("ja", "JP"),
        new Locale("ko", "KR"),
        new Locale("it", "IT"),
        new Locale("th", "TH"),
        new Locale("ar", "SA"),
        new Locale("zh", "CN"),
        new Locale("zh", "CN"),
        new Locale("zh", "CN"),
        new Locale("zh", "TW"),
        new Locale("iw", "IL"),
        new Locale("ja", "JP"),
        new Locale("ja", "JP"),
        new Locale("ko", "KR"),
        new Locale("it", "IT"),
        new Locale("it", "IT"),
        new Locale("th", "TH"),
    };

    // expected conversion results for the date strings of the sample locales

    private static byte[][] expectedBytes = {
        { 0x31, 0x30, 0x20, (byte) 0xE3, (byte) 0xC7, (byte) 0xED, (byte) 0xE6, 0x2C, 0x20, 0x32, 0x30, 0x30, 0x31, },
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xC4, (byte) 0xEA, 0x35, (byte) 0xD4, (byte) 0xC2, 0x31, 0x30, (byte) 0xC8, (byte) 0xD5, 0x20, (byte) 0xD0, (byte) 0xC7, (byte) 0xC6, (byte) 0xDA, (byte) 0xCB, (byte) 0xC4},
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xA6, 0x7E, 0x35, (byte) 0xA4, (byte) 0xEB, 0x31, 0x30, (byte) 0xA4, (byte) 0xE9, 0x20, (byte) 0xAC, (byte)0x50, (byte) 0xB4, (byte) 0xC1, (byte) 0xA5, (byte) 0x7C},
        { (byte) 0xE9, (byte) 0xE5, (byte) 0xED, 0x20, (byte) 0xE7, (byte) 0xEE, (byte) 0xE9, (byte) 0xF9, (byte) 0xE9, 0x20, 0x31, 0x30, 0x20, (byte) 0xEE, (byte) 0xE0, (byte) 0xE9, 0x20, 0x32, 0x30, 0x30, 0x31, },
        { 0x32, 0x30, 0x30, 0x31, (byte) 0x94, 0x4E, 0x35, (byte) 0x8C, (byte) 0x8E, 0x31, 0x30, (byte) 0x93, (byte) 0xFA, },
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xB3, (byte) 0xE2, 0x20, 0x35, (byte) 0xBF, (byte) 0xF9, 0x20, 0x31, 0x30, (byte) 0xC0, (byte) 0xCF, 0x20, (byte) 0xB8, (byte) 0xF1, (byte) 0xBF, (byte) 0xE4, (byte) 0xC0, (byte) 0xCF, },
        { 0x67, 0x69, 0x6F, 0x76, 0x65, 0x64, (byte) 0xEC, 0x20, 0x31, 0x30, 0x20, 0x6D, 0x61, 0x67, 0x67, 0x69, 0x6F, 0x20, 0x32, 0x30, 0x30, 0x31, },
        { (byte) 0xC7, (byte) 0xD1, (byte) 0xB9, (byte) 0xBE, (byte) 0xC4, (byte) 0xCB, (byte) 0xD1, (byte) 0xCA, (byte) 0xBA, (byte) 0xB4, (byte) 0xD5, (byte) 0xB7, (byte) 0xD5, (byte) 0xE8, 0x20, 0x31, 0x30, 0x20, (byte) 0xBE, (byte) 0xC4, (byte) 0xC9, (byte) 0xC0, (byte) 0xD2, (byte) 0xA4, (byte) 0xC1, 0x20, (byte) 0xBE, 0x2E, (byte) 0xC8, 0x2E, 0x20, 0x32, 0x35, 0x34, 0x34, },
        { 0x31, 0x30, 0x20, (byte) 0xE5, (byte) 0xC7, (byte) 0xEA, (byte) 0xE8, 0x2C, 0x20, 0x32, 0x30, 0x30, 0x31, },
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xC4, (byte) 0xEA, 0x35, (byte) 0xD4, (byte) 0xC2, 0x31, 0x30, (byte) 0xC8, (byte) 0xD5, 0x20, (byte) 0xD0, (byte) 0xC7, (byte) 0xC6, (byte) 0xDA, (byte) 0xCB, (byte) 0xC4},
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xE5, (byte) 0xB9, (byte) 0xB4, 0x35, (byte) 0xE6, (byte) 0x9C, (byte) 0x88, 0x31, 0x30, (byte) 0xE6, (byte) 0x97, (byte) 0xA5, 0x20, (byte) 0xE6, (byte)0x98, (byte) 0x9F, (byte) 0xE6, (byte) 0x9C, (byte) 0x9F, (byte) 0xE5, (byte) 0x9B, (byte) 0x9B},
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xC4, (byte) 0xEA, 0x35, (byte) 0xD4, (byte) 0xC2, 0x31, 0x30, (byte) 0xC8, (byte) 0xD5, 0x20, (byte) 0xD0, (byte) 0xC7, (byte) 0xC6, (byte) 0xDA, (byte) 0xCB, (byte) 0xC4},
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xC8, (byte) 0xA1, 0x35, (byte) 0xC5, (byte) 0xCC, 0x31, 0x30, (byte) 0xC5, (byte) 0xCA, 0x20, (byte) 0xD1, (byte) 0xD3, (byte) 0xDF, (byte) 0xE6, (byte) 0xC6, (byte) 0xBE},
        { (byte) 0xE9, (byte) 0xE5, (byte) 0xED, 0x20, (byte) 0xE7, (byte) 0xEE, (byte) 0xE9, (byte) 0xF9, (byte) 0xE9, 0x20, 0x31, 0x30, 0x20, (byte) 0xEE, (byte) 0xE0, (byte) 0xE9, 0x20, 0x32, 0x30, 0x30, 0x31, },
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xC7, (byte) 0xAF, 0x35, (byte) 0xB7, (byte) 0xEE, 0x31, 0x30, (byte) 0xC6, (byte) 0xFC, },
        { 0x32, 0x30, 0x30, 0x31, (byte) 0x94, 0x4E, 0x35, (byte) 0x8C, (byte) 0x8E, 0x31, 0x30, (byte) 0x93, (byte) 0xFA, },
        { 0x32, 0x30, 0x30, 0x31, (byte) 0xB3, (byte) 0xE2, 0x20, 0x35, (byte) 0xBF, (byte) 0xF9, 0x20, 0x31, 0x30, (byte) 0xC0, (byte) 0xCF, 0x20, (byte) 0xB8, (byte) 0xF1, (byte) 0xBF, (byte) 0xE4, (byte) 0xC0, (byte) 0xCF, },
        { 0x67, 0x69, 0x6F, 0x76, 0x65, 0x64, (byte) 0xEC, 0x20, 0x31, 0x30, 0x20, 0x6D, 0x61, 0x67, 0x67, 0x69, 0x6F, 0x20, 0x32, 0x30, 0x30, 0x31, },
        { 0x67, 0x69, 0x6F, 0x76, 0x65, 0x64, (byte) 0xEC, 0x20, 0x31, 0x30, 0x20, 0x6D, 0x61, 0x67, 0x67, 0x69, 0x6F, 0x20, 0x32, 0x30, 0x30, 0x31, },
        { (byte) 0xC7, (byte) 0xD1, (byte) 0xB9, (byte) 0xBE, (byte) 0xC4, (byte) 0xCB, (byte) 0xD1, (byte) 0xCA, (byte) 0xBA, (byte) 0xB4, (byte) 0xD5, (byte) 0xB7, (byte) 0xD5, (byte) 0xE8, 0x20, 0x31, 0x30, 0x20, (byte) 0xBE, (byte) 0xC4, (byte) 0xC9, (byte) 0xC0, (byte) 0xD2, (byte) 0xA4, (byte) 0xC1, 0x20, (byte) 0xBE, 0x2E, (byte) 0xC8, 0x2E, 0x20, 0x32, 0x35, 0x34, 0x34, },
    };


    private static boolean testRequiredEncodings() {
        boolean pass = true;

        for (int i = 0; i < requiredEncodings.length; i++) {
            String encoding = requiredEncodings[i];
            Locale sampleLocale = sampleLocales[i];
            try {
                int index = 0;
                while (!sampleLocale.equals(requiredLocales[index])) {
                    index++;
                }
                byte[] out = requiredLocaleDates[index].getBytes(encoding);
                byte[] expected = expectedBytes[i];
                if (out.length != expected.length) {
                    reportConversionError(encoding, expected, out);
                    pass = false;
                } else {
                    for (int j = 0; j < out.length; j++) {
                        if (out[j] != expected[j]) {
                            reportConversionError(encoding, expected, out);
                            pass = false;
                            break;
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                System.out.println("Encoding not available: " + encoding);
                pass = false;
            }
        }
        return pass;
    }

    private static void reportConversionError(String encoding,
            byte[] expected, byte[] actual) {

        System.out.println("Incorrect conversion for encoding: " + encoding);
        System.out.println("Expected output:");
        dumpBytes(expected);
        System.out.println("Actual output:");
        dumpBytes(actual);
    }

    private static void dumpBytes(byte[] bytes) {
        System.out.print("        { ");
        for (int i = 0; i < bytes.length; i++) {
             byte b = bytes[i];
             if (b < 0) {
                 System.out.print("(byte) ");
             }
             System.out.print("0x" + toHex((b & 0x00F0) >> 4)
                     + toHex((b & 0x000F)) + ", ");
        }
        System.out.println("},");
    }

    private static char toHex(int i) {
        if (i <= 9) {
            return (char) ('0' + i);
        } else {
            return (char) ('A' + i - 10);
        }
    }
}
