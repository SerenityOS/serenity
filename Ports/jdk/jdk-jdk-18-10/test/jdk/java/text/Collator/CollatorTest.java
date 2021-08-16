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

import java.lang.reflect.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;
import java.text.*;

/**
 * CollatorTest is a base class for tests that can be run conveniently from
 * the command line as well as under the Java test harness.
 * <p>
 * Sub-classes implement a set of methods named Test<something>. Each
 * of these methods performs some test. Test methods should indicate
 * errors by calling either err or errln.  This will increment the
 * errorCount field and may optionally print a message to the log.
 * Debugging information may also be added to the log via the log
 * and logln methods.  These methods will add their arguments to the
 * log only if the test is being run in verbose mode.
 */
public abstract class CollatorTest extends IntlTest {

    //------------------------------------------------------------------------
    // These methods are utilities specific to the Collation tests..
    //------------------------------------------------------------------------

    protected void assertEqual(CollationElementIterator i1, CollationElementIterator i2) {
        int c1, c2, count = 0;
        do {
            c1 = i1.next();
            c2 = i2.next();
            if (c1 != c2) {
                errln("    " + count + ": " + c1 + " != " + c2);
                break;
            }
            count++;
        } while (c1 != CollationElementIterator.NULLORDER);
    }

    // Replace nonprintable characters with unicode escapes
    static protected String prettify(String str) {
        StringBuffer result = new StringBuffer();

        String zero = "0000";

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch < 0x09 || (ch > 0x0A && ch < 0x20)|| (ch > 0x7E && ch < 0xA0) || ch > 0x100) {
                String hex = Integer.toString((int)ch,16);

                result.append("\\u" + zero.substring(0, 4 - hex.length()) + hex);
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    // Produce a printable representation of a CollationKey
    static protected String prettify(CollationKey key) {
        StringBuffer result = new StringBuffer();
        byte[] bytes = key.toByteArray();

        for (int i = 0; i < bytes.length; i += 2) {
            int val = (bytes[i] << 8) + bytes[i+1];
            result.append(Integer.toString(val, 16) + " ");
        }
        return result.toString();
    }

    //------------------------------------------------------------------------
    // Everything below here is boilerplate code that makes it possible
    // to add a new test by simply adding a function to an existing class
    //------------------------------------------------------------------------

    protected void doTest(Collator col, int strength,
                          String[] source, String[] target, int[] result) {
        if (source.length != target.length) {
            errln("Data size mismatch: source = " +
                  source.length + ", target = " + target.length);

            return; // Return if "-nothrow" is specified.
        }
        if (source.length != result.length) {
            errln("Data size mismatch: source & target = " +
                  source.length + ", result = " + result.length);

            return; // Return if "-nothrow" is specified.
        }

        col.setStrength(strength);
        for (int i = 0; i < source.length ; i++) {
            doTest(col, source[i], target[i], result[i]);
        }
    }

    protected void doTest(Collator col,
                          String source, String target, int result) {
        char relation = '=';
        if (result <= -1) {
            relation = '<';
        } else if (result >= 1) {
            relation = '>';
        }

        int compareResult = col.compare(source, target);
        CollationKey sortKey1 = col.getCollationKey(source);
        CollationKey sortKey2 = col.getCollationKey(target);
        int keyResult = sortKey1.compareTo(sortKey2);
        if (compareResult != keyResult) {
            errln("Compare and Collation Key results are different! Source = " +
                  source + " Target = " + target);
        }
        if (keyResult != result) {
            errln("Collation Test failed! Source = " + source + " Target = " +
                  target + " result should be " + relation);
        }
    }
}
