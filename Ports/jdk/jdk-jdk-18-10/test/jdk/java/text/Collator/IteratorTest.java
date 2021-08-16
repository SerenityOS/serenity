/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4062985 4108758 4108762 4157299
 * @library /java/text/testlib
 * @summary Test CollationElementIterator, particularly the new methods in 1.2
 */
/*
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation is copyrighted
 * and owned by IBM, Inc. These materials are provided under terms of a
 * License Agreement between IBM and Sun. This technology is protected by
 * multiple US and International patents. This notice and attribution to IBM
 * may not be removed.
 */

import java.util.Locale;
import java.text.*;

public class IteratorTest extends CollatorTest {
    // TODO:
    //  - Test previous() with contracting character sequences, which don't work
    //      at the moment.
    //  - Test edge cases on setOffset(), e.g. offset > length, etc.
    //
    public static void main(String[] args) throws Exception {
        new IteratorTest().run(args);
    }

    /**
     * Test for CollationElementIterator.previous()
     *
     * @bug 4108758 - Make sure it works with contracting characters
     *
     */
    public void TestPrevious() throws ParseException {
        // A basic test to see if it's working at all
        backAndForth(en_us.getCollationElementIterator(test1));

        // Test with a contracting character sequence
        RuleBasedCollator c1 = new RuleBasedCollator(
            "< a,A < b,B < c,C, d,D < z,Z < ch,cH,Ch,CH" );

        backAndForth(c1.getCollationElementIterator("abchdcba"));

        // Test with an expanding character sequence
        RuleBasedCollator c2 = new RuleBasedCollator(
            "< a < b < c/abd < d" );

        backAndForth(c2.getCollationElementIterator("abcd"));

        // Now try both
        RuleBasedCollator c3 = new RuleBasedCollator(
            "< a < b < c/aba < d < z < ch" );

        backAndForth(c3.getCollationElementIterator("abcdbchdc"));
    }

    /**
     * Test for getOffset() and setOffset()
     */
    public void TestOffset() {
        CollationElementIterator iter = en_us.getCollationElementIterator(test1);

        // Run all the way through the iterator, then get the offset
        int orders[] = getOrders(iter);

        int offset = iter.getOffset();
        if (offset != test1.length()) {
            System.out.println("offset at end != length: "
                                + offset + " vs " + test1.length());
        }

        // Now set the offset back to the beginning and see if it works
        iter.setOffset(0);
        assertEqual(iter, en_us.getCollationElementIterator(test1));

        // TODO: try iterating halfway through a messy string.
    }

    /**
     * Test for setText()
     */
    public void TestSetText() {
        CollationElementIterator iter1 = en_us.getCollationElementIterator(test1);
        CollationElementIterator iter2 = en_us.getCollationElementIterator(test2);

        // Run through the second iterator just to exercise it
        int c = iter2.next();
        int i = 0;
        while ( ++i < 10 && c != CollationElementIterator.NULLORDER) {
            c = iter2.next();
        }

        // Now set it to point to the same string as the first iterator
        iter2.setText(test1);
        assertEqual(iter1, iter2);
    }

    /** @bug 4108762
     * Test for getMaxExpansion()
     */
    public void TestMaxExpansion() throws ParseException {
        // Try a simple one first:
        // The only expansion ends with 'e' and has length 2
        String[][] test1 = {
            { "< a & ae = \u00e4 < b < e", "" },
            { "a",  "1" },
            { "b",  "1" },
            { "e",  "2" },
        };
        verifyExpansion(test1);

        // Now a more complicated one:
        //   "a1" --> "ae"
        //   "z" --> "aeef"
        //
        String[][] test2 = {
            { "< a & ae = a1 & aeef = z < b < e < f", "" },
            { "a",  "1" },
            { "b",  "1" },
            { "e",  "2" },
            { "f",  "4" },
        };
        verifyExpansion(test2);
    }

