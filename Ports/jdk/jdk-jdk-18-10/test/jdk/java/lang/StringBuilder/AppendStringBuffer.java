/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6206780
 * @summary Test StringBuilder.append(StringBuffer);
 * @key randomness
 */

import java.util.Random;

public class AppendStringBuffer {
    private static Random generator = new Random();

    public static void main(String[] args) throws Exception {
        for (int i=0; i<1000; i++) {
            StringBuffer sb1 = generateTestBuffer(10, 100);
            StringBuffer sb2 = generateTestBuffer(10, 100);
            StringBuffer sb3 = generateTestBuffer(10, 100);
            String s1 = sb1.toString();
            String s2 = sb2.toString();
            String s3 = sb3.toString();

            String concatResult = new String(s1+s2+s3);

            StringBuilder test = new StringBuilder();
            test.append(sb1);
            test.append(sb2);
            test.append(sb3);

            if (!test.toString().equals(concatResult))
                throw new RuntimeException("StringBuffer.append failure");
        }
    }

    private static int getRandomIndex(int constraint1, int constraint2) {
        int range = constraint2 - constraint1;
        int x = generator.nextInt(range);
        return constraint1 + x;
    }

    private static StringBuffer generateTestBuffer(int min, int max) {
        StringBuffer aNewStringBuffer = new StringBuffer(120);
        int aNewLength = getRandomIndex(min, max);
        for(int y=0; y<aNewLength; y++) {
            int achar = generator.nextInt(30)+30;
            char test = (char)(achar);
            aNewStringBuffer.append(test);
        }
        return aNewStringBuffer;
    }
}
