/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6255956
 * @summary Test equals and hashCode for descriptors
 * @author Eamonn McManus
 *
 * @run clean EqualsHashCodeTest
 * @run build EqualsHashCodeTest
 * @run main EqualsHashCodeTest
 */

import java.util.Arrays;
import javax.management.*;
import javax.management.modelmbean.DescriptorSupport;

public class EqualsHashCodeTest {
    public static void main(String[] args) throws Exception {
        int[] squares = {1, 4, 9, 16};
        int[] serauqs = {16, 9, 4, 1};
        int[][] numbers = {squares, serauqs};

        Descriptor sq1 =
            new ImmutableDescriptor(new String[] {"name", "rank", "squares",
                                                  "null", "numbers"},
                                    new Object[] {"Foo McBar", "lowly",
                                                  squares.clone(), null,
                                                  numbers});
        Descriptor sq2 =
            new DescriptorSupport(new String[] {"Name", "Rank", "SquareS",
                                                "NULL", "NuMbErS"},
                                  new Object[] {"Foo McBar", "lowly",
                                                squares.clone(), null,
                                                numbers});
        Descriptor sq3 = (Descriptor) sq2.clone();
        Descriptor sq4 = ImmutableDescriptor.union(sq1, sq2);

        String[] names = sq1.getFieldNames();
        Object[] values = sq1.getFieldValues((String[]) null);
        Object[] values2 = sq1.getFieldValues(names);
        if (!Arrays.deepEquals(values, values2)) {
            throw new Exception("Arrays not equal: " +
                    Arrays.deepToString(values) + Arrays.deepToString(values2));
        }

        int expectedHashCode = 0;
        for (int i = 0; i < names.length; i++) {
            Object value = values[i];
            int h;
            if (value == null)
                h = 0;
            else if (value instanceof int[])
                h = Arrays.hashCode((int[]) value);
            else if (value instanceof Object[])
                h = Arrays.deepHashCode((Object[]) value);
            else
                h = value.hashCode();
            expectedHashCode += names[i].toLowerCase().hashCode() ^ h;
        }
        for (Descriptor d : new Descriptor[] {sq1, sq2, sq3, sq4}) {
            System.out.println("Testing hashCode for " +
                               d.getClass().getName() + ": " + d);
            if (d.hashCode() != expectedHashCode) {
                throw new Exception("Bad hashCode: expected " +
                                    expectedHashCode + ", got " + d.hashCode() +
                                    ", for " + d);
            }
        }

        int i;
        for (i = 0; i < names.length; i++) {
            if (names[i].equals("squares")) {
                values[i] = serauqs.clone();
                break;
            }
        }
        if (i >= names.length)
            throw new Exception("Internal error: no squares name");
        Descriptor qs1 = new ImmutableDescriptor(names, values);
        values[i] = serauqs.clone();
        Descriptor qs2 = new DescriptorSupport(names, values);

        System.out.println("Testing equality...");

        Object[][] equivalenceClasses = {
            {sq1, sq2, sq3, sq4},
            {qs1, qs2},
        };
        for (Object[] equivClass : equivalenceClasses) {
            for (Object a : equivClass) {
                for (Object b : equivClass) {
                    if (!a.equals(b)) {
                        throw new Exception("Should be equal but not: " +
                                            a + " :: " + b);
                    }
                }
                for (Object[] equivClass2 : equivalenceClasses) {
                    if (equivClass2 == equivClass)
                        continue;
                    for (Object b : equivClass2) {
                        if (a.equals(b)) {
                            throw new Exception("Should not be equal: " +
                                                a + " :: " + b);
                        }
                    }
                }
            }
        }

        System.out.println("TEST PASSED");
    }
}
