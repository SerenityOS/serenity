/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4162796 4162796
 * @summary Test indexOf and lastIndexOf
 * @key randomness
 */

import java.util.Random;

public class IndexOf {

    static Random generator = new Random();
    private static boolean failure = false;

    public static void main(String[] args) throws Exception {
        simpleTest();
        compareIndexOfLastIndexOf();
        compareStringStringBuffer();

        if (failure)
           throw new RuntimeException("One or more BitSet failures.");
    }

    private static void report(String testName, int failCount) {
        System.err.println(testName+": " +
                         (failCount==0 ? "Passed":"Failed("+failCount+")"));
        if (failCount > 0)
            failure = true;
    }

    private static String generateTestString(int min, int max) {
        StringBuffer aNewString = new StringBuffer(120);
        int aNewLength = getRandomIndex(min, max);
        for(int y=0; y<aNewLength; y++) {
            int achar = generator.nextInt(30)+30;
            char test = (char)(achar);
            aNewString.append(test);
        }
        return aNewString.toString();
    }

    private static int getRandomIndex(int constraint1, int constraint2) {
        int range = constraint2 - constraint1;
        int x = generator.nextInt(range);
        return constraint1 + x;
    }

    private static void simpleTest() {
        int failCount = 0;
        String sourceString;
        StringBuffer sourceBuffer;
        String targetString;

        for (int i=0; i<10000; i++) {
            do {
                sourceString = generateTestString(99, 100);
                sourceBuffer = new StringBuffer(sourceString);
                targetString = generateTestString(10, 11);
            } while (sourceString.indexOf(targetString) != -1);

            int index1 = generator.nextInt(90) + 5;
            sourceBuffer = sourceBuffer.replace(index1, index1, targetString);

            if (sourceBuffer.indexOf(targetString) != index1)
                failCount++;
            if (sourceBuffer.indexOf(targetString, 5) != index1)
                failCount++;
            if (sourceBuffer.indexOf(targetString, 99) == index1)
                failCount++;
        }

        report("Basic Test                   ", failCount);
    }

    // Note: it is possible although highly improbable that failCount will
    // be > 0 even if everthing is working ok
    private static void compareIndexOfLastIndexOf() {
        int failCount = 0;
        String sourceString;
        StringBuffer sourceBuffer;
        String targetString;

        for (int i=0; i<10000; i++) {
            do {
                sourceString = generateTestString(99, 100);
                sourceBuffer = new StringBuffer(sourceString);
                targetString = generateTestString(10, 11);
            } while (sourceString.indexOf(targetString) != -1);

            int index1 = generator.nextInt(100);
            sourceBuffer = sourceBuffer.replace(index1, index1, targetString);

            // extremely remote possibility of > 1 match
            int matches = 0;
            int index2 = -1;
            while((index2 = sourceBuffer.indexOf(targetString,index2+1)) != -1)
                matches++;
            if (matches > 1)
                continue;

            if (sourceBuffer.indexOf(targetString) !=
                sourceBuffer.lastIndexOf(targetString))
                failCount++;
            sourceString = sourceBuffer.toString();
            if (sourceString.indexOf(targetString) !=
                sourceString.lastIndexOf(targetString))
                failCount++;
        }

        report("IndexOf vs LastIndexOf       ", failCount);
    }

    private static void compareStringStringBuffer() {
        int failCount = 0;

        for (int x=0; x<10000; x++) {
            String testString = generateTestString(1, 100);
            int len = testString.length();

            StringBuffer testBuffer = new StringBuffer(len);
            testBuffer.append(testString);
            if (!testString.equals(testBuffer.toString()))
                throw new RuntimeException("Initial equality failure");

            int x1 = 0;
            int x2 = 1000;
            while(x2 > testString.length()) {
                x1 = generator.nextInt(len);
                x2 = generator.nextInt(100);
                x2 = x1 + x2;
            }
            String fragment = testString.substring(x1,x2);

            int sAnswer = testString.indexOf(fragment);
            int sbAnswer = testBuffer.indexOf(fragment);

            if (sAnswer != sbAnswer)
                failCount++;

            int testIndex = getRandomIndex(-100, 100);

            sAnswer = testString.indexOf(fragment, testIndex);
            sbAnswer = testBuffer.indexOf(fragment, testIndex);

            if (sAnswer != sbAnswer)
                failCount++;

            sAnswer = testString.lastIndexOf(fragment);
            sbAnswer = testBuffer.lastIndexOf(fragment);

            if (sAnswer != sbAnswer)
                failCount++;

            testIndex = getRandomIndex(-100, 100);

            sAnswer = testString.lastIndexOf(fragment, testIndex);
            sbAnswer = testBuffer.lastIndexOf(fragment, testIndex);

            if (sAnswer != sbAnswer)
                failCount++;
        }

        report("String vs StringBuffer       ", failCount);
    }

}
