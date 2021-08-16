/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5047314
 * @summary verify that compare() and getCollationKey() don't go into an infinite loop for unfinished Thai/Lao text.
 * @run main/timeout=60 Bug5047314
 */
import java.text.Collator;
import java.util.Locale;

public class Bug5047314 {

    private static Collator colLao = Collator.getInstance(new Locale("lo"));
    private static Collator colThai = Collator.getInstance(new Locale("th"));

    private static String[] textLao = {
        "\u0ec0", "\u0ec1", "\u0ec2", "\u0ec3", "\u0ec4"
    };
    private static String[] textThai = {
        "\u0e40", "\u0e41", "\u0e42", "\u0e43", "\u0e44"
    };

    public static void main(String[] args) {
        testLao1();
        testLao2();
        testThai1();
        testThai2();
    }

    private static void testLao1() {
        System.out.print("Test(Lao 1) .... ");
        for (int i = 0; i < textLao.length; i++) {
            colLao.compare(textLao[i], textLao[i]);
        }
        System.out.println("Passed.");
    }

    private static void testLao2() {
        System.out.print("Test(Lao 2) .... ");
        for (int i = 0; i < textLao.length; i++) {
            colLao.compare(textLao[i], textLao[i]);
        }
        System.out.println("Passed.");
    }

    private static void testThai1() {
        System.out.print("Test(Thai 1) .... ");
        for (int i = 0; i < textThai.length; i++) {
            colThai.compare(textThai[i], textThai[i]);
        }
        System.out.println("Passed.");
    }

    private static void testThai2() {
        System.out.print("Test(Thai 2) .... ");
        for (int i = 0; i < textThai.length; i++) {
            colThai.getCollationKey(textThai[i]);
        }
        System.out.println("Passed.");
    }

}
