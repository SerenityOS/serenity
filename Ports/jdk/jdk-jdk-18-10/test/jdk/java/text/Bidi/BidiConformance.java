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
 * @bug 6850113 8032446 8255242
 * @summary confirm the behavior of new Bidi implementation. (Backward compatibility)
 * @modules java.desktop
 */

import java.awt.font.NumericShaper;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.text.Bidi;
import java.util.Arrays;

public class BidiConformance {

    /* internal flags */
    private static boolean error = false;
    private static boolean verbose = false;
    private static boolean abort = false;

    private static final byte MAX_EXPLICIT_LEVEL = 125;

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-verbose")) {
                verbose = true;
            } else if (arg.equals("-abort")) {
                abort = true;
            }
        }

        BidiConformance bc = new BidiConformance();
        bc.test();

        if (error) {
            throw new RuntimeException("Failed.");
        } else {
            System.out.println("Passed.");
        }
    }

    private void test() {
        testConstants();
        testConstructors();
        testMethods();

        testMethods4Constructor1();  // Bidi(AttributedCharacterIterator)
        testMethods4Constructor2();  // Bidi(String, int)
        testMethods4Constructor3();  // Bidi(char[], ...)
    }

    private void testConstants() {
        System.out.println("*** Test constants");

        checkResult("Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT",
                     -2, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        checkResult("Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT",
                     -1, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        checkResult("Bidi.DIRECTION_LEFT_TO_RIGHT",
                     0, Bidi.DIRECTION_LEFT_TO_RIGHT);
        checkResult("Bidi.DIRECTION_RIGHT_TO_LEFT",
                     1, Bidi.DIRECTION_RIGHT_TO_LEFT);
    }

    private void testConstructors() {
        System.out.println("*** Test constructors");

        testConstructor1();  // Bidi(AttributedCharacterIterator)
        testConstructor2();  // Bidi(String, int)
        testConstructor3();  // Bidi(char[], ...)
    }

    private void testMethods() {
        System.out.println("*** Test methods");

        testMethod_createLineBidi1();
        testMethod_createLineBidi2();
        testMethod_getLevelAt();
        testMethod_getRunLevel();
        testMethod_getRunLimit();
        testMethod_getRunStart();
        testMethod_reorderVisually1();
        testMethod_reorderVisually2();
        testMethod_requiresBidi();
    }

    private void testMethods4Constructor1() {
        System.out.println("*** Test methods for constructor 1");

        String paragraph;
        Bidi bidi;
        NumericShaper ns = NumericShaper.getShaper(NumericShaper.ARABIC);

        for (int textNo = 0; textNo < data4Constructor1.length; textNo++) {
            paragraph = data4Constructor1[textNo][0];
            int start = paragraph.indexOf('<')+1;
            int limit = paragraph.indexOf('>');
            int testNo;

            System.out.println("*** Test textNo=" + textNo +
                ": Bidi(AttributedCharacterIterator\"" +
                toReadableString(paragraph) + "\") " +
                "  start=" + start + ", limit=" + limit);

            // Test 0
            testNo = 0;
            System.out.println(" Test#" + testNo +": RUN_DIRECTION_LTR");
            AttributedString astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_LTR);
            bidi = new Bidi(astr.getIterator());

            callTestEachMethod4Constructor1(textNo, testNo, bidi);

            // Test 1
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIRECTION_LTR, BIDI_EMBEDDING(1)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_LTR);
            astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(1),
                              start, limit);
            bidi = new Bidi(astr.getIterator());
            callTestEachMethod4Constructor1(textNo, testNo, bidi);

            // Test 2
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIERCTION_LTR, BIDI_EMBEDDING(2)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_LTR);
            astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(2),
                              start, limit);
            bidi = new Bidi(astr.getIterator());
            callTestEachMethod4Constructor1(textNo, testNo, bidi);

            // Test 3
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIRECTIOIN_LTR, BIDI_EMBEDDING(-3)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_LTR);
            astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(-3),
                              start, limit);
            bidi = new Bidi(astr.getIterator());
            callTestEachMethod4Constructor1(textNo, testNo, bidi);

            // Test 4
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIRECTION_LTR, BIDI_EMBEDDING(-4)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_LTR);
            astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(-4),
                              start, limit);
            bidi = new Bidi(astr.getIterator());
            callTestEachMethod4Constructor1(textNo, testNo, bidi);

            // Test 5
            ++testNo;
            System.out.println(" Test#" + testNo + ": RUN_DIRECTION_RTL");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_RTL);
            bidi = new Bidi(astr.getIterator());
            callTestEachMethod4Constructor1(textNo, testNo, bidi);

            // Test 6
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIRECTION_RTL, BIDI_EMBEDDING(1)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_RTL);
            astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(1),
                              start, limit);
            try {
                bidi = new Bidi(astr.getIterator());
                callTestEachMethod4Constructor1(textNo, testNo, bidi);
            }
            catch (IllegalArgumentException e) {
                errorHandling("  Unexpected exception: " + e);
            }

            // Test 7
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIRECTION_RTL, BIDI_EMBEDDING(2)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_RTL);
            astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(2),
                              start, limit);
            try {
                bidi = new Bidi(astr.getIterator());
                callTestEachMethod4Constructor1(textNo, testNo, bidi);
            }
            catch (IllegalArgumentException e) {
                errorHandling("  Unexpected exception: " + e);
            }

            // Test 8
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIRECTION_RTL, BIDI_EMBEDDING(-3)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_RTL);
            astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(-3),
                              start, limit);
            try {
                bidi = new Bidi(astr.getIterator());
                callTestEachMethod4Constructor1(textNo, testNo, bidi);
            }
            catch (IllegalArgumentException e) {
                errorHandling("  Unexpected exception: " + e);
            }

            // Test 9
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIRECTION_RTL, BIDI_EMBEDDING(-4)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_RTL);
            astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(-4),
                              start, limit);
            try {
                bidi = new Bidi(astr.getIterator());
                callTestEachMethod4Constructor1(textNo, testNo, bidi);
            }
            catch (IllegalArgumentException e) {
                errorHandling("  Unexpected exception: " + e);
            }

            // Test 10
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": TextAttribute not specified");
            astr = new AttributedString(paragraph);
            bidi = new Bidi(astr.getIterator());
            callTestEachMethod4Constructor1(textNo, testNo, bidi);

            // Test 11
            ++testNo;
            System.out.println(" Test#" + testNo +
                ": RUN_DIRECTION_LTR, NUMERIC_SHAPING(ARABIC)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_LTR);
            astr.addAttribute(TextAttribute.NUMERIC_SHAPING, ns);
            bidi = new Bidi(astr.getIterator());
            callTestEachMethod4Constructor1(textNo, testNo, bidi);

            // Test 12
            ++testNo;
            System.out.println(" Test#" + testNo +
                 ": RUN_DIRECTION_RTL, NUMERIC_SHAPING(ARABIC)");
            astr = new AttributedString(paragraph);
            astr.addAttribute(TextAttribute.RUN_DIRECTION,
                              TextAttribute.RUN_DIRECTION_RTL);
            astr.addAttribute(TextAttribute.NUMERIC_SHAPING, ns);
            bidi = new Bidi(astr.getIterator());
            callTestEachMethod4Constructor1(textNo, testNo, bidi);
        }
    }

    private void testMethods4Constructor2() {
        System.out.println("*** Test methods for constructor 2");

        String paragraph;
        Bidi bidi;

        for (int textNo = 0; textNo < data4Constructor2.length; textNo++) {
            paragraph = data4Constructor2[textNo][0];
            for (int flagNo = 0; flagNo < FLAGS.length; flagNo++) {
                int flag = FLAGS[flagNo];

                System.out.println("*** Test textNo=" + textNo +
                    ": Bidi(\"" + toReadableString(paragraph) +
                    "\", " + getFlagName(flag) + ")");

                bidi = new Bidi(paragraph, flag);
                callTestEachMethod4Constructor2(textNo, flagNo, bidi);
            }
        }
    }

    private void testMethods4Constructor3() {
        System.out.println("*** Test methods for constructor 3");

        String paragraph;
        Bidi bidi;

        for (int textNo = 0; textNo < data4Constructor3.length; textNo++) {
            paragraph = data4Constructor3[textNo][0];
            char[] c = paragraph.toCharArray();
            int start = paragraph.indexOf('<')+1;
            byte[][] embeddings = (c.length < emb4Constructor3[1][0].length) ?
                                  emb4Constructor3[0] : emb4Constructor3[1];
            for (int flagNo = 0; flagNo < FLAGS.length; flagNo++) {
                int flag = FLAGS[flagNo];
                for (int embNo = 0; embNo < embeddings.length; embNo++) {
                    int dataNo = flagNo * FLAGS.length + embNo;

                    System.out.println("*** Test textNo=" + textNo +
                        ": Bidi(char[]\"" + toReadableString(paragraph) +
                        "\", 0, embeddings={" + toString(embeddings[embNo]) +
                        "}, " + c.length + ", " +
                       getFlagName(flag) + ")" + "  dataNo=" + dataNo);

                    try {
                        bidi = new Bidi(c, 0, embeddings[embNo], 0,
                                        c.length, flag);
                        callTestEachMethod4Constructor3(textNo, dataNo, bidi);
                    }
                    catch (Exception e) {
                        errorHandling("  Unexpected exception: " + e);
                    }
                }
            }
        }
    }

    private void testConstructor1() {
        Bidi bidi;

        try {
            bidi = new Bidi(null);
            errorHandling("Bidi((AttributedCharacterIterator)null) " +
                "should throw an IAE.");
        }
        catch (IllegalArgumentException e) {
        }
        catch (NullPointerException e) {
            errorHandling("Bidi((AttributedCharacterIterator)null) " +
                "should not throw an NPE but an IAE.");
        }

        String paragraph = data4Constructor1[1][0];
        int start = paragraph.indexOf('<')+1;
        int limit = paragraph.indexOf('>');
        AttributedString astr = new AttributedString(paragraph);
        astr.addAttribute(TextAttribute.RUN_DIRECTION,
                          TextAttribute.RUN_DIRECTION_RTL);
        astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(-MAX_EXPLICIT_LEVEL),
                          start, limit);
        try {
            bidi = new Bidi(astr.getIterator());
            for (int i = start; i < limit; i++) {
                if (bidi.getLevelAt(i) != MAX_EXPLICIT_LEVEL) {
                    errorHandling("Bidi(AttributedCharacterIterator).getLevelAt(" +
                        i + ") should not be " + bidi.getLevelAt(i) +
                        " but MAX_EXPLICIT_LEVEL-1 when BIDI_EMBEDDING is -MAX_EXPLICIT_LEVEL.");
                }
            }
        }
        catch (Exception e) {
            errorHandling("  Unexpected exception: " + e);
        }

        astr = new AttributedString(paragraph);
        astr.addAttribute(TextAttribute.RUN_DIRECTION,
                          TextAttribute.RUN_DIRECTION_RTL);
        astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(-(MAX_EXPLICIT_LEVEL+1)),
                          start, limit);
        try {
            bidi = new Bidi(astr.getIterator());
            for (int i = start; i < limit; i++) {
                if (bidi.getLevelAt(i) != 1) {
                    errorHandling("Bidi(AttributedCharacterIterator).getLevelAt() " +
                        "should be 1 when BIDI_EMBEDDING is -(MAX_EXPLICIT_LEVEL+1).");
                }
            }
        }
        catch (Exception e) {
            errorHandling("  Unexpected exception: " + e);
        }

        astr = new AttributedString(paragraph);
        astr.addAttribute(TextAttribute.RUN_DIRECTION,
                          TextAttribute.RUN_DIRECTION_RTL);
        astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(MAX_EXPLICIT_LEVEL-1),
                          start, limit);
        try {
            bidi = new Bidi(astr.getIterator());
            for (int i = start; i < limit; i++) {
                if (bidi.getLevelAt(i) != MAX_EXPLICIT_LEVEL) {
                    errorHandling("Bidi(AttributedCharacterIterator).getLevelAt() " +
                        "should be MAX_EXPLICIT_LEVEL when BIDI_EMBEDDING is MAX_EXPLICIT_LEVEL-1.");
                }
            }
        }
        catch (Exception e) {
            errorHandling("  Unexpected exception: " + e);
        }

        astr = new AttributedString(paragraph);
        astr.addAttribute(TextAttribute.RUN_DIRECTION,
                          TextAttribute.RUN_DIRECTION_RTL);
        astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(MAX_EXPLICIT_LEVEL),
                          start, limit);
        try {
            bidi = new Bidi(astr.getIterator());
            for (int i = start; i < limit; i++) {
                if (bidi.getLevelAt(i) != MAX_EXPLICIT_LEVEL) {
                    errorHandling("Bidi(AttributedCharacterIterator).getLevelAt(" +
                        i + ") should not be " + bidi.getLevelAt(i) +
                        " but MAX_EXPLICIT_LEVEL when BIDI_EMBEDDING is MAX_EXPLICIT_LEVEL.");
                }
            }
        }
        catch (Exception e) {
            errorHandling("  Unexpected exception: " + e);
        }

        astr = new AttributedString(paragraph);
        astr.addAttribute(TextAttribute.RUN_DIRECTION,
                          TextAttribute.RUN_DIRECTION_RTL);
        astr.addAttribute(TextAttribute.BIDI_EMBEDDING, new Integer(MAX_EXPLICIT_LEVEL+1),
                          start, limit);
        try {
            bidi = new Bidi(astr.getIterator());
            for (int i = start; i < limit; i++) {
                if (bidi.getLevelAt(i) != 1) {
                    errorHandling("Bidi(AttributedCharacterIterator).getLevelAt(" +
                         i + ") should not be " + bidi.getLevelAt(i) +
                        " but 1 when BIDI_EMBEDDING is MAX_EXPLICIT_LEVEL+1.");
                }
            }
        }
        catch (Exception e) {
            errorHandling("  Unexpected exception: " + e);
        }
    }

    private void testConstructor2() {
        Bidi bidi;

        try {
            bidi = new Bidi(null, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            errorHandling("Bidi((String)null, DIRECTION_DEFAULT_LEFT_TO_RIGHT)" +
                " should throw an IAE.");
        }
        catch (IllegalArgumentException e) {
        }
        catch (NullPointerException e) {
            errorHandling("Bidi((String)null, DIRECTION_DEFAULT_LEFT_TO_RIGHT) " +
                "should not throw an NPE but an IAE.");
        }

        try {
            bidi = new Bidi("abc", -3);
        }
        catch (Exception e) {
            errorHandling("Bidi(\"abc\", -3) should not throw an exception: " +
                e);
        }

        try {
            bidi = new Bidi("abc", 2);
        }
        catch (Exception e) {
            errorHandling("Bidi(\"abc\", 2) should not throw an exception: " +
                e);
        }
    }

    private void testConstructor3() {
        char[] text = {'a', 'b', 'c', 'd', 'e'};
        byte[] embeddings = {0, 0, 0, 0, 0};
        Bidi bidi;

        try {
            bidi = new Bidi(null, 0, embeddings, 0, 5,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            errorHandling("Bidi(char[], ...) should throw an IAE " +
                "when text=null.");
        }
        catch (IllegalArgumentException e) {
        }
        catch (NullPointerException e) {
            errorHandling("Bidi(char[], ...) should not throw an NPE " +
                "but an IAE when text=null.");
        }

        try {
            bidi = new Bidi(text, -1, embeddings, 0, 5,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            errorHandling("Bidi(char[], ...) should throw an IAE " +
                "when textStart is incorrect(-1: too small).");
        }
        catch (IllegalArgumentException e) {
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("Bidi(char[], ...) should not throw an NPE " +
                "but an IAE when textStart is incorrect(-1: too small).");
        }

        try {
            bidi = new Bidi(text, 4, embeddings, 0, 2,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            errorHandling("Bidi(char[], ...) should throw an IAE " +
                "when textStart is incorrect(4: too large).");
        }
        catch (IllegalArgumentException e) {
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("Bidi(char[], ...) should not throw an NPE " +
                "but an IAE when textStart is incorrect(4: too large).");
        }

        byte[] actualLevels = new byte[text.length];
        byte[] validEmbeddings1 = {0, -MAX_EXPLICIT_LEVEL, -(MAX_EXPLICIT_LEVEL-1), -2, -1};
        byte[] expectedLevels1  = {0,  MAX_EXPLICIT_LEVEL,  MAX_EXPLICIT_LEVEL-1,  2,  1};
        try {
            bidi = new Bidi(text, 0, validEmbeddings1, 0, 5,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            for (int i = 0; i < text.length; i++) {
                actualLevels[i] = (byte)bidi.getLevelAt(i);
            }
            if (!Arrays.equals(expectedLevels1, actualLevels)) {
                errorHandling("Bidi(char[], ...).getLevelAt()" +
                    " should be {" + toString(actualLevels) +
                    "} when embeddings are {" +
                    toString(expectedLevels1) + "}.");
            }
        }
        catch (Exception e) {
            errorHandling("Bidi(char[], ...) should not throw an exception " +
                "when embeddings is valid(-MAX_EXPLICIT_LEVEL).");
        }

        byte[] validEmbeddings2 = {0,  MAX_EXPLICIT_LEVEL,  MAX_EXPLICIT_LEVEL-1,  2,  1};
        byte[] expectedLevels2  = {0,  MAX_EXPLICIT_LEVEL+1,  MAX_EXPLICIT_LEVEL-1,  2,  2};
        try {
            bidi = new Bidi(text, 0, validEmbeddings2, 0, 5,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            for (int i = 0; i < text.length; i++) {
                actualLevels[i] = (byte)bidi.getLevelAt(i);
            }
            if (!Arrays.equals(expectedLevels2, actualLevels)) {
                errorHandling("Bidi(char[], ...).getLevelAt()" +
                    " should be {" + toString(actualLevels) +
                    "} when embeddings are {" +
                    toString(expectedLevels2) + "}.");
            }
        }
        catch (Exception e) {
            errorHandling("Bidi(char[], ...) should not throw an exception " +
                "when embeddings is valid(MAX_EXPLICIT_LEVEL).");
        }

        byte[] invalidEmbeddings1 = {0, -(MAX_EXPLICIT_LEVEL+1), 0, 0, 0};
        try {
            bidi = new Bidi(text, 0, invalidEmbeddings1, 0, 5,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            if (bidi.getLevelAt(1) != 0) {
                errorHandling("Bidi(char[], ...).getLevelAt(1) should be 0 " +
                    "when embeddings[1] is -(MAX_EXPLICIT_LEVEL+1).");
            }
        }
        catch (Exception e) {
            errorHandling("Bidi(char[], ...) should not throw an exception " +
                "even when embeddings includes -(MAX_EXPLICIT_LEVEL+1).");
        }

        byte[] invalidEmbeddings2 = {0, MAX_EXPLICIT_LEVEL+1, 0, 0, 0};
        try {
            bidi = new Bidi(text, 0, invalidEmbeddings2, 0, 5,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            if (bidi.getLevelAt(1) != 0) {
                errorHandling("Bidi(char[], ...).getLevelAt(1) should be 0 " +
                    "when embeddings[1] is MAX_EXPLICIT_LEVEL+1.");
            }
        }
        catch (Exception e) {
            errorHandling("Bidi(char[], ...) should not throw an exception " +
                "even when embeddings includes MAX_EXPLICIT_LEVEL+1.");
        }

        try {
            bidi = new Bidi(text, 0, embeddings, 0, -1,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            errorHandling("Bidi(char[], ...) should throw an IAE " +
                "when paragraphLength=-1(too small).");
        }
        catch (IllegalArgumentException e) {
        }
        catch (NegativeArraySizeException e) {
            errorHandling("Bidi(char[], ...) should not throw an NASE " +
                "but an IAE when paragraphLength=-1(too small).");
        }

        try {
            bidi = new Bidi(text, 0, embeddings, 0, 6,
                            Bidi.DIRECTION_LEFT_TO_RIGHT);
            errorHandling("Bidi(char[], ...) should throw an IAE " +
                "when paragraphLength=6(too large).");
        }
        catch (IllegalArgumentException e) {
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("Bidi(char[], ...) should not throw an AIOoBE " +
                "but an IAE when paragraphLength=6(too large).");
        }

        try {
            bidi = new Bidi(text, 0, embeddings, 0, 4, -3);
        }
        catch (Exception e) {
            errorHandling("Bidi(char[], ...) should not throw an exception " +
                "even when flag=-3(too small).");
        }

        try {
            bidi = new Bidi(text, 0, embeddings, 0, 5, 2);
        }
        catch (Exception e) {
            errorHandling("Bidi(char[], ...) should not throw an exception " +
                "even when flag=2(too large).");
        }
    }

    private void callTestEachMethod4Constructor1(int textNo,
                                                 int testNo,
                                                 Bidi bidi) {
        testEachMethod(bidi,
                       data4Constructor1[textNo][0],
                       data4Constructor1[textNo][testNo+1],
                       baseIsLTR4Constructor1[textNo][testNo],
                       isLTR_isRTL4Constructor1[textNo][0][testNo],
                       isLTR_isRTL4Constructor1[textNo][1][testNo]);
    }

    private void callTestEachMethod4Constructor2(int textNo,
                                                 int flagNo,
                                                 Bidi bidi) {
        testEachMethod(bidi,
                       data4Constructor2[textNo][0],
                       data4Constructor2[textNo][flagNo+1],
                       baseIsLTR4Constructor2[textNo][flagNo],
                       isLTR_isRTL4Constructor2[textNo][0][flagNo],
                       isLTR_isRTL4Constructor2[textNo][1][flagNo]);
    }

    private void callTestEachMethod4Constructor3(int textNo,
                                                 int dataNo,
                                                 Bidi bidi) {
        testEachMethod(bidi,
                       data4Constructor3[textNo][0],
                       data4Constructor3[textNo][dataNo+1],
                       baseIsLTR4Constructor3[textNo][dataNo],
                       isLTR_isRTL4Constructor3[textNo][0][dataNo],
                       isLTR_isRTL4Constructor3[textNo][1][dataNo]);
    }

    private StringBuilder sb = new StringBuilder();
    private void testEachMethod(Bidi bidi,
                                String text,
                                String expectedLevels,
                                boolean expectedBaseIsLTR,
                                boolean expectedIsLTR,
                                boolean expectedIsRTL
                               ) {
        /* Test baseIsLeftToRight() */
        boolean actualBoolean = bidi.baseIsLeftToRight();
        checkResult("baseIsLeftToRight()", expectedBaseIsLTR, actualBoolean);

        /* Test getBaseLevel() */
        int expectedInt = (expectedBaseIsLTR) ? 0 : 1;
        int actualInt = bidi.getBaseLevel();
        checkResult("getBaseLevel()", expectedInt, actualInt);

        /* Test getLength() */
        expectedInt = text.length();
        actualInt = bidi.getLength();
        checkResult("getLength()", expectedInt, actualInt);

        /* Test getLevelAt() */
        sb.setLength(0);
        for (int i = 0; i < text.length(); i++) {
            sb.append(bidi.getLevelAt(i));
        }
        checkResult("getLevelAt()", expectedLevels, sb.toString());

        /* Test getRunCount() */
        expectedInt = getRunCount(expectedLevels);
        actualInt = bidi.getRunCount();
        checkResult("getRunCount()", expectedInt, actualInt);

        /* Test getRunLevel(), getRunLimit() and getRunStart() */
        if (expectedInt == actualInt) {
            int runCount = expectedInt;
            int[] expectedRunLevels = getRunLevels_int(runCount, expectedLevels);
            int[] expectedRunLimits = getRunLimits(runCount, expectedLevels);
            int[] expectedRunStarts = getRunStarts(runCount, expectedLevels);
            int[] actualRunLevels = new int[runCount];
            int[] actualRunLimits = new int[runCount];
            int[] actualRunStarts = new int[runCount];

            for (int k = 0; k < runCount; k++) {
                actualRunLevels[k] = bidi.getRunLevel(k);
                actualRunLimits[k] = bidi.getRunLimit(k);
                actualRunStarts[k] = bidi.getRunStart(k);
            }

            checkResult("getRunLevel()", expectedRunLevels, actualRunLevels);
            checkResult("getRunStart()", expectedRunStarts, actualRunStarts);
            checkResult("getRunLimit()", expectedRunLimits, actualRunLimits);
        }

        /* Test isLeftToRight() */
        boolean expectedBoolean = expectedIsLTR;
        actualBoolean = bidi.isLeftToRight();
        checkResult("isLeftToRight()", expectedBoolean, actualBoolean);

        /* Test isMixed() */
        expectedBoolean = !(expectedIsLTR || expectedIsRTL);
        actualBoolean = bidi.isMixed();
        checkResult("isMixed()", expectedBoolean, actualBoolean);

        /* Test isRightToLeft() */
        expectedBoolean = expectedIsRTL;
        actualBoolean = bidi.isRightToLeft();
        checkResult("isRightToLeft()", expectedBoolean, actualBoolean);
    }

    private int getRunCount(String levels) {
        int len = levels.length();
        char c = levels.charAt(0);
        int runCount = 1;

        for (int index = 1; index < len; index++) {
            if (levels.charAt(index) != c) {
                runCount++;
                c = levels.charAt(index);
            }
        }

        return runCount;
    }

    private int[] getRunLevels_int(int runCount, String levels) {
        int[] array = new int[runCount];
        int len = levels.length();
        char c = levels.charAt(0);
        int i = 0;
        array[i++] = c - '0';

        for (int index = 1; index < len; index++) {
            if (levels.charAt(index) != c) {
                c = levels.charAt(index);
                array[i++] = c - '0';
            }
        }

        return array;
    }

    private byte[] getRunLevels_byte(int runCount, String levels) {
        byte[] array = new byte[runCount];
        int len = levels.length();
        char c = levels.charAt(0);
        int i = 0;
        array[i++] = (byte)(c - '0');

        for (int index = 1; index < len; index++) {
            if (levels.charAt(index) != c) {
                c = levels.charAt(index);
                array[i++] = (byte)(c - '0');
            }
        }

        return array;
    }

    private int[] getRunLimits(int runCount, String levels) {
        int[] array = new int[runCount];
        int len = levels.length();
        char c = levels.charAt(0);
        int i = 0;

        for (int index = 1; index < len; index++) {
            if (levels.charAt(index) != c) {
                c = levels.charAt(index);
                array[i++] = index;
            }
        }
        array[i] = len;

        return array;
    }

    private int[] getRunStarts(int runCount, String levels) {
        int[] array = new int[runCount];
        int len = levels.length();
        char c = levels.charAt(0);
        int i = 1;

        for (int index = 1; index < len; index++) {
            if (levels.charAt(index) != c) {
                c = levels.charAt(index);
                array[i++] = index;
            }
        }

        return array;
    }

    private String[] getObjects(int runCount, String text, String levels) {
        String[] array = new String[runCount];
        int[] runLimits = getRunLimits(runCount, levels);
        int runStart = 0;

        for (int i = 0; i < runCount; i++) {
            array[i] = text.substring(runStart, runLimits[i]);
            runStart = runLimits[i];
        }

        return array;
    }

    private void testMethod_createLineBidi1() {
        System.out.println("*** Test createLineBidi() 1");

        String str = " ABC 123. " + HebrewABC + " " + NKo123 + ". ABC 123";

        int lineStart = str.indexOf('.') + 2;
        int lineLimit = str.lastIndexOf('.') + 2;
        Bidi bidi = new Bidi(str, FLAGS[0]);
        Bidi lineBidi = bidi.createLineBidi(lineStart, lineLimit);

        checkResult("getBaseLevel()",
            bidi.getBaseLevel(), lineBidi.getBaseLevel());
        checkResult("getLevelAt(5)",
            bidi.getLevelAt(lineStart+5), lineBidi.getLevelAt(5));
    }

    private void testMethod_createLineBidi2() {
        System.out.println("*** Test createLineBidi() 2");

        Bidi bidi = new Bidi(data4Constructor1[0][0], FLAGS[0]);
        int len = data4Constructor1[0][0].length();

        try {
            Bidi lineBidi = bidi.createLineBidi(0, len);
        }
        catch (Exception e) {
            errorHandling("createLineBidi(0, textLength)" +
                " should not throw an exception.");
        }

        try {
            Bidi lineBidi = bidi.createLineBidi(-1, len);
            errorHandling("createLineBidi(-1, textLength)" +
                " should throw an IAE.");
        }
        catch (IllegalArgumentException e) {
        }

        try {
            Bidi lineBidi = bidi.createLineBidi(0, len+1);
            errorHandling("createLineBidi(0, textLength+1)" +
                " should throw an IAE.");
        }
        catch (IllegalArgumentException e) {
        }
    }

    /*
     * Confirm that getLevelAt() doesn't throw an exception for invalid offset
     * unlike ICU4J.
     */
    private void testMethod_getLevelAt() {
        System.out.println("*** Test getLevelAt()");

        Bidi bidi = new Bidi(data4Constructor1[1][0], FLAGS[0]);
        int len = data4Constructor1[1][0].length();

        try {
            int level = bidi.getLevelAt(-1);
            if (level != bidi.getBaseLevel()) {
                errorHandling("getLevelAt(-1) returned a wrong level." +
                    " Expected=" + bidi.getBaseLevel() + ", got=" + level);
            }
        }
        catch (Exception e) {
            errorHandling("getLevelAt(-1) should not throw an exception.");
        }

        try {
            int level = bidi.getLevelAt(len+1);
            if (level != bidi.getBaseLevel()) {
                errorHandling("getLevelAt(textLength+1)" +
                    " returned a wrong level." +
                    " Expected=" + bidi.getBaseLevel() + ", got=" + level);
            }
        }
        catch (Exception e) {
            errorHandling("getLevelAt(-1) should not throw an exception.");
        }
    }

    private void testMethod_getRunLevel() {
        System.out.println("*** Test getRunLevel()");

        String str = "ABC 123";
        Bidi bidi = new Bidi(str, Bidi.DIRECTION_LEFT_TO_RIGHT);
        try {
            if (bidi.getRunLevel(-1) != 0 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 0 ||   // runCount - 1
                bidi.getRunLevel(1) != 0 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 0) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = "ABC " + HebrewABC + " 123";
        bidi = new Bidi(str, Bidi.DIRECTION_LEFT_TO_RIGHT);
        try {
            if (bidi.getRunLevel(-1) != 0 ||  // runCount - 4 (out of range)
                bidi.getRunLevel(0) != 0 ||   // runCount - 3
                bidi.getRunLevel(1) != 1 ||   // runCount - 2
                bidi.getRunLevel(2) != 2 ||   // runCount - 1
                bidi.getRunLevel(3) != 0 ||   // runCount     (out of range)
                bidi.getRunLevel(4) != 0) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = "ABC";
        bidi = new Bidi(str, Bidi.DIRECTION_LEFT_TO_RIGHT);
        try {
            if (bidi.getRunLevel(-1) != 0 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 0 ||   // runCount - 1
                bidi.getRunLevel(1) != 0 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 0) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = "ABC";
        bidi = new Bidi(str, Bidi.DIRECTION_RIGHT_TO_LEFT);
        try {
            if (bidi.getRunLevel(-1) != 1 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 2 ||   // runCount - 1
                bidi.getRunLevel(1) != 1 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 1) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = "ABC";
        bidi = new Bidi(str, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        try {
            if (bidi.getRunLevel(-1) != 0 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 0 ||   // runCount - 1
                bidi.getRunLevel(1) != 0 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 0) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = "ABC";
        bidi = new Bidi(str, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        try {
            if (bidi.getRunLevel(-1) != 0 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 0 ||   // runCount - 1
                bidi.getRunLevel(1) != 0 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 0) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = HebrewABC;
        bidi = new Bidi(str, Bidi.DIRECTION_LEFT_TO_RIGHT);
        try {
            if (bidi.getRunLevel(-1) != 0 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 1 ||   // runCount - 1
                bidi.getRunLevel(1) != 0 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 0) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = HebrewABC;
        bidi = new Bidi(str, Bidi.DIRECTION_RIGHT_TO_LEFT);
        try {
            if (bidi.getRunLevel(-1) != 1 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 1 ||   // runCount - 1
                bidi.getRunLevel(1) != 1 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 1) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = HebrewABC;
        bidi = new Bidi(str, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        try {
            if (bidi.getRunLevel(-1) != 1 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 1 ||   // runCount - 1
                bidi.getRunLevel(1) != 1 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 1) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }

        str = HebrewABC;
        bidi = new Bidi(str, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        try {
            if (bidi.getRunLevel(-1) != 1 ||  // runCount - 2 (out of range)
                bidi.getRunLevel(0) != 1 ||   // runCount - 1
                bidi.getRunLevel(1) != 1 ||   // runCount     (out of range)
                bidi.getRunLevel(2) != 1) {   // runCount + 1 (out of range)
                errorHandling("Incorrect getRunLevel() value(s).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLevel() should not throw an exception: " + e);
        }
    }

    private void testMethod_getRunLimit() {
        System.out.println("*** Test getRunLimit()");

        String str = "ABC 123";
        int length = str.length();
        Bidi bidi = new Bidi(str, Bidi.DIRECTION_LEFT_TO_RIGHT);

        try {
            if (bidi.getRunLimit(-1) != length ||  // runCount - 2
                bidi.getRunLimit(0) != length ||   // runCount - 1
                bidi.getRunLimit(1) != length ||   // runCount
                bidi.getRunLimit(2) != length) {   // runCount + 1
                errorHandling("getRunLimit() should return " + length +
                    " when getRunCount() is 1.");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLimit() should not throw an exception " +
                "when getRunCount() is 1.");
        }

        str = "ABC " + ArabicABC + " 123";
        length = str.length();
        bidi = new Bidi(str, Bidi.DIRECTION_LEFT_TO_RIGHT);

        try {
            bidi.getRunLimit(-1);
            errorHandling("getRunLimit() should throw an AIOoBE " +
                "when run is -1(too small).");
        }
        catch (ArrayIndexOutOfBoundsException e) {
        }
        catch (IllegalArgumentException e) {
            errorHandling("getRunLimit() should not throw an IAE " +
                "but an AIOoBE when run is -1(too small).");
        }

        try {
            bidi.getRunLimit(0);
            bidi.getRunLimit(1);
            bidi.getRunLimit(2);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("getRunLimit() should not throw an AIOOBE " +
                "when run is from 0 to 2(runCount-1).");
        }

        try {
            bidi.getRunLimit(3);
            errorHandling("getRunLimit() should throw an AIOoBE " +
                "when run is 3(same as runCount).");
        }
        catch (ArrayIndexOutOfBoundsException e) {
        }
        catch (IllegalArgumentException e) {
            errorHandling("getRunLimit() should not throw an IAE " +
                "but an AIOoBE when run is 3(same as runCount).");
        }
    }

    private void testMethod_getRunStart() {
        System.out.println("*** Test getRunStart()");

        String str = "ABC 123";
        int length = str.length();
        Bidi bidi = new Bidi(str, Bidi.DIRECTION_LEFT_TO_RIGHT);

        try {
            if (bidi.getRunStart(-1) != 0 ||  // runCount - 2
                bidi.getRunStart(0) != 0 ||   // runCount - 1
                bidi.getRunStart(1) != 0 ||   // runCount
                bidi.getRunStart(2) != 0) {   // runCount + 1
                errorHandling("getRunStart() should return 0" +
                    " when getRunCount() is 1.");
            }
        }
        catch (Exception e) {
            errorHandling("getRunLimit() should not throw an exception" +
                " when getRunCount() is 1.");
        }

        str = "ABC " + NKoABC + " 123";
        length = str.length();
        bidi = new Bidi(str, Bidi.DIRECTION_LEFT_TO_RIGHT);

        try {
            bidi.getRunStart(-1);
            errorHandling("getRunStart() should throw an AIOoBE" +
                " when run is -1(too small).");
        }
        catch (ArrayIndexOutOfBoundsException e) {
        }
        catch (IllegalArgumentException e) {
            errorHandling("getRunStart() should not throw an IAE " +
                "but an AIOoBE when run is -1(too small).");
        }

        try {
            bidi.getRunStart(0);
            bidi.getRunStart(1);
            bidi.getRunStart(2);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("getRunStart() should not throw an AIOOBE " +
                "when run is from 0 to 2(runCount-1).");
        }

        try {
            if (bidi.getRunStart(3) != length) {
                errorHandling("getRunStart() should return " + length +
                    " when run is 3(same as runCount).");
            }
        }
        catch (Exception e) {
            errorHandling("getRunStart() should not throw an exception " +
                "when run is 3(same as runCount).");
        }

        try {
            bidi.getRunStart(4);
            errorHandling("getRunStart() should throw an AIOoBE " +
                "when run is runCount+1(too large).");
        }
        catch (ArrayIndexOutOfBoundsException e) {
        }
        catch (IllegalArgumentException e) {
            errorHandling("getRunStart() should not throw an IAE " +
                "but an AIOoBE when run is runCount+1(too large).");
        }
    }

    private void testMethod_reorderVisually1() {
        System.out.println("*** Test reorderVisually() 1");

        for (int textNo = 0; textNo < data4reorderVisually.length; textNo++) {
            Object[] objects = data4reorderVisually[textNo][0];
            byte[] levels = getLevels(data4reorderVisually[textNo]);
            Object[] expectedObjects = data4reorderVisually[textNo][2];

            Bidi.reorderVisually(levels, 0, objects, 0, objects.length);

            checkResult("textNo=" + textNo + ": reorderVisually(levels=[" +
                toString(levels) + "], objects=[" + toString(objects) + "])",
                expectedObjects, objects);
        }
    }

    private void testMethod_reorderVisually2() {
        System.out.println("*** Test reorderVisually() 2");

        Object[] objects = data4reorderVisually[0][0];
        byte[] levels = getLevels(data4reorderVisually[0]);
        int count = objects.length;
        int llen = levels.length;
        int olen = objects.length;

        try {
            Bidi.reorderVisually(null, 0, objects, 0, count);
            errorHandling("reorderVisually() should throw a NPE " +
                "when levels is null.");
        }
        catch (NullPointerException e) {
        }

        try {
            Bidi.reorderVisually(levels, -1, objects, 0, count);
            errorHandling("reorderVisually() should throw an IAE " +
                "when levelStart is -1.");
        }
        catch (IllegalArgumentException e) {
            if (!e.getMessage().equals(
                            "Value levelStart -1 is out of range 0 to " + (llen - 1))) {
                errorHandling("reorderVisually() should throw an IAE" +
                        " mentioning levelStart is beyond the levels range. Message: " + e.getMessage());
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("reorderVisually() should not throw an AIOoBE " +
                "but an IAE when levelStart is -1.");
        }

        try {
            Bidi.reorderVisually(levels, llen, objects, 0, count);
            errorHandling("reorderVisually() should throw an IAE " +
                "when levelStart is 6(levels.length).");
        }
        catch (IllegalArgumentException e) {
            if (!e.getMessage().equals(
                    "Value levelStart " + llen + " is out of range 0 to " + (llen - 1))) {
                errorHandling("reorderVisually() should throw an IAE" +
                        " mentioning levelStart is beyond the levels range. Message: " + e.getMessage());
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("reorderVisually() should not throw an AIOoBE " +
                "but an IAE when levelStart is 6(levels.length).");
        }

        try {
            Bidi.reorderVisually(levels, 0, null, 0, count);
            errorHandling("reorderVisually() should throw a NPE" +
                " when objects is null.");
        }
        catch (NullPointerException e) {
        }

        try {
            Bidi.reorderVisually(levels, 0, objects, -1, count);
            errorHandling("reorderVisually() should throw an IAE" +
                " when objectStart is -1.");
        }
        catch (IllegalArgumentException e) {
            if (!e.getMessage().equals(
                    "Value objectStart -1 is out of range 0 to " + (olen - 1))) {
                errorHandling("reorderVisually() should throw an IAE" +
                        " mentioning objectStart is beyond the objects range. Message: " + e.getMessage());
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("reorderVisually() should not throw an AIOoBE " +
                "but an IAE when objectStart is -1.");
        }

        try {
            Bidi.reorderVisually(levels, 0, objects, 6, objects.length);
            errorHandling("reorderVisually() should throw an IAE " +
                "when objectStart is 6(objects.length).");
        }
        catch (IllegalArgumentException e) {
            if (!e.getMessage().equals(
                    "Value objectStart 6 is out of range 0 to " + (olen - 1))) {
                errorHandling("reorderVisually() should throw an IAE" +
                        " mentioning objectStart is beyond the objects range. Message: " + e.getMessage());
            }
        }

        try {
            Bidi.reorderVisually(levels, 0, objects, 0, -1);
            errorHandling("reorderVisually() should throw an IAE " +
                "when count is -1.");
        }
        catch (IllegalArgumentException e) {
            if (!e.getMessage().equals(
                    "Value count -1 is less than zero, or objectStart + count " +
                    "is beyond objects length " + olen)) {
                errorHandling("reorderVisually() should throw an IAE" +
                        " mentioning objectStart/count is beyond the objects range. Message: " + e.getMessage());
            }
        }
        catch (NegativeArraySizeException e) {
            errorHandling("reorderVisually() should not throw an NASE " +
                "but an IAE when count is -1.");
        }

        try {
            Bidi.reorderVisually(levels, 0, objects, 0, count+1);
            errorHandling("reorderVisually() should throw an IAE " +
                "when count is 7(objects.length+1).");
        }
        catch (IllegalArgumentException e) {
            if (!e.getMessage().equals(
                    "Value count " + (count + 1) + " is less than zero, or objectStart + count " +
                    "is beyond objects length " + olen)) {
                errorHandling("reorderVisually() should throw an IAE" +
                        " mentioning objectStart/count is beyond the objects range. Message: " + e.getMessage());
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("reorderVisually() should not throw an AIOoBE " +
                "but an IAE when count is 7(objects.length+1).");
        }

        try {
            Bidi.reorderVisually(levels, 0, objects, 0, 0);
            checkResult("reorderVisually(count=0)",
                data4reorderVisually[0][0], objects);
        }
        catch (Exception e) {
            errorHandling("reorderVisually() should not throw an exception" +
                " when count is 0.");
        }
    }

    private void testMethod_requiresBidi() {
        System.out.println("*** Test requiresBidi()");

        String paragraph;
        char[] text;
        Bidi bidi;

        for (int textNo = 0; textNo < data4Constructor2.length; textNo++) {
            paragraph = data4Constructor2[textNo][0];
            text = paragraph.toCharArray();
            boolean rBidi = Bidi.requiresBidi(text, 0, text.length);
            if (rBidi != requiresBidi4Constructor2[textNo]) {
                error = true;
                System.err.println("Unexpected requiresBidi() value" +
                    " for requiresBidi(\"" + paragraph + "\", " + 0 + ", " +
                    text.length + ")." +
                    "\n    Expected: " + requiresBidi4Constructor2[textNo] +
                    "\n    Got     : " + rBidi);
            } else if (verbose) {
                System.out.println("  Okay : requiresBidi() for" +
                    " requiresBidi(\"" + paragraph + "\", " + 0 + ", " +
                    text.length + ")  Got: " + rBidi);
            }
        }

        char[] txt = {'A', 'B', 'C', 'D', 'E'};
        int textLength = txt.length;

        try {
            Bidi.requiresBidi(txt, -1, textLength);
            errorHandling("requiresBidi() should throw an IAE" +
                " when start is -1(too small).");
        }
        catch (IllegalArgumentException e) {
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("requiresBidi() should not throw an AIOoBE " +
                "but an IAE when start is -1(too small).");
        }

        try {
            Bidi.requiresBidi(txt, textLength, textLength);
        }
        catch (Exception e) {
            errorHandling("requiresBidi() should not throw an exception " +
                "when start is textLength.");
        }

        try {
            Bidi.requiresBidi(txt, textLength+1, textLength);
            errorHandling("requiresBidi() should throw an IAE" +
                " when start is textLength+1(too large).");
        }
        catch (IllegalArgumentException e) {
        }

        try {
            Bidi.requiresBidi(txt, 0, -1);
            errorHandling("requiresBidi() should throw an IAE" +
                " when limit is -1(too small).");
        }
        catch (IllegalArgumentException e) {
        }

        try {
            Bidi.requiresBidi(txt, 0, textLength+1);
            errorHandling("requiresBidi() should throw an IAE" +
                " when limit is textLength+1(too large).");
        }
        catch (IllegalArgumentException e) {
            if (!e.getMessage().equals(
                    "Value start 0 is out of range 0 to " + (textLength + 1) +
                    ", or limit " + (textLength + 1) + " is beyond the text length " + textLength)) {
                errorHandling("requiresBidi() should throw an IAE" +
                        " mentioning limit is beyond the text length. Message: " + e.getMessage());
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            errorHandling("requiresBidi() should not throw an AIOoBE " +
                "but an IAE when limit is textLength+1(too large).");
        }
    }

    private void checkResult(String name,
                             int expectedValue,
                             int actualValue) {
        if (expectedValue != actualValue) {
            errorHandling("Unexpected " + name + " value." +
                " Expected: " + expectedValue + " Got: " + actualValue);
        } else if (verbose) {
            System.out.println("  Okay : " + name + " = " + actualValue);
        }
    }

    private void checkResult(String name,
                             boolean expectedValue,
                             boolean actualValue) {
        if (expectedValue != actualValue) {
            errorHandling("Unexpected " + name + " value." +
                " Expected: " + expectedValue + " Got: " + actualValue);
        } else if (verbose) {
            System.out.println("  Okay : " + name + " = " + actualValue);
        }
    }

    private void checkResult(String name,
                             String expectedValue,
                             String actualValue) {
        if (!expectedValue.equals(actualValue)) {
            errorHandling("Unexpected " + name + " value." +
                "\n\tExpected: \"" + expectedValue + "\"" +
                "\n\tGot:      \"" + actualValue + "\"");
        } else if (verbose) {
            System.out.println("  Okay : " + name + " = \"" +
                actualValue + "\"");
        }
    }

    private void checkResult(String name,
                             int[] expectedValues,
                             int[] actualValues) {
        if (!Arrays.equals(expectedValues, actualValues)) {
            errorHandling("Unexpected " + name + " value." +
                "\n\tExpected: " + toString(expectedValues) + "" +
                "\n\tGot:      " + toString(actualValues) + "");
        } else if (verbose) {
            System.out.println("  Okay : " + name + " = " +
                toString(actualValues));
        }
    }

    private void checkResult(String name,
                             Object[] expectedValues,
                             Object[] actualValues) {
        if (!Arrays.equals(expectedValues, actualValues)) {
            errorHandling("Unexpected " + name + " value." +
                "\n\tExpected: [" + toString(expectedValues) +
                "]\n\tGot:      [" + toString(actualValues) + "]");
        } else if (verbose) {
            System.out.println("  Okay : " + name + " Reordered objects = [" +
                toString(actualValues) + "]");
        }
    }

    private void errorHandling(String msg) {
        if (abort) {
            throw new RuntimeException("Error: " + msg);
        } else {
            error = true;
            System.err.println("**Error:" + msg);
        }
    }

    private String toString(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length-1; i++) {
            sb.append((int)values[i]);
            sb.append(' ');
        }
        sb.append((int)values[values.length-1]);

        return sb.toString();
    }

    private String toString(byte[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length-1; i++) {
            sb.append((byte)values[i]);
            sb.append(' ');
        }
        sb.append((byte)values[values.length-1]);

        return sb.toString();
    }

    private String toString(Object[] values) {
        StringBuilder sb = new StringBuilder();
        String name;

        for (int i = 0; i < values.length-1; i++) {
            if ((name = getStringName((String)values[i])) != null) {
                sb.append(name);
                sb.append(", ");
            } else {
                sb.append('"');
                sb.append((String)values[i]);
                sb.append("\", ");
            }
        }
        if ((name = getStringName((String)values[values.length-1])) != null) {
            sb.append(name);
        } else {
            sb.append('"');
            sb.append((String)values[values.length-1]);
            sb.append('\"');
        }

        return sb.toString();
    }

    private String getStringName(String str) {
        if (ArabicABC.equals(str)) return "ArabicABC";
        else if (Arabic123.equals(str)) return "Arabic123";
        else if (PArabicABC.equals(str)) return "ArabicABC(Presentation form)";
        else if (HebrewABC.equals(str)) return "HebrewABC";
        else if (KharoshthiABC.equals(str)) return "KharoshthiABC(RTL)";
        else if (Kharoshthi123.equals(str)) return "Kharoshthi123(RTL)";
        else if (NKoABC.equals(str)) return "NKoABC(RTL)";
        else if (NKo123.equals(str)) return "NKo123(RTL)";
        else if (OsmanyaABC.equals(str)) return "OsmanyaABC(LTR)";
        else if (Osmanya123.equals(str)) return "Osmanya123(LTR)";
        else return null;
    }

    private String getFlagName(int flag) {
        if (flag == -2 || flag == 0x7e) return FLAGNAMES[0];
        else if (flag == -1 || flag == 0x7f) return FLAGNAMES[1];
        else if (flag == 0) return FLAGNAMES[2];
        else if (flag == 1) return FLAGNAMES[3];
        else return "Unknown(0x" + Integer.toHexString(flag) + ")";
    }

    private String toReadableString(String str) {
         String s = str;

         s = s.replaceAll(ArabicABC, "ArabicABC");
         s = s.replaceAll(Arabic123, "Arabic123");
         s = s.replaceAll(PArabicABC, "ArabicABC(Presentation form)");
         s = s.replaceAll(HebrewABC, "HebrewABC");
         s = s.replaceAll(KharoshthiABC, "KharoshthiABC");
         s = s.replaceAll(Kharoshthi123, "Kharoshthi123");
         s = s.replaceAll(NKoABC, "NKoABC");
         s = s.replaceAll(NKo123, "NKo123");
         s = s.replaceAll(OsmanyaABC, "OsmanyaABC");
         s = s.replaceAll(Osmanya123, "Osmanya123");

         return s;
    }

    private  byte[] getLevels(Object[][] data) {
        int levelLength = data[0].length;
        byte[] array = new byte[levelLength];
        int textIndex = 0;

        for (int i = 0; i < levelLength; i++) {
            array[i] = (byte)(((String)data[1][0]).charAt(textIndex) - '0');
            textIndex += ((String)data[0][i]).length();
        }

        return array;
    }


    /* Bidi pubilc constants */
    private static final int[] FLAGS = {
        Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT,  // -2 (0x7e in ICU4J)
        Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT,  // -1 (0x7f in ICU4J)
        Bidi.DIRECTION_LEFT_TO_RIGHT,          //  0
        Bidi.DIRECTION_RIGHT_TO_LEFT           //  1
    };

    /* Bidi pubilc constants names */
    private static final String[] FLAGNAMES = {
        "DIRECTION_DEFAULT_LEFT_TO_RIGHT",  // -2
        "DIRECTION_DEFAULT_RIGHT_TO_LEFT",  // -1
        "DIRECTION_LEFT_TO_RIGHT",          //  0
        "DIRECTION_RIGHT_TO_LEFT",          //  1
    };

    /* Bidirectional Character Types */
    private static final char L   = '\u200E';
    private static final char R   = '\u202F';
    private static final char LRE = '\u202A';
    private static final char RLE = '\u202B';
    private static final char PDF = '\u202C';
    private static final char LRO = '\u202D';
    private static final char RLO = '\u202E';
    private static final char LRI = '\u2066';
    private static final char RLI = '\u2067';
    private static final char FSI = '\u2068';
    private static final char PDI = '\u2069';

    /*
     *  0x05D0-0x05EA:   [R]   Hewbrew letters (Strong)
     *  0x0627-0x063A:   [AL]  Arabic letters (Strong)
     *  0x0660-0x0669:   [AN]  Arabic-Indic digits (Weak)
     *  0x07CA-0x07E7:   [R]   NKo letters (Strong)
     *  0x07C0-0x07C9:   [R]   NKo digits (Strong)
     *  0xFE50-0xFEFF:   [AL]  Arabic presentaion form (Strong)
     *  0x10480-0x1049D: [L]   Osmanya letters (Strong)
     *  0x104A0-0x104A9: [L]   Osmanya digits (Strong)
     *  0x10A10-0x10A33: [R]   Kharoshthi letters (Strong)
     *  0x10A40-0x10A43: [R]   Kharoshthi digits (Strong)
     *
     *  0x200E:          [L]   Left-to-right mark (Implicit, Strong)
     *  0x200F:          [R]   Right-to-left mark (Implicit, Strong)
     *  0x202A:          [LRE] Left-to-right embedding (Explicit, Strong)
     *  0x202B:          [RLE] Right-to-left embedding (Explicit, Strong)
     *  0x202C:          [PDF] Pop directional formatting (Explicit, Weak)
     *  0x202D:          [LRO] Left-to-right override (Explicit, Strong)
     *  0x202E:          [RLO] Right-to-left override (Explicit, Strong)
     */

    /* Right-to-left */
    private static String ArabicABC = "\u0627\u0628\u0629";
    private static String Arabic123 = "\u0661\u0662\u0663";
    private static String PArabicABC = "\uFE97\uFE92\uFE8E";
    private static String HebrewABC = "\u05D0\u05D1\u05D2";
    private static String KharoshthiABC =
        new String(Character.toChars(0x10A10)) +
        new String(Character.toChars(0x10A11)) +
        new String(Character.toChars(0x10A12));
    private static String Kharoshthi123 =
        new String(Character.toChars(0x10A40)) +
        new String(Character.toChars(0x10A41)) +
        new String(Character.toChars(0x10A42));
    private static String NKoABC = "\u07CA\u07CB\u07CC";
    private static String NKo123 = "\u07C1\u07C2\u07C3";

    /* Left-to-right */
    private static String OsmanyaABC =
        new String(Character.toChars(0x10480)) +
        new String(Character.toChars(0x10481)) +
        new String(Character.toChars(0x10482));
    private static String Osmanya123 =
        new String(Character.toChars(0x104A0)) +
        new String(Character.toChars(0x104A1)) +
        new String(Character.toChars(0x104A2));

    /* --------------------------------------------------------------------- */

    /*
     * Test data for Bidi(char[], ...) constructor and methods
     */

    /* Text for Bidi processing and its levels */
    private static String[][] data4Constructor1 = {
        /* For Text #0 */
        {"abc <ABC XYZ> xyz.",
             "000000000000000000", "000002222222000000", "000000000000000000",
             "000003333333000000", "000000000000000000",
             "222222222222222221", "222222222222222221", "222222222222222221",
             "222113333333112221", "222224444444222221",
             "000000000000000000", "000000000000000000", "222222222222222221"},

        /* For Text #1 */
        {"ABC <" + HebrewABC + " " + NKo123 + "> XYZ.",
             "000001111111000000", "000001111111000000", "000003333333000000",
             "000003333333000000", "000000000000000000",
             "222111111111112221", "222111111111112221", "222223333333222221",
             "222113333333112221", "222224444444222221",
             "000001111111000000", "000001111111000000", "222111111111112221"},

        /* For Text #2 */
        {NKoABC + " <ABC XYZ> " + NKo123 + ".",
             "111000000000001110", "111112222222111110", "111002222222001110",
             "111113333333111110", "111004444444001110",
             "111112222222111111", "111112222222111111", "111112222222111111",
             "111111111111111111", "111114444444111111",
             "111112222222111111", "111000000000001110", "111112222222111111"},

        /* For Text #3 */
        {HebrewABC + " <" + ArabicABC + " " + Arabic123 + "> " + NKo123 + ".",
             "111111111222111110", "111111111222111110", "111003333444001110",
             "111113333333111110", "111004444444001110",
             "111111111222111111", "111111111222111111", "111113333444111111",
             "111111111111111111", "111114444444111111",
             "111111111222111111", "111111111222111110", "111111111222111111"},

        /* For Text #4 */
        {"abc <" + NKoABC + " 123> xyz.",
             "000001111222000000", "000001111222000000", "000003333444000000",
             "000003333333000000", "000000000000000000",
             "222111111222112221", "222111111222112221", "222223333444222221",
             "222113333333112221", "222224444444222221",
             "000001111222000000", "000001111222000000", "222111111222112221"},

        /* For Text #5 */
        {"abc <ABC " + NKo123 + "> xyz.",
             "000000000111000000", "000002221111000000", "000002222333000000",
             "000003333333000000", "000000000000000000",
             "222222221111112221", "222222221111112221", "222222222333222221",
             "222113333333112221", "222224444444222221",
             "000000000111000000", "000000000111000000", "222222221111112221"},

        /* For Text #6 */
        {ArabicABC + " <" + NKoABC + " 123" + "> " + Arabic123 + ".",
             "111111111222112220", "111111111222112220", "111003333444002220",
             "111113333333112220", "111004444444002220",
             "111111111222112221", "111111111222112221", "111113333444112221",
             "111113333333112221", "111114444444112221",
             "111111111222112221", "111111111222112220", "111111111222112221"},

        /* For Text #7 */
        {ArabicABC + " <XYZ " + NKoABC + "> " + Arabic123 + ".",
             "111000000111112220", "111112221111112220", "111002222333002220",
             "111113333333112220", "111004444444002220",
             "111112221111112221", "111112221111112221", "111112222333112221",
             "111113333333112221", "111114444444112221",
             "111112221111112221", "111000000111112220", "111112221111112221"},

        /* For Text #8 */
        {OsmanyaABC + " <" + KharoshthiABC + " " + Kharoshthi123 + "> " +
         Osmanya123 + ".",
             "000000001111111111111000000000", "000000001111111111111000000000",
             "000000003333333333333000000000", "000000003333333333333000000000",
             "000000000000000000000000000000",
             "222222111111111111111112222221", "222222111111111111111112222221",
             "222222223333333333333222222221", "222222113333333333333112222221",
             "222222224444444444444222222221",
             "000000001111111111111000000000", "000000001111111111111000000000",
             "222222111111111111111112222221"},

        /* For Text #9 */
        {KharoshthiABC + " <" + OsmanyaABC + " " + Osmanya123 + "> " +
         Kharoshthi123 + ".",
             "111111000000000000000001111110", "111111112222222222222111111110",
             "111111002222222222222001111110", "111111113333333333333111111110",
             "111111004444444444444001111110",
             "111111112222222222222111111111", "111111112222222222222111111111",
             "111111112222222222222111111111", "111111111111111111111111111111",
             "111111114444444444444111111111",
             "111111112222222222222111111111", "111111000000000000000001111110",
             "111111112222222222222111111111"},
    };

    /* Golden data for baseIsLeftToRight() results */
    private static boolean[][] baseIsLTR4Constructor1 = {
        /* For Text #0 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         true,  true,  false},

        /* For Text #1 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         true,  true,  false},

        /* For Text #2 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         false, true,  false},

        /* For Text #3 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         false, true,  false},

        /* For Text #4 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         true,  true,  false},

        /* For Text #5 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         true,  true,  false},

        /* For Text #6 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         false, true,  false},

        /* For Text #7 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         false, true,  false},

        /* For Text #8 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         true,  true,  false},

        /* For Text #9 */
        {true,  true,  true,  true,  true,
         false, false, false, false, false,
         false, true,  false},
    };

    /* Golden data for isLeftToRight() & isRightToLeft() results */
    private static boolean[][][] isLTR_isRTL4Constructor1 = {
        /* For Text #0 */
         /* isLeftToRight() results */
        {{true,  false, true,  false, true,
          false, false, false, false, false,
          true,  true,  false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, false, false,
          false, false, false}},

        /* For Text #1 */
         /* isLeftToRight() results */
        {{false, false, false, false, true,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, false, false,
          false, false, false}},

        /* For Text #2 */
         /* isLeftToRight() results */
        {{false, false, false, false, false,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, true,  false,
          false, false, false}},

        /* For Text #3 */
         /* isLeftToRight() results */
        {{false, false, false, false, false,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, true,  false,
          false, false, false}},

        /* For Text #4 */
         /* isLeftToRight() results */
        {{false, false, false, false, true,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, false, false,
          false, false, false}},

        /* For Text #5 */
         /* isLeftToRight() results */
        {{false, false, false, false, true,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, false, false,
          false, false, false}},

        /* For Text #6 */
         /* isLeftToRight() results */
        {{false, false, false, false, false,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, false, false,
          false, false, false}},

        /* For Text #7 */
         /* isLeftToRight() results */
        {{false, false, false, false, false,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, false, false,
          false, false, false}},

        /* For Text #8 */
         /* isLeftToRight() results */
        {{false, false, false, false, true,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, false, false,
          false, false, false}},

        /* For Text #9 */
         /* isLeftToRight() results */
        {{false, false, false, false, false,
          false, false, false, false, false,
          false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false, false,
          false, false, false, true,  false,
          false, false, false}},
    };

    /* --------------------------------------------------------------------- */

    /*
     * Test data for Bidi(String, int) constructor and methods
     */

    /* Text for Bidi processing and its levels */
    private static String[][] data4Constructor2 = {
        /* For Text #0 */
        {" ABC 123.",
             "000000000", "000000000", "000000000", "122222221"},

        /* For Text #1 */
        {" ABC " + HebrewABC + " " + NKo123 + " 123.",
             "00000111111112220", "00000111111112220", "00000111111112220",
             "12221111111112221"},

        /* For Text #2 */
        {" ABC " + ArabicABC + " " + Arabic123 + " 123.",
             "00000111122212220", "00000111122212220", "00000111122212220",
             "12221111122212221"},

        /* For Text #3 */
        {" " + NKoABC + " ABC 123 " + NKo123 + ".",
             "11111222222211111", "11111222222211111", "01110000000001110",
             "11111222222211111"},

        /* For Text #4 */
        {" " + ArabicABC + " ABC 123 " + Arabic123 + ".",
             "11111222222212221", "11111222222212221", "01110000000002220",
             "11111222222212221"},

        /* For Text #5 */
        {" " + HebrewABC + " " + NKo123 + ".",
             "111111111", "111111111", "011111110", "111111111"},

        /* For Text #6 */
        {" " + ArabicABC + " " + Arabic123 + ".",
             "111112221", "111112221", "011112220", "111112221"},

        /* For Text #7 */
        {" " + KharoshthiABC + " " + Kharoshthi123 + ".",
             "111111111111111", "111111111111111", "011111111111110",
             "111111111111111"},

        /* For Text #8 */
        {L + HebrewABC + " " + NKo123 + ".",
             "011111110", "011111110", "011111110", "211111111"},

        /* For Text #9 */
        {R + "ABC " + Osmanya123 + ".",
             "000000000000", "000000000000", "000000000000", "122222222221"},

        /* For Text #10 */
        {"ABC " + PArabicABC + " " + PArabicABC + " 123",
             "000011111111222", "000011111111222", "000011111111222",
             "222111111111222"},

        /* For Text #11 */
        {RLE + "ABC " + HebrewABC + " " + NKo123 + "." + PDF,
             "22221111111110", "22221111111110", "22221111111110",
             "44443333333331"},

        /* For Text #12 */
        {"He said \"" + RLE + "ABC " + HebrewABC + " " + NKo123 + PDF + ".\"",
             "000000000222211111111000", "000000000222211111111000",
             "000000000222211111111000", "222222211444433333333111"},

        /* For Text #13 */
        {LRO + "He said \"" + RLE + "ABC " + NKoABC + " " + NKo123 + PDF +
         ".\"" + PDF,
             "22222222224444333333332220", "22222222224444333333332220",
             "22222222224444333333332220", "22222222224444333333332221"},

        /* For Text #14 */
        {LRO + "He said \"" + RLE + "ABC " + HebrewABC + " " + NKo123 + PDF +
         ".\"",  // PDF missing
             "2222222222444433333333222", "2222222222444433333333222",
             "2222222222444433333333222", "2222222222444433333333222"},

        /* For Text #15 */
        {"Did you say '" + LRE + "he said \"" + RLE + "ABC " + HebrewABC +
         " " + NKo123 + PDF + "\"" + PDF + "'?",
             "0000000000000222222222244443333333322000",
             "0000000000000222222222244443333333322000",
             "0000000000000222222222244443333333322000",
             "2222222222222222222222244443333333322111"},

        /* For Text #16 */
        {RLO + "Did you say '" + LRE + "he said \"" + RLE + "ABC " +
         HebrewABC + " " + NKo123 + PDF + "\"" + PDF + "'?" + PDF,
             "111111111111112222222222444433333333221110",
             "111111111111112222222222444433333333221110",
             "111111111111112222222222444433333333221110",
             "333333333333334444444444666655555555443331"},

        /* For Text #17 */
        {RLO + "Did you say '" + LRE + "he said \"" + RLE + "ABC " +
         HebrewABC + " " + NKo123 + PDF + "\"" + PDF + "'?",  // PDF missing
             "11111111111111222222222244443333333322111",
             "11111111111111222222222244443333333322111",
             "11111111111111222222222244443333333322111",
             "33333333333333444444444466665555555544333"},

        /* For Text #18 */
        {" ABC (" + ArabicABC + " " + Arabic123 + ") 123.",
             "0000001111222002220", "0000001111222002220",
             "0000001111222002220", "1222111111222112221"},

        /* For Text #19 */
        {" " + HebrewABC + " (ABC 123) " + NKo123 + ".",
             "1111112222222111111", "1111112222222111111",
             "0111000000000001110", "1111112222222111111"},

        /* For Text #20 */
        {" He said \"" + RLE + "ABC " + NKoABC + " " + NKo123 + PDF + ".\" ",
             "00000000002222111111110000", "00000000002222111111110000",
             "00000000002222111111110000", "12222222114444333333331111"},

        /* For Text #21 */
        {" Did you say '" + LRE + "he said \"" + RLE + "ABC " + HebrewABC +
         " " + NKo123 + PDF + "\"" + PDF + "'? ",
             "000000000000002222222222444433333333220000",
             "000000000000002222222222444433333333220000",
             "000000000000002222222222444433333333220000",
             "122222222222222222222222444433333333221111"},

        /* For Text #22 */
        {RLE + OsmanyaABC + " " + KharoshthiABC + " " + Kharoshthi123 + "." +
         PDF,
             "22222221111111111111110", "22222221111111111111110",
             "22222221111111111111110", "44444443333333333333331"},

        /* For Text #23 */
        {" ABC (" + Arabic123 + " " + ArabicABC + ") 123.",
             "0000002221111002220", "0000002221111002220",
             "0000002221111002220", "1222112221111112221"},

        /* For Text #24 */
        {" 123 (" + ArabicABC + " " + Arabic123 + ") ABC.",
             "1222111111222112221", "1222111111222112221",
             "0000001111222000000", "1222111111222112221"},

        /* For Text #25 */
        {" 123 (" + Arabic123 + " " + ArabicABC + ") ABC.",
             "1222112221111112221", "1222112221111112221",
             "0000002221111000000", "1222112221111112221"},

        /* For Text #26 */
        {" " + ArabicABC + " (ABC 123) "  + Arabic123 + ".",
             "1111112222222112221", "1111112222222112221",
             "0111000000000002220", "1111112222222112221"},

        /* For Text #27 */
        {" " + ArabicABC + " (123 ABC) "  + Arabic123 + ".",
             "1111112221222112221", "1111112221222112221",
             "0111002220000002220", "1111112221222112221"},

        /* For Text #28 */
        {" " + Arabic123 + " (ABC 123) "  + ArabicABC + ".",
             "0222000000000001110", "0222000000000001110",
             "0222000000000001110", "1222112222222111111"},

        /* For Text #29 */
        {" " + Arabic123 + " (123 ABC) "  + ArabicABC + ".",
             "0222000000000001110", "0222000000000001110",
             "0222000000000001110", "1222112221222111111"},

        /* For Text #30 */
        {RLI + "ABC " + ArabicABC + " " + ArabicABC + "." + PDI,
             "02221111111110", "14443333333331",
             "02221111111110", "14443333333331"},

        /* For Text #31 */
        {"ABC abc \"" + RLI + "IJK " + ArabicABC + " " + ArabicABC + PDI +
         ".\" \"" + RLI + ArabicABC + " " + ArabicABC + PDI + ",\" xyz XYZ.",
             "0000000000222111111110000001111111000000000000",
             "0000000000222111111110000001111111000000000000",
             "0000000000222111111110000001111111000000000000",
             "2222222222444333333332222223333333222222222221"},

        /* For Text #32 */
        {ArabicABC + " " + ArabicABC + " '" + LRI + "abc def \"" + RLI +
         "xyz " + ArabicABC + " " + ArabicABC + PDI + "\"" + PDI + "'?",
             "111111111122222222224443333333322111",
             "111111111122222222224443333333322111",
             "111111100022222222224443333333322000",
             "111111111122222222224443333333322111"},

        /* For Text #33 */
        {FSI + Arabic123 + " ABC " + ArabicABC + " " + ArabicABC + "." + PDI,
             "044422222333333320", "144422222333333321",
             "044422222333333320", "144422222333333321"},

        /* For Text #34 */
        {FSI + "123 ABC " + ArabicABC + " " + ArabicABC + "." + PDI,
             "022222222333333320", "122222222333333321",
             "022222222333333320", "122222222333333321"},

        /* For Text #35 */
        {FSI + "123 " + ArabicABC + " ABC " + ArabicABC + "." + PDI,
             "022211111222111110", "144433333444333331",
             "022211111222111110", "144433333444333331"},

        /* For Text #36 */
        {FSI + Arabic123 + " " + ArabicABC + " ABC " + ArabicABC + "." + PDI,
             "022211111222111110", "144433333444333331",
             "022211111222111110", "144433333444333331"},

        /* For Text #37 */
        {FSI + Arabic123 + " 123." + PDI,
             "0444222220", "1444222221", "0444222220", "1444222221"},

        /* For Text #38 */
        {FSI + "123 " + Arabic123 + "." + PDI,
             "0222244420", "1222244421", "0222244420", "1222244421"},
    };

    /* Golden data for baseIsLeftToRight() results */
    private static boolean[][] baseIsLTR4Constructor2 = {
        /* For Text #0 - $4  */
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {false, false, true,  false},
        {false, false, true,  false},

        /* For Text #5 - $9  */
        {false, false, true,  false},
        {false, false, true,  false},
        {false, false, true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},

        /* For Text #10 - $14  */
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},

        /* For Text #15 - $19  */
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {false, false, true,  false},

        /* For Text #20 - $24  */
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},
        {false, false, true,  false},

        /* For Text #25 - $29  */
        {false, false, true,  false},
        {false, false, true,  false},
        {false, false, true,  false},
        {true,  true,  true,  false},
        {true,  true,  true,  false},

        /* For Text #30 - $34  */
        {true,  false, true,  false},
        {true,  true,  true,  false},
        {false, false, true,  false},
        {true,  false, true,  false},
        {true , false, true,  false},

        /* For Text #35 - $38  */
        {true,  false, true,  false},
        {true,  false, true,  false},
        {true,  false, true,  false},
        {true,  false, true,  false},
    };

    /* Golden data for isLeftToRight() & isRightToLeft() results */
    private static boolean[][][] isLTR_isRTL4Constructor2 = {
        /* isLeftToRight() results   &   isRightToLeft() results   */
        /* For Text #0 - $4  */
        {{true,  true,  true,  false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},

        /* For Text #5 - $9  */
        {{false, false, false, false}, {true,  true,  false, true }},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {true,  true,  false, true }},
        {{false, false, false, false}, {false, false, false, false}},
        {{true,  true,  true,  false}, {false, false, false, false}},

        /* For Text #10 - $14  */
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},

        /* For Text #15 - $19  */
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},

        /* For Text #20 - $24  */
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},

        /* For Text #25 - $29  */
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},

        /* For Text #30 - $34  */
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},

        /* For Text #35 - $37  */
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
        {{false, false, false, false}, {false, false, false, false}},
    };

    /* Golden data for requiresBidi() results */
    private static boolean[] requiresBidi4Constructor2 = {
        /* For Text #0 - $9  */
        false, true,  true,  true,  true,
        true,  true,  true,  true,  false,

        /* For Text #10 - $19  */
        true,  true,  true,  true,  true,
        true,  true,  true,  true,  true,

        /* For Text #20 - $29  */
        true,  true,  true,  true,  true,
        true,  true,  true,  true,  true,

        /* For Text #30 - $37  */
        true,  true,  true,  true,  true,
        true,  true,  true,  true,
    };

    /* --------------------------------------------------------------------- */

    /*
     * Test data for Bidi(char[], ...) constructor and methods
     */

    /* Enbeddings */
    private static byte[][][] emb4Constructor3 = {
        /* Embeddings for paragraphs which don't include surrogate pairs. */
        {{0, 0, 0, 0, 0,  1,  1,  1,  1,  1,  1,  1, 0, 0, 0, 0, 0, 0},
         {0, 0, 0, 0, 0,  2,  2,  2,  2,  2,  2,  2, 0, 0, 0, 0, 0, 0},
         {0, 0, 0, 0, 0, -3, -3, -3, -3, -3, -3, -3, 0, 0, 0, 0, 0, 0},
         {0, 0, 0, 0, 0, -4, -4, -4, -4, -4, -4, -4, 0, 0, 0, 0, 0, 0}},

        /* Embeddings for paragraphs which include surrogate pairs. */
        {{ 0,  0,  0,  0,  0,  0,  0,  0,
           1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
           0,  0,  0,  0,  0,  0,  0,  0,  0},
         { 0,  0,  0,  0,  0,  0,  0,  0,
           2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,
           0,  0,  0,  0,  0,  0,  0,  0,  0},
         { 0,  0,  0,  0,  0,  0,  0,  0,
          -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
           0,  0,  0,  0,  0,  0,  0,  0,  0},
         { 0,  0,  0,  0,  0,  0,  0,  0,
          -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4,
           0,  0,  0,  0,  0,  0,  0,  0,  0}},
    };

    /* Text for Bidi processing and its levels */
    private static String[][] data4Constructor3 = {
        /* For Text #0 */
        {"abc <ABC XYZ> xyz.",
             /* DIRECTION_DEFAULT_LEFT_TO_RIGHT */
             "000002222222000000", "000000000000000000",
             "000003333333000000", "000000000000000000",
             /* DIRECTION_DEFAULT_RIGHT_TO_LEFT */
             "222222222222222221", "222222222222222221",
             "222113333333112221", "222224444444222221",
             /* DIRECTION_LEFT_TO_RIGHT */
             "000002222222000000", "000000000000000000",
             "000003333333000000", "000000000000000000",
             /* DIRECTION_RIGHT_TO_LEFT */
             "222222222222222221", "222222222222222221",
             "222113333333112221", "222224444444222221"},

        /* For Text #1 */
        {"ABC <" + HebrewABC + " " + NKo123 + "> XYZ.",
             /* DIRECTION_DEFAULT_LEFT_TO_RIGHT */
             "000001111111000000", "000003333333000000",
             "000003333333000000", "000000000000000000",
             /* DIRECTION_DEFAULT_RIGHT_TO_LEFT */
             "222111111111112221", "222223333333222221",
             "222113333333112221", "222224444444222221",
             /* DIRECTION_LEFT_TO_RIGHT */
             "000001111111000000", "000003333333000000",
             "000003333333000000", "000000000000000000",
             /* DIRECTION_RIGHT_TO_LEFT */
             "222111111111112221", "222223333333222221",
             "222113333333112221", "222224444444222221"},

        /* For Text #2 */
        {NKoABC + " <ABC XYZ> " + NKo123 + ".",
             /* DIRECTION_DEFAULT_LEFT_TO_RIGHT */
             "111112222222111111", "111112222222111111",
             "111111111111111111", "111114444444111111",
             /* DIRECTION_DEFAULT_RIGHT_TO_LEFT */
             "111112222222111111", "111112222222111111",
             "111111111111111111", "111114444444111111",
             /* DIRECTION_LEFT_TO_RIGHT */
             "111112222222111110", "111002222222001110",
             "111113333333111110", "111004444444001110",
             /* DIRECTION_RIGHT_TO_LEFT */
             "111112222222111111", "111112222222111111",
             "111111111111111111", "111114444444111111"},

        /* For Text #3 */
        {HebrewABC + " <" + ArabicABC + " " + Arabic123 + "> " + NKo123 + ".",
             /* DIRECTION_DEFAULT_LEFT_TO_RIGHT */
             "111111111222111111", "111113333444111111",
             "111111111111111111", "111114444444111111",
             /* DIRECTION_DEFAULT_RIGHT_TO_LEFT */
             "111111111222111111", "111113333444111111",
             "111111111111111111", "111114444444111111",
             /* DIRECTION_LEFT_TO_RIGHT */
             "111111111222111110", "111003333444001110",
             "111113333333111110", "111004444444001110",
             /* DIRECTION_RIGHT_TO_LEFT */
             "111111111222111111", "111113333444111111",
             "111111111111111111", "111114444444111111"},

        /* For Text #4 */
        {"abc <123 456> xyz.",
             /* DIRECTION_DEFAULT_LEFT_TO_RIGHT */
             "000002221222000000", "000000000000000000",
             "000003333333000000", "000000000000000000",
             /* DIRECTION_DEFAULT_RIGHT_TO_LEFT */
             "222222222222222221", "222222222222222221",
             "222113333333112221", "222224444444222221",
             /* DIRECTION_LEFT_TO_RIGHT */
             "000002221222000000", "000000000000000000",
             "000003333333000000", "000000000000000000",
             /* DIRECTION_RIGHT_TO_LEFT */
             "222222222222222221", "222222222222222221",
             "222113333333112221", "222224444444222221"},

        /* For Text #5 */
        {OsmanyaABC + " <" + KharoshthiABC + " " + Kharoshthi123 + "> " +
         Osmanya123 + ".",
             /* DIRECTION_DEFAULT_LEFT_TO_RIGHT */
             "000000001111111111111000000000", "000000003333333333333000000000",
             "000000003333333333333000000000", "000000000000000000000000000000",
             /* DIRECTION_DEFAULT_RIGHT_TO_LEFT */
             "222222111111111111111112222221", "222222223333333333333222222221",
             "222222113333333333333112222221", "222222224444444444444222222221",
             /* DIRECTION_LEFT_TO_RIGHT */
             "000000001111111111111000000000", "000000003333333333333000000000",
             "000000003333333333333000000000", "000000000000000000000000000000",
             /* DIRECTION_RIGHT_TO_LEFT */
             "222222111111111111111112222221", "222222223333333333333222222221",
             "222222113333333333333112222221", "222222224444444444444222222221"},

        /* For Text #6 */
        {KharoshthiABC + " <" + OsmanyaABC + " " + Osmanya123 + "> " +
         Kharoshthi123 + ".",
             /* DIRECTION_DEFAULT_LEFT_TO_RIGHT */
             "111111112222222222222111111111", "111111112222222222222111111111",
             "111111111111111111111111111111", "111111114444444444444111111111",
             /* DIRECTION_DEFAULT_RIGHT_TO_LEFT */
             "111111112222222222222111111111", "111111112222222222222111111111",
             "111111111111111111111111111111", "111111114444444444444111111111",
             /* DIRECTION_LEFT_TO_RIGHT */
             "111111112222222222222111111110", "111111002222222222222001111110",
             "111111113333333333333111111110", "111111004444444444444001111110",
             /* DIRECTION_RIGHT_TO_LEFT */
             "111111112222222222222111111111", "111111112222222222222111111111",
             "111111111111111111111111111111", "111111114444444444444111111111"},
    };

    /* Golden data for baseIsLeftToRight() results */
    private static boolean[][] baseIsLTR4Constructor3 = {
        /* For Text #0 */
        {true,  true,  true,  true,    // DIRECTION_DEFAULT_LEFT_TO_RIGHT
         true,  true,  true,  true,    // DIRECTION_DEFAULT_RIGHT_TO_LEFT
         true,  true,  true,  true,    // DIRECTION_LEFT_TO_RIGHT
         false, false, false, false},  // DIRECTION_RIGHT_TO_LEFT

        /* For Text #1 */
        {true,  true,  true,  true,
         true,  true,  true,  true,
         true,  true,  true,  true,
         false, false, false, false},

        /* For Text #2 */
        {false, false, false, false,
         false, false, false, false,
         true,  true,  true,  true,
         false, false, false, false},

        /* For Text #3 */
        {false, false, false, false,
         false, false, false, false,
         true,  true,  true,  true,
         false, false, false, false},

        /* For Text #4 */
        {true,  true,  true,  true,
         true,  true,  true,  true,
         true,  true,  true,  true,
         false, false, false, false},

        /* For Text #5 */
        {true,  true,  true,  true,
         true,  true,  true,  true,
         true,  true,  true,  true,
         false, false, false, false},

        /* For Text #6 */
        {false, false, false, false,
         false, false, false, false,
         true,  true,  true,  true,
         false, false, false, false},
    };

    /* Golden data for isLeftToRight() & isRightToLeft() results */
    private static boolean[][][] isLTR_isRTL4Constructor3 = {
        /* For Text #0 */
         /* isLeftToRight() results */
        {{false, true,  false, true,     // DIRECTION_DEFAULT_LEFT_TO_RIGHT
          false, false, false, false,    // DIRECTION_DEFAULT_RIGHT_TO_LEFT
          false, true,  false, true,     // DIRECTION_LEFT_TO_RIGHT
          false, false, false, false},   // DIRECTION_RIGHT_TO_LEFT
         /* isRightToLeft() results   */
         {false, false, false, false,    // DIRECTION_DEFAULT_LEFT_TO_RIGHT
          false, false, false, false,    // DIRECTION_DEFAULT_RIGHT_TO_LEFT
          false, false, false, false,    // DIRECTION_LEFT_TO_RIGHT
          false, false, false, false}},  // DIRECTION_RIGHT_TO_LEFTT

        /* For Text #1 */
         /* isLeftToRight() results */
        {{false, false, false, true,
          false, false, false, false,
          false, false, false, true,
          false, false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false,
          false, false, false, false,
          false, false, false, false,
          false, false, false, false}},

        /* For Text #2 */
         /* isLeftToRight() results */
        {{false, false, false, false,
          false, false, false, false,
          false, false, false, false,
          false, false, false, false},
         /* isRightToLeft() results   */
         {false, false, true,  false,
          false, false, true,  false,
          false, false, false, false,
          false, false, true,  false}},

        /* For Text #3 */
         /* isLeftToRight() results */
        {{false, false, false, false,
          false, false, false, false,
          false, false, false, false,
          false, false, false, false},
         /* isRightToLeft() results   */
         {false, false, true,  false,
          false, false, true,  false,
          false, false, false, false,
          false, false, true,  false}},

        /* For Text #4 */
         /* isLeftToRight() results */
        {{false, true,  false, true,
          false, false, false, false,
          false, true,  false, true,
          false, false, false, false },
         /* isRightToLeft() results   */
         {false, false, false, false,
          false, false, false, false,
          false, false, false, false,
          false, false, false, false}},

        /* For Text #5 */
         /* isLeftToRight() results */
        {{false, false, false, true,
          false, false, false, false,
          false, false, false, true,
          false, false, false, false},
         /* isRightToLeft() results   */
         {false, false, false, false,
          false, false, false, false,
          false, false, false, false,
          false, false, false, false}},

        /* For Text #6 */
         /* isLeftToRight() results */
        {{false, false, false, false,
          false, false, false, false,
          false, false, false, false,
          false, false, false, false},
         /* isRightToLeft() results   */
         {false, false, true,  false,
          false, false, true,  false,
          false, false, false, false,
          false, false, true,  false}},
    };

    /* --------------------------------------------------------------------- */

    /*
     * Test data for reorderVisually() methods
     */

    private static Object[][][] data4reorderVisually = {
        {{"ABC", " ", "abc", " ", ArabicABC, "."},   // Original text
         {"000000001110"},                           // levels
         {"ABC", " ", "abc", " ", ArabicABC, "."}},  // Reordered text

        {{"ABC", " ", HebrewABC, " ", NKoABC, "."},
         {"222111111111"},
         {".", NKoABC, " ", HebrewABC, " ", "ABC"}},

        {{OsmanyaABC, " ", HebrewABC, " ", KharoshthiABC, "."},
         {"222222111111111111"},
         {".", KharoshthiABC, " ", HebrewABC, " ", OsmanyaABC,}},

        {{"ABC", " ", Osmanya123, " ", "\"", OsmanyaABC, " ", Kharoshthi123,
          " ", KharoshthiABC, ".", "\""},
         {"0000000000002222221111111111111100"},
         {"ABC", " ", Osmanya123, " ", "\"", KharoshthiABC, " ", Kharoshthi123,
          " ", OsmanyaABC, ".", "\""}},
    };

}
