/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6850113 8174270
 * @summary Verify the return value of digit() for some digits.
 * @modules java.base/jdk.internal.icu.lang
 * @compile -XDignore.symbol.file=true Bug6850113.java
 * @run main Bug6850113
 */

import jdk.internal.icu.lang.UCharacter;

public class Bug6850113 {

    public static void main(String[] args) {
        boolean err = false;

        // Fullwidth Latin capital letters
        for (int i = 0xff21, j = 10; i <= 0xff3a; i++, j++) {
            if (UCharacter.digit(i, 36) != j) {
                err = true;
                System.out.println("Error: UCharacter.digit(0x" +
                    Integer.toHexString(i) + ", 36) returned " +
                    UCharacter.digit(i, 36) + ", expected=" + j);
            }
        }

        // Fullwidth Latin small letters
        for (int i = 0xff41, j = 10; i <= 0xff5a; i++, j++) {
            if (UCharacter.digit(i, 36) != j) {
                err = true;
                System.out.println("Error: UCharacter.digit(0x" +
                    Integer.toHexString(i) + ", 36) returned " +
                    UCharacter.digit(i, 36) + ", expected=" + j);
            }
        }

        if (err) {
            throw new RuntimeException("UCharacter.digit():  Wrong return value");
        }
   }
}
