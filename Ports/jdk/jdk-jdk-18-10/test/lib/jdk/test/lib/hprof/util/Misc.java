/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.util;
import java.util.*;

/**
 * Miscellaneous functions I couldn't think of a good place to put.
 *
 * @author      Bill Foote
 */


public class Misc {

    private static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7',
                                     '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public final static String toHex(int addr) {
        char[] buf = new char[8];
        int i = 0;
        for (int s = 28; s >= 0; s -= 4) {
            buf[i++] = digits[(addr >> s) & 0xf];
        }
        return "0x" + new String(buf);
    }

    public final static String toHex(long addr) {
        return "0x" + Long.toHexString(addr);
    }

    public final static long parseHex(String value) {
        long result = 0;
        if (value.length() < 2 || value.charAt(0) != '0' ||
            value.charAt(1) != 'x') {
            return -1L;
        }
        for(int i = 2; i < value.length(); i++) {
            result *= 16;
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                result += (ch - '0');
            } else if (ch >= 'a' && ch <= 'f') {
                result += (ch - 'a') + 10;
            } else if (ch >= 'A' && ch <= 'F') {
                result += (ch - 'A') + 10;
            } else {
                throw new NumberFormatException("" + ch
                                        + " is not a valid hex digit");
            }
        }
        return result;
    }

    public static String encodeHtml(String str) {
        final int len = str.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (ch == '<') {
                sb.append("&lt;");
            } else if (ch == '>') {
                sb.append("&gt;");
            } else if (ch == '"') {
                sb.append("&quot;");
            } else if (ch == '\'') {
                sb.append("&#039;");
            } else if (ch == '&') {
                sb.append("&amp;");
            } else if (ch < ' ') {
                sb.append("&#").append((int)ch).append(';');
            } else {
                int c = (ch & 0xFFFF);
                if (c > 127) {
                    sb.append("&#").append(c).append(';');
                } else {
                    sb.append(ch);
                }
            }
        }
        return sb.toString();
    }
}
