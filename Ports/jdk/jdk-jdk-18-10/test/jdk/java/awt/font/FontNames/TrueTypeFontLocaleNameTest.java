
/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8043509
 * @summary Test locale famlly falls back to same language before English
 * @run main/othervm TrueTypeFontLocaleNameTest
 */

import java.awt.Font;
import java.util.Locale;

public class TrueTypeFontLocaleNameTest {

    public static void main(String[] args) {

        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase().startsWith("win")) {
            return;
        }
        System.setProperty("user.language", "de");
        System.setProperty("user.country", "AT");
        Locale de_atLocale = new Locale("de", "AT");
        Locale.setDefault(de_atLocale);

        String family = "Verdana";
        Font font = new Font(family, Font.BOLD, 12);
        if (!font.getFamily(Locale.ENGLISH).equals(family)) {
            System.out.println(family + " not found - skipping test.");
            return;
        }

        String atFontName = font.getFontName();
        Locale deGELocale = new Locale("de", "GE");
        String deFontName = font.getFontName(deGELocale);
        System.out.println("Austrian font name: " + atFontName);
        System.out.println("German font name: " + deFontName);

        String deLangFullName = "Verdana Fett";
        // We expect "Fett" for "Bold" when the language is German.
        // This font does have that so these should both be equal and
        // say "Verdana Fett"
        if (!deFontName.equals(atFontName)) {
            throw new RuntimeException("Font names differ " +
                                       deFontName + " " + atFontName);
        }
        if (!deLangFullName.equals(deFontName)) {
            throw new RuntimeException("Font name is not " + deLangFullName +
                                       " instead got " + deFontName);
        }
    }
}
