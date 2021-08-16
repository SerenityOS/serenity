/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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


public class Util {

    private Util() { }

    // Returns -1 if equal, o.w. returns index of first difference
    //
    public static int cmp(byte[] ba, byte[] bb) {
        int n = Math.min(ba.length, bb.length);
        for (int i = 0; i < n; i++) {
            if ((i >= ba.length) || (i >= bb.length))
                return i;
            if (ba[i] != bb[i])
                return i;
        }
        if (ba.length != bb.length)
            return 0;
        return -1;
    }

    // Returns -1 if equal, o.w. returns index of first difference
    //
    public static int cmp(char[] ca, char[] cb) {
        int n = Math.min(ca.length, cb.length);
        for (int i = 0; i < n; i++) {
            if ((i >= ca.length) || (i >= cb.length))
                return i;
            if (ca[i] != cb[i])
                return i;
        }
        if (ca.length != cb.length)
            return 0;
        return -1;
    }

    public static String toString(byte[] ba, int off, int len) {
        StringBuffer sb = new StringBuffer();
        for (int i = off; i < off + len; i++) {
            int c = ba[i];
            if (c == '\\') {
                sb.append("\\\\");
                continue;
            }
            if ((c >= ' ') && (c < 0x7f)) {
                sb.append((char)c);
                continue;
            }
            sb.append("\\x");
            sb.append(Integer.toHexString(c & 0xff));
        }
        return sb.toString();
    }

    public static String toString(byte[] ba) {
        return toString(ba, 0, ba.length);
    }

    public static String toString(char[] ca, int off, int len) {
        StringBuffer sb = new StringBuffer();
        for (int i = off; i < off + len; i++) {
            char c = ca[i];
            if (c == '\\') {
                sb.append("\\\\");
                continue;
            }
            if ((c >= ' ') && (c < 0x7f)) {
                sb.append(c);
                continue;
            }
            sb.append("\\u");
            String s = Integer.toHexString(c);
            while (s.length() < 4)
                s = "0" + s;
            sb.append(s);
        }
        return sb.toString();
    }

    public static String toString(char[] ca) {
        return toString(ca, 0, ca.length);
    }

    public static String toString(String s) {
        return toString(s.toCharArray());
    }

    public static String toString(char c) {
        return toString(new char[]{ c });
    }

}
