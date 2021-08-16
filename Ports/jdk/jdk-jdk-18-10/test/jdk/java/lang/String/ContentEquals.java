/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4242309 4982981
 * @summary Test equals and contentEquals in String
 * @key randomness
 */
import java.util.Random;
import java.nio.CharBuffer;

// Yes, I used cut and paste for this test. Yes more code
// sharing could have been accomplished.
public class ContentEquals {

    private static Random rnd = new Random();
    private static final int ITERATIONS = 1000;
    private static final int STR_LEN = 20;

    public static void main(String[] args) throws Exception {
        testStringBuffer();
        testStringBuilder();
        testString();
        testCharSequence();
    }

    // Test a StringBuffer, which uses the old method from 1.4
    public static void testStringBuffer() throws Exception {
        for (int i=0; i<ITERATIONS; i++) {
            int length = rnd.nextInt(STR_LEN) + 1;
            StringBuffer testStringBuffer = new StringBuffer();
            for(int x=0; x<length; x++) {
                char aChar = (char)rnd.nextInt();
                testStringBuffer.append(aChar);
            }
            String testString = testStringBuffer.toString();
            char c = testStringBuffer.charAt(0);
            testStringBuffer.setCharAt(0, 'c');
            testStringBuffer.setCharAt(0, c);
            if (!testString.contentEquals(testStringBuffer))
                throw new RuntimeException("ContentsEqual failure");
        }
    }

    // Test StringBuilder as a CharSequence using new method in 1.5
    public static void testStringBuilder() throws Exception {
        for (int i=0; i<ITERATIONS; i++) {
            int length = rnd.nextInt(STR_LEN) + 1;
            StringBuilder testStringBuilder = new StringBuilder();
            for(int x=0; x<length; x++) {
                char aChar = (char)rnd.nextInt();
                testStringBuilder.append(aChar);
            }
            String testString = testStringBuilder.toString();
            char c = testStringBuilder.charAt(0);
            testStringBuilder.setCharAt(0, 'c');
            testStringBuilder.setCharAt(0, c);
            if (!testString.contentEquals(testStringBuilder))
                throw new RuntimeException("ContentsEqual failure");
        }
    }

    // Test a String as a CharSequence. This takes a different codepath in
    // the new method in 1.5
    public static void testString() throws Exception {
        for (int i=0; i<ITERATIONS; i++) {
            int length = rnd.nextInt(STR_LEN) + 1;
            StringBuilder testStringBuilder = new StringBuilder();
            for(int x=0; x<length; x++) {
                char aChar = (char)rnd.nextInt();
                testStringBuilder.append(aChar);
            }
            String testString = testStringBuilder.toString();
            char c = testStringBuilder.charAt(0);
            testStringBuilder.setCharAt(0, 'c');
            testStringBuilder.setCharAt(0, c);
            if (!testString.contentEquals(testStringBuilder.toString()))
                throw new RuntimeException("ContentsEqual failure");
        }
    }

    // Test a CharSequence that is not an AbstractStringBuilder,
    // this takes a different codepath in the new method in 1.5
    public static void testCharSequence() throws Exception {
        for (int i=0; i<ITERATIONS; i++) {
            int length = rnd.nextInt(STR_LEN) + 1;
            StringBuilder testStringBuilder = new StringBuilder();
            for(int x=0; x<length; x++) {
                char aChar = (char)rnd.nextInt();
                testStringBuilder.append(aChar);
            }
            String testString = testStringBuilder.toString();
            char c = testStringBuilder.charAt(0);
            testStringBuilder.setCharAt(0, 'c');
            testStringBuilder.setCharAt(0, c);
            CharBuffer buf = CharBuffer.wrap(testStringBuilder.toString());
            if (!testString.contentEquals(buf))
                throw new RuntimeException("ContentsEqual failure");
        }
    }
}
