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
 * @bug 4899380
 * @summary Character.is<Property>(int codePoint) methods should return false
 *          for negative codepoint values. The to<CaseMap> methods should return
 *          the original codePoint for invalid codePoint ranges.
 * @author John O'Conner
 */

public class TestNegativeCodepoint {

    public static void main(String[] args) {

      int[] invalidCodePoints = { -1, -'a', 0x110000};
      for (int x = 0; x < invalidCodePoints.length; ++x) {
        int cp = invalidCodePoints[x];

        System.out.println("Testing codepoint: " + cp);
         // test all of the is<Property> methods
        if (Character.isLowerCase(cp) ||
            Character.isUpperCase(cp) ||
            Character.isTitleCase(cp) ||
            Character.isISOControl(cp) ||
            Character.isLetterOrDigit(cp) ||
            Character.isLetter(cp) ||
            Character.isDigit(cp) ||
            Character.isDefined(cp) ||
            Character.isJavaIdentifierStart(cp) ||
            Character.isJavaIdentifierPart(cp) ||
            Character.isUnicodeIdentifierStart(cp) ||
            Character.isUnicodeIdentifierPart(cp) ||
            Character.isIdentifierIgnorable(cp) ||
            Character.isSpaceChar(cp) ||
            Character.isWhitespace(cp) ||
            Character.isMirrored(cp) ||

            // test the case mappings
            Character.toLowerCase(cp) != cp ||
            Character.toUpperCase(cp) != cp ||
            Character.toTitleCase(cp) != cp ||

            // test directionality of invalid codepoints
            Character.getDirectionality(cp) != Character.DIRECTIONALITY_UNDEFINED ||

            // test type
            Character.getType(cp) != Character.UNASSIGNED ||

            // test numeric and digit  values
            Character.getNumericValue(cp) != -1 ||
            Character.digit(cp, 10) != -1 ) {

            System.out.println("Failed.");
            throw new RuntimeException();
        }

        // test block value
        Character.UnicodeBlock block = null;
        try {
            block = Character.UnicodeBlock.of(cp);
            // if we haven't already thrown an exception because of the illegal
            // arguments, then we need to throw one because of an error in the of() method
            System.out.println("Failed.");
            throw new RuntimeException();
        }
        catch(IllegalArgumentException e) {
            // Since we're testing illegal values, we
            // expect to land here every time. If not,
            // our test has failed
        }

      }
      System.out.println("Passed.");
    }
}
