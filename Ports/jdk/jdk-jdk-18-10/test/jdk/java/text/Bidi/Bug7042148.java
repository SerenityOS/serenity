/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7042148
 * @summary verify that Bidi.baseIsLeftToRight() returns the correct value even if an incorrect position is set in the given AttributedCharacterIterator.
 * @modules java.desktop
 */
import java.awt.font.*;
import java.text.*;
import java.util.*;

public class Bug7042148 {

    private static boolean err = false;

    public static void main(String[] args) {
        testDirection();

        if (err) {
            throw new RuntimeException("Failed");
        } else {
            System.out.println("Passed.");
        }
    }

    private static void testDirection() {
        Map attrLTR = new HashMap();
        attrLTR.put(TextAttribute.RUN_DIRECTION,
                    TextAttribute.RUN_DIRECTION_LTR);
        Map attrRTL = new HashMap();
        attrRTL.put(TextAttribute.RUN_DIRECTION,
                    TextAttribute.RUN_DIRECTION_RTL);

        String str1 = "A\u05e0";
        String str2 = "\u05e0B";

        test(str1, attrLTR, Bidi.DIRECTION_LEFT_TO_RIGHT);
        test(str1, attrRTL, Bidi.DIRECTION_RIGHT_TO_LEFT);
        test(str2, attrLTR, Bidi.DIRECTION_LEFT_TO_RIGHT);
        test(str2, attrRTL, Bidi.DIRECTION_RIGHT_TO_LEFT);
    }

    private static void test(String text, Map attr, int dirFlag) {
        boolean expected = (dirFlag == Bidi.DIRECTION_LEFT_TO_RIGHT);

        Bidi bidi = new Bidi(text, dirFlag);
        boolean got = bidi.baseIsLeftToRight();
        if (got != expected) {
            err = true;
            System.err.println("wrong Bidi(String, int).baseIsLeftToRight() value: " +
                               "\n\ttext=" + text +
                               "\n\tExpected=" + expected +
                               "\n\tGot=" + got);
        }

        AttributedString as = new AttributedString(text, attr);
        AttributedCharacterIterator itr = as.getIterator();
        itr.last();
        itr.next();
        bidi = new Bidi(itr);
        got = bidi.baseIsLeftToRight();
        if (got != expected) {
            err = true;
            System.err.println("Wrong Bidi(AttributedCharacterIterator).baseIsLeftToRight() value: " +
                               "\n\ttext=" + text +
                               "\n\tExpected=" + expected +
                               "\n\tGot=" + got);
        }
    }

}
