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
 * @bug 5036680
 * @summary Test the ObjectName.compareTo() method.
 * @author Luis-Miguel Alventosa
 *
 * @run clean ComparatorTest
 * @run build ComparatorTest
 * @run main ComparatorTest
 */

import javax.management.*;

public class ComparatorTest {

    private static final char LT = '<';
    private static final char EQ = '=';
    private static final char GT = '>';

    private static final String tests[][] = {
        //
        // domains
        //
        { String.valueOf(LT), ":k1=v1", "d:k1=v1" },
        { String.valueOf(EQ), "d:k1=v1", "d:k1=v1" },
        { String.valueOf(GT), "d2:k1=v1", "d1:k1=v1" },
        //
        // "type=" key property
        //
        { String.valueOf(GT), "d:type=a,k1=v1", "d:k1=v1" },
        { String.valueOf(GT), "d:type=a,k1=v1", "d:type=" },
        { String.valueOf(GT), "d:type=a,k1=v1", "d:type=,k1=v1" },
        { String.valueOf(LT), "d:type=a,k1=v1", "d:type=b,k1=v1" },
        { String.valueOf(LT), "d:type=a,k2=v2", "d:type=b,k1=v1" },
        //
        // canonical form
        //
        { String.valueOf(EQ), "d:k1=v1,k2=v2", "d:k2=v2,k1=v1" },
        { String.valueOf(LT), "d:k1=v1,k2=v2", "d:k1=v1,k3=v3" },
        { String.valueOf(LT), "d:k1=v1,k2=v2", "d:k2=v2,k1=v1,k3=v3" },
        //
        // wildcards
        //
        { String.valueOf(LT), "d:k1=v1", "d:k1=v1,*" },
        { String.valueOf(GT), "d:k1=v1,k2=v2", "d:k1=v1,*" },
        { String.valueOf(GT), "domain:k1=v1", "?:k1=v1" },
        { String.valueOf(GT), "domain:k1=v1", "*:k1=v1" },
        { String.valueOf(GT), "domain:k1=v1", "domai?:k1=v1" },
        { String.valueOf(GT), "domain:k1=v1", "domai*:k1=v1" },
        { String.valueOf(GT), "domain:k1=v1", "do?ain:k1=v1" },
        { String.valueOf(GT), "domain:k1=v1", "do*ain:k1=v1" },
    };

    private static boolean compare(char comparator, String on1, String on2) {
        boolean ok = false;
        System.out.println("Test " + on1 + " " + comparator + " " + on2);
        try {
            ObjectName o1 = ObjectName.getInstance(on1);
            ObjectName o2 = ObjectName.getInstance(on2);
            int result = o1.compareTo(o2);
            switch (comparator) {
                case LT:
                    if (result < 0)
                        ok = true;
                    break;
                case EQ:
                    if (result == 0)
                        ok = true;
                    break;
                case GT:
                    if (result > 0)
                        ok = true;
                    break;
                default:
                    throw new IllegalArgumentException(
                              "Test incorrect: case: " + comparator);
            }
        } catch (Exception e) {
            ok = false;
            System.out.println("Got Unexpected Exception = " + e.toString());
        }
        return ok;
    }

    private static boolean lessThan(String on1, String on2) {
        return compare(LT, on1, on2);
    }

    private static boolean equalTo(String on1, String on2) {
        return compare(EQ, on1, on2);
    }

    private static boolean greaterThan(String on1, String on2) {
        return compare(GT, on1, on2);
    }

    private static int runTest(char comparator, String on1, String on2) {
        System.out.println("----------------------------------------------");
        boolean ok = false;
        boolean lt = lessThan(on1, on2);
        boolean eq = equalTo(on1, on2);
        boolean gt = greaterThan(on1, on2);
        switch (comparator) {
            case LT:
                ok = lt && !eq && !gt;
                System.out.println("Comparison result: LessThan");
                break;
            case EQ:
                ok = !lt && eq && !gt;
                System.out.println("Comparison result: EqualTo");
                break;
            case GT:
                ok = !lt && !eq && gt;
                System.out.println("Comparison result: GreaterThan");
                break;
            default:
                throw new IllegalArgumentException(
                          "Test incorrect: case: " + comparator);
        }
        if (ok)
            System.out.println("Test passed!");
        else
            System.out.println("Test failed!");
        System.out.println("----------------------------------------------");
        return ok ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {

        int error = 0;

        // Check null values
        //
        System.out.println("----------------------------------------------");
        System.out.println("Test ObjectName.compareTo(null)");
        try {
            new ObjectName("d:k=v").compareTo(null);
            error++;
            System.out.println("Didn't get expected NullPointerException!");
            System.out.println("Test failed!");
        } catch (NullPointerException e) {
            System.out.println("Got expected exception = " + e.toString());
            System.out.println("Test passed!");
        } catch (Exception e) {
            error++;
            System.out.println("Got unexpected exception = " + e.toString());
            System.out.println("Test failed!");
        }
        System.out.println("----------------------------------------------");

        // Compare ObjectNames
        //
        for (int i = 0; i < tests.length; i++)
            error += runTest(tests[i][0].charAt(0), tests[i][1], tests[i][2]);

        if (error > 0) {
            final String msg = "Test FAILED! Got " + error + " error(s)";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        } else {
            System.out.println("Test PASSED!");
        }
    }
}
