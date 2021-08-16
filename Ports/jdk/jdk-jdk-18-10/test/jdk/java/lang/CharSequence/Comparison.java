/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8137326
 * @summary Test to verify the compare method for the CharSequence class.
 * @run testng Comparison
 */
public class Comparison {
    static char SEP = ':';

    static String[][] books = {
        {"Biography", "Steve Jobs"},
        {"Biography", "Elon Musk: Tesla, SpaceX, and the Quest for a Fantastic Future"},
        {"Law", "Law 101: Everything You Need to Know About American Law, Fourth Edition"},
        {"Law", "The Tools of Argument: How the Best Lawyers Think, Argue, and Win"},
        {"History", "The History Book (Big Ideas Simply Explained)"},
        {"History", "A People's History of the United States"},
    };

    /**
     * Verifies the compare method by comparing StringBuilder objects with String
     * objects.
     */
    @Test
    public void compareWithString() {
        Set<StringBuilder> sbSet = constructSBSet();
        Set<String> sSet = constructStringSet();
        Iterator<StringBuilder> iSB = sbSet.iterator();
        Iterator<String> iS = sSet.iterator();
        while (iSB.hasNext()) {
            int result = CharSequence.compare(iSB.next(), iS.next());

            Assert.assertTrue(result == 0, "Comparing item by item");
        }
    }

    /**
     * Verify comparison between two CharSequence implementations, including String,
     * StringBuffer and StringBuilder.
     *
     * Note: CharBuffer states that "A char buffer is not comparable to any other type of object."
     */
    @Test
    public void testCompare() {
        StringBuilder sb1 = generateTestBuilder(65, 70, 97, 102);
        StringBuilder sb2 = generateTestBuilder(65, 70, 97, 102);
        StringBuilder sb3 = generateTestBuilder(65, 71, 97, 103);

        Assert.assertTrue(CharSequence.compare(sb1, sb2) == 0, "Compare between StringBuilders");
        Assert.assertFalse(CharSequence.compare(sb1, sb3) == 0, "Compare between StringBuilders");

        Assert.assertTrue(CharSequence.compare(sb1, sb2.toString()) == 0, "Compare between a StringBuilder and String");
        Assert.assertFalse(CharSequence.compare(sb1, sb3.toString()) == 0, "Compare between a StringBuilder and String");

        StringBuffer buf1 = generateTestBuffer(65, 70, 97, 102);
        StringBuffer buf2 = generateTestBuffer(65, 70, 97, 102);
        StringBuffer buf3 = generateTestBuffer(65, 71, 97, 103);

        Assert.assertTrue(CharSequence.compare(buf1, buf2) == 0, "Compare between StringBuffers");
        Assert.assertFalse(CharSequence.compare(buf1, buf3) == 0, "Compare between StringBuffers");

        Assert.assertTrue(CharSequence.compare(sb1, buf2) == 0, "Compare between a StringBuilder and StringBuffer");
        Assert.assertFalse(CharSequence.compare(sb1, buf3) == 0, "Compare between a StringBuilder and StringBuffer");

        CharSequence cs1 = (CharSequence)buf1;
        CharSequence cs2 = (CharSequence)sb1;
        @SuppressWarnings("unchecked")
        int result = ((Comparable<Object>)cs1).compareTo(buf2);
         Assert.assertTrue(result == 0, "Compare between a StringBuilder and StringBuffer");
    }


    private Set<String> constructStringSet() {
        Set<String> sSet = new TreeSet<>();
        for (String[] book : books) {
            sSet.add(book[0] + SEP + book[1]);
        }
        return sSet;
    }

    private Set<StringBuilder> constructSBSet() {
        Set<StringBuilder> sbSet = new TreeSet<>();
        for (String[] book : books) {
            sbSet.add(new StringBuilder(book[0]).append(SEP).append(book[1]));
        }
        return sbSet;
    }

    private static StringBuilder generateTestBuilder(int from1, int to1,
            int from2, int to2) {
        StringBuilder aBuffer = new StringBuilder(50);

        for (int i = from1; i < to1; i++) {
            aBuffer.append((char)i);
        }
        for (int i = from2; i < to2; i++) {
            aBuffer.append((char)i);
        }
        return aBuffer;
    }

    private static StringBuffer generateTestBuffer(int from1, int to1,
            int from2, int to2) {
        StringBuffer aBuffer = new StringBuffer(50);

        for (int i = from1; i < to1; i++) {
            aBuffer.append((char)i);
        }
        for (int i = from2; i < to2; i++) {
            aBuffer.append((char)i);
        }
        return aBuffer;
    }
}
