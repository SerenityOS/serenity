/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202771 8221431 8229831
 * @summary Check j.l.Character.isDigit/isLetter/isLetterOrDigit/isSpaceChar
 * /isWhitespace/isTitleCase/isISOControl/isIdentifierIgnorable
 * /isJavaIdentifierStart/isJavaIdentifierPart/isUnicodeIdentifierStart
 * /isUnicodeIdentifierPart
 * @library /lib/testlibrary/java/lang
 * @run main CharPropTest
 */

import java.nio.file.Files;
import java.util.stream.Stream;

public class CharPropTest {
    private static int diffs = 0;
    private static int rangeStart = 0x0000;
    private static boolean isRange = false;

    public static void main(String[] args) throws Exception {
        try (Stream<String> lines = Files.lines(UCDFiles.UNICODE_DATA)) {
            lines.map(String::trim)
                 .filter(line -> line.length() != 0 && line.charAt(0) != '#')
                 .forEach(line -> handleOneLine(line));

            if (diffs != 0) {
                throw new RuntimeException("Total differences: " + diffs);
            }
        }
    }

    private static void handleOneLine(String line) {
        String[] fields = line.split(";");
        int currentCp = Integer.parseInt(fields[0], 16);
        String name = fields[1];
        String category = fields[2];

        // Except single code point, also handle ranges like the following:
        // 3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
        // 4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;
        if (isRange) {
            if (name.endsWith("Last>")) {
                for (int cp = rangeStart; cp <= currentCp; cp++) {
                    testCodePoint(cp, category);
                }
            } else {
                throw new RuntimeException("Not a valid range, first range <"
                        + Integer.toHexString(rangeStart) + "> without last.");
            }
            isRange = false;
        } else {
            if (name.endsWith("First>")) {
                rangeStart = currentCp;
                isRange = true;
            } else {
                testCodePoint(currentCp, category);
            }
        }
    }

    private static void testCodePoint(int codePoint, String category) {
        isDigitTest(codePoint, category);
        isLetterTest(codePoint, category);
        isLetterOrDigitTest(codePoint, category);

        isSpaceCharTest(codePoint, category);
        isWhitespaceTest(codePoint, category);

        isTitleCaseTest(codePoint, category);

        isISOControlTest(codePoint);

        isIdentifierIgnorableTest(codePoint, category);
        isJavaIdentifierStartTest(codePoint, category);
        isJavaIdentifierPartTest(codePoint, category);
        isUnicodeIdentifierStartTest(codePoint, category);
        isUnicodeIdentifierPartTest(codePoint, category);
    }

    private static void isDigitTest(int codePoint, String category) {
        boolean actual = Character.isDigit(codePoint);
        boolean expected = category.equals("Nd");
        if (actual != expected) {
            printDiff(codePoint, "isDigit", actual, expected);
        }
    }

    private static void isLetterTest(int codePoint, String category) {
        boolean actual = Character.isLetter(codePoint);
        boolean expected = isLetter(category);
        if (actual != expected) {
            printDiff(codePoint, "isLetter", actual, expected);
        }
    }

    private static void isLetterOrDigitTest(int codePoint, String category) {
        boolean actual = Character.isLetterOrDigit(codePoint);
        boolean expected = isLetter(category) || category.equals("Nd");
        if (actual != expected) {
            printDiff(codePoint, "isLetterOrDigit", actual, expected);
        }
    }

    private static void isSpaceCharTest(int codePoint, String category) {
        boolean actual = Character.isSpaceChar(codePoint);
        boolean expected = isSpaceChar(category);
        if (actual != expected) {
            printDiff(codePoint, "isSpaceChar", actual, expected);
        }
    }

    private static void isWhitespaceTest(int codePoint, String category) {
        boolean actual = Character.isWhitespace(codePoint);
        boolean expected = isWhitespace(codePoint, category);
        if (actual != expected) {
            printDiff(codePoint, "isWhitespace", actual, expected);
        }
    }

    private static void isTitleCaseTest(int codePoint, String category) {
        boolean actual = Character.isTitleCase(codePoint);
        boolean expected = category.equals("Lt");
        if (actual != expected) {
            printDiff(codePoint, "isTitleCase", actual, expected);
        }
    }

    private static void isISOControlTest(int codePoint) {
        boolean actual = Character.isISOControl(codePoint);
        boolean expected = isISOControl(codePoint);
        if (actual != expected) {
            printDiff(codePoint, "isISOControl", actual, expected);
        }
    }

    private static void isIdentifierIgnorableTest(int codePoint, String category) {
        boolean actual = Character.isIdentifierIgnorable(codePoint);
        boolean expected = isIdentifierIgnorable(codePoint, category);
        if (actual != expected) {
            printDiff(codePoint, "isIdentifierIgnorable", actual, expected);
        }
    }

    private static void isJavaIdentifierStartTest(int codePoint, String category) {
        boolean actual = Character.isJavaIdentifierStart(codePoint);
        boolean expected = isJavaIdentifierStart(category);
        if (actual != expected) {
            printDiff(codePoint, "isJavaIdentifierStart", actual, expected);
        }
    }

