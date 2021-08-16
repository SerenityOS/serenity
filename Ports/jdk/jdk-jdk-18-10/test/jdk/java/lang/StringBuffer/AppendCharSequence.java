/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4812591 4705328 5019111 8218228
 * @summary Test append and insert methods with CharSequence params
 * @key randomness
 */

import java.util.Random;

public class AppendCharSequence {
    private static Random generator = new Random();

    public static void main(String[] args) throws Exception {
        bash();
        checkNulls();
        checkOffsets();
        checkConstructor();
    }

    // Sanity test of contents
    private static void bash() throws Exception {
        for (int i=0; i<1000; i++) {
            StringBuffer sb1 = generateTestBuffer(0, 100);
            StringBuffer sb2 = generateTestBuffer(0, 100);
            StringBuffer sb3 = generateTestBuffer(0, 100);
            StringBuffer sb4 = generateTestBuffer(0, 100);
            StringBuffer sb5 = new StringBuffer();

            String s1 = sb1.toString();
            String s2 = sb2.toString();
            String s3 = sb3.toString();
            String s4 = sb4.toString();
            String s5 = null;

            // append(CharSequence cs)
            sb5.append((CharSequence)sb1);
            s5 = sb1.toString();

            if (!sb5.toString().equals(s5))
                throw new RuntimeException("StringBuffer.append failure 1");

            // append (CharSequence cs, int start, int end)
            int index = generator.nextInt(100);
            int len = generator.nextInt(100);
            while (index > sb2.length() - len) {
                index = generator.nextInt(100);
                len = generator.nextInt(100);
            }
            sb5.append((CharSequence)sb2, index, index + len);
            s5 = s5 + sb2.toString().substring(index, index + len);

            if (!sb5.toString().equals(s5))
                throw new RuntimeException("StringBuffer.append failure 2");

            // insert(int dstOffset, CharSequence cs)
            index = generator.nextInt(100);
            while (index > s5.length()) {
                index = generator.nextInt(100);
            }
            sb5.insert(index, (CharSequence)sb3);
            s5 = new StringBuffer(s5).insert(index, sb3).toString();

            if (!sb5.toString().equals(s5))
                throw new RuntimeException("StringBuffer.insert failure 1");

            // insert(int dstOffset, CharSequence s, int start, int end)
            int index1 = generator.nextInt(100);
            while (index1 > s5.length()) {
                index1 = generator.nextInt(100);
            }
            int index2 = generator.nextInt(100);
            len = generator.nextInt(100);
            while (index2 > sb4.length() - len) {
                index2 = generator.nextInt(100);
                len = generator.nextInt(100);
            }
            sb5.insert(index1, (CharSequence)sb4, index2, index2 + len);
            s5 = new StringBuffer(s5).insert(index1, s4.toCharArray(),
                                             index2, len).toString();

            if (!sb5.toString().equals(s5))
                throw new RuntimeException("StringBuffer.insert failure 2");
        }
    }

    private static int getRandomIndex(int constraint1, int constraint2) {
        int range = constraint2 - constraint1;
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

    // Check handling of null as "null"
    private static void checkNulls() throws Exception {
        StringBuffer sb1 = new StringBuffer();
        CharSequence cs = null;
        sb1.append("test");
        sb1.append(cs);
        if (!sb1.toString().equals("testnull"))
            throw new RuntimeException("StringBuffer.append failure 3");

        sb1 = new StringBuffer();
        sb1.append("test", 0, 2);
        sb1.append(cs, 0, 2);
        if (!sb1.toString().equals("tenu"))
            throw new RuntimeException("StringBuffer.append failure 4");

        sb1 = new StringBuffer("test");
        sb1.insert(2, cs);
        if (!sb1.toString().equals("tenullst"))
            throw new RuntimeException("StringBuffer.insert failure 3");

        sb1 = new StringBuffer("test");
        sb1.insert(2, cs, 0, 2);
        if (!sb1.toString().equals("tenust"))
            throw new RuntimeException("StringBuffer.insert failure 4");
    }

    // Test the bounds checking
    private static void checkOffsets() throws Exception {

        // append (CharSeqeunce cs, int start, int end)
        for (int i=0; i<100; i++) {
            StringBuffer sb = generateTestBuffer(0, 80);
            CharSequence cs = (CharSequence)generateTestBuffer(0, 80);
            int index = 0;
            int len = 0;
            while (index <= cs.length() - len) {
                index = generator.nextInt(100) - 50;
                len = generator.nextInt(100) - 50;
                if (index < 0)
                    break;
                if (len < 0)
                    break;
            }
            try {
                sb.append(cs, index, index + len);
                throw new RuntimeException("Append bounds checking failure");
            } catch (IndexOutOfBoundsException e) {
                // Correct result
            }
        }

        // insert(int dstOffset, CharSequence cs)
        for (int i=0; i<100; i++) {
            StringBuffer sb = new StringBuffer("test1");
            CharSequence cs = (CharSequence)new StringBuffer("test2");
            int index = 0;
            while (index <= sb.length()) {
                index = generator.nextInt(100) - 50;
                if (index < 0)
                    break;
            }
            try {
                sb.insert(index, cs);
                throw new RuntimeException("Insert bounds checking failure");
            } catch (IndexOutOfBoundsException e) {
                // Correct result
            }
        }

        // insert(int dstOffset, CharSequence s, int start, int end)
        for (int i=0; i<100; i++) {
            StringBuffer sb = new StringBuffer("test1");
            CharSequence cs = (CharSequence)new StringBuffer("test2");
            int index1 = 0;
            while (index1 <= sb.length()) {
                index1 = generator.nextInt(100) - 50;
                if (index1 < 0)
                    break;
            }
            int index2 = 0;
            int len = 0;
            while (index2 < sb.length() - len) {
                index2 = generator.nextInt(100) - 50;
                len = generator.nextInt(100) - 50;
                if (index2 < 0)
                    break;
                if (len < 0)
                    break;
            }
            try {
                sb.insert(index1, cs, index2, index2 + len);
                throw new RuntimeException("Insert bounds checking failure");
            } catch (IndexOutOfBoundsException e) {
                // Correct result
            }
        }
    }

    // Test the CharSequence constructor
    private static void checkConstructor() throws Exception {
        for (int i=0; i<100; i++) {
            StringBuffer sb = generateTestBuffer(0, 100);
            CharSequence cs = (CharSequence)sb;
            StringBuffer sb2 = new StringBuffer(cs);
            if (!sb.toString().equals(sb2.toString())) {
                throw new RuntimeException("CharSequence constructor failure");
            }
        }
        checkNegativeLenCharSeq(-1);
        checkNegativeLenCharSeq(-16);
        checkNegativeLenCharSeq(-17);
        checkNegativeLenCharSeq(Integer.MIN_VALUE);
    }

    // Test constructing from CharSequence of negative length
    private static void checkNegativeLenCharSeq(int len) {
        try {
            CharSequence seq = new MyNegativeLenCharSeq(len);
            StringBuffer sb = new StringBuffer(seq);
        } catch (NegativeArraySizeException expected) {
        } catch (Throwable exc) {
            throw new RuntimeException("Unexpected: " + exc, exc);
        }
    }

    private static class MyNegativeLenCharSeq implements CharSequence {
        int length;
        MyNegativeLenCharSeq(int length) {
            this.length = length;
        }
        public char charAt(int i) {
            throw new UnsupportedOperationException();
        }
        public int length() { return length; }
        public CharSequence subSequence(int st, int e) {
            throw new UnsupportedOperationException();
        }
        public String toString() { return ""; }
    }
}
