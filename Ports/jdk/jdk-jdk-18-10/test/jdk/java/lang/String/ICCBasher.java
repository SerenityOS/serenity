/*
 * Copyright (c) 1998, 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4152868
 * @summary test Case Insensitive Comparator in String
 * @key randomness
 */

import java.util.*;

public class ICCBasher {

    static final int TEST_SIZE = 20;
    static final int STRING_SIZE = 5;
    static final int CHAR_VALUE_LIMIT = 128;

    public static void main(String[] args) throws Exception {
        LinkedList L1 = new LinkedList();
        LinkedList L2 = new LinkedList();
        LinkedList L3 = new LinkedList();
        LinkedList L4 = new LinkedList();

        // First generate L1 and L2 with random lower case chars
        //System.out.println("Generate L1 and L2");
        Random generator = new Random();
        int achar=0;
        StringBuffer entryBuffer = new StringBuffer(10);
        String snippet = null;
        for (int x=0; x<TEST_SIZE * 2; x++) {
            for(int y=0; y<STRING_SIZE; y++) {
                achar = generator.nextInt(CHAR_VALUE_LIMIT);
                char test = (char)(achar);
                entryBuffer.append(test);
            }
            snippet = entryBuffer.toString();
            snippet.toLowerCase();
            if (x < TEST_SIZE)
                L1.add(snippet);
            else
                L2.add(snippet);
        }

        // Concatenate L1 and L2 to form L3
        //System.out.println("Generate L3");
        for (int x=0; x<TEST_SIZE; x++) {
            String entry = (String)L1.get(x) + (String)L2.get(x);
            L3.add(entry);
        }

        // Randomly toUpper L1 and L2
        //System.out.println("Modify L1 and L2");
        for (int x=0; x<TEST_SIZE; x++) {
            achar = generator.nextInt();
            if (achar > 0) {
                String mod = (String)L1.get(x);
                mod = mod.toUpperCase();
                L1.set(x, mod);
            }
            achar = generator.nextInt();
            if (achar > 0) {
                String mod = (String)L2.get(x);
                mod = mod.toUpperCase();
                L2.set(x, mod);
            }
        }

        // Concatenate L1 and L2 to form L4
        //System.out.println("Generate L4");
        for (int x=0; x<TEST_SIZE; x++) {
            String entry = (String)L1.get(x) + (String)L2.get(x);
            L4.add(entry);
        }

        // Sort L3 and L4 using case insensitive comparator
        //System.out.println("Sort L3 and L4");
        Collections.sort(L3, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(L4, String.CASE_INSENSITIVE_ORDER);

        // Check to see that order of L3 and L4 are identical
        // ignoring case considerations
        //System.out.println("Check order of L3 and L4");
        for (int x=0; x<TEST_SIZE; x++) {
            String one = (String)L3.get(x);
            String two = (String)L4.get(x);
            if (!one.equalsIgnoreCase(two))
                throw new RuntimeException("Case Insensitive Sort Failure.");
        }

    }
}
