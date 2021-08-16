/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeClass;

/*
 * Base class of tests for Compact String.
 *
 */
public class CompactString {

    final Map<String, Map<String, String>> map = new HashMap<>();

    enum StringSources {
        EMPTY(STRING_EMPTY, BYTE_ARRAY_EMTPY, CHAR_ARRAY_EMPTY,
                POINT_ARRAY_EMTPY), LDUPLICATE(STRING_LDUPLICATE,
                BYTE_ARRAY_LDUPLICATE, CHAR_ARRAY_LDUPLICATE,
                POINT_ARRAY_LDUPLICATE), LLONG(STRING_LLONG, BYTE_ARRAY_LLONG,
                CHAR_ARRAY_LLONG, POINT_ARRAY_LLONG), L1(STRING_L1,
                BYTE_ARRAY_L1, CHAR_ARRAY_L1, POINT_ARRAY_L1), L2(STRING_L2,
                BYTE_ARRAY_L2, CHAR_ARRAY_L2, POINT_ARRAY_L2), L4(STRING_L4,
                BYTE_ARRAY_L4, CHAR_ARRAY_L4, POINT_ARRAY_L4), UDUPLICATE(
                STRING_UDUPLICATE, BYTE_ARRAY_UDUPLICATE,
                CHAR_ARRAY_UDUPLICATE, POINT_ARRAY_UDUPLICATE), U1(STRING_U1,
                BYTE_ARRAY_U1, CHAR_ARRAY_U1, POINT_ARRAY_U1), U2(STRING_U2,
                BYTE_ARRAY_U2, CHAR_ARRAY_U2, POINT_ARRAY_U2), MDUPLICATE1(
                STRING_MDUPLICATE1, BYTE_ARRAY_MDUPLICATE1,
                CHAR_ARRAY_MDUPLICATE1, POINT_ARRAY_MDUPLICATE1), MDUPLICATE2(
                STRING_MDUPLICATE2, BYTE_ARRAY_MDUPLICATE2,
                CHAR_ARRAY_MDUPLICATE2, POINT_ARRAY_MDUPLICATE2), MLONG1(
                STRING_MLONG1, BYTE_ARRAY_MLONG1, CHAR_ARRAY_MLONG1,
                POINT_ARRAY_MLONG1), MLONG2(STRING_MLONG2, BYTE_ARRAY_MLONG2,
                CHAR_ARRAY_MLONG2, POINT_ARRAY_MLONG2), M11(STRING_M11,
                BYTE_ARRAY_M11, CHAR_ARRAY_M11, POINT_ARRAY_M11), M12(
                STRING_M12, BYTE_ARRAY_M12, CHAR_ARRAY_M12, POINT_ARRAY_M12), SUPPLEMENTARY(
                STRING_SUPPLEMENTARY, BYTE_ARRAY_SUPPLEMENTARY,
                CHAR_ARRAY_SUPPLEMENTARY, POINT_ARRAY_SUPPLEMENTARY), SUPPLEMENTARY_LOWERCASE(
                STRING_SUPPLEMENTARY_LOWERCASE,
                BYTE_ARRAY_SUPPLEMENTARY_LOWERCASE,
                CHAR_ARRAY_SUPPLEMENTARY_LOWERCASE,
                POINT_ARRAY_SUPPLEMENTARY_LOWERCASE);

        private StringSources(String s, byte[] b, char[] c, int[] i) {
            str = s;
            ba = b;
            ca = c;
            ia = i;
        }

        String getString() {
            return str;
        }

        byte[] getByteArray() {
            return ba;
        }

        char[] getCharArray() {
            return ca;
        }

        int[] getIntArray() {
            return ia;
        }

        private final String str;
        private final byte[] ba;
        private final char[] ca;
        private final int[] ia;
    }

    protected static final String DEFAULT_CHARSET_NAME = "UTF-8";
    protected static final Charset DEFAULT_CHARSET = Charset
            .forName(DEFAULT_CHARSET_NAME);

    protected static final String STRING_EMPTY = "";
    protected static final byte[] BYTE_ARRAY_EMTPY = new byte[0];
    protected static final char[] CHAR_ARRAY_EMPTY = new char[0];
    protected static final int[] POINT_ARRAY_EMTPY = new int[0];

