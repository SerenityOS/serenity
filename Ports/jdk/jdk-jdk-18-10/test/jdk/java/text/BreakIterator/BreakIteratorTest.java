/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4035266 4052418 4068133 4068137 4068139 4086052 4095322 4097779
 *      4097920 4098467 4111338 4113835 4117554 4143071 4146175 4152117
 *      4152416 4153072 4158381 4214367 4217703 4638433 8264765
 * @library /java/text/testlib
 * @run main/timeout=2000 BreakIteratorTest
 * @summary test BreakIterator
 */

/*
 *
 *
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * Portions copyright (c) 2007 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

public class BreakIteratorTest extends IntlTest
{
    private BreakIterator characterBreak;
    private BreakIterator wordBreak;
    private BreakIterator lineBreak;
    private BreakIterator sentenceBreak;

    public static void main(String[] args) throws Exception {
        new BreakIteratorTest().run(args);
    }

    public BreakIteratorTest()
    {
        characterBreak = BreakIterator.getCharacterInstance();
        wordBreak = BreakIterator.getWordInstance();
        lineBreak = BreakIterator.getLineInstance();
        sentenceBreak = BreakIterator.getSentenceInstance();
    }

    //=========================================================================
    // general test subroutines
    //=========================================================================

    private void generalIteratorTest(BreakIterator bi, Vector expectedResult) {
        StringBuffer buffer = new StringBuffer();
        String text;
        for (int i = 0; i < expectedResult.size(); i++) {
            text = (String)expectedResult.elementAt(i);
            buffer.append(text);
        }
        text = buffer.toString();

        bi.setText(text);

        Vector nextResults = testFirstAndNext(bi, text);
        Vector previousResults = testLastAndPrevious(bi, text);

        logln("comparing forward and backward...");
        int errs = getErrorCount();
        compareFragmentLists("forward iteration", "backward iteration", nextResults,
                        previousResults);
        if (getErrorCount() == errs) {
            logln("comparing expected and actual...");
            compareFragmentLists("expected result", "actual result", expectedResult,
                            nextResults);
        }

        int[] boundaries = new int[expectedResult.size() + 3];
        boundaries[0] = BreakIterator.DONE;
        boundaries[1] = 0;
        for (int i = 0; i < expectedResult.size(); i++)
            boundaries[i + 2] = boundaries[i + 1] + ((String)expectedResult.elementAt(i)).
                            length();
        boundaries[boundaries.length - 1] = BreakIterator.DONE;

        testFollowing(bi, text, boundaries);
        testPreceding(bi, text, boundaries);
        testIsBoundary(bi, text, boundaries);

        doMultipleSelectionTest(bi, text);
    }

    private Vector testFirstAndNext(BreakIterator bi, String text) {
        int p = bi.first();
        int lastP = p;
        Vector<String> result = new Vector<String>();

        if (p != 0)
            errln("first() returned " + p + " instead of 0");
        while (p != BreakIterator.DONE) {
            p = bi.next();
            if (p != BreakIterator.DONE) {
                if (p <= lastP)
                    errln("next() failed to move forward: next() on position "
                                    + lastP + " yielded " + p);

                result.addElement(text.substring(lastP, p));
            }
            else {
                if (lastP != text.length())
                    errln("next() returned DONE prematurely: offset was "
                                    + lastP + " instead of " + text.length());
            }
            lastP = p;
        }
        return result;
    }

    private Vector testLastAndPrevious(BreakIterator bi, String text) {
        int p = bi.last();
        int lastP = p;
        Vector<String> result = new Vector<String>();

        if (p != text.length())
            errln("last() returned " + p + " instead of " + text.length());
        while (p != BreakIterator.DONE) {
            p = bi.previous();
            if (p != BreakIterator.DONE) {
                if (p >= lastP)
                    errln("previous() failed to move backward: previous() on position "
                                    + lastP + " yielded " + p);

                result.insertElementAt(text.substring(p, lastP), 0);
            }
            else {
                if (lastP != 0)
                    errln("previous() returned DONE prematurely: offset was "
                                    + lastP + " instead of 0");
            }
            lastP = p;
        }
        return result;
    }

    private void compareFragmentLists(String f1Name, String f2Name, Vector f1, Vector f2) {
        int p1 = 0;
        int p2 = 0;
        String s1;
        String s2;
        int t1 = 0;
        int t2 = 0;

        while (p1 < f1.size() && p2 < f2.size()) {
            s1 = (String)f1.elementAt(p1);
            s2 = (String)f2.elementAt(p2);
            t1 += s1.length();
            t2 += s2.length();

            if (s1.equals(s2)) {
                debugLogln("   >" + s1 + "<");
                ++p1;
                ++p2;
            }
            else {
                int tempT1 = t1;
                int tempT2 = t2;
                int tempP1 = p1;
                int tempP2 = p2;

                while (tempT1 != tempT2 && tempP1 < f1.size() && tempP2 < f2.size()) {
                    while (tempT1 < tempT2 && tempP1 < f1.size()) {
                        tempT1 += ((String)f1.elementAt(tempP1)).length();
                        ++tempP1;
                    }
                    while (tempT2 < tempT1 && tempP2 < f2.size()) {
                        tempT2 += ((String)f2.elementAt(tempP2)).length();
                        ++tempP2;
                    }
                }
                logln("*** " + f1Name + " has:");
                while (p1 <= tempP1 && p1 < f1.size()) {
                    s1 = (String)f1.elementAt(p1);
                    t1 += s1.length();
                    debugLogln(" *** >" + s1 + "<");
                    ++p1;
                }
                logln("***** " + f2Name + " has:");
                while (p2 <= tempP2 && p2 < f2.size()) {
                    s2 = (String)f2.elementAt(p2);
                    t2 += s2.length();
                    debugLogln(" ***** >" + s2 + "<");
                    ++p2;
                }
                errln("Discrepancy between " + f1Name + " and " + f2Name + "\n---\n" + f1 +"\n---\n" + f2);
            }
        }
    }

    private void testFollowing(BreakIterator bi, String text, int[] boundaries) {
        logln("testFollowing():");
        int p = 2;
        int i = 0;
        try {
            for (i = 0; i <= text.length(); i++) {  // change to <= when new BI code goes in
                if (i == boundaries[p])
                    ++p;

                int b = bi.following(i);
                logln("bi.following(" + i + ") -> " + b);
                if (b != boundaries[p])
                    errln("Wrong result from following() for " + i + ": expected " + boundaries[p]
                          + ", got " + b);
            }
        } catch (IllegalArgumentException illargExp) {
            errln("IllegalArgumentException caught from following() for offset: " + i);
        }
    }

    private void testPreceding(BreakIterator bi, String text, int[] boundaries) {
        logln("testPreceding():");
        int p = 0;
        int i = 0;
        try {
            for (i = 0; i <= text.length(); i++) {  // change to <= when new BI code goes in
                int b = bi.preceding(i);
                logln("bi.preceding(" + i + ") -> " + b);
                if (b != boundaries[p])
                    errln("Wrong result from preceding() for " + i + ": expected " + boundaries[p]
                          + ", got " + b);

                if (i == boundaries[p + 1])
                    ++p;
            }
        } catch (IllegalArgumentException illargExp) {
            errln("IllegalArgumentException caught from preceding() for offset: " + i);
        }
    }

    private void testIsBoundary(BreakIterator bi, String text, int[] boundaries) {
        logln("testIsBoundary():");
        int p = 1;
        boolean isB;
        for (int i = 0; i <= text.length(); i++) {  // change to <= when new BI code goes in
            isB = bi.isBoundary(i);
            logln("bi.isBoundary(" + i + ") -> " + isB);

            if (i == boundaries[p]) {
                if (!isB)
                    errln("Wrong result from isBoundary() for " + i + ": expected true, got false");
                ++p;
            }
            else {
                if (isB)
                    errln("Wrong result from isBoundary() for " + i + ": expected false, got true");
            }
        }
    }

    private void doMultipleSelectionTest(BreakIterator iterator, String testText)
    {
        logln("Multiple selection test...");
        BreakIterator testIterator = (BreakIterator)iterator.clone();
        int offset = iterator.first();
        int testOffset;
        int count = 0;

        do {
            testOffset = testIterator.first();
            testOffset = testIterator.next(count);
            logln("next(" + count + ") -> " + testOffset);
            if (offset != testOffset)
                errln("next(n) and next() not returning consistent results: for step " + count + ", next(n) returned " + testOffset + " and next() had " + offset);

            if (offset != BreakIterator.DONE) {
                count++;
                offset = iterator.next();
            }
        } while (offset != BreakIterator.DONE);

        // now do it backwards...
        offset = iterator.last();
        count = 0;

        do {
            testOffset = testIterator.last();
            testOffset = testIterator.next(count);
            logln("next(" + count + ") -> " + testOffset);
            if (offset != testOffset)
                errln("next(n) and next() not returning consistent results: for step " + count + ", next(n) returned " + testOffset + " and next() had " + offset);

            if (offset != BreakIterator.DONE) {
                count--;
                offset = iterator.previous();
            }
        } while (offset != BreakIterator.DONE);
    }

    private void doBreakInvariantTest(BreakIterator tb, String testChars)
    {
        StringBuffer work = new StringBuffer("aaa");
        int errorCount = 0;

        // a break should always occur after CR (unless followed by LF), LF, PS, and LS
        String breaks = /*"\r\n\u2029\u2028"*/"\n\u2029\u2028";
                            // change this back when new BI code is added

        for (int i = 0; i < breaks.length(); i++) {
            work.setCharAt(1, breaks.charAt(i));
            for (int j = 0; j < testChars.length(); j++) {
                work.setCharAt(0, testChars.charAt(j));
                for (int k = 0; k < testChars.length(); k++) {
                    char c = testChars.charAt(k);

                    // if a cr is followed by lf, don't do the check (they stay together)
                    if (work.charAt(1) == '\r' && (c == '\n'))
                        continue;

                    // CONTROL (Cc) and FORMAT (Cf) Characters are to be ignored
                    // for breaking purposes as per UTR14
                    int type1 = Character.getType(work.charAt(1));
                    int type2 = Character.getType(c);
                    if (type1 == Character.CONTROL || type1 == Character.FORMAT ||
                        type2 == Character.CONTROL || type2 == Character.FORMAT) {
                        continue;
                    }

                    work.setCharAt(2, c);
                    tb.setText(work.toString());
                    boolean seen2 = false;
                    for (int l = tb.first(); l != BreakIterator.DONE; l = tb.next()) {
                        if (l == 2)
                            seen2 = true;
                    }
                    if (!seen2) {
                        errln("No break between U+" + Integer.toHexString((int)(work.charAt(1)))
                                    + " and U+" + Integer.toHexString((int)(work.charAt(2))));
                        errorCount++;
                        if (errorCount >= 75)
                            return;
                    }
                }
            }
        }
    }

    private void doOtherInvariantTest(BreakIterator tb, String testChars)
    {
        StringBuffer work = new StringBuffer("a\r\na");
        int errorCount = 0;

        // a break should never occur between CR and LF
        for (int i = 0; i < testChars.length(); i++) {
            work.setCharAt(0, testChars.charAt(i));
            for (int j = 0; j < testChars.length(); j++) {
                work.setCharAt(3, testChars.charAt(j));
                tb.setText(work.toString());
                for (int k = tb.first(); k != BreakIterator.DONE; k = tb.next())
                    if (k == 2) {
                        errln("Break between CR and LF in string U+" + Integer.toHexString(
                                (int)(work.charAt(0))) + ", U+d U+a U+" + Integer.toHexString(
                                (int)(work.charAt(3))));
                        errorCount++;
                        if (errorCount >= 75)
                            return;
                    }
            }
        }

        // a break should never occur before a non-spacing mark, unless it's preceded
        // by a line terminator
        work.setLength(0);
        work.append("aaaa");
        for (int i = 0; i < testChars.length(); i++) {
            char c = testChars.charAt(i);
            if (c == '\n' || c == '\r' || c == '\u2029' || c == '\u2028' || c == '\u0003')
                continue;
            work.setCharAt(1, c);
            for (int j = 0; j < testChars.length(); j++) {
                c = testChars.charAt(j);
                if (Character.getType(c) != Character.NON_SPACING_MARK && Character.getType(c)
                        != Character.ENCLOSING_MARK)
                    continue;
                work.setCharAt(2, c);

                // CONTROL (Cc) and FORMAT (Cf) Characters are to be ignored
                // for breaking purposes as per UTR14
                int type1 = Character.getType(work.charAt(1));
                int type2 = Character.getType(work.charAt(2));
                if (type1 == Character.CONTROL || type1 == Character.FORMAT ||
                    type2 == Character.CONTROL || type2 == Character.FORMAT) {
                    continue;
                }

                tb.setText(work.toString());
                for (int k = tb.first(); k != BreakIterator.DONE; k = tb.next())
                    if (k == 2) {
                        errln("Break between U+" + Integer.toHexString((int)(work.charAt(1)))
                                + " and U+" + Integer.toHexString((int)(work.charAt(2))));
                        errorCount++;
                        if (errorCount >= 75)
                            return;
                    }
            }
        }
    }

    public void debugLogln(String s) {
        final String zeros = "0000";
        String temp;
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c < '\u007f')
                out.append(c);
            else {
                out.append("\\u");
                temp = Integer.toHexString((int)c);
                out.append(zeros.substring(0, 4 - temp.length()));
                out.append(temp);
            }
        }
        logln(out.toString());
    }

    //=========================================================================
    // tests
    //=========================================================================

    public void TestWordBreak() {

        Vector<String> wordSelectionData = new Vector<String>();

        wordSelectionData.addElement("12,34");

        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\u00A2"); //cent sign
        wordSelectionData.addElement("\u00A3"); //pound sign
        wordSelectionData.addElement("\u00A4"); //currency sign
        wordSelectionData.addElement("\u00A5"); //yen sign
        wordSelectionData.addElement("alpha-beta-gamma");
        wordSelectionData.addElement(".");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("Badges");
        wordSelectionData.addElement("?");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("BADGES");
        wordSelectionData.addElement("!");
        wordSelectionData.addElement("?");
        wordSelectionData.addElement("!");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("We");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("don't");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("need");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("no");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("STINKING");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("BADGES");
        wordSelectionData.addElement("!");
        wordSelectionData.addElement("!");
        wordSelectionData.addElement("!");

        wordSelectionData.addElement("012.566,5");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("123.3434,900");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("1000,233,456.000");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("1,23.322%");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("123.1222");

        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\u0024123,000.20");

        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("179.01\u0025");

        wordSelectionData.addElement("Hello");
        wordSelectionData.addElement(",");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("how");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("are");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("you");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("X");
        wordSelectionData.addElement(" ");

        wordSelectionData.addElement("Now");
        wordSelectionData.addElement("\r");
        wordSelectionData.addElement("is");
        wordSelectionData.addElement("\n");
        wordSelectionData.addElement("the");
        wordSelectionData.addElement("\r\n");
        wordSelectionData.addElement("time");
        wordSelectionData.addElement("\n");
        wordSelectionData.addElement("\r");
        wordSelectionData.addElement("for");
        wordSelectionData.addElement("\r");
        wordSelectionData.addElement("\r");
        wordSelectionData.addElement("all");
        wordSelectionData.addElement(" ");

        generalIteratorTest(wordBreak, wordSelectionData);
    }

    public void TestBug4097779() {
        Vector<String> wordSelectionData = new Vector<String>();

        wordSelectionData.addElement("aa\u0300a");
        wordSelectionData.addElement(" ");

        generalIteratorTest(wordBreak, wordSelectionData);
    }

    public void TestBug4098467Words() {
        Vector<String> wordSelectionData = new Vector<String>();

        // What follows is a string of Korean characters (I found it in the Yellow Pages
        // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
        // it correctly), first as precomposed syllables, and then as conjoining jamo.
        // Both sequences should be semantically identical and break the same way.
        // precomposed syllables...
        wordSelectionData.addElement("\uc0c1\ud56d");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\ud55c\uc778");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\uc5f0\ud569");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\uc7a5\ub85c\uad50\ud68c");
        wordSelectionData.addElement(" ");
        // conjoining jamo...
        wordSelectionData.addElement("\u1109\u1161\u11bc\u1112\u1161\u11bc");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\u1112\u1161\u11ab\u110b\u1175\u11ab");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\u110b\u1167\u11ab\u1112\u1161\u11b8");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\u110c\u1161\u11bc\u1105\u1169\u1100\u116d\u1112\u116c");
        wordSelectionData.addElement(" ");

        generalIteratorTest(wordBreak, wordSelectionData);
    }

    public void TestBug4117554Words() {
        Vector<String> wordSelectionData = new Vector<String>();

        // this is a test for bug #4117554: the ideographic iteration mark (U+3005) should
        // count as a Kanji character for the purposes of word breaking
        wordSelectionData.addElement("abc");
        wordSelectionData.addElement("\u4e01\u4e02\u3005\u4e03\u4e03");
        wordSelectionData.addElement("abc");

        generalIteratorTest(wordBreak, wordSelectionData);
    }

    public void TestSentenceBreak() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        sentenceSelectionData.addElement("This is a simple sample sentence. ");
        sentenceSelectionData.addElement("(This is it.) ");
        sentenceSelectionData.addElement("This is a simple sample sentence. ");
        sentenceSelectionData.addElement("\"This isn\'t it.\" ");
        sentenceSelectionData.addElement("Hi! ");
        sentenceSelectionData.addElement("This is a simple sample sentence. ");
        sentenceSelectionData.addElement("It does not have to make any sense as you can see. ");
        sentenceSelectionData.addElement("Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura. ");
        sentenceSelectionData.addElement("Che la dritta via aveo smarrita. ");
        sentenceSelectionData.addElement("He said, that I said, that you said!! ");

        sentenceSelectionData.addElement("Don't rock the boat.\u2029");

        sentenceSelectionData.addElement("Because I am the daddy, that is why. ");
        sentenceSelectionData.addElement("Not on my time (el timo.)! ");

        sentenceSelectionData.addElement("So what!!\u2029");

        sentenceSelectionData.addElement("\"But now,\" he said, \"I know!\" ");
        sentenceSelectionData.addElement("Harris thumbed down several, including \"Away We Go\" (which became the huge success Oklahoma!). ");
        sentenceSelectionData.addElement("One species, B. anthracis, is highly virulent.\n");
        sentenceSelectionData.addElement("Wolf said about Sounder:\"Beautifully thought-out and directed.\" ");
        sentenceSelectionData.addElement("Have you ever said, \"This is where \tI shall live\"? ");
        sentenceSelectionData.addElement("He answered, \"You may not!\" ");
        sentenceSelectionData.addElement("Another popular saying is: \"How do you do?\". ");
        sentenceSelectionData.addElement("Yet another popular saying is: \'I\'m fine thanks.\' ");
        sentenceSelectionData.addElement("What is the proper use of the abbreviation pp.? ");
        sentenceSelectionData.addElement("Yes, I am definatelly 12\" tall!!");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug4113835() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // test for bug #4113835: \n and \r count as spaces, not as paragraph breaks
        sentenceSelectionData.addElement("Now\ris\nthe\r\ntime\n\rfor\r\rall\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug4111338() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // test for bug #4111338: Don't break sentences at the boundary between CJK
        // and other letters
        sentenceSelectionData.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165:\"JAVA\u821c"
                + "\u8165\u7fc8\u51ce\u306d,\u2494\u56d8\u4ec0\u60b1\u8560\u51ba"
                + "\u611d\u57b6\u2510\u5d46\".\u2029");
        sentenceSelectionData.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8"
                + "\u97e4JAVA\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8\u4ec0"
                + "\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");
        sentenceSelectionData.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8\u97e4"
                + "\u6470\u8790JAVA\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8"
                + "\u4ec0\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");
        sentenceSelectionData.addElement("He said, \"I can go there.\"\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug4117554Sentences() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // Treat fullwidth variants of .!? the same as their
        // normal counterparts
        sentenceSelectionData.addElement("I know I'm right\uff0e ");
        sentenceSelectionData.addElement("Right\uff1f ");
        sentenceSelectionData.addElement("Right\uff01 ");

        // Don't break sentences at boundary between CJK and digits
        sentenceSelectionData.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8"
                + "\u97e48888\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8\u4ec0"
                + "\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");

        // Break sentence between a sentence terminator and
        // opening punctuation
        sentenceSelectionData.addElement("no?");
        sentenceSelectionData.addElement("(yes)");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug4158381() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // Don't break sentence after period if it isn't followed by a space
        sentenceSelectionData.addElement("Test <code>Flags.Flag</code> class.  ");
        sentenceSelectionData.addElement("Another test.\u2029");

        // No breaks when there are no terminators around
        sentenceSelectionData.addElement("<P>Provides a set of "
                + "&quot;lightweight&quot; (all-java<FONT SIZE=\"-2\"><SUP>TM"
                + "</SUP></FONT> language) components that, "
                + "to the maximum degree possible, work the same on all platforms.  ");
        sentenceSelectionData.addElement("Another test.\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug4143071() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // Make sure sentences that end with digits work right
        sentenceSelectionData.addElement("Today is the 27th of May, 1998.  ");
        sentenceSelectionData.addElement("Tomorrow with be 28 May 1998.  ");
        sentenceSelectionData.addElement("The day after will be the 30th.\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug4152416() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // Make sure sentences ending with a capital letter are treated correctly
        sentenceSelectionData.addElement("The type of all primitive "
                + "<code>boolean</code> values accessed in the target VM.  ");
        sentenceSelectionData.addElement("Calls to xxx will return an "
                + "implementor of this interface.\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug4152117() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // Make sure sentence breaking is handling punctuation correctly
        // [COULD NOT REPRODUCE THIS BUG, BUT TEST IS HERE TO MAKE SURE
        // IT DOESN'T CROP UP]
        sentenceSelectionData.addElement("Constructs a randomly generated "
                + "BigInteger, uniformly distributed over the range <tt>0</tt> "
                + "to <tt>(2<sup>numBits</sup> - 1)</tt>, inclusive.  ");
        sentenceSelectionData.addElement("The uniformity of the distribution "
                + "assumes that a fair source of random bits is provided in "
                + "<tt>rnd</tt>.  ");
        sentenceSelectionData.addElement("Note that this constructor always "
                + "constructs a non-negative BigInteger.\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug8264765() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // Comma should not be regarded as the start of a sentence,
        // otherwise the backwards rule would break the following sentence.
        sentenceSelectionData.addElement(
            "Due to a problem (e.g., software bug), the server is down. ");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestLineBreak() {
        Vector<String> lineSelectionData = new Vector<String>();

        lineSelectionData.addElement("Multi-");
        lineSelectionData.addElement("Level ");
        lineSelectionData.addElement("example ");
        lineSelectionData.addElement("of ");
        lineSelectionData.addElement("a ");
        lineSelectionData.addElement("semi-");
        lineSelectionData.addElement("idiotic ");
        lineSelectionData.addElement("non-");
        lineSelectionData.addElement("sensical ");
        lineSelectionData.addElement("(non-");
        lineSelectionData.addElement("important) ");
        lineSelectionData.addElement("sentence. ");

        lineSelectionData.addElement("Hi  ");
        lineSelectionData.addElement("Hello ");
        lineSelectionData.addElement("How\n");
        lineSelectionData.addElement("are\r");
        lineSelectionData.addElement("you\u2028");
        lineSelectionData.addElement("fine.\t");
        lineSelectionData.addElement("good.  ");

        lineSelectionData.addElement("Now\r");
        lineSelectionData.addElement("is\n");
        lineSelectionData.addElement("the\r\n");
        lineSelectionData.addElement("time\n");
        lineSelectionData.addElement("\r");
        lineSelectionData.addElement("for\r");
        lineSelectionData.addElement("\r");
        lineSelectionData.addElement("all");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    public void TestBug4068133() {
        Vector<String> lineSelectionData = new Vector<String>();

        lineSelectionData.addElement("\u96f6");
        lineSelectionData.addElement("\u4e00\u3002");
        lineSelectionData.addElement("\u4e8c\u3001");
        lineSelectionData.addElement("\u4e09\u3002\u3001");
        lineSelectionData.addElement("\u56db\u3001\u3002\u3001");
        lineSelectionData.addElement("\u4e94,");
        lineSelectionData.addElement("\u516d.");
        lineSelectionData.addElement("\u4e03.\u3001,\u3002");
        lineSelectionData.addElement("\u516b");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    public void TestBug4086052() {
        Vector<String> lineSelectionData = new Vector<String>();

        lineSelectionData.addElement("foo\u00a0bar ");
//        lineSelectionData.addElement("foo\ufeffbar");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    public void TestBug4097920() {
        Vector<String> lineSelectionData = new Vector<String>();

        lineSelectionData.addElement("dog,");
        lineSelectionData.addElement("cat,");
        lineSelectionData.addElement("mouse ");
        lineSelectionData.addElement("(one)");
        lineSelectionData.addElement("(two)\n");

        generalIteratorTest(lineBreak, lineSelectionData);
    }
    /*
    public void TestBug4035266() {
        Vector<String> lineSelectionData = new Vector<String>();

        lineSelectionData.addElement("The ");
        lineSelectionData.addElement("balance ");
        lineSelectionData.addElement("is ");
        lineSelectionData.addElement("$-23,456.78, ");
        lineSelectionData.addElement("not ");
        lineSelectionData.addElement("-$32,456.78!\n");

        generalIteratorTest(lineBreak, lineSelectionData);
    }
    */
    public void TestBug4098467Lines() {
        Vector<String> lineSelectionData = new Vector<String>();

        // What follows is a string of Korean characters (I found it in the Yellow Pages
        // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
        // it correctly), first as precomposed syllables, and then as conjoining jamo.
        // Both sequences should be semantically identical and break the same way.
        // precomposed syllables...
        lineSelectionData.addElement("\uc0c1");
        lineSelectionData.addElement("\ud56d ");
        lineSelectionData.addElement("\ud55c");
        lineSelectionData.addElement("\uc778 ");
        lineSelectionData.addElement("\uc5f0");
        lineSelectionData.addElement("\ud569 ");
        lineSelectionData.addElement("\uc7a5");
        lineSelectionData.addElement("\ub85c");
        lineSelectionData.addElement("\uad50");
        lineSelectionData.addElement("\ud68c ");
        // conjoining jamo...
        lineSelectionData.addElement("\u1109\u1161\u11bc\u1112\u1161\u11bc ");
        lineSelectionData.addElement("\u1112\u1161\u11ab\u110b\u1175\u11ab ");
        lineSelectionData.addElement("\u110b\u1167\u11ab\u1112\u1161\u11b8 ");
        lineSelectionData.addElement("\u110c\u1161\u11bc\u1105\u1169\u1100\u116d\u1112\u116c");

        if (Locale.getDefault().getLanguage().equals("th")) {
            logln("This test is skipped in th locale.");
            return;
        }

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    public void TestBug4117554Lines() {
        Vector<String> lineSelectionData = new Vector<String>();

        // Fullwidth .!? should be treated as postJwrd
        lineSelectionData.addElement("\u4e01\uff0e");
        lineSelectionData.addElement("\u4e02\uff01");
        lineSelectionData.addElement("\u4e03\uff1f");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    public void TestBug4217703() {
        if (Locale.getDefault().getLanguage().equals("th")) {
            logln("This test is skipped in th locale.");
            return;
        }

        Vector<String> lineSelectionData = new Vector<String>();

        // There shouldn't be a line break between sentence-ending punctuation
        // and a closing quote
        lineSelectionData.addElement("He ");
        lineSelectionData.addElement("said ");
        lineSelectionData.addElement("\"Go!\"  ");
        lineSelectionData.addElement("I ");
        lineSelectionData.addElement("went.  ");

        lineSelectionData.addElement("Hashtable$Enumeration ");
        lineSelectionData.addElement("getText().");
        lineSelectionData.addElement("getIndex()");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    private static final String graveS = "S\u0300";
    private static final String acuteBelowI = "i\u0317";
    private static final String acuteE = "e\u0301";
    private static final String circumflexA = "a\u0302";
    private static final String tildeE = "e\u0303";

    public void TestCharacterBreak() {
        Vector<String> characterSelectionData = new Vector<String>();

        characterSelectionData.addElement(graveS);
        characterSelectionData.addElement(acuteBelowI);
        characterSelectionData.addElement("m");
        characterSelectionData.addElement("p");
        characterSelectionData.addElement("l");
        characterSelectionData.addElement(acuteE);
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("s");
        characterSelectionData.addElement(circumflexA);
        characterSelectionData.addElement("m");
        characterSelectionData.addElement("p");
        characterSelectionData.addElement("l");
        characterSelectionData.addElement(tildeE);
        characterSelectionData.addElement(".");
        characterSelectionData.addElement("w");
        characterSelectionData.addElement(circumflexA);
        characterSelectionData.addElement("w");
        characterSelectionData.addElement("a");
        characterSelectionData.addElement("f");
        characterSelectionData.addElement("q");
        characterSelectionData.addElement("\n");
        characterSelectionData.addElement("\r");
        characterSelectionData.addElement("\r\n");
        characterSelectionData.addElement("\n");

        generalIteratorTest(characterBreak, characterSelectionData);
    }

    public void TestBug4098467Characters() {
        Vector<String> characterSelectionData = new Vector<String>();

        // What follows is a string of Korean characters (I found it in the Yellow Pages
        // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
        // it correctly), first as precomposed syllables, and then as conjoining jamo.
        // Both sequences should be semantically identical and break the same way.
        // precomposed syllables...
        characterSelectionData.addElement("\uc0c1");
        characterSelectionData.addElement("\ud56d");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\ud55c");
        characterSelectionData.addElement("\uc778");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\uc5f0");
        characterSelectionData.addElement("\ud569");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\uc7a5");
        characterSelectionData.addElement("\ub85c");
        characterSelectionData.addElement("\uad50");
        characterSelectionData.addElement("\ud68c");
        characterSelectionData.addElement(" ");
        // conjoining jamo...
        characterSelectionData.addElement("\u1109\u1161\u11bc");
        characterSelectionData.addElement("\u1112\u1161\u11bc");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\u1112\u1161\u11ab");
        characterSelectionData.addElement("\u110b\u1175\u11ab");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\u110b\u1167\u11ab");
        characterSelectionData.addElement("\u1112\u1161\u11b8");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\u110c\u1161\u11bc");
        characterSelectionData.addElement("\u1105\u1169");
        characterSelectionData.addElement("\u1100\u116d");
        characterSelectionData.addElement("\u1112\u116c");

        generalIteratorTest(characterBreak, characterSelectionData);
    }

    public void TestBug4153072() {
        BreakIterator iter = BreakIterator.getWordInstance();
        String str = "...Hello, World!...";
        int begin = 3;
        int end = str.length() - 3;
        boolean gotException = false;
        boolean dummy;

        iter.setText(new StringCharacterIterator(str, begin, end, begin));
        for (int index = -1; index < begin + 1; ++index) {
            try {
                dummy = iter.isBoundary(index);
                if (index < begin)
                    errln("Didn't get exception with offset = " + index +
                                    " and begin index = " + begin);
            }
            catch (IllegalArgumentException e) {
                if (index >= begin)
                    errln("Got exception with offset = " + index +
                                    " and begin index = " + begin);
            }
        }
    }

    public void TestBug4146175Sentences() {
        Vector<String> sentenceSelectionData = new Vector<String>();

        // break between periods and opening punctuation even when there's no
        // intervening space
        sentenceSelectionData.addElement("end.");
        sentenceSelectionData.addElement("(This is\u2029");

        // treat the fullwidth period as an unambiguous sentence terminator
        sentenceSelectionData.addElement("\u7d42\u308f\u308a\uff0e");
        sentenceSelectionData.addElement("\u300c\u3053\u308c\u306f");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestBug4146175Lines() {
        if (Locale.getDefault().getLanguage().equals("th")) {
            logln("This test is skipped in th locale.");
            return;
        }

        Vector<String> lineSelectionData = new Vector<String>();

        // the fullwidth comma should stick to the preceding Japanese character
        lineSelectionData.addElement("\u7d42\uff0c");
        lineSelectionData.addElement("\u308f");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    public void TestBug4214367() {
        if (Locale.getDefault().getLanguage().equals("th")) {
            logln("This test is skipped in th locale.");
            return;
        }

        Vector<String> wordSelectionData = new Vector<String>();

        // the hiragana and katakana iteration marks and the long vowel mark
        // are not being treated correctly by the word-break iterator
        wordSelectionData.addElement("\u3042\u3044\u309d\u3042\u309e\u3042\u30fc\u3042");
        wordSelectionData.addElement("\u30a2\u30a4\u30fd\u30a2\u30fe\u30a2\u30fc\u30a2");

        generalIteratorTest(wordBreak, wordSelectionData);
    }

    private static final String cannedTestChars // characters fo the class Cc are ignorable for breaking
        = /*"\u0000\u0001\u0002\u0003\u0004*/" !\"#$%&()+-01234<=>ABCDE[]^_`abcde{}|\u00a0\u00a2"
        + "\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9\u00ab\u00ad\u00ae\u00af\u00b0\u00b2\u00b3"
        + "\u00b4\u00b9\u00bb\u00bc\u00bd\u02b0\u02b1\u02b2\u02b3\u02b4\u0300\u0301\u0302\u0303"
        + "\u0304\u05d0\u05d1\u05d2\u05d3\u05d4\u0903\u093e\u093f\u0940\u0949\u0f3a\u0f3b\u2000"
        + "\u2001\u2002\u200c\u200d\u200e\u200f\u2010\u2011\u2012\u2028\u2029\u202a\u203e\u203f"
        + "\u2040\u20dd\u20de\u20df\u20e0\u2160\u2161\u2162\u2163\u2164";

    public void TestSentenceInvariants()
    {
        BreakIterator e = BreakIterator.getSentenceInstance();
        doOtherInvariantTest(e, cannedTestChars + ".,\u3001\u3002\u3041\u3042\u3043\ufeff");
    }

    public void TestWordInvariants()
    {
        if (Locale.getDefault().getLanguage().equals("th")) {
            logln("This test is skipped in th locale.");
            return;
        }

        BreakIterator e = BreakIterator.getWordInstance();
        doBreakInvariantTest(e, cannedTestChars + "\',.\u3041\u3042\u3043\u309b\u309c\u30a1\u30a2"
            + "\u30a3\u4e00\u4e01\u4e02");
        doOtherInvariantTest(e, cannedTestChars + "\',.\u3041\u3042\u3043\u309b\u309c\u30a1\u30a2"
            + "\u30a3\u4e00\u4e01\u4e02");
    }

    public void TestLineInvariants()
    {
        if (Locale.getDefault().getLanguage().equals("th")) {
            logln("This test is skipped in th locale.");
            return;
        }

        BreakIterator e = BreakIterator.getLineInstance();
        String testChars = cannedTestChars + ".,;:\u3001\u3002\u3041\u3042\u3043\u3044\u3045"
            + "\u30a3\u4e00\u4e01\u4e02";
        doBreakInvariantTest(e, testChars);
        doOtherInvariantTest(e, testChars);

        int errorCount = 0;

        // in addition to the other invariants, a line-break iterator should make sure that:
        // it doesn't break around the non-breaking characters
        String noBreak = "\u00a0\u2007\u2011\ufeff";
        StringBuffer work = new StringBuffer("aaa");
        for (int i = 0; i < testChars.length(); i++) {
            char c = testChars.charAt(i);
            if (c == '\r' || c == '\n' || c == '\u2029' || c == '\u2028' || c == '\u0003')
                continue;
            work.setCharAt(0, c);
            for (int j = 0; j < noBreak.length(); j++) {
                work.setCharAt(1, noBreak.charAt(j));
                for (int k = 0; k < testChars.length(); k++) {
                    work.setCharAt(2, testChars.charAt(k));
                    // CONTROL (Cc) and FORMAT (Cf) Characters are to be ignored
                    // for breaking purposes as per UTR14
                    int type1 = Character.getType(work.charAt(1));
                    int type2 = Character.getType(work.charAt(2));
                    if (type1 == Character.CONTROL || type1 == Character.FORMAT ||
                        type2 == Character.CONTROL || type2 == Character.FORMAT) {
                        continue;
                    }
                    e.setText(work.toString());
                    for (int l = e.first(); l != BreakIterator.DONE; l = e.next()) {
                        if (l == 1 || l == 2) {
                            //errln("Got break between U+" + Integer.toHexString((int)
                            //        (work.charAt(l - 1))) + " and U+" + Integer.toHexString(
                            //        (int)(work.charAt(l))) + "\ntype1 = " + type1 + "\ntype2 = " + type2);
                            // as per UTR14 spaces followed by a GLUE character should allow
                            // line breaking
                            if (work.charAt(l-1) == '\u0020' && (work.charAt(l) == '\u00a0' ||
                                                                 work.charAt(l) == '\u0f0c' ||
                                                                 work.charAt(l) == '\u2007' ||
                                                                 work.charAt(l) == '\u2011' ||
                                                                 work.charAt(l) == '\u202f' ||
                                                                 work.charAt(l) == '\ufeff')) {
                                continue;
                            }
                            errln("Got break between U+" + Integer.toHexString((int)
                                    (work.charAt(l - 1))) + " and U+" + Integer.toHexString(
                                    (int)(work.charAt(l))));
                            errorCount++;
                            if (errorCount >= 75)
                                return;
                        }
                    }
                }
            }
        }

        // The following test has so many exceptions that it would be better to write a new set of data
        // that tested exactly what should be tested
        // Until that point it will be commented out
        /*

        // it does break after dashes (unless they're followed by a digit, a non-spacing mark,
        // a currency symbol, a space, a format-control character, a regular control character,
        // a line or paragraph separator, or another dash)
        String dashes = "-\u00ad\u2010\u2012\u2013\u2014";
        for (int i = 0; i < testChars.length(); i++) {
            work.setCharAt(0, testChars.charAt(i));
            for (int j = 0; j < dashes.length(); j++) {
                work.setCharAt(1, dashes.charAt(j));
                for (int k = 0; k < testChars.length(); k++) {
                    char c = testChars.charAt(k);
                    if (Character.getType(c) == Character.DECIMAL_DIGIT_NUMBER ||
                        Character.getType(c) == Character.OTHER_NUMBER ||
                        Character.getType(c) == Character.NON_SPACING_MARK ||
                        Character.getType(c) == Character.ENCLOSING_MARK ||
                        Character.getType(c) == Character.CURRENCY_SYMBOL ||
                        Character.getType(c) == Character.DASH_PUNCTUATION ||
                        Character.getType(c) == Character.SPACE_SEPARATOR ||
                        Character.getType(c) == Character.FORMAT ||
                        Character.getType(c) == Character.CONTROL ||
                        Character.getType(c) == Character.END_PUNCTUATION ||
                        Character.getType(c) == Character.FINAL_QUOTE_PUNCTUATION ||
                        Character.getType(c) == Character.OTHER_PUNCTUATION ||
                        c == '\'' || c == '\"' ||
                        // category EX as per UTR14
                        c == '!' || c == '?' || c == '\ufe56' || c == '\ufe57' || c == '\uff01' || c == '\uff1f' ||
                        c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029' ||
                        c == '\u0003' || c == '\u2007' || c == '\u2011' ||
                        c == '\ufeff')
                        continue;
                    work.setCharAt(2, c);
                    e.setText(work.toString());
                    boolean saw2 = false;
                    for (int l = e.first(); l != BreakIterator.DONE; l = e.next())
                        if (l == 2)
                            saw2 = true;
                    if (!saw2) {
                        errln("Didn't get break between U+" + Integer.toHexString((int)
                                    (work.charAt(1))) + " and U+" + Integer.toHexString(
                                    (int)(work.charAt(2))));
                        errorCount++;
                        if (errorCount >= 75)
                            return;
                    }
                }
            }
        }
        */
    }

    public void TestCharacterInvariants()
    {
        BreakIterator e = BreakIterator.getCharacterInstance();
        doBreakInvariantTest(e, cannedTestChars + "\u1100\u1101\u1102\u1160\u1161\u1162\u11a8"
            + "\u11a9\u11aa");
        doOtherInvariantTest(e, cannedTestChars + "\u1100\u1101\u1102\u1160\u1161\u1162\u11a8"
            + "\u11a9\u11aa");
    }

    public void TestEmptyString()
    {
        String text = "";
        Vector<String> x = new Vector<String>();
        x.addElement(text);

        generalIteratorTest(lineBreak, x);
    }

    public void TestGetAvailableLocales()
    {
        Locale[] locList = BreakIterator.getAvailableLocales();

        if (locList.length == 0)
            errln("getAvailableLocales() returned an empty list!");
        // I have no idea how to test this function...
    }


    /**
     * Bug 4095322
     */
    public void TestJapaneseLineBreak()
    {
        StringBuffer testString = new StringBuffer("\u4e00x\u4e8c");
        // Breaking on <Kanji>$<Kanji> is inconsistent

        /* Characters in precedingChars and followingChars have been updated
         * from Unicode 2.0.14-based to 3.0.0-based when 4638433 was fixed.
         * In concrete terms,
         *   0x301F : Its category was changed from Ps to Pe since Unicode 2.1.
         *   0x169B & 0x169C : added since Unicode 3.0.0.
         */
        String precedingChars =
            /* Puctuation, Open */
          "([{\u201a\u201e\u2045\u207d\u208d\u2329\u3008\u300a\u300c\u300e\u3010\u3014\u3016\u3018\u301a\u301d\ufe35\ufe37\ufe39\ufe3b\ufe3d\ufe3f\ufe41\ufe43\ufe59\ufe5b\ufe5d\uff08\uff3b\uff5b\uff62\u169b"
            /* Punctuation, Initial quote */
          + "\u00ab\u2018\u201b\u201c\u201f\u2039"
            /* Symbol, Currency */
          + "\u00a5\u00a3\u00a4\u20a0";

        String followingChars =
            /* Puctuation, Close */
          ")]}\u2046\u207e\u208e\u232a\u3009\u300b\u300d\u300f\u3011\u3015\u3017\u3019\u301b\u301e\u301f\ufd3e\ufe36\ufe38\ufe3a\ufe3c\ufe3e\ufe40\ufe42\ufe44\ufe5a\ufe5c\ufe5e\uff09\uff3d\uff5d\uff63\u169c"
            /* Punctuation, Final quote */
          + "\u00bb\u2019\u201d\u203a"
            /* Punctuation, Other */
          + "!%,.:;\u3001\u3002\u2030\u2031\u2032\u2033\u2034"
            /* Punctuation, Dash */
          + "\u2103\u2109"
            /* Symbol, Currency */
          + "\u00a2"
            /* Letter, Modifier */
          + "\u3005\u309d\u309e"
            /* Letter, Other */
          + "\u3063\u3083\u3085\u3087\u30c3\u30e3\u30e5\u30e7\u30fc\u30fd\u30fe"
           /* Mark, Non-Spacing */
          + "\u0300\u0301\u0302"
            /* Symbol, Modifier */
          + "\u309b\u309c"
            /* Symbol, Other */
          + "\u00b0";

        BreakIterator iter = BreakIterator.getLineInstance(Locale.JAPAN);

        for (int i = 0; i < precedingChars.length(); i++) {
            testString.setCharAt(1, precedingChars.charAt(i));
            iter.setText(testString.toString());
            int j = iter.first();
            if (j != 0) {
                errln("ja line break failure: failed to start at 0 and bounced at " + j);
            }
            j = iter.next();
            if (j != 1) {
                errln("ja line break failure: failed to stop before '"
                        + precedingChars.charAt(i) + "' (\\u"
                        + Integer.toString(precedingChars.charAt(i), 16)
                        + ") at 1 and bounded at " + j);
            }
            j = iter.next();
            if (j != 3) {
                errln("ja line break failure: failed to skip position after '"
                        + precedingChars.charAt(i) + "' (\\u"
                        + Integer.toString(precedingChars.charAt(i), 16)
                        + ") at 3 and bounded at " + j);
            }
        }

        for (int i = 0; i < followingChars.length(); i++) {
            testString.setCharAt(1, followingChars.charAt(i));
            iter.setText(testString.toString());
            int j = iter.first();
            if (j != 0) {
                errln("ja line break failure: failed to start at 0 and bounded at " + j);
            }
            j = iter.next();
            if (j != 2) {
                errln("ja line break failure: failed to skip position before '"
                        + followingChars.charAt(i) + "' (\\u"
                        + Integer.toString(followingChars.charAt(i), 16)
                        + ") at 2 and bounded at " + j);
            }
            j = iter.next();
            if (j != 3) {
                errln("ja line break failure: failed to stop after '"
                        + followingChars.charAt(i) + "' (\\u"
                        + Integer.toString(followingChars.charAt(i), 16)
                        + ") at 3 and bounded at " + j);
            }
        }
    }

    /**
     * Bug 4638433
     */
    public void TestLineBreakBasedOnUnicode3_0_0()
    {
        BreakIterator iter;
        int i;

        /* Latin Extend-B characters
         * 0x0218-0x0233 which have been added since Unicode 3.0.0.
         */
        iter = BreakIterator.getWordInstance(Locale.US);
        iter.setText("\u0216\u0217\u0218\u0219\u021A");
        i = iter.first();
        i = iter.next();
        if (i != 5) {
            errln("Word break failure: failed to stop at 5 and bounded at " + i);
        }


        iter = BreakIterator.getLineInstance(Locale.US);

        /* <Three(Nd)><Two(Nd)><Low Double Prime Quotation Mark(Pe)><One(Nd)>
         * \u301f has changed its category from Ps to Pe since Unicode 2.1.
         */
        iter.setText("32\u301f1");
        i = iter.first();
        i = iter.next();
        if (i != 3) {
            errln("Line break failure: failed to skip before \\u301F(Pe) at 3 and bounded at " + i);
        }

        /* Mongolian <Letter A(Lo)><Todo Soft Hyphen(Pd)><Letter E(Lo)>
         * which have been added since Unicode 3.0.0.
         */
        iter.setText("\u1820\u1806\u1821");
        i = iter.first();
        i = iter.next();
        if (i != 2) {
            errln("Mongolian line break failure: failed to skip position before \\u1806(Pd) at 2 and bounded at " + i);
        }

        /* Khmer <ZERO(Nd)><Currency Symbol(Sc)><ONE(Nd)> which have
         * been added since Unicode 3.0.0.
         */
        iter.setText("\u17E0\u17DB\u17E1");
        i = iter.first();
        i = iter.next();
        if (i != 1) {
            errln("Khmer line break failure: failed to stop before \\u17DB(Sc) at 1 and bounded at " + i);
        }
        i = iter.next();
        if (i != 3) {
            errln("Khmer line break failure: failed to skip position after \\u17DB(Sc) at 3 and bounded at " + i);
        }

        /* Ogham <Letter UR(Lo)><Space Mark(Zs)><Letter OR(Lo)> which have
         * been added since Unicode 3.0.0.
         */
        iter.setText("\u1692\u1680\u1696");
        i = iter.first();
        i = iter.next();
        if (i != 2) {
            errln("Ogham line break failure: failed to skip postion before \\u1680(Zs) at 2 and bounded at " + i);
        }


        // Confirm changes in BreakIteratorRules_th.java have been reflected.
        iter = BreakIterator.getLineInstance(new Locale("th", ""));

        /* Thai <Seven(Nd)>
         *      <Left Double Quotation Mark(Pi)>
         *      <Five(Nd)>
         *      <Right Double Quotation Mark(Pf)>
         *      <Three(Nd)>
         */
        iter.setText("\u0E57\u201C\u0E55\u201D\u0E53");
        i = iter.first();
        i = iter.next();
        if (i != 1) {
            errln("Thai line break failure: failed to stop before \\u201C(Pi) at 1 and bounded at " + i);
        }
        i = iter.next();
        if (i != 4) {
            errln("Thai line break failure: failed to stop after \\u201D(Pf) at 4 and bounded at " + i);
        }
    }

    /**
     * Bug 4068137
     */
    public void TestEndBehavior()
    {
        String testString = "boo.";
        BreakIterator wb = BreakIterator.getWordInstance();
        wb.setText(testString);

        if (wb.first() != 0)
            errln("Didn't get break at beginning of string.");
        if (wb.next() != 3)
            errln("Didn't get break before period in \"boo.\"");
        if (wb.current() != 4 && wb.next() != 4)
            errln("Didn't get break at end of string.");
    }

    // [serialization test has been removed pursuant to bug #4152965]

    /**
     * Bug 4450804
     */
    public void TestLineBreakContractions() {
        Vector<String> expected = new Vector<String>();

        expected.add("These ");
        expected.add("are ");
        expected.add("'foobles'. ");
        expected.add("Don't ");
        expected.add("you ");
        expected.add("like ");
        expected.add("them?");
        generalIteratorTest(lineBreak, expected);
    }

}
