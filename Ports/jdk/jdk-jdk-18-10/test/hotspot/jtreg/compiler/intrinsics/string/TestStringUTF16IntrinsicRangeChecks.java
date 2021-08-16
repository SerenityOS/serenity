/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8158168
 * @summary Verifies that callers of StringUTF16 intrinsics throw array out of bounds exceptions.
 * @library /compiler/patches /test/lib
 * @build java.base/java.lang.Helper
 * @run main/othervm -Xbatch -XX:CompileThreshold=100 -XX:+UnlockDiagnosticVMOptions -XX:DisableIntrinsic=_getCharStringU,_putCharStringU compiler.intrinsics.string.TestStringUTF16IntrinsicRangeChecks
 * @run main/othervm -Xbatch -XX:CompileThreshold=100 -esa -ea -XX:+UnlockDiagnosticVMOptions -XX:DisableIntrinsic=_getCharStringU,_putCharStringU compiler.intrinsics.string.TestStringUTF16IntrinsicRangeChecks
 */
package compiler.intrinsics.string;

import java.lang.reflect.Field;
import java.util.Arrays;

public class TestStringUTF16IntrinsicRangeChecks {

    public static void main(String[] args) throws Exception {
        byte[] val = new byte[2];
        byte[] b4  = new byte[4];
        char[] c4  = new char[4];
        String s4 = new String(c4);
        byte[] valHigh = new byte[2];
        byte[] valLow  = new byte[2];
        Helper.putCharSB(valHigh, 0, Character.MIN_HIGH_SURROGATE);
        Helper.putCharSB(valLow,  0, Character.MIN_LOW_SURROGATE);

        for (int i = 0; i < 1000; ++i) {
            getChars((int)1234, -5, -5 + 4, val);
            getChars((int)1234, -1, -1 + 4, val);
            getChars((int)1234,  0,  0 + 4, val);
            getChars((int)1234,  1,  1 + 4, val);

            getChars((long)1234, -5, -5 + 4, val);
            getChars((long)1234, -1, -1 + 4, val);
            getChars((long)1234,  0,  0 + 4, val);
            getChars((long)1234,  1,  1 + 4, val);

            byte[] val2 = Arrays.copyOf(val, val.length);
            putCharSB(val2, -1, '!');
            putCharSB(val2,  1, '!');

            byte[] val4 = Arrays.copyOf(b4, b4.length);
            char[] c2  = new char[2];
            String s2 = new String(c2);

            putCharsSB(val4, -3, c2, 0, 2);
            putCharsSB(val4, -1, c2, 0, 2);
            putCharsSB(val4,  0, c4, 0, 4);
            putCharsSB(val4,  1, c2, 0, 2);
            putCharsSB(val4, -3, s2, 0, 2);
            putCharsSB(val4, -1, s2, 0, 2);
            putCharsSB(val4,  0, s4, 0, 4);
            putCharsSB(val4,  1, s2, 0, 2);

            codePointAtSB(valHigh, -1, 1);
            codePointAtSB(valHigh, -1, 2);
            codePointAtSB(valHigh,  0, 2);
            codePointAtSB(valHigh,  1, 2);

            codePointBeforeSB(valLow,  0);
            codePointBeforeSB(valLow, -1);
            codePointBeforeSB(valLow,  2);

            if (Helper.codePointCountSB(valHigh, 0, 1) != 1) {
                throw new AssertionError("codePointCountSB");
            }
            if (Helper.codePointCountSB(valLow, 0, 1) != 1) {
                throw new AssertionError("codePointCountSB");
            }
            codePointCountSB(valHigh, -1, 0);
            codePointCountSB(valHigh, -1, 2);
            codePointCountSB(valHigh,  0, 2);

            charAt(val, -1);
            charAt(val,  1);

            contentEquals(b4, val, -1);
            contentEquals(b4, val,  2);
            contentEquals(val, s4,  2);
            contentEquals(val, s4, -1);

            StringBuilder sb = new StringBuilder();
            sb.append((String)null).append(true).append(false);
            if (!sb.toString().equals("nulltruefalse")) {
                throw new AssertionError("append");
            }

            putCharsAt(val2, -1, '1', '2', '3', '4');
            putCharsAt(val2,  0, '1', '2', '3', '4');
            putCharsAt(val2,  2, '1', '2', '3', '4');
            putCharsAt(val2, -1, '1', '2', '3', '4', '5');
            putCharsAt(val2,  0, '1', '2', '3', '4', '5');
            putCharsAt(val2,  2, '1', '2', '3', '4', '5');

            reverse(valHigh, -1);
            reverse(valHigh,  2);
            reverse(valLow,  -1);
            reverse(valLow,   2);

            byte[] d4 = new byte[4];
            inflate(b4, 0, d4, -1, 2);
            inflate(b4, 0, d4,  3, 2);
            inflate(b4, 0, d4,  4, 1);

            byte[] b0 = new byte[0];
            byte[] b1 = new byte[1];
            byte[] b2 = new byte[2];
            byte[] t1 = new byte[] {1};
            byte[] t2 = new byte[] {1, 2};
            byte[] t4 = new byte[] {1, 2, 3, 4};
            indexOf(b1,  1, t2,  1, 0);
            indexOf(b2,  1, t1,  1, 0);
            indexOf(b2,  2, t2,  1, 0);
            indexOf(b2,  1, t2,  2, 0);
            indexOf(b2, -1, t2,  1, 0);
            indexOf(b2,  1, t2, -1, 0);
            indexOf(b2,  1, t2,  1, 1);

            indexOfLatin1(b1,  1, t1,  1, 0);
            indexOfLatin1(b2,  2, t1,  1, 0);
            indexOfLatin1(b2,  1, b0,  1, 0);
            indexOfLatin1(b2,  1, t1,  2, 0);
            indexOfLatin1(b2, -1, t1,  1, 0);
            indexOfLatin1(b2,  2, t1,  1, 0);
            indexOfLatin1(b2,  1, t1, -1, 0);
            indexOfLatin1(b2,  1, t1,  2, 0);

            lastIndexOf(b1, t2, 1, 0);
            lastIndexOf(b2, t4, 2, 0);
            lastIndexOf(b2, t2, 1, 0);
            lastIndexOf(b2, t2, 1, 1);

            lastIndexOfLatin1(b1, t1, 1, 0);
            lastIndexOfLatin1(b2, t2, 2, 0);
            lastIndexOfLatin1(b2, t1, 1, 0);
            lastIndexOfLatin1(b2, t1, 1, 1);
        }
    }

