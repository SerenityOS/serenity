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
 * @bug 4397357 6565620 6959267 8032446 8072600 8221431
 * @summary Confirm normal case mappings are handled correctly.
 * @library /lib/testlibrary/java/lang
 * @run main/timeout=200 UnicodeCasingTest
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UnicodeCasingTest {

    private static boolean err = false;

    // Locales which are used for testing
    private static List<Locale> locales = new ArrayList<>();
    static {
        locales.add(new Locale("az", ""));
        locales.addAll(java.util.Arrays.asList(Locale.getAvailableLocales()));
    }


    public static void main(String[] args) {
        UnicodeCasingTest specialCasingTest = new UnicodeCasingTest();
        specialCasingTest.test();
    }

    private void test() {
        Locale defaultLocale = Locale.getDefault();

        BufferedReader in = null;

        try {
            File file = UCDFiles.UNICODE_DATA.toFile();

            int locale_num = locales.size();
            for (int l = 0; l < locale_num; l++) {
                Locale locale = locales.get(l);
                Locale.setDefault(locale);
                System.out.println("Testing on " + locale + " locale....");

                in = new BufferedReader(new FileReader(file));

                String line;
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
        catch (Exception e) {
            err = true;
            e.printStackTrace();
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (Exception e) {
                }
            }

            Locale.setDefault(defaultLocale);

            if (err) {
                throw new RuntimeException("UnicodeCasingTest failed.");
            } else {
                System.out.println("UnicodeCasingTest passed.");
            }
        }
    }

    private void test(String line) {
        String[] fields = line.split(";", 15);
        int orig = convert(fields[0]);

        if (fields[12].length() != 0) {
            testUpperCase(orig, convert(fields[12]));
        } else {
            testUpperCase(orig, orig);
        }

        if (fields[13].length() != 0) {
            testLowerCase(orig, convert(fields[13]));
        } else {
            testLowerCase(orig, orig);
        }

        if (fields[14].length() != 0) {
            testTitleCase(orig, convert(fields[14]));
        } else {
            testTitleCase(orig, orig);
        }
    }

    private void testUpperCase(int orig, int expected) {
        int got = Character.toUpperCase(orig);

        if (expected != got) {
            err = true;
            System.err.println("toUpperCase(" +
                ") failed.\n\tOriginal: " + toString(orig) +
                "\n\tGot:      " + toString(got) +
                "\n\tExpected: " + toString(expected));
        }
    }

    private void testLowerCase(int orig, int expected) {
        int got = Character.toLowerCase(orig);

        if (expected != got) {
            err = true;
            System.err.println("toLowerCase(" +
                ") failed.\n\tOriginal: " + toString(orig) +
                "\n\tGot:      " + toString(got) +
                "\n\tExpected: " + toString(expected));
        }
    }

    private void testTitleCase(int orig, int expected) {
        int got = Character.toTitleCase(orig);

        if (expected != got) {
            err = true;
            System.err.println("toTitleCase(" +
                ") failed.\n\tOriginal: " + toString(orig) +
                "\n\tGot:      " + toString(got) +
                "\n\tExpected: " + toString(expected));
        }
    }

    private int convert(String str) {
        return Integer.parseInt(str, 16);
    }

    private String toString(int i) {
        return Integer.toHexString(i).toUpperCase();
    }

}