    /*
     * @bug 4157299
     */
    public void TestClearBuffers() throws ParseException {
        RuleBasedCollator c = new RuleBasedCollator("< a < b < c & ab = d");
        CollationElementIterator i = c.getCollationElementIterator("abcd");
        int e0 = i.next();   // save the first collation element
        i.setOffset(3);      // go to the expanding character
        i.next();            // but only use up half of it
        i.setOffset(0);      // go back to the beginning
        int e = i.next();    // and get this one again
        if (e != e0) {
           errln("got " + Integer.toString(e, 16) + ", expected " +
                       Integer.toString(e0, 16));
        }
    }

    //------------------------------------------------------------------------
    // Internal utilities
    //

    private void backAndForth(CollationElementIterator iter) {
        // Run through the iterator forwards and stick it into an array
        int [] orders = getOrders(iter);

        // Now go through it backwards and make sure we get the same values
        int index = orders.length;
        int o;

        while ((o = iter.previous()) != CollationElementIterator.NULLORDER) {
            if (o != orders[--index]) {
                errln("Mismatch at index " + index + ": "
                        + orders[index] + " vs " + o);
                break;
            }
        }
        if (index != 0) {
            errln("Didn't get back to beginning - index is " + index);

            iter.reset();
            err("next: ");
            while ((o = iter.next()) != NULLORDER) {
                err( Integer.toHexString(o) + " ");
            }
            errln("");

            err("prev: ");
            while ((o = iter.previous()) != NULLORDER) {
                 err( Integer.toHexString(o) + " ");
            }
            errln("");
        }
    }

    /**
     * Verify that getMaxExpansion works on a given set of collation rules
     *
     * The first row of the "tests" array contains the collation rules
     * at index 0, and the string at index 1 is ignored.
     *
     * Subsequent rows of the array contain a character and a number, both
     * represented as strings.  The character's collation order is determined,
     * and getMaxExpansion is called for that character.  If its value is
     * not equal to the specified number, an error results.
     */
    private void verifyExpansion(String[][] tests) throws ParseException
    {
        RuleBasedCollator coll = new RuleBasedCollator(tests[0][0]);
        CollationElementIterator iter = coll.getCollationElementIterator("");

        for (int i = 1; i < tests.length; i++) {
            // First get the collation key that the test string expands to
            iter.setText(tests[i][0]);

            int order = iter.next();

            if (order == NULLORDER || iter.next() != NULLORDER) {
                iter.reset();
                errln("verifyExpansion: '" + tests[i][0] +
                    "' has multiple orders:" + orderString(iter));
            }

            int expansion = iter.getMaxExpansion(order);
            int expect = new Integer(tests[i][1]).intValue();

            if (expansion != expect) {
                errln("expansion for '" + tests[i][0] + "' is wrong: " +
                    "expected " + expect + ", got " + expansion);
            }
        }
    }

    /**
     * Return an integer array containing all of the collation orders
     * returned by calls to next on the specified iterator
     */
    private int[] getOrders(CollationElementIterator iter)
    {
        int maxSize = 100;
        int size = 0;
        int[] orders = new int[maxSize];

        int order;
        while ((order = iter.next()) != NULLORDER) {
            if (size == maxSize) {
                maxSize *= 2;
                int[] temp = new int[maxSize];
                System.arraycopy(orders, 0, temp, 0, size);
                orders = temp;
            }
            orders[size++] = order;
        }

        if (orders.length > size) {
            int[] temp = new int[size];
            System.arraycopy(orders, 0, temp, 0, size);
            orders = temp;
        }
        return orders;
    };

    /**
     * Return a string containing all of the collation orders
     * returned by calls to next on the specified iterator
     */
    private String orderString(CollationElementIterator iter) {
        StringBuffer buf = new StringBuffer();

        int order;
        while ((order = iter.next()) != NULLORDER) {
            buf.append( Integer.toHexString(order) + " ");
        }
        return buf.toString();
    }

    static final private int NULLORDER = CollationElementIterator.NULLORDER;
    RuleBasedCollator en_us = (RuleBasedCollator)Collator.getInstance(Locale.US);

    String test1 = "What subset of all possible test cases?";
    String test2 = "has the highest probability of detecting";
}
