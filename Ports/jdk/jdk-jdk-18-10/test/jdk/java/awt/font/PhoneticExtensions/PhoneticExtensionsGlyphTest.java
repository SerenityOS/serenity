/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202696
 * @summary  Verifies if Phonetic extensions are getting displayed.
 */

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Locale;

public class PhoneticExtensionsGlyphTest {
    private static final String[] logicalFonts = {"dialog", "dialoginput", "serif", "sansserif", "monospaced"};

    private static final String phoneticExtnChars = "\u1D00 \u1D01 \u1D02 \u1D03 \u1D04 \u1D05 \u1D06 \u1D07 \u1D08 \u1D09\n"
                                                   +"\u1D0A \u1D0B \u1D0C \u1D0D \u1D0E \u1D0F \u1D10 \u1D11 \u1D12 \u1D13\n"
                                                   +"\u1D14 \u1D15 \u1D16 \u1D17 \u1D18 \u1D19 \u1D1A \u1D1B \u1D1C \u1D1D\n"
                                                   +"\u1D1E \u1D1F \u1D20 \u1D21 \u1D22 \u1D23 \u1D24 \u1D25 \u1D26 \u1D27\n"
                                                   +"\u1D28 \u1D29 \u1D2A \u1D2B \u1D2C \u1D2D \u1D2E \u1D2F \u1D30 \u1D31\n"
                                                   +"\u1D32 \u1D33 \u1D34 \u1D35 \u1D36 \u1D37 \u1D38 \u1D39 \u1D3A \u1D3B\n"
                                                   +"\u1D3C \u1D3D \u1D3E \u1D3F \u1D40 \u1D41 \u1D42 \u1D43 \u1D44 \u1D45\n"
                                                   +"\u1D46 \u1D47 \u1D48 \u1D49 \u1D4A \u1D4B \u1D4C \u1D4D \u1D4E \u1D4F\n"
                                                   +"\u1D50 \u1D51 \u1D52 \u1D53 \u1D54 \u1D55 \u1D56 \u1D57 \u1D58 \u1D59\n"
                                                   +"\u1D5A \u1D5B \u1D5C \u1D5D \u1D5E \u1D5F \u1D60 \u1D61 \u1D62 \u1D63\n"
                                                   +"\u1D64 \u1D65 \u1D66 \u1D67 \u1D68 \u1D69 \u1D6A \u1D6B \u1D6C \u1D6D\n"
                                                   +"\u1D6E \u1D6F \u1D70 \u1D71 \u1D72 \u1D73 \u1D74 \u1D75 \u1D76 \u1D77\n"
                                                   +"\u1D78 \u1D79 \u1D7A \u1D7B \u1D7C \u1D7D \u1D7E \u1D7F";

    public static void main(String[] args) throws Exception {
        if (!System.getProperty("os.name").startsWith("Win")) {
            return;
        }

        if(!canDisplayPhoneticChars()) {
            throw new RuntimeException("Phonetic extensions failed to display.");
        }
    }

    private static boolean isLogicalFont(Font f) {
        String fontName = f.getFamily().toLowerCase(Locale.ROOT);
        for (int i = 0; i < logicalFonts.length; i++) {
            if (logicalFonts[i].equals(fontName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canDisplayPhoneticChars() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = ge.getAllFonts();
        boolean ret = false;
        for (Font font : fonts) {
            if (isLogicalFont(font) && font.canDisplayUpTo(phoneticExtnChars) == -1) {
                ret = true;
                break;
            }
        }
        return ret;
    }
}
