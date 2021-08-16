/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4922813
 * @summary Check the new impl of encodePath will not cause regression
 * @modules java.base/sun.net.www
   @key randomness
 */

import java.util.BitSet;
import java.io.File;
import java.util.Random;
import sun.net.www.ParseUtil;

public class ParseUtil_4922813 {
    public static void main(String[] argv) throws Exception {

        int num = 400;
        while (num-- >= 0) {
            String source = getTestSource();
            String ec = sun.net.www.ParseUtil.encodePath(source);
            String v117 = ParseUtil_V117.encodePath(source);
            if (!ec.equals(v117)) {
                throw new RuntimeException("Test Failed for : \n"
                                           + "   source  =<"
                                           + getUnicodeString(source)
                                           + ">");
            }

        }
    }

    static int maxCharCount = 200;
    static int maxCodePoint = 0x10ffff;
    static Random random;
    static String getTestSource() {
        if (random == null) {
            long seed = System.currentTimeMillis();
            random = new Random(seed);
        }
        String source = "";
        int i = 0;
        int count = random.nextInt(maxCharCount) + 1;
        while (i < count) {
            int codepoint = random.nextInt(127);
            source = source + String.valueOf((char)codepoint);

            codepoint = random.nextInt(0x7ff);
            source = source + String.valueOf((char)codepoint);

            codepoint = random.nextInt(maxCodePoint);
            source = source + new String(Character.toChars(codepoint));

            i += 3;
        }
        return source;
    }

    static String getUnicodeString(String s){
        String unicodeString = "";
        for(int j=0; j< s.length(); j++){
             unicodeString += "0x"+ Integer.toString(s.charAt(j), 16);
        }
        return unicodeString;
    }
}
class ParseUtil_V117 {
    static BitSet encodedInPath;
    static {
        encodedInPath = new BitSet(256);

        // Set the bits corresponding to characters that are encoded in the
        // path component of a URI.

        // These characters are reserved in the path segment as described in
        // RFC2396 section 3.3.
        encodedInPath.set('=');
        encodedInPath.set(';');
        encodedInPath.set('?');
        encodedInPath.set('/');

        // These characters are defined as excluded in RFC2396 section 2.4.3
        // and must be escaped if they occur in the data part of a URI.
        encodedInPath.set('#');
        encodedInPath.set(' ');
        encodedInPath.set('<');
        encodedInPath.set('>');
        encodedInPath.set('%');
        encodedInPath.set('"');
        encodedInPath.set('{');
        encodedInPath.set('}');
        encodedInPath.set('|');
        encodedInPath.set('\\');
        encodedInPath.set('^');
        encodedInPath.set('[');
        encodedInPath.set(']');
        encodedInPath.set('`');

        // US ASCII control characters 00-1F and 7F.
        for (int i=0; i<32; i++)
            encodedInPath.set(i);
        encodedInPath.set(127);
    }
    /**
     * Constructs an encoded version of the specified path string suitable
     * for use in the construction of a URL.
     *
     * A path separator is replaced by a forward slash. The string is UTF8
     * encoded. The % escape sequence is used for characters that are above
     * 0x7F or those defined in RFC2396 as reserved or excluded in the path
     * component of a URL.
     */
    public static String encodePath(String path) {
        StringBuffer sb = new StringBuffer();
        int n = path.length();
        for (int i=0; i<n; i++) {
            char c = path.charAt(i);
            if (c == File.separatorChar)
                sb.append('/');
            else {
                if (c <= 0x007F) {
                    if (encodedInPath.get(c))
                        escape(sb, c);
                    else
                        sb.append(c);
                } else if (c > 0x07FF) {
                    escape(sb, (char)(0xE0 | ((c >> 12) & 0x0F)));
                    escape(sb, (char)(0x80 | ((c >>  6) & 0x3F)));
                    escape(sb, (char)(0x80 | ((c >>  0) & 0x3F)));
                } else {
                    escape(sb, (char)(0xC0 | ((c >>  6) & 0x1F)));
                    escape(sb, (char)(0x80 | ((c >>  0) & 0x3F)));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Appends the URL escape sequence for the specified char to the
     * specified StringBuffer.
     */
    private static void escape(StringBuffer s, char c) {
        s.append('%');
        s.append(Character.forDigit((c >> 4) & 0xF, 16));
        s.append(Character.forDigit(c & 0xF, 16));
    }
}