    static void getChars(int i, int begin, int end, byte[] value) {
        try {
            Helper.getChars(i, begin, end, value);
            throw new AssertionError("getChars");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void getChars(long l, int begin, int end, byte[] value) {
        try {
            Helper.getChars(l, begin, end, value);
            throw new AssertionError("getChars");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void putCharSB(byte[] val, int index, int c) {
        try {
            Helper.putCharSB(val, index, c);
            throw new AssertionError("putCharSB");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void putCharsSB(byte[] val, int index, char[] ca, int off, int end) {
        try {
            Helper.putCharsSB(val, index, ca, off, end);
            throw new AssertionError("putCharsSB");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void putCharsSB(byte[] val, int index, CharSequence s, int off, int end) {
        try {
            Helper.putCharsSB(val, index, s, off, end);
            throw new AssertionError("putCharsSB");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void codePointAtSB(byte[] val, int index, int end) {
        try {
            Helper.codePointAtSB(val, index, end);
            throw new AssertionError("codePointAtSB");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void codePointBeforeSB(byte[] val, int index) {
        try {
            Helper.codePointBeforeSB(val, index);
            throw new AssertionError("codePointBeforeSB");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void codePointCountSB(byte[] val, int beginIndex, int endIndex) {
        try {
            Helper.codePointCountSB(val, beginIndex, endIndex);
            throw new AssertionError("codePointCountSB");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void charAt(byte[] v, int index) {
        try {
            Helper.charAt(v, index);
            throw new AssertionError("charAt");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void contentEquals(byte[] v1, byte[] v2, int len) {
        try {
            Helper.contentEquals(v1, v2, len);
            throw new AssertionError("contentEquals");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void contentEquals(byte[] v, CharSequence cs, int len) {
        try {
            Helper.contentEquals(v, cs, len);
            throw new AssertionError("contentEquals");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void putCharsAt(byte[] v, int i, char c1, char c2, char c3, char c4) {
        try {
            Helper.putCharsAt(v, i, c1, c2, c3, c4);
            throw new AssertionError("putCharsAt");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void putCharsAt(byte[] v, int i, char c1, char c2, char c3, char c4, char c5) {
        try {
            Helper.putCharsAt(v, i, c1, c2, c3, c4, c5);
            throw new AssertionError("putCharsAt");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void reverse(byte[] v, int len) {
        try {
            Helper.reverse(v, len);
            throw new AssertionError("reverse");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void inflate(byte[] v1, int o1, byte[] v2, int o2, int len) {
        try {
            Helper.inflate(v1, o1, v2, o2, len);
            throw new AssertionError("inflate");
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void indexOf(byte[] v1, int l1, byte[] v2, int l2, int from) {
        try {
            if (Helper.indexOf(v1, l1, v2, l2, from) != -1) {
                throw new AssertionError("indexOf");
            }
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void lastIndexOf(byte[] v1, byte[] v2, int l2, int from) {
        try {
            if (Helper.lastIndexOf(v1, v2, l2, from) != -1) {
                throw new AssertionError("lastIndexOf");
            }
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void indexOfLatin1(byte[] v1, int l1, byte[] v2, int l2, int from) {
        try {
            if (Helper.indexOfLatin1(v1, l1, v2, l2, from) != -1) {
                throw new AssertionError("indexOfLatin1");
            }
        } catch (IndexOutOfBoundsException io) {
        }
    }

    static void lastIndexOfLatin1(byte[] v1, byte[] v2, int l2, int from) {
        try {
            if (Helper.lastIndexOfLatin1(v1, v2, l2, from) != -1) {
                throw new AssertionError("lastIndexOfLatin1");
            }
        } catch (IndexOutOfBoundsException io) {
        }
    }
}
