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
 * @bug 4048446 4051866 4053636 4054238 4054734 4054736 4058613 4059820 4060154
 *      4062418 4065540 4066189 4066696 4076676 4078588 4079231 4081866 4087241
 *      4087243 4092260 4095316 4101940 4103436 4114076 4114077 4124632 4132736
 *      4133509 4139572 4141640 4179126 4179686 4244884 4663220
 * @library /java/text/testlib
 * @summary Regression tests for Collation and associated classes
 * @modules jdk.localedata
 */
/*
(C) Copyright Taligent, Inc. 1996 - All Rights Reserved
(C) Copyright IBM Corp. 1996 - All Rights Reserved

  The original version of this source code and documentation is copyrighted and
owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These materials are
provided under terms of a License Agreement between Taligent and Sun. This
technology is protected by multiple US and International patents. This notice and
attribution to Taligent may not be removed.
  Taligent is a registered trademark of Taligent, Inc.
*/

import java.text.*;
import java.util.Locale;
import java.util.Vector;


public class Regression extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new Regression().run(args);
    }

    // CollationElementIterator.reset() doesn't work
    //
    public void Test4048446() {
        CollationElementIterator i1 = en_us.getCollationElementIterator(test1);
        CollationElementIterator i2 = en_us.getCollationElementIterator(test1);

        while ( i1.next() != CollationElementIterator.NULLORDER ) {
        }
        i1.reset();

        assertEqual(i1, i2);
    }


    // Collator -> rules -> Collator round-trip broken for expanding characters
    //
    public void Test4051866() throws ParseException {
        // Build a collator containing expanding characters
        RuleBasedCollator c1 = new RuleBasedCollator("< o "
                                                    +"& oe ,o\u3080"
                                                    +"& oe ,\u1530 ,O"
                                                    +"& OE ,O\u3080"
                                                    +"& OE ,\u1520"
                                                    +"< p ,P");

        // Build another using the rules from  the first
        RuleBasedCollator c2 = new RuleBasedCollator(c1.getRules());

        // Make sure they're the same
        if (!c1.getRules().equals(c2.getRules())) {
            errln("Rules are not equal");
        }
    }

    // Collator thinks "black-bird" == "black"
    //
    public void Test4053636() {
        if (en_us.equals("black-bird","black")) {
            errln("black-bird == black");
        }
    }


    // CollationElementIterator will not work correctly if the associated
    // Collator object's mode is changed
    //
    public void Test4054238() {
        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();

        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        CollationElementIterator i1 = en_us.getCollationElementIterator(test3);

        c.setDecomposition(Collator.NO_DECOMPOSITION);
        CollationElementIterator i2 = en_us.getCollationElementIterator(test3);

        // At this point, BOTH iterators should use NO_DECOMPOSITION, since the
        // collator itself is in that mode
        assertEqual(i1, i2);
    }

    // Collator.IDENTICAL documented but not implemented
    //
    public void Test4054734() {
        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();
        try {
            c.setStrength(Collator.IDENTICAL);
        }
        catch (Exception e) {
            errln("Caught " + e.toString() + " setting Collator.IDENTICAL");
        }

        String[] decomp = {
            "\u0001",   "<",    "\u0002",
            "\u0001",   "=",    "\u0001",
            "A\u0001",  ">",    "~\u0002",      // Ensure A and ~ are not compared bitwise
            "\u00C0",   "=",    "A\u0300"       // Decomp should make these equal
        };
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        compareArray(c, decomp);

        String[] nodecomp = {
            "\u00C0",   ">",    "A\u0300"       // A-grave vs. A combining-grave
        };
        c.setDecomposition(Collator.NO_DECOMPOSITION);
        compareArray(c, nodecomp);
    }

    // Full Decomposition mode not implemented
    //
    public void Test4054736() {
        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();
        c.setDecomposition(Collator.FULL_DECOMPOSITION);

        String[] tests = {
            "\uFB4f", "=", "\u05D0\u05DC",  // Alef-Lamed vs. Alef, Lamed
        };

        compareArray(c, tests);
    }

    // Collator.getInstance() causes an ArrayIndexOutofBoundsException for Korean
    //
    public void Test4058613() {
        // Creating a default collator doesn't work when Korean is the default
        // locale

        Locale oldDefault = Locale.getDefault();

        Locale.setDefault( Locale.KOREAN );
        try {
            Collator c = Collator.getInstance();

            // Since the fix to this bug was to turn of decomposition for Korean collators,
            // ensure that's what we got
            if (c.getDecomposition() != Collator.NO_DECOMPOSITION) {
              errln("Decomposition is not set to NO_DECOMPOSITION");
            }
        }
        finally {
            Locale.setDefault(oldDefault);
        }
    }

    // RuleBasedCollator.getRules does not return the exact pattern as input
    // for expanding character sequences
    //
    public void Test4059820() {
        RuleBasedCollator c = null;
        try {
            c = new RuleBasedCollator("< a < b , c/a < d < z");
        } catch (ParseException e) {
            errln("Exception building collator: " + e.toString());
            return;
        }
        if ( c.getRules().indexOf("c/a") == -1) {
            errln("returned rules do not contain 'c/a'");
        }
    }

    // MergeCollation::fixEntry broken for "& H < \u0131, \u0130, i, I"
    //
    public void Test4060154() {
        RuleBasedCollator c = null;
        try {
            c = new RuleBasedCollator("< g, G < h, H < i, I < j, J"
                                      + " & H < \u0131, \u0130, i, I" );
        } catch (ParseException e) {
            errln("Exception building collator: " + e.toString());
            return;
        }
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);

        String[] tertiary = {
            "A",        "<",    "B",
            "H",        "<",    "\u0131",
            "H",        "<",    "I",
            "\u0131",   "<",    "\u0130",
            "\u0130",   "<",    "i",
            "\u0130",   ">",    "H",
        };
        c.setStrength(Collator.TERTIARY);
        compareArray(c, tertiary);

        String[] secondary = {
            "H",        "<",    "I",
            "\u0131",   "=",    "\u0130",
        };
        c.setStrength(Collator.PRIMARY);
        compareArray(c, secondary);
    };

    // Secondary/Tertiary comparison incorrect in French Secondary
    //
    public void Test4062418() throws ParseException {
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.FRANCE);
        c.setStrength(Collator.SECONDARY);

        String[] tests = {
                "p\u00eache",    "<",    "p\u00e9ch\u00e9",    // Comparing accents from end, p\u00e9ch\u00e9 is greater
        };

        compareArray(c, tests);
    }

    // Collator.compare() method broken if either string contains spaces
    //
    public void Test4065540() {
        if (en_us.compare("abcd e", "abcd f") == 0) {
            errln("'abcd e' == 'abcd f'");
        }
    }

    // Unicode characters need to be recursively decomposed to get the
    // correct result. For example,
    // u1EB1 -> \u0103 + \u0300 -> a + \u0306 + \u0300.
    //
    public void Test4066189() {
        String test1 = "\u1EB1";
        String test2 = "a\u0306\u0300";

        RuleBasedCollator c1 = (RuleBasedCollator) en_us.clone();
        c1.setDecomposition(Collator.FULL_DECOMPOSITION);
        CollationElementIterator i1 = en_us.getCollationElementIterator(test1);

        RuleBasedCollator c2 = (RuleBasedCollator) en_us.clone();
        c2.setDecomposition(Collator.NO_DECOMPOSITION);
        CollationElementIterator i2 = en_us.getCollationElementIterator(test2);

        assertEqual(i1, i2);
    }

    // French secondary collation checking at the end of compare iteration fails
    //
    public void Test4066696() {
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(Locale.FRANCE);
        c.setStrength(Collator.SECONDARY);

        String[] tests = {
            "\u00e0",   "<",     "\u01fa",       // a-grave <  A-ring-acute
        };

        compareArray(c, tests);
    }


    // Bad canonicalization of same-class combining characters
    //
    public void Test4076676() {
        // These combining characters are all in the same class, so they should not
        // be reordered, and they should compare as unequal.
        String s1 = "A\u0301\u0302\u0300";
        String s2 = "A\u0302\u0300\u0301";

        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();
        c.setStrength(Collator.TERTIARY);

        if (c.compare(s1,s2) == 0) {
            errln("Same-class combining chars were reordered");
        }
    }


    // RuleBasedCollator.equals(null) throws NullPointerException
    //
    public void Test4079231() {
        try {
            if (en_us.equals(null)) {
                errln("en_us.equals(null) returned true");
            }
        }
        catch (Exception e) {
            errln("en_us.equals(null) threw " + e.toString());
        }
    }

    // RuleBasedCollator breaks on "< a < bb" rule
    //
    public void Test4078588() throws ParseException {
        RuleBasedCollator rbc=new RuleBasedCollator("< a < bb");

        int result = rbc.compare("a","bb");

        if (result != -1) {
            errln("Compare(a,bb) returned " + result + "; expected -1");
        }
    }

    // Combining characters in different classes not reordered properly.
    //
    public void Test4081866() throws ParseException {
        // These combining characters are all in different classes,
        // so they should be reordered and the strings should compare as equal.
        String s1 = "A\u0300\u0316\u0327\u0315";
        String s2 = "A\u0327\u0316\u0315\u0300";

        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();
        c.setStrength(Collator.TERTIARY);

        // Now that the default collators are set to NO_DECOMPOSITION
        // (as a result of fixing bug 4114077), we must set it explicitly
        // when we're testing reordering behavior.  -- lwerner, 5/5/98
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);

        if (c.compare(s1,s2) != 0) {
            errln("Combining chars were not reordered");
        }
    }

    // string comparison errors in Scandinavian collators
    //
    public void Test4087241() {
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(
                                                        new Locale("da", "DK"));
        c.setStrength(Collator.SECONDARY);

        String[] tests = {
            "\u007a",   "<",    "\u00e6",       // z        < ae
            "a\u0308",  "<",    "a\u030a",      // a-unlaut < a-ring
            "Y",        "<",    "u\u0308",      // Y        < u-umlaut
        };

        compareArray(c, tests);
    }

    // CollationKey takes ignorable strings into account when it shouldn't
    //
    public void Test4087243() {
        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();
        c.setStrength(Collator.TERTIARY);

        String[] tests = {
            "123",      "=",    "123\u0001",        // 1 2 3  =  1 2 3 ctrl-A
        };

        compareArray(c, tests);
    }

    // Mu/micro conflict
    // Micro symbol and greek lowercase letter Mu should sort identically
    //
    public void Test4092260() {
        Collator c = Collator.getInstance(new Locale("el", ""));

        // will only be equal when FULL_DECOMPOSITION is used
        c.setDecomposition(Collator.FULL_DECOMPOSITION);

        String[] tests = {
            "\u00B5",      "=",    "\u03BC",
        };

        compareArray(c, tests);
    }

    void Test4095316() {
        Collator c = Collator.getInstance(new Locale("el", "GR"));
        c.setStrength(Collator.TERTIARY);
        // javadocs for RuleBasedCollator clearly specify that characters containing compatability
        // chars MUST use FULL_DECOMPOSITION to get accurate comparisons.
        c.setDecomposition(Collator.FULL_DECOMPOSITION);

        String[] tests = {
            "\u03D4",      "=",    "\u03AB",
        };

        compareArray(c, tests);
    }

    public void Test4101940() {
        try {
            RuleBasedCollator c = new RuleBasedCollator("< a < b");
            CollationElementIterator i = c.getCollationElementIterator("");
            i.reset();

            if (i.next() != i.NULLORDER) {
                errln("next did not return NULLORDER");
            }
        }
        catch (Exception e) {
            errln("Caught " + e );
        }
    }

    // Collator.compare not handling spaces properly
    //
    public void Test4103436() {
        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();
        c.setStrength(Collator.TERTIARY);

        String[] tests = {
            "file",      "<",    "file access",
            "file",      "<",    "fileaccess",
        };

        compareArray(c, tests);
    }

    // Collation not Unicode conformant with Hangul syllables
    //
    public void Test4114076() {
        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();
        c.setStrength(Collator.TERTIARY);

        //
        // With Canonical decomposition, Hangul syllables should get decomposed
        // into Jamo, but Jamo characters should not be decomposed into
        // conjoining Jamo
        //
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        String[] test1 = {
            "\ud4db",   "=",    "\u1111\u1171\u11b6",
        };
        compareArray(c, test1);

        // Full decomposition result should be the same as canonical decomposition
        // for all hangul.
        c.setDecomposition(Collator.FULL_DECOMPOSITION);
        compareArray(c, test1);

    }


    // Collator.getCollationKey was hanging on certain character sequences
    //
    public void Test4124632() throws Exception {
        Collator coll = Collator.getInstance(Locale.JAPAN);

        try {
            coll.getCollationKey("A\u0308bc");
        } catch (OutOfMemoryError e) {
            errln("Ran out of memory -- probably an infinite loop");
        }
    }

    // sort order of french words with multiple accents has errors
    //
    public void Test4132736() {
        Collator c = Collator.getInstance(Locale.FRANCE);

        String[] test1 = {
            "e\u0300e\u0301",   "<",    "e\u0301e\u0300",
            "e\u0300\u0301",    ">",    "e\u0301\u0300",
        };
        compareArray(c, test1);
    }

    // The sorting using java.text.CollationKey is not in the exact order
    //
    public void Test4133509() {
        String[] test1 = {
            "Exception",    "<",    "ExceptionInInitializerError",
            "Graphics",     "<",    "GraphicsEnvironment",
            "String",       "<",    "StringBuffer",
        };
        compareArray(en_us, test1);
    }

    // Collation with decomposition off doesn't work for Europe
    //
    public void Test4114077() {
        // Ensure that we get the same results with decomposition off
        // as we do with it on....

        RuleBasedCollator c = (RuleBasedCollator) en_us.clone();
        c.setStrength(Collator.TERTIARY);

        String[] test1 = {
            "\u00C0",        "=", "A\u0300",        // Should be equivalent
            "p\u00eache",         ">", "p\u00e9ch\u00e9",
            "\u0204",        "=", "E\u030F",
            "\u01fa",        "=", "A\u030a\u0301",  // a-ring-acute -> a-ring, acute
                                                    //   -> a, ring, acute
            "A\u0300\u0316", "<", "A\u0316\u0300",  // No reordering --> unequal
        };
        c.setDecomposition(Collator.NO_DECOMPOSITION);
        compareArray(c, test1);

        String[] test2 = {
            "A\u0300\u0316", "=", "A\u0316\u0300",      // Reordering --> equal
        };
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        compareArray(c, test2);
    }

    // Support for Swedish gone in 1.1.6 (Can't create Swedish collator)
    //
    public void Test4141640() {
        //
        // Rather than just creating a Swedish collator, we might as well
        // try to instantiate one for every locale available on the system
        // in order to prevent this sort of bug from cropping up in the future
        //
        Locale[] locales = Collator.getAvailableLocales();

        for (int i = 0; i < locales.length; i++) {
            try {
                Collator c = Collator.getInstance(locales[i]);
            } catch (Exception e) {
                errln("Caught " + e + " creating collator for " + locales[i]);
            }
        }
    }

    // getCollationKey throws exception for spanish text
    // Cannot reproduce this bug on 1.2, however it DOES fail on 1.1.6
    //
    public void Test4139572() {
        //
        // Code pasted straight from the bug report
        //
        // create spanish locale and collator
        Locale l = new Locale("es", "es");
        Collator col = Collator.getInstance(l);

        // this spanish phrase kills it!
        col.getCollationKey("Nombre De Objeto");
    }

    // RuleBasedCollator doesn't use getCollationElementIterator internally
    //
    public void Test4146160() throws ParseException {
        //
        // Use a custom collator class whose getCollationElementIterator
        // methods increment a count....
        //
        My4146160Collator.count = 0;
        new My4146160Collator().getCollationKey("1");
        if (My4146160Collator.count < 1) {
            errln("getCollationElementIterator not called");
        }

        My4146160Collator.count = 0;
        new My4146160Collator().compare("1", "2");
        if (My4146160Collator.count < 1) {
            errln("getCollationElementIterator not called");
        }
    }

    static class My4146160Collator extends RuleBasedCollator {
        public My4146160Collator() throws ParseException {
            super(Regression.en_us.getRules());
        }

        public CollationElementIterator getCollationElementIterator(
                                            String text) {
            count++;
            return super.getCollationElementIterator(text);
        }
        public CollationElementIterator getCollationElementIterator(
                                            CharacterIterator text) {
            count++;
            return super.getCollationElementIterator(text);
        }

        public static int count = 0;
    };

    // CollationElementIterator.previous broken for expanding char sequences
    //
    public void Test4179686() throws ParseException {

        // Create a collator with a few expanding character sequences in it....
        RuleBasedCollator coll = new RuleBasedCollator(en_us.getRules()
                                                    + " & ae ; \u00e4 & AE ; \u00c4"
                                                    + " & oe ; \u00f6 & OE ; \u00d6"
                                                    + " & ue ; \u00fc & UE ; \u00dc");

        String text = "T\u00f6ne"; // o-umlaut

        CollationElementIterator iter = coll.getCollationElementIterator(text);
        Vector elements = new Vector();
        int elem;

        // Iterate forward and collect all of the elements into a Vector
        while ((elem = iter.next()) != iter.NULLORDER) {
            elements.addElement(new Integer(elem));
        }

        // Now iterate backward and make sure they're the same
        int index = elements.size() - 1;
        while ((elem = iter.previous()) != iter.NULLORDER) {
            int expect = ((Integer)elements.elementAt(index)).intValue();

            if (elem != expect) {
                errln("Mismatch at index " + index
                      + ": got " + Integer.toString(elem,16)
                      + ", expected " + Integer.toString(expect,16));
            }
            index--;
        }
    }

    public void Test4244884() throws ParseException {
        RuleBasedCollator coll = (RuleBasedCollator)Collator.getInstance(Locale.US);
        coll = new RuleBasedCollator(coll.getRules()
                + " & C < ch , cH , Ch , CH < cat < crunchy");

        String[] testStrings = new String[] {
            "car",
            "cave",
            "clamp",
            "cramp",
            "czar",
            "church",
            "catalogue",
            "crunchy",
            "dog"
        };

        for (int i = 1; i < testStrings.length; i++) {
            if (coll.compare(testStrings[i - 1], testStrings[i]) >= 0) {
                errln("error: \"" + testStrings[i - 1]
                    + "\" is greater than or equal to \"" + testStrings[i]
                    + "\".");
            }
        }
    }

    public void Test4179216() throws ParseException {
        // you can position a CollationElementIterator in the middle of
        // a contracting character sequence, yielding a bogus collation
        // element
        RuleBasedCollator coll = (RuleBasedCollator)Collator.getInstance(Locale.US);
        coll = new RuleBasedCollator(coll.getRules()
                + " & C < ch , cH , Ch , CH < cat < crunchy");
        String testText = "church church catcatcher runcrunchynchy";
        CollationElementIterator iter = coll.getCollationElementIterator(
                testText);

        // test that the "ch" combination works properly
        iter.setOffset(4);
        int elt4 = CollationElementIterator.primaryOrder(iter.next());

        iter.reset();
        int elt0 = CollationElementIterator.primaryOrder(iter.next());

        iter.setOffset(5);
        int elt5 = CollationElementIterator.primaryOrder(iter.next());

        if (elt4 != elt0 || elt5 != elt0)
            errln("The collation elements at positions 0 (" + elt0 + "), 4 ("
                    + elt4 + "), and 5 (" + elt5 + ") don't match.");

        // test that the "cat" combination works properly
        iter.setOffset(14);
        int elt14 = CollationElementIterator.primaryOrder(iter.next());

        iter.setOffset(15);
        int elt15 = CollationElementIterator.primaryOrder(iter.next());

        iter.setOffset(16);
        int elt16 = CollationElementIterator.primaryOrder(iter.next());

        iter.setOffset(17);
        int elt17 = CollationElementIterator.primaryOrder(iter.next());

        iter.setOffset(18);
        int elt18 = CollationElementIterator.primaryOrder(iter.next());

        iter.setOffset(19);
        int elt19 = CollationElementIterator.primaryOrder(iter.next());

        if (elt14 != elt15 || elt14 != elt16 || elt14 != elt17
                || elt14 != elt18 || elt14 != elt19)
            errln("\"cat\" elements don't match: elt14 = " + elt14 + ", elt15 = "
            + elt15 + ", elt16 = " + elt16 + ", elt17 = " + elt17
            + ", elt18 = " + elt18 + ", elt19 = " + elt19);

        // now generate a complete list of the collation elements,
        // first using next() and then using setOffset(), and
        // make sure both interfaces return the same set of elements
        iter.reset();

        int elt = iter.next();
        int count = 0;
        while (elt != CollationElementIterator.NULLORDER) {
            ++count;
            elt = iter.next();
        }

        String[] nextElements = new String[count];
        String[] setOffsetElements = new String[count];
        int lastPos = 0;

        iter.reset();
        elt = iter.next();
        count = 0;
        while (elt != CollationElementIterator.NULLORDER) {
            nextElements[count++] = testText.substring(lastPos, iter.getOffset());
            lastPos = iter.getOffset();
            elt = iter.next();
        }
        count = 0;
        for (int i = 0; i < testText.length(); ) {
            iter.setOffset(i);
            lastPos = iter.getOffset();
            elt = iter.next();
            setOffsetElements[count++] = testText.substring(lastPos, iter.getOffset());
            i = iter.getOffset();
        }
        for (int i = 0; i < nextElements.length; i++) {
            if (nextElements[i].equals(setOffsetElements[i])) {
                logln(nextElements[i]);
            } else {
                errln("Error: next() yielded " + nextElements[i] + ", but setOffset() yielded "
                    + setOffsetElements[i]);
            }
        }
    }

    public void Test4216006() throws Exception {
        // rule parser barfs on "<\u00e0=a\u0300", and on other cases
        // where the same token (after normalization) appears twice in a row
        boolean caughtException = false;
        try {
            RuleBasedCollator dummy = new RuleBasedCollator("\u00e0<a\u0300");
        }
        catch (ParseException e) {
            caughtException = true;
        }
        if (!caughtException) {
            throw new Exception("\"a<a\" collation sequence didn't cause parse error!");
        }

        RuleBasedCollator collator = new RuleBasedCollator("<\u00e0=a\u0300");
        collator.setDecomposition(Collator.FULL_DECOMPOSITION);
        collator.setStrength(Collator.IDENTICAL);

        String[] tests = {
            "a\u0300", "=", "\u00e0",
            "\u00e0",  "=", "a\u0300"
        };

        compareArray(collator, tests);
    }

    public void Test4171974() {
        // test French accent ordering more thoroughly
        String[] frenchList = {
            "\u0075\u0075",     // u u
            "\u00fc\u0075",     // u-umlaut u
            "\u01d6\u0075",     // u-umlaut-macron u
            "\u016b\u0075",     // u-macron u
            "\u1e7b\u0075",     // u-macron-umlaut u
            "\u0075\u00fc",     // u u-umlaut
            "\u00fc\u00fc",     // u-umlaut u-umlaut
            "\u01d6\u00fc",     // u-umlaut-macron u-umlaut
            "\u016b\u00fc",     // u-macron u-umlaut
            "\u1e7b\u00fc",     // u-macron-umlaut u-umlaut
            "\u0075\u01d6",     // u u-umlaut-macron
            "\u00fc\u01d6",     // u-umlaut u-umlaut-macron
            "\u01d6\u01d6",     // u-umlaut-macron u-umlaut-macron
            "\u016b\u01d6",     // u-macron u-umlaut-macron
            "\u1e7b\u01d6",     // u-macron-umlaut u-umlaut-macron
            "\u0075\u016b",     // u u-macron
            "\u00fc\u016b",     // u-umlaut u-macron
            "\u01d6\u016b",     // u-umlaut-macron u-macron
            "\u016b\u016b",     // u-macron u-macron
            "\u1e7b\u016b",     // u-macron-umlaut u-macron
            "\u0075\u1e7b",     // u u-macron-umlaut
            "\u00fc\u1e7b",     // u-umlaut u-macron-umlaut
            "\u01d6\u1e7b",     // u-umlaut-macron u-macron-umlaut
            "\u016b\u1e7b",     // u-macron u-macron-umlaut
            "\u1e7b\u1e7b"      // u-macron-umlaut u-macron-umlaut
        };
        Collator french = Collator.getInstance(Locale.FRENCH);

        logln("Testing French order...");
        checkListOrder(frenchList, french);

        logln("Testing French order without decomposition...");
        french.setDecomposition(Collator.NO_DECOMPOSITION);
        checkListOrder(frenchList, french);

        String[] englishList = {
            "\u0075\u0075",     // u u
            "\u0075\u00fc",     // u u-umlaut
            "\u0075\u01d6",     // u u-umlaut-macron
            "\u0075\u016b",     // u u-macron
            "\u0075\u1e7b",     // u u-macron-umlaut
            "\u00fc\u0075",     // u-umlaut u
            "\u00fc\u00fc",     // u-umlaut u-umlaut
            "\u00fc\u01d6",     // u-umlaut u-umlaut-macron
            "\u00fc\u016b",     // u-umlaut u-macron
            "\u00fc\u1e7b",     // u-umlaut u-macron-umlaut
            "\u01d6\u0075",     // u-umlaut-macron u
            "\u01d6\u00fc",     // u-umlaut-macron u-umlaut
            "\u01d6\u01d6",     // u-umlaut-macron u-umlaut-macron
            "\u01d6\u016b",     // u-umlaut-macron u-macron
            "\u01d6\u1e7b",     // u-umlaut-macron u-macron-umlaut
            "\u016b\u0075",     // u-macron u
            "\u016b\u00fc",     // u-macron u-umlaut
            "\u016b\u01d6",     // u-macron u-umlaut-macron
            "\u016b\u016b",     // u-macron u-macron
            "\u016b\u1e7b",     // u-macron u-macron-umlaut
            "\u1e7b\u0075",     // u-macron-umlaut u
            "\u1e7b\u00fc",     // u-macron-umlaut u-umlaut
            "\u1e7b\u01d6",     // u-macron-umlaut u-umlaut-macron
            "\u1e7b\u016b",     // u-macron-umlaut u-macron
            "\u1e7b\u1e7b"      // u-macron-umlaut u-macron-umlaut
        };
        Collator english = Collator.getInstance(Locale.ENGLISH);

        logln("Testing English order...");
        checkListOrder(englishList, english);

        logln("Testing English order without decomposition...");
        english.setDecomposition(Collator.NO_DECOMPOSITION);
        checkListOrder(englishList, english);
    }

    private void checkListOrder(String[] sortedList, Collator c) {
        // this function uses the specified Collator to make sure the
        // passed-in list is already sorted into ascending order
        for (int i = 0; i < sortedList.length - 1; i++) {
            if (c.compare(sortedList[i], sortedList[i + 1]) >= 0) {
                errln("List out of order at element #" + i + ": "
                        + prettify(sortedList[i]) + " >= "
                        + prettify(sortedList[i + 1]));
            }
        }
    }

    // CollationElementIterator set doesn't work propertly with next/prev
    public void Test4663220() {
        RuleBasedCollator collator = (RuleBasedCollator)Collator.getInstance(Locale.US);
        CharacterIterator stringIter = new StringCharacterIterator("fox");
        CollationElementIterator iter = collator.getCollationElementIterator(stringIter);

        int[] elements_next = new int[3];
        logln("calling next:");
        for (int i = 0; i < 3; ++i) {
            logln("[" + i + "] " + (elements_next[i] = iter.next()));
        }

        int[] elements_fwd = new int[3];
        logln("calling set/next:");
        for (int i = 0; i < 3; ++i) {
            iter.setOffset(i);
            logln("[" + i + "] " + (elements_fwd[i] = iter.next()));
        }

        for (int i = 0; i < 3; ++i) {
            if (elements_next[i] != elements_fwd[i]) {
                errln("mismatch at position " + i +
                      ": " + elements_next[i] +
                      " != " + elements_fwd[i]);
            }
        }
    }

    //------------------------------------------------------------------------
    // Internal utilities
    //
    private void compareArray(Collator c, String[] tests) {
        for (int i = 0; i < tests.length; i += 3) {

            int expect = 0;
            if (tests[i+1].equals("<")) {
                expect = -1;
            } else if (tests[i+1].equals(">")) {
                expect = 1;
            } else if (tests[i+1].equals("=")) {
                expect = 0;
            } else {
                expect = Integer.decode(tests[i+1]).intValue();
            }

            int result = c.compare(tests[i], tests[i+2]);
            if (sign(result) != sign(expect))
            {
                errln( i/3 + ": compare(" + prettify(tests[i])
                                    + " , " + prettify(tests[i+2])
                                    + ") got " + result + "; expected " + expect);
            }
            else
            {
                // Collator.compare worked OK; now try the collation keys
                CollationKey k1 = c.getCollationKey(tests[i]);
                CollationKey k2 = c.getCollationKey(tests[i+2]);

                result = k1.compareTo(k2);
                if (sign(result) != sign(expect)) {
                    errln( i/3 + ": key(" + prettify(tests[i])
                                        + ").compareTo(key(" + prettify(tests[i+2])
                                        + ")) got " + result + "; expected " + expect);

                    errln("  " + prettify(k1) + " vs. " + prettify(k2));
                }
            }
        }
    }

    private static final int sign(int i) {
        if (i < 0) return -1;
        if (i > 0) return 1;
        return 0;
    }


    static RuleBasedCollator en_us = (RuleBasedCollator)Collator.getInstance(Locale.US);

    String test1 = "XFILE What subset of all possible test cases has the highest probability of detecting the most errors?";
    String test2 = "Xf ile What subset of all possible test cases has the lowest probability of detecting the least errors?";
    String test3 = "a\u00FCbeck Gr\u00F6\u00DFe L\u00FCbeck";
}