    protected static final String STRING_LDUPLICATE = "ABABABABAB";
    protected static final byte[] BYTE_ARRAY_LDUPLICATE = new byte[] { 'A', 'B',
            'A', 'B', 'A', 'B', 'A', 'B', 'A', 'B' };
    protected static final char[] CHAR_ARRAY_LDUPLICATE = new char[] { 'A', 'B',
            'A', 'B', 'A', 'B', 'A', 'B', 'A', 'B' };
    protected static final int[] POINT_ARRAY_LDUPLICATE = new int[] { 'A', 'B',
            'A', 'B', 'A', 'B', 'A', 'B', 'A', 'B' };

    protected static final String STRING_LLONG = "ABCDEFGH";
    protected static final byte[] BYTE_ARRAY_LLONG = new byte[] { 'A', 'B', 'C',
            'D', 'E', 'F', 'G', 'H' };
    protected static final char[] CHAR_ARRAY_LLONG = new char[] { 'A', 'B', 'C',
            'D', 'E', 'F', 'G', 'H' };
    protected static final int[] POINT_ARRAY_LLONG = new int[] { 'A', 'B', 'C',
            'D', 'E', 'F', 'G', 'H' };

    protected static final String STRING_L1 = "A";
    protected static final byte[] BYTE_ARRAY_L1 = new byte[] { 'A' };
    protected static final char[] CHAR_ARRAY_L1 = new char[] { 'A' };
    protected static final int[] POINT_ARRAY_L1 = new int[] { 'A' };

    protected static final String STRING_L2 = "AB";
    protected static final byte[] BYTE_ARRAY_L2 = new byte[] { 'A', 'B' };
    protected static final char[] CHAR_ARRAY_L2 = new char[] { 'A', 'B' };
    protected static final int[] POINT_ARRAY_L2 = new int[] { 'A', 'B' };

    protected static final String STRING_L4 = "ABCD";
    protected static final byte[] BYTE_ARRAY_L4 = new byte[] { 'A', 'B', 'C', 'D' };
    protected static final char[] CHAR_ARRAY_L4 = new char[] { 'A', 'B', 'C', 'D' };
    protected static final int[] POINT_ARRAY_L4 = new int[] { 'A', 'B', 'C', 'D' };

    /*
     * Because right now ASCII is the default encoding parameter for source code
     * in JDK build environment, so we escape them. same as below.
     */
    protected static final String STRING_UDUPLICATE = "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22";
    protected static final byte[] BYTE_ARRAY_UDUPLICATE = getBytes(STRING_UDUPLICATE);
    protected static final char[] CHAR_ARRAY_UDUPLICATE = new char[] { '\uFF21',
            '\uFF22', '\uFF21', '\uFF22', '\uFF21', '\uFF22', '\uFF21',
            '\uFF22', '\uFF21', '\uFF22' };
    protected static final int[] POINT_ARRAY_UDUPLICATE = new int[] { '\uFF21',
            '\uFF22', '\uFF21', '\uFF22', '\uFF21', '\uFF22', '\uFF21',
            '\uFF22', '\uFF21', '\uFF22' };

    protected static final String STRING_U1 = "\uFF21";
    protected static final byte[] BYTE_ARRAY_U1 = getBytes(STRING_U1);
    protected static final char[] CHAR_ARRAY_U1 = new char[] { '\uFF21' };
    protected static final int[] POINT_ARRAY_U1 = new int[] { '\uFF21' };

    protected static final String STRING_U2 = "\uFF21\uFF22";
    protected static final byte[] BYTE_ARRAY_U2 = getBytes(STRING_U2);
    protected static final char[] CHAR_ARRAY_U2 = new char[] { '\uFF21', '\uFF22' };
    protected static final int[] POINT_ARRAY_U2 = new int[] { '\uFF21', '\uFF22' };

