/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4546734 5007612
 * @summary Test StringBuffer.trimToSize
 * @key randomness
 */

import java.util.Random;

public class Trim {
    private static Random generator = new Random();

    public static void main(String[] args) throws Exception {
        bash();
        //capacityCheck();
    }

    // Make sure trimToSize is safe to use; it should never cause an
    // exception or mutation
    private static void bash() throws Exception {
        for (int i=0; i<1000; i++) {
            StringBuffer sb1 = generateTestBuffer(0, 100);
            StringBuffer sb2 = new StringBuffer(sb1);
            sb1.trimToSize();
            if (!sb1.toString().equals(sb2.toString()))
                throw new RuntimeException(
                    "trim mutated stringbuffer contents");
            // Append a random sb
            StringBuffer sb3 = generateTestBuffer(0, 100);
            sb1.append(sb3);
            sb2.append(sb3);
            if (generator.nextInt(2) == 0)
                sb1.trimToSize();
            else
                sb2.trimToSize();
            if (!sb1.toString().equals(sb2.toString()))
                throw new RuntimeException(
                    "trim mutated stringbuffer contents");
            // Append sb with lots of extra space
            sb3 = new StringBuffer(100);
            sb3.append("a");
            sb1.append(sb3);
            sb2.append(sb3);
            if (generator.nextInt(2) == 0)
                sb1.trimToSize();
            else
                sb2.trimToSize();
            if (!sb1.toString().equals(sb2.toString()))
                throw new RuntimeException(
                    "trim mutated stringbuffer contents");
        }
    }

    // This test gives some assurance that trimToSize is working but
    // it should not be part of an automated run, and a failure here
    // is not against spec; this method is provided simply to be run
    // by hand and assure the engineer that it is working. The test
    // may stop working at some time in the future depending on
    // how String and StringBuffer are implemented because it depends
    // upon the capacity method.
    private static void capacityCheck() {
        for (int i=0; i<100; i++) {
            int sizeNeeded = generator.nextInt(1000)+1;
            int sizeExtra = generator.nextInt(100) + 1;
            StringBuffer sb = new StringBuffer(sizeNeeded + sizeExtra);
            StringBuffer sb2 = generateTestBuffer(sizeNeeded, sizeNeeded);
            if (sb2.length() != sizeNeeded)
                throw new RuntimeException("sb generated incorrectly");
            sb.append(sb2);
            int oldCapacity = sb.capacity();
            sb.trimToSize();
            int newCapacity = sb.capacity();
            if (oldCapacity == newCapacity)
                throw new RuntimeException("trim failed");
        }
    }

    private static int getRandomIndex(int constraint1, int constraint2) {
        int range = constraint2 - constraint1;
        if (range <= 0)
            return constraint1;
        int x = generator.nextInt(range);
        return constraint1 + x;
    }

    private static StringBuffer generateTestBuffer(int min, int max) {
        StringBuffer aNewStringBuffer = new StringBuffer();
        int aNewLength = getRandomIndex(min, max);
        for(int y=0; y<aNewLength; y++) {
            int achar = generator.nextInt(30)+30;
            char test = (char)(achar);
            aNewStringBuffer.append(test);
        }
        return aNewStringBuffer;
    }

}
