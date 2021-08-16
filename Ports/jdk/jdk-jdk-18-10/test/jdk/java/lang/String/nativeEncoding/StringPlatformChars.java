/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm/native -Xcheck:jni StringPlatformChars
 */
import java.util.Arrays;

public class StringPlatformChars {

    private static final String JNU_ENCODING = System.getProperty("sun.jnu.encoding");

    public static void main(String... args) throws Exception {
        System.out.println("sun.jnu.encoding: " + JNU_ENCODING);
        System.loadLibrary("stringPlatformChars");

        // Test varying lengths, provoking different allocation paths
        StringBuilder unicodeSb = new StringBuilder();
        StringBuilder asciiSb = new StringBuilder();
        StringBuilder latinSb = new StringBuilder();

        for (int i = 0; i < 2000; i++) {
            unicodeSb.append('\uFEFE');
            testString(unicodeSb.toString());

            asciiSb.append('x');
            testString(asciiSb.toString());

            latinSb.append('\u00FE');
            testString(latinSb.toString());

            testString(latinSb.toString() + asciiSb.toString() + unicodeSb.toString());
        }

        // Exhaustively test simple Strings made up of all possible chars:
        for (char c = '\u0001'; c < Character.MAX_VALUE; c++) {
            testString(String.valueOf(c));
        }
        // Special case: \u0000 is treated as end-of-string in the native code,
        // so strings with it should be truncated:
        if (getBytes("\u0000abcdef").length != 0 ||
            getBytes("a\u0000bcdef").length != 1) {
            System.out.println("Mismatching values for strings including \\u0000");
            throw new AssertionError();
        }
    }

    private static void testString(String s) throws Exception {
        byte[] nativeBytes = getBytes(s);
        byte[] stringBytes = s.getBytes(JNU_ENCODING);

        if (!Arrays.equals(nativeBytes, stringBytes)) {
            System.out.println("Mismatching values for: '" + s + "' " + Arrays.toString(s.chars().toArray()));
            System.out.println("Native: " + Arrays.toString(nativeBytes));
            System.out.println("String: " + Arrays.toString(stringBytes));
            throw new AssertionError(s);
        }

        String javaNewS = new String(nativeBytes, JNU_ENCODING);
        String nativeNewS = newString(nativeBytes);
        if (!javaNewS.equals(nativeNewS)) {
            System.out.println("New string via native doesn't match via java: '" + javaNewS + "' and '" + nativeNewS + "'");
            throw new AssertionError(s);
        }
    }

    static native byte[] getBytes(String string);

    static native String newString(byte[] bytes);
}
