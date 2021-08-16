/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8239569
 * @summary Phi of AddP transformation can cause offset in constant array to be dropped
 *
 * @run main/othervm -XX:-BackgroundCompilation TestWrongOffsetConstantArrayConstant
 */

public class TestWrongOffsetConstantArrayConstant {
    private static volatile char volatileField;

    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            if (test('o') != 'o') {
                throw new RuntimeException("bad result");
            }
        }
    }


    static final byte[] str = {'f', 'o'};

    private static char test(char b1) {
        // Trigger extra rounds of loop opts
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                for (int l = 0; l < 3; l++) {

                }
            }
        }
        // Will fully unroll
        int i = 0;
        for (; i < str.length; i++) {
            final byte b = str[i]; // LoadB
            if (b == b1) {
                break;
            }
        }
        // LoadUB that shares address with LoadB above
        // Will split thru phi
        final char c = (char) (str[i] & 0xff);
        volatileField = c;
        // LoadUB that shares address with LoadB and LoadUB above
        final char c2 = (char) (str[i] & 0xff);
        return c2;
    }
}
