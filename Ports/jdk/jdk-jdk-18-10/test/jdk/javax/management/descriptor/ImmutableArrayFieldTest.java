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
 * @bug 6299180
 * @summary Test that immutability of ImmutableDescriptor cannot be
 * compromised by modifying field values that are arrays.
 * @author Eamonn McManus
 *
 * @run clean ImmutableArrayFieldTest
 * @run build ImmutableArrayFieldTest
 * @run main ImmutableArrayFieldTest
 */

import java.util.Arrays;
import javax.management.ImmutableDescriptor;

public class ImmutableArrayFieldTest {
    public static void main(String[] args) throws Exception {
        boolean ok = true;
        ImmutableDescriptor d = new ImmutableDescriptor(
                new String[] {
                    "strings", "ints", "booleans",
                },
                new Object[] {
                    new String[] {"foo"},
                    new int[] {5},
                    new boolean[] {false},
                });

        String[] strings = (String[]) d.getFieldValue("strings");
        strings[0] = "bar";
        strings = (String[]) d.getFieldValue("strings");
        if (!strings[0].equals("foo")) {
            System.out.println("FAILED: modified string array field");
            ok = false;
        }

        int[] ints = (int[]) d.getFieldValue("ints");
        ints[0] = 0;
        ints = (int[]) d.getFieldValue("ints");
        if (ints[0] != 5) {
            System.out.println("FAILED: modified int array field");
            ok = false;
        }

        boolean[] bools = (boolean[]) d.getFieldValue("booleans");
        bools[0] = true;
        bools = (boolean[]) d.getFieldValue("booleans");
        if (bools[0]) {
            System.out.println("FAILED: modified boolean array field");
            ok = false;
        }

        Object[] values = d.getFieldValues("strings", "ints", "booleans");
        ((String[]) values[0])[0] = "bar";
        ((int[]) values[1])[0] = 0;
        ((boolean[]) values[2])[0] = true;
        values = d.getFieldValues("strings", "ints", "booleans");
        if (!((String[]) values[0])[0].equals("foo") ||
                ((int[]) values[1])[0] != 5 ||
                ((boolean[]) values[2])[0]) {
            System.out.println("FAILED: getFieldValues modifiable: " +
                    Arrays.deepToString(values));
        }

        if (ok)
            System.out.println("TEST PASSED");
        else
            throw new Exception("Array field values were modifiable");
    }
}