    private static void isJavaIdentifierPartTest(int codePoint, String category) {
        boolean actual = Character.isJavaIdentifierPart(codePoint);
        boolean expected = isJavaIdentifierPart(codePoint, category);
        if (actual != expected) {
            printDiff(codePoint, "isJavaIdentifierPart", actual, expected);
        }
    }

    private static void isUnicodeIdentifierStartTest(int codePoint, String category) {
        boolean actual = Character.isUnicodeIdentifierStart(codePoint);
        boolean expected = isUnicodeIdentifierStart(codePoint, category);
        if (actual != expected) {
            printDiff(codePoint, "isUnicodeIdentifierStart", actual, expected);
        }
    }

    private static void isUnicodeIdentifierPartTest(int codePoint, String category) {
        boolean actual = Character.isUnicodeIdentifierPart(codePoint);
        boolean expected = isUnicodeIdentifierPart(codePoint, category);
        if (actual != expected) {
            printDiff(codePoint, "isUnicodeIdentifierPart", actual, expected);
        }
    }

    private static boolean isLetter(String category) {
        return category.equals("Lu") || category.equals("Ll")
               || category.equals("Lt") || category.equals("Lm")
               || category.equals("Lo");
    }

    private static boolean isSpaceChar(String category) {
        return category.equals("Zs") || category.equals("Zl")
               || category.equals("Zp");
    }

    private static boolean isWhitespace(int codePoint, String category) {
        if (isSpaceChar(category) && codePoint != Integer.parseInt("00A0", 16)
                && codePoint != Integer.parseInt("2007", 16)
                && codePoint != Integer.parseInt("202F", 16)) {
            return true;
        } else {
            if (codePoint == Integer.parseInt("0009", 16)
                    || codePoint == Integer.parseInt("000A", 16)
                    || codePoint == Integer.parseInt("000B", 16)
                    || codePoint == Integer.parseInt("000C", 16)
                    || codePoint == Integer.parseInt("000D", 16)
                    || codePoint == Integer.parseInt("001C", 16)
                    || codePoint == Integer.parseInt("001D", 16)
                    || codePoint == Integer.parseInt("001E", 16)
                    || codePoint == Integer.parseInt("001F", 16)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isISOControl(int codePoint) {
        return (codePoint > 0x00 && codePoint < 0x1f)
               || (codePoint > 0x7f && codePoint < 0x9f)
               || (codePoint == 0x00 || codePoint == 0x1f || codePoint == 0x7f || codePoint == 0x9f);
    }

    private static boolean isIdentifierIgnorable(int codePoint, String category) {
        if (category.equals("Cf")) {
            return true;
        } else {
            int a1 = Integer.parseInt("0000", 16);
            int a2 = Integer.parseInt("0008", 16);
            int b1 = Integer.parseInt("000E", 16);
            int b2 = Integer.parseInt("001B", 16);
            int c1 = Integer.parseInt("007F", 16);
            int c2 = Integer.parseInt("009F", 16);

            if ((codePoint > a1 && codePoint < a2) || (codePoint > b1 && codePoint < b2)
                    || (codePoint > c1 && codePoint < c2) || (codePoint == a1 || codePoint == a2
                    || codePoint == b1 || codePoint == b2 || codePoint == c1 || codePoint == c2)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJavaIdentifierStart(String category) {
        return isLetter(category) || category.equals("Nl") || category.equals("Sc")
               || category.equals("Pc");
    }

    private static boolean isJavaIdentifierPart(int codePoint, String category) {
        return isLetter(category) || category.equals("Sc") || category.equals("Pc")
               || category.equals("Nd") || category.equals("Nl")
               || category.equals("Mc") || category.equals("Mn")
               || isIdentifierIgnorable(codePoint, category);
    }

    private static boolean isUnicodeIdentifierStart(int codePoint, String category) {
        return isLetter(category) || category.equals("Nl")
               || isOtherIDStart(codePoint);
    }

    private static boolean isUnicodeIdentifierPart(int codePoint, String category) {
        return isLetter(category) || category.equals("Pc") || category.equals("Nd")
               || category.equals("Nl") || category.equals("Mc") || category.equals("Mn")
               || isIdentifierIgnorable(codePoint, category)
               || isOtherIDStart(codePoint)
               || isOtherIDContinue(codePoint);
    }

    private static boolean isOtherIDStart(int codePoint) {
        return codePoint == 0x1885 ||
               codePoint == 0x1886 ||
               codePoint == 0x2118 ||
               codePoint == 0x212E ||
               codePoint == 0x309B ||
               codePoint == 0x309C;
    }

    private static boolean isOtherIDContinue(int codePoint) {
        return codePoint == 0x00B7 ||
               codePoint == 0x0387 ||
              (codePoint >= 0x1369 && codePoint <= 0x1371) ||
               codePoint == 0x19DA;
    }

    private static void printDiff(int codePoint, String method, boolean actual, boolean expected) {
        System.out.println("Not equal at codePoint <" + Integer.toHexString(codePoint)
                + ">, method: " + method
                + ", actual: " + actual + ", expected: " + expected);
        diffs++;
    }
}