    protected static final String STRING_MDUPLICATE1 = "\uFF21A\uFF21A\uFF21A\uFF21A\uFF21A";
    protected static final byte[] BYTE_ARRAY_MDUPLICATE1 = getBytes(STRING_MDUPLICATE1);
    protected static final char[] CHAR_ARRAY_MDUPLICATE1 = new char[] { '\uFF21',
            'A', '\uFF21', 'A', '\uFF21', 'A', '\uFF21', 'A', '\uFF21', 'A' };
    protected static final int[] POINT_ARRAY_MDUPLICATE1 = new int[] { '\uFF21',
            'A', '\uFF21', 'A', '\uFF21', 'A', '\uFF21', 'A', '\uFF21', 'A' };

    protected static final String STRING_MDUPLICATE2 = "A\uFF21A\uFF21A\uFF21A\uFF21A\uFF21";
    protected static final byte[] BYTE_ARRAY_MDUPLICATE2 = getBytes(STRING_MDUPLICATE2);
    protected static final char[] CHAR_ARRAY_MDUPLICATE2 = new char[] { 'A',
            '\uFF21', 'A', '\uFF21', 'A', '\uFF21', 'A', '\uFF21', 'A',
            '\uFF21' };
    protected static final int[] POINT_ARRAY_MDUPLICATE2 = new int[] { 'A',
            '\uFF21', 'A', '\uFF21', 'A', '\uFF21', 'A', '\uFF21', 'A',
            '\uFF21' };

    protected static final String STRING_MLONG1 = "A\uFF21B\uFF22C\uFF23D\uFF24E\uFF25F\uFF26G\uFF27H\uFF28";
    protected static final byte[] BYTE_ARRAY_MLONG1 = getBytes(STRING_MLONG1);
    protected static final char[] CHAR_ARRAY_MLONG1 = new char[] { 'A', '\uFF21',
            'B', '\uFF22', 'C', '\uFF23', 'D', '\uFF24', 'E', '\uFF25', 'F',
            '\uFF26', 'G', '\uFF27', 'H', '\uFF28' };
    protected static final int[] POINT_ARRAY_MLONG1 = new int[] { 'A', '\uFF21',
            'B', '\uFF22', 'C', '\uFF23', 'D', '\uFF24', 'E', '\uFF25', 'F',
            '\uFF26', 'G', '\uFF27', 'H', '\uFF28' };

    protected static final String STRING_MLONG2 = "\uFF21A\uFF22B\uFF23C\uFF24D\uFF25E\uFF26F\uFF27G\uFF28H";
    protected static final byte[] BYTE_ARRAY_MLONG2 = getBytes(STRING_MLONG2);
    protected static final char[] CHAR_ARRAY_MLONG2 = new char[] { '\uFF21', 'A',
            '\uFF22', 'B', '\uFF23', 'C', '\uFF24', 'D', '\uFF25', 'E',
            '\uFF26', 'F', '\uFF27', 'G', '\uFF28', 'H' };
    protected static final int[] POINT_ARRAY_MLONG2 = new int[] { '\uFF21', 'A',
            '\uFF22', 'B', '\uFF23', 'C', '\uFF24', 'D', '\uFF25', 'E',
            '\uFF26', 'F', '\uFF27', 'G', '\uFF28', 'H' };

    protected static final String STRING_M11 = "A\uFF21";
    protected static final byte[] BYTE_ARRAY_M11 = getBytes(STRING_M11);
    protected static final char[] CHAR_ARRAY_M11 = new char[] { 'A', '\uFF21' };
    protected static final int[] POINT_ARRAY_M11 = new int[] { 'A', '\uFF21' };

    protected static final String STRING_M12 = "\uFF21A";
    protected static final byte[] BYTE_ARRAY_M12 = getBytes(STRING_M12);
    protected static final char[] CHAR_ARRAY_M12 = new char[] { '\uFF21', 'A' };
    protected static final int[] POINT_ARRAY_M12 = new int[] { '\uFF21', 'A' };

    protected static final String STRING_SUPPLEMENTARY = "\uD801\uDC00\uD801\uDC01\uFF21A";
    protected static final byte[] BYTE_ARRAY_SUPPLEMENTARY = getBytes(STRING_SUPPLEMENTARY);
    protected static final char[] CHAR_ARRAY_SUPPLEMENTARY = new char[] {
            '\uD801', '\uDC00', '\uD801', '\uDC01', '\uFF21', 'A' };
    protected static final int[] POINT_ARRAY_SUPPLEMENTARY = new int[] {
            '\uD801', '\uDC00', '\uD801', '\uDC01', '\uFF21', 'A' };

