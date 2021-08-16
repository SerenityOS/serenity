/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8039390
 * @summary Basic test for null argument
 */

import java.util.Formatter;
import java.util.Locale;

public class NullArg {

    public static void main(String [] args) {
        char[] cs = new char[] {
            'b', 'B', 'h', 'H', 's', 'S', 'c', 'C', 'd', 'o', 'x', 'X',
            'e', 'E', 'f', 'g', 'G', 'a', 'A', 't', 'T',
        };
        char[] tcs = new char[] {
            'H', 'I', 'k', 'l', 'l', 'M', 'S', 'L', 'N', 'p', 'z', 'Z', 's',
            'Q', 'B', 'b', 'h', 'A', 'a', 'C', 'Y', 'y', 'j', 'm', 'd', 'e',
            'R', 'T', 'r', 'D', 'F', 'c'
        };
        for (char c : cs) {
            String expected = (c == 'b' || c == 'B') ? "false" : "null";
            if (Character.isUpperCase(c)) {
                expected = expected.toUpperCase(Locale.ROOT);
            }
            if (c == 't' || c == 'T') {
                for (char ct : tcs) {
                    if (!String.format("%" + c + ct, null).equals(expected)) {
                        throw new RuntimeException("%t" + ct + "null check failed.");
                    }
                }
            } else {
                if (!String.format("%" + c , null).equals(expected)) {
                    throw new RuntimeException("%" + c + "null check failed.");
                }
            }
        }
    }
}
