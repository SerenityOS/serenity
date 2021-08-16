/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4397357 6565620 6959267 7070436 7198195 8032446 8072600 8221431
 * @summary Confirm normal case mappings are handled correctly.
 * @library /lib/testlibrary/java/lang
 * @run main/timeout=200 UnicodeCasingTest
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnicodeCasingTest {

    private static boolean err = false;

    // Locales which are used for testing
    private static List<Locale> locales =  new ArrayList<>();
    static {
        locales.add(new Locale("az", ""));
        locales.addAll(java.util.Arrays.asList(Locale.getAvailableLocales()));
    }

    // Default locale
    private static String defaultLang;

    // List for Unicode characters whose mappings are included in
    // SpecialCasing.txt and mappings in UnicodeData.txt isn't applicable.
    private static Map<String, String> excludeList = new HashMap<>();

    public static void main(String[] args) {
        UnicodeCasingTest specialCasingTest = new UnicodeCasingTest();
        specialCasingTest.test();
    }

    private void test() {
        Locale defaultLocale = Locale.getDefault();
        BufferedReader in = null;
        try {
            // First, we create exlude lists of characters whose mappings exist
            // in SpecialCasing.txt and mapping rules in UnicodeData.txt aren't
            // applicable.
            in = Files.newBufferedReader(UCDFiles.SPECIAL_CASING.toRealPath());
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                updateExcludeList(line);
            }
            in.close();
            in = null;
            int locale_num = locales.size();
            for (int l = 0; l < locale_num; l++) {
                Locale locale = locales.get(l);
                Locale.setDefault(locale);
                defaultLang = locale.getLanguage();
//                System.out.println("Testing on " + locale + " locale....");
                System.err.println("Testing on " + locale + " locale....");
                in = Files.newBufferedReader(UCDFiles.UNICODE_DATA.toRealPath());
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        continue;
                    }
                    test(line);
                }
                in.close();
                in = null;
            }
        }
        catch (IOException e) {
            err = true;
            e.printStackTrace();
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                }
            }

            Locale.setDefault(defaultLocale);

            if (err) {
                throw new RuntimeException("UnicodeCasingTest failed.");
            } else {
                System.out.println("*** UnicodeCasingTest passed.");
            }
        }
    }

    private void updateExcludeList(String line) {
        int index = line.indexOf('#');
        if (index != -1) {
            line = line.substring(0, index);
        }

        String lang = null;
        String condition = null;
        String[] fields = line.split("; ");

        // If the given character is mapped to multiple characters under the
        // normal condition, add it to the exclude list.
        if (fields.length == 4) {
            excludeList.put(fields[0], "all");
        } else if (fields.length == 5) {
            if (fields[4].length() == 2) { /// locale
                if (excludeList.get(fields[0]) == null) {
                    excludeList.put(fields[0], fields[4]);
                }
            }
        }
    }

    private void test(String line) {
        String[] fields = line.split(";", 15);
        String orig = convert(fields[0]);

        String lang = excludeList.get(fields[0]);
        if (!"all".equals(lang) && !defaultLang.equals(lang)) {
            if (fields[12].length() == 0) {
                testUpperCase(orig, convert(fields[0]));
            } else {
                testUpperCase(orig, convert(fields[12]));
            }

            if (fields[13].length() == 0) {
                testLowerCase(orig, convert(fields[0]));
            } else {
                testLowerCase(orig, convert(fields[13]));
            }
        }
    }

    private void testUpperCase(String orig, String expected) {
        String got = orig.toUpperCase();

        // Ugly workaround for special mappings for az and tr locales....
        if (orig.equals("\u0069") &&
            (defaultLang.equals("az") || defaultLang.equals("tr"))) {
            expected = "\u0130";
        }

        if (!expected.equals(got)) {
            err = true;
            System.err.println("toUpperCase(" +
                ") failed.\n\tOriginal: " + toString(orig) +
                "\n\tGot:      " + toString(got) +
                "\n\tExpected: " + toString(expected));
        }
    }

    private void testLowerCase(String orig, String expected) {
        String got = orig.toLowerCase();
        // Ugly workaround for special mappings for az and tr locales....
        if (orig.equals("\u0049") &&
            (defaultLang.equals("az") || defaultLang.equals("tr"))) {
            expected = "\u0131";
        }

        if (!expected.equals(got)) {
            err = true;
            System.err.println("toLowerCase(" +
                ") failed.\n\tOriginal: " + toString(orig) +
                "\n\tGot:      " + toString(got) +
                "\n\tExpected: " + toString(expected));
        }
    }

    StringBuilder sb = new StringBuilder();

    private String convert(String str) {
        sb.setLength(0);

        String[] tokens = str.split(" ");
        for (String token : tokens) {
            int j = Integer.parseInt(token, 16);
            if (j < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                sb.append((char)j);
            } else {
                sb.append(Character.toChars(j));
            }
        }

        return sb.toString();
    }

    private String toString(String str) {
        sb.setLength(0);

        int len = str.length();
        for (int i = 0; i < len; i++) {
            sb.append("0x").append(Integer.toHexString(str.charAt(i)).toUpperCase()).append(" ");
        }

        return sb.toString();
    }

}