    protected static final String STRING_SUPPLEMENTARY_LOWERCASE = "\uD801\uDC28\uD801\uDC29\uFF41a";
    protected static final byte[] BYTE_ARRAY_SUPPLEMENTARY_LOWERCASE = getBytes(STRING_SUPPLEMENTARY_LOWERCASE);
    protected static final char[] CHAR_ARRAY_SUPPLEMENTARY_LOWERCASE = new char[] {
            '\uD801', '\uDC28', '\uD801', '\uDC29', '\uFF41', 'a' };
    protected static final int[] POINT_ARRAY_SUPPLEMENTARY_LOWERCASE = new int[] {
            '\uD801', '\uDC28', '\uD801', '\uDC29', '\uFF41', 'a' };

    protected static final String SRC_BYTE_ARRAY_WITH_CHARSETNAME = "source from byte array with charset name";
    protected static final String SRC_BYTE_ARRAY_WITH_CHARSET = "source from byte array with charset";
    protected static final String SRC_CHAR_ARRAY = "source from char array";
    protected static final String SRC_POINT_ARRAY = "source from code point array";
    protected static final String SRC_STRING = "source from String";
    protected static final String SRC_STRINGBUFFER = "source from StringBuffer";
    protected static final String SRC_STRINGBUILDER = "source from StringBuilder";
    protected static final String SRC_COPYVALUEOF = "source from copyValueOf from char array";
    protected static final String SRC_VALUEOF = "source from valueOf from char array";

    static {
        System.out
                .println(String
                        .format("====== The platform's default charset is \"%s\", we're using \"%s\" for testing.",
                                Charset.defaultCharset().name(),
                                DEFAULT_CHARSET_NAME));
    }

    private static byte[] getBytes(String str) {
        byte[] res = null;
        try {
            res = str.getBytes(DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException("caught UnsupportedEncodingException!!!", e);
        }
        return res;
    }

    private void setUpOneString(String content, byte[] ba, char[] ca, int[] cpa)
            throws UnsupportedEncodingException {
        final Map<String, String> m = new HashMap<>();
        m.put(SRC_BYTE_ARRAY_WITH_CHARSETNAME, new String(ba,
                DEFAULT_CHARSET_NAME));
        m.put(SRC_BYTE_ARRAY_WITH_CHARSET, new String(ba, DEFAULT_CHARSET));
        m.put(SRC_CHAR_ARRAY, new String(ca));
        m.put(SRC_POINT_ARRAY, new String(cpa, 0, cpa.length));
        m.put(SRC_STRING, new String(content));
        m.put(SRC_STRINGBUFFER, new String(new StringBuffer(content)));
        m.put(SRC_STRINGBUILDER, new String(new StringBuilder(content)));
        m.put(SRC_COPYVALUEOF, String.copyValueOf(ca));
        m.put(SRC_VALUEOF, String.valueOf(ca));
        map.put(content, m);
    }

    /*
     * Set up the test data, use 9 ways to construct one String.
     *
     * @throws UnsupportedEncodingException
     *         If the named charset is not supported in setUpOneString(xxx).
     */
    @BeforeClass
    public void setUp() throws UnsupportedEncodingException {
        for (StringSources src : StringSources.values()) {
            setUpOneString(src.getString(), src.getByteArray(),
                    src.getCharArray(), src.getIntArray());
        }
    }

    /*
     * Escape non-ASCII characters since not all systems support them.
     */
    protected String escapeNonASCIIs(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c > 0x7F) {
                sb.append("\\u").append(Integer.toHexString((int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /*
     * Escape non-ASCII characters since not all systems support them.
     */
    protected String escapeNonASCII(char c) {
        StringBuilder sb = new StringBuilder();
        if (c > 0x7F) {
            sb.append("\\u").append(Integer.toHexString((int) c));
        } else {
            sb.append(c);
        }
        return sb.toString();
    }
}
