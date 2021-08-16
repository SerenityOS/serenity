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


public class TransformUtil {
    public static final String BeforePattern = "this-should-be-transformed";
    public static final String AfterPattern  = "this-has-been--transformed";
    public static final String ParentCheckPattern = "parent-transform-check: ";
    public static final String ChildCheckPattern = "child-transform-check: ";

    /**
     * @return the number of occurrences of the <code>from</code> string that
     * have been replaced.
     */
    public static int replace(byte buff[], String from, String to) {
        if (to.length() != from.length()) {
            throw new RuntimeException("bad strings");
        }
        byte f[] = asciibytes(from);
        byte t[] = asciibytes(to);
        byte f0 = f[0];

        int numReplaced = 0;
        int max = buff.length - f.length;
        for (int i = 0; i < max; ) {
            if (buff[i] == f0 && replace(buff, f, t, i)) {
                i += f.length;
                numReplaced++;
            } else {
                i++;
            }
        }
        return numReplaced;
    }

    public static boolean replace(byte buff[], byte f[], byte t[], int i) {
        for (int x = 0; x < f.length; x++) {
            if (buff[x+i] != f[x]) {
                return false;
            }
        }
        for (int x = 0; x < f.length; x++) {
            buff[x+i] = t[x];
        }
        return true;
    }

    static byte[] asciibytes(String s) {
        byte b[] = new byte[s.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte)s.charAt(i);
        }
        return b;
    }
}
