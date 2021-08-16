/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test Comparison of New Collators against Old Collators in the en_US locale
 * @modules jdk.localedata
 */

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Locale;
import java.text.BreakIterator;
import java.lang.Math;

public class NewVSOld_th_TH {
    public static void main(String args[]) throws FileNotFoundException,
                                                  UnsupportedEncodingException,
                                                  IOException {
        final String ENCODING = "UTF-8";
        final Locale THAI_LOCALE = new Locale("th", "TH");

        String rawFileName = "test_th_TH.txt";
        String oldFileName = "broken_th_TH.txt";
        StringBuilder rawText = new StringBuilder();
        StringBuilder oldText = new StringBuilder();
        StringBuilder cookedText = new StringBuilder();

        File f;
        f = new File(System.getProperty("test.src", "."), rawFileName);

        try (InputStreamReader rawReader =
                 new InputStreamReader(new FileInputStream(f), ENCODING)) {
            int c;
            while ((c = rawReader.read()) != -1) {
                rawText.append((char) c);
            }
        }

        f = new File(System.getProperty("test.src", "."), oldFileName);
        try (InputStreamReader oldReader =
                 new InputStreamReader(new FileInputStream(f), ENCODING)) {
            int c;
            while ((c = oldReader.read()) != -1) {
                oldText.append((char) c);
            }
        }

        BreakIterator breakIterator = BreakIterator.getWordInstance(THAI_LOCALE);
        breakIterator.setText(rawText.toString());

        int start = breakIterator.first();
        for (int end = breakIterator.next();
             end != BreakIterator.DONE;
             start = end, end = breakIterator.next()) {
             cookedText.append(rawText.substring(start, end));
             cookedText.append("\n");
        }

        String cooked = cookedText.toString();
        String old = oldText.toString();
        if (cooked.compareTo(old) != 0) {
            throw new RuntimeException("Text not broken the same as with the old BreakIterators");
        }
    }
}
