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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8137326
 * @summary Test to verify the Comparable implementation for the StringBuilder class.
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
     * Verifies the Comparable implementation by comparing with two TreeSet that
     * contain either StringBuilder or String.
     */
    @Test
    public void compareWithString() {
        Set<StringBuilder> sbSet = constructSBSet();
        Set<String> sSet = constructStringSet();
        Iterator<StringBuilder> iSB = sbSet.iterator();
        Iterator<String> iS = sSet.iterator();
        while (iSB.hasNext()) {
            String temp1 = iSB.next().toString();
            System.out.println(temp1);
            String temp2 = iS.next();
            System.out.println(temp2);

            Assert.assertTrue(temp1.equals(temp2), "Comparing item by item");
        }

    }

    /**
     * Compares between StringBuilders
     */
    @Test
    public void testCompare() {
        StringBuilder sb1 = generateTestBuffer(65, 70, 97, 102);
        StringBuilder sb2 = generateTestBuffer(65, 70, 97, 102);
        StringBuilder sb3 = generateTestBuffer(65, 71, 97, 103);

        System.out.println(sb1.toString());
        System.out.println(sb2.toString());
        System.out.println(sb3.toString());
        Assert.assertTrue(sb1.compareTo(sb2) == 0, "Compare sb1 and sb2");
        Assert.assertFalse(sb1.compareTo(sb3) == 0, "Compare sb1 and sb3");
    }

    /**
     * Verifies that the comparison is from index 0 to length() - 1 of the two
     * character sequences.
     */
    @Test
    public void testModifiedSequence() {
        StringBuilder sb1 = generateTestBuffer(65, 70, 97, 102);
        StringBuilder sb2 = generateTestBuffer(65, 70, 98, 103);

        // contain different character sequences
        Assert.assertFalse(sb1.compareTo(sb2) == 0, "Compare the sequences before truncation");

        // the first 5 characters however are the same
        sb1.setLength(5);
        sb2.setLength(5);

        System.out.println(sb1.toString());
        System.out.println(sb2.toString());

        Assert.assertTrue(sb1.compareTo(sb2) == 0, "Compare sb1 and sb2");
        Assert.assertTrue(sb1.toString().compareTo(sb2.toString()) == 0, "Compare strings of sb1 and sb2");
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

    private static StringBuilder generateTestBuffer(int from1, int to1,
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
}
