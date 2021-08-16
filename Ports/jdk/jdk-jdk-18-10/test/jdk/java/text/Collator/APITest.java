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

/*
 * @test
 * @library /java/text/testlib
 * @summary test Collation API
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

import java.util.Locale;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.text.CollationKey;
import java.text.CollationElementIterator;

public class APITest extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new APITest().run(args);
    }

    final void doAssert(boolean condition, String message)
    {
        if (!condition) {
            err("ERROR: ");
            errln(message);
        }
    }

    public final void TestProperty( )
    {
        Collator col = null;
        try {
            col = Collator.getInstance(Locale.ROOT);
            logln("The property tests begin : ");
            logln("Test ctors : ");
            doAssert(col.compare("ab", "abc") < 0, "ab < abc comparison failed");
            doAssert(col.compare("ab", "AB") < 0, "ab < AB comparison failed");
            doAssert(col.compare("black-bird", "blackbird") > 0, "black-bird > blackbird comparison failed");
            doAssert(col.compare("black bird", "black-bird") < 0, "black bird < black-bird comparison failed");
            doAssert(col.compare("Hello", "hello") > 0, "Hello > hello comparison failed");

            logln("Test ctors ends.");
            logln("testing Collator.getStrength() method ...");
            doAssert(col.getStrength() == Collator.TERTIARY, "collation object has the wrong strength");
            doAssert(col.getStrength() != Collator.PRIMARY, "collation object's strength is primary difference");

            logln("testing Collator.setStrength() method ...");
            col.setStrength(Collator.SECONDARY);
            doAssert(col.getStrength() != Collator.TERTIARY, "collation object's strength is secondary difference");
            doAssert(col.getStrength() != Collator.PRIMARY, "collation object's strength is primary difference");
            doAssert(col.getStrength() == Collator.SECONDARY, "collation object has the wrong strength");

            logln("testing Collator.setDecomposition() method ...");
            col.setDecomposition(Collator.NO_DECOMPOSITION);
            doAssert(col.getDecomposition() != Collator.FULL_DECOMPOSITION, "collation object's strength is secondary difference");
            doAssert(col.getDecomposition() != Collator.CANONICAL_DECOMPOSITION, "collation object's strength is primary difference");
            doAssert(col.getDecomposition() == Collator.NO_DECOMPOSITION, "collation object has the wrong strength");
        } catch (Exception foo) {
            errln("Error : " + foo.getMessage());
            errln("Default Collator creation failed.");
        }
        logln("Default collation property test ended.");
        logln("Collator.getRules() testing ...");
        doAssert(((RuleBasedCollator)col).getRules().length() != 0, "getRules() result incorrect" );
        logln("getRules tests end.");
        try {
            col = Collator.getInstance(Locale.FRENCH);
            col.setStrength(Collator.PRIMARY);
            logln("testing Collator.getStrength() method again ...");
            doAssert(col.getStrength() != Collator.TERTIARY, "collation object has the wrong strength");
            doAssert(col.getStrength() == Collator.PRIMARY, "collation object's strength is not primary difference");

            logln("testing French Collator.setStrength() method ...");
            col.setStrength(Collator.TERTIARY);
            doAssert(col.getStrength() == Collator.TERTIARY, "collation object's strength is not tertiary difference");
            doAssert(col.getStrength() != Collator.PRIMARY, "collation object's strength is primary difference");
            doAssert(col.getStrength() != Collator.SECONDARY, "collation object's strength is secondary difference");

        } catch (Exception bar) {
            errln("Error :  " + bar.getMessage());
            errln("Creating French collation failed.");
        }

        logln("Create junk collation: ");
        Locale abcd = new Locale("ab", "CD", "");
        Collator junk = null;
        try {
            junk = Collator.getInstance(abcd);
        } catch (Exception err) {
            errln("Error : " + err.getMessage());
            errln("Junk collation creation failed, should at least return the collator for the base bundle.");
        }
        try {
            col = Collator.getInstance(Locale.ROOT);
            doAssert(col.equals(junk), "The base bundle's collation should be returned.");
        } catch (Exception exc) {
            errln("Error : " + exc.getMessage());
            errln("Default collation comparison, caching not working.");
        }

        logln("Collator property test ended.");
    }

    public final void TestHashCode( )
    {
        logln("hashCode tests begin.");
        Collator col1 = null;
        try {
            col1 = Collator.getInstance(Locale.ROOT);
        } catch (Exception foo) {
            errln("Error : " + foo.getMessage());
            errln("Default collation creation failed.");
        }
        Collator col2 = null;
        Locale dk = new Locale("da", "DK", "");
        try {
            col2 = Collator.getInstance(dk);
        } catch (Exception bar) {
            errln("Error : " + bar.getMessage());
            errln("Danish collation creation failed.");
            return;
        }
        Collator col3 = null;
        try {
            col3 = Collator.getInstance(Locale.ROOT);
        } catch (Exception err) {
            errln("Error : " + err.getMessage());
            errln("2nd default collation creation failed.");
        }
        logln("Collator.hashCode() testing ...");

        if (col1 != null) {
            doAssert(col1.hashCode() != col2.hashCode(), "Hash test1 result incorrect");
            if (col3 != null) {
                doAssert(col1.hashCode() == col3.hashCode(), "Hash result not equal");
            }
        }

        logln("hashCode tests end.");
    }

    //----------------------------------------------------------------------------
    // ctor -- Tests the constructor methods
    //
    public final void TestCollationKey( )
    {
        logln("testing CollationKey begins...");
        Collator col = null;
        try {
            col = Collator.getInstance(Locale.ROOT);
        } catch (Exception foo) {
            errln("Error : " + foo.getMessage());
            errln("Default collation creation failed.");
        }
        if (col == null) {
            return;
        }

        String test1 = "Abcda", test2 = "abcda";
        logln("Use tertiary comparison level testing ....");
        CollationKey sortk1 = col.getCollationKey(test1);
        CollationKey sortk2 = col.getCollationKey(test2);
        doAssert(sortk1.compareTo(sortk2) > 0,
                    "Result should be \"Abcda\" >>> \"abcda\"");
        CollationKey sortk3 = sortk2;
        CollationKey sortkNew = sortk1;
        doAssert(sortk1 != sortk2, "The sort keys should be different");
        doAssert(sortk1.hashCode() != sortk2.hashCode(), "sort key hashCode() failed");
        doAssert(sortk2.compareTo(sortk3) == 0, "The sort keys should be the same");
        doAssert(sortk1 == sortkNew, "The sort keys assignment failed");
        doAssert(sortk1.hashCode() == sortkNew.hashCode(), "sort key hashCode() failed");
        doAssert(sortkNew != sortk3, "The sort keys should be different");
        doAssert(sortk1.compareTo(sortk3) > 0, "Result should be \"Abcda\" >>> \"abcda\"");
        doAssert(sortk2.compareTo(sortk3) == 0, "Result should be \"abcda\" == \"abcda\"");
        long    cnt1, cnt2;
        byte byteArray1[] = sortk1.toByteArray();
        byte byteArray2[] = sortk2.toByteArray();
        doAssert(byteArray1 != null && byteArray2 != null, "CollationKey.toByteArray failed.");
        logln("testing sortkey ends...");
    }
    //----------------------------------------------------------------------------
    // ctor -- Tests the constructor methods
    //
    public final void TestElemIter( )
    {
        logln("testing sortkey begins...");
        Collator col = null;
        try {
            col = Collator.getInstance();
        } catch (Exception foo) {
            errln("Error : " + foo.getMessage());
            errln("Default collation creation failed.");
        }
        RuleBasedCollator rbCol;
        if (col instanceof RuleBasedCollator) {
            rbCol = (RuleBasedCollator) col;
        } else {
            return;
        }
        String testString1 = "XFILE What subset of all possible test cases has the highest probability of detecting the most errors?";
        String testString2 = "Xf ile What subset of all possible test cases has the lowest probability of detecting the least errors?";
        logln("Constructors and comparison testing....");
        CollationElementIterator iterator1 = rbCol.getCollationElementIterator(testString1);
        CollationElementIterator iterator2 = rbCol.getCollationElementIterator(testString1);
        CollationElementIterator iterator3 = rbCol.getCollationElementIterator(testString2);
        int order1, order2, order3;
        order1 = iterator1.next();
        order2 = iterator2.next();
        doAssert(order1 == order2, "The order result should be the same");

        order3 = iterator3.next();
        doAssert(CollationElementIterator.primaryOrder(order1)
                     == CollationElementIterator.primaryOrder(order3),
                 "The primary orders should be the same");
        doAssert(CollationElementIterator.secondaryOrder(order1)
                     == CollationElementIterator.secondaryOrder(order3),
                 "The secondary orders should be the same");
        doAssert(CollationElementIterator.tertiaryOrder(order1)
                     == CollationElementIterator.tertiaryOrder(order3),
                 "The tertiary orders should be the same");

        order1 = iterator1.next();
        order3 = iterator3.next();
        doAssert(CollationElementIterator.primaryOrder(order1)
                     == CollationElementIterator.primaryOrder(order3),
                 "The primary orders should be identical");
        doAssert(CollationElementIterator.tertiaryOrder(order1)
                     != CollationElementIterator.tertiaryOrder(order3),
                 "The tertiary orders should be different");

        order1 = iterator1.next();
        order3 = iterator3.next();
        doAssert(CollationElementIterator.secondaryOrder(order1)
                     != CollationElementIterator.secondaryOrder(order3),
                 "The secondary orders should be different");
        doAssert(order1 != CollationElementIterator.NULLORDER,
                 "Unexpected end of iterator reached");

        iterator1.reset();
        iterator2.reset();
        iterator3.reset();
        order1 = iterator1.next();
        order2 = iterator2.next();
        doAssert(order1 == order2, "The order result should be the same");

        order3 = iterator3.next();
        doAssert(CollationElementIterator.primaryOrder(order1)
                     == CollationElementIterator.primaryOrder(order3),
                 "The orders should be the same");
        doAssert(CollationElementIterator.secondaryOrder(order1)
                     == CollationElementIterator.secondaryOrder(order3),
                 "The orders should be the same");
        doAssert(CollationElementIterator.tertiaryOrder(order1)
                     == CollationElementIterator.tertiaryOrder(order3),
                 "The orders should be the same");

        order1 = iterator1.next();
        order2 = iterator2.next();
        order3 = iterator3.next();
        doAssert(CollationElementIterator.primaryOrder(order1)
                     == CollationElementIterator.primaryOrder(order3),
                 "The primary orders should be identical");
        doAssert(CollationElementIterator.tertiaryOrder(order1)
                     != CollationElementIterator.tertiaryOrder(order3),
                 "The tertiary orders should be different");

        order1 = iterator1.next();
        order3 = iterator3.next();
        doAssert(CollationElementIterator.secondaryOrder(order1)
                     != CollationElementIterator.secondaryOrder(order3),
                 "The secondary orders should be different");
        doAssert(order1 != CollationElementIterator.NULLORDER, "Unexpected end of iterator reached");
        logln("testing CollationElementIterator ends...");
    }

    public final void TestGetAll()
    {
        Locale[] list = Collator.getAvailableLocales();
        for (int i = 0; i < list.length; ++i) {
            log("Locale name: ");
            log(list[i].toString());
            log(" , the display name is : ");
            logln(list[i].getDisplayName());
        }
    }
}
