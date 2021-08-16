/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6623219
 * @summary Test canDisplayUpTo with supplementary characters.
 */

import java.awt.*;
import java.text.*;

public class SupplementaryCanDisplayUpToTest {
    // Lists consisting of a font name, test text, and its expected
    // return value. Test text uses private area code point U+F0000
    // (\udb80\udc00).
    private static String[][] DATA = {
        // Windows
        { "Meiryo", "\ud87e\udd45\ud87e\udd47\udb80\udc00", "4" },
        { "Meiryo", "\ud87e\udd45\ud87e\udd47\udb80Z", "4" },
        { "Meiryo", "\ud87e\udd45\ud87e\udd47\udb80", "4" },
        { "Meiryo", "\ud87e\udd45\ud87e\udd47\udc00", "4" },
        { "Meiryo", "\ud87e\udd45\ud87e\udd47", "-1" },

        // Linux
        { "AR PL UMing TW", "\ud87e\udc25\ud87e\udc3b\udb80\udc00", "4" },
        { "AR PL UMing TW", "\ud87e\udc25\ud87e\udc3b\udb80Z", "4" },
        { "AR PL UMing TW", "\ud87e\udc25\ud87e\udc3b\udb80", "4" },
        { "AR PL UMing TW", "\ud87e\udc25\ud87e\udc3b\udc00", "4" },
        { "AR PL UMing TW", "\ud87e\udc25\ud87e\udc3b", "-1" },

        // Solaris
        { "FZMingTi", "\ud87e\udc25\ud87e\udc3b\udb80\udc00", "4" },
        { "FZMingTi", "\ud87e\udc25\ud87e\udc3b\udb80Z", "4" },
        { "FZMingTi", "\ud87e\udc25\ud87e\udc3b\udb80", "4" },
        { "FZMingTi", "\ud87e\udc25\ud87e\udc3b\udc00", "4" },
        { "FZMingTi", "\ud87e\udc25\ud87e\udc3b", "-1" },
    };
    private static int errorcount = 0;

    public static void main(String[] args) {
        for (String[] data : DATA) {
            String fontname = data[0];
            Font font = new Font(fontname, Font.PLAIN, 16);
            if (font.getFamily().equals(Font.DIALOG)) {
                // Skip any unavailable fonts.
                continue;
            }

            System.out.printf("Testing with font '%s'... ", fontname);
            int errors = 0;
            String text = data[1];
            int expected = Integer.parseInt(data[2]);

            int result = font.canDisplayUpTo(text);
            if (result != expected) {
                System.err.println("canDisplayUpTo(String) returns " + result);
                errors++;
            }

            result = font.canDisplayUpTo(text.toCharArray(), 0, text.length());
            if (result != expected) {
                System.err.println("canDisplayUpTo(char[], int, int) returns " + result);
                errors++;
            }

            CharacterIterator iter = new StringCharacterIterator(text);
            result = font.canDisplayUpTo(iter, iter.getBeginIndex(), iter.getEndIndex());
            if (result != expected) {
                System.err.println("canDisplayUpTo(CharacterIterator, int, int) returns " + result);
                errors++;
            }

            if (errors == 0) {
                System.out.println("passed");
            } else {
                System.out.println("failed");
                errorcount += errors;
            }
        }
        if (errorcount > 0) {
            throw new RuntimeException("SupplementaryCanDisplayUpToTest: failed");
        }
    }
}
