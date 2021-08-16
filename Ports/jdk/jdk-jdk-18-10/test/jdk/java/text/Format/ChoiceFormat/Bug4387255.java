/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4387255
 * @summary Verifies that ChoiceFormat can handle large numbers of choices
 */

import java.text.ChoiceFormat;

public class Bug4387255 {

    private static final double[] doubles = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35};

    private static final String[] strings = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"};

    private static final String pattern =
            "0#0|1#1|2#2|3#3|4#4|5#5|6#6|7#7|8#8|9#9" +
            "|10#A|11#B|12#C|13#D|14#E|15#F|16#G|17#H|18#I|19#J" +
            "|20#K|21#L|22#M|23#N|24#O|25#P|26#Q|27#R|28#S|29#T" +
            "|30#U|31#V|32#W|33#X|34#Y|35#Z";

    public static void main(String[] args) throws Exception {
        ChoiceFormat choiceFormat1 = new ChoiceFormat(doubles, strings);
        ChoiceFormat choiceFormat2 = new ChoiceFormat(pattern);
        if (!choiceFormat1.equals(choiceFormat2)) {
            System.out.println("choiceFormat1: " + choiceFormat1.toPattern());
            System.out.println("choiceFormat2: " + choiceFormat2.toPattern());
            throw new RuntimeException();
        }

        for (int i = 0; i < doubles.length; i++) {
            String result = choiceFormat2.format(doubles[i]);
            if (!result.equals(strings[i])) {
                throw new RuntimeException("Wrong format result - expected " +
                        strings[i] + ", got " + result);
            }
        }
    }
}
