/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary StringConcatFactory MH_INLINE_SIZED_EXACT strategy does not work with -XX:-CompactStrings
 * @bug 8148869
 *
 * @compile -XDstringConcat=indy CompactStringsInitialCoder.java
 * @run main/othervm -Xverify:all -XX:+CompactStrings CompactStringsInitialCoder
 *
 * @compile -XDstringConcat=indyWithConstants CompactStringsInitialCoder.java
 * @run main/othervm -Xverify:all -XX:+CompactStrings CompactStringsInitialCoder
 *
 * @compile -XDstringConcat=indy CompactStringsInitialCoder.java
 * @run main/othervm -Xverify:all -XX:-CompactStrings CompactStringsInitialCoder
 *
 * @compile -XDstringConcat=indyWithConstants CompactStringsInitialCoder.java
 * @run main/othervm -Xverify:all -XX:-CompactStrings CompactStringsInitialCoder
*/
import java.lang.StringBuilder;

public class CompactStringsInitialCoder {

    static String strEmpty   = "";
    static String strLatin1  = "\u0042";
    static String strUTF16   = "\u4242";
    static char   charLatin1 = '\u0042';
    static char   charUTF16  = '\u4242';

    public static void main(String[] args) throws Exception {
        test("\u0042", "" + '\u0042');
        test("\u4242", "" + '\u4242');

        test("\u0042", "" + charLatin1);
        test("\u4242", "" + charUTF16);

        test("\u0042", strEmpty + '\u0042');
        test("\u4242", strEmpty + '\u4242');

        test("\u0042\u0042", strLatin1 + '\u0042');
        test("\u0042\u4242", strLatin1 + '\u4242');
        test("\u4242\u0042", strUTF16  + '\u0042');
        test("\u4242\u4242", strUTF16  + '\u4242');

        test("\u0042\u0042", "\u0042" + charLatin1);
        test("\u0042\u4242", "\u0042" + charUTF16);
        test("\u4242\u0042", "\u4242" + charLatin1);
        test("\u4242\u4242", "\u4242" + charUTF16);

        test("\u0042\u0042", "" + charLatin1 + charLatin1);
        test("\u0042\u4242", "" + charLatin1 + charUTF16);
        test("\u4242\u0042", "" + charUTF16  + charLatin1);
        test("\u4242\u4242", "" + charUTF16  + charUTF16);
    }

    public static void test(String expected, String actual) {
       if (!expected.equals(actual)) {
           StringBuilder sb = new StringBuilder();
           sb.append("Expected = ");
           sb.append(expected);
           sb.append(", actual = ");
           sb.append(actual);
           throw new IllegalStateException(sb.toString());
       }
    }


}
