/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8196740
 * @summary Check j.l.Character.digit(int,int) for Latin1 characters
 */

public class Latin1Digit {

    public static void main(String[] args) throws Exception {
        for (int ch = 0; ch < 256; ++ch) {
            for (int radix = -256; radix <= 256; ++radix) {
                test(ch, radix);
            }
            test(ch, Integer.MIN_VALUE);
            test(ch, Integer.MAX_VALUE);
        }
    }

    static void test(int ch, int radix) throws Exception {
        int d1 = Character.digit(ch, radix);
        int d2 = canonicalDigit(ch, radix);
        if (d1 != d2) {
            throw new Exception("Wrong result for char="
                    + ch + " (" + (char)ch + "), radix="
                    + radix + "; " + d1 + " != " + d2);
        }
    }

    // canonical version of Character.digit(int,int) for Latin1
    static int canonicalDigit(int ch, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            return -1;
        }
        if (ch >= '0' && ch <= '9' && ch < (radix + '0')) {
            return ch - '0';
        }
        if (ch >= 'A' && ch <= 'Z' && ch < (radix + 'A' - 10)) {
            return ch - 'A' + 10;
        }
        if (ch >= 'a' && ch <= 'z' && ch < (radix + 'a' - 10)) {
            return ch - 'a' + 10;
        }
        return -1;
    }
}
