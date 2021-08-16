/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.codegen;

import jdk.test.lib.Asserts;

/**
 * @test
 * @bug 8255763 8258894
 * @summary Tests GCM's store placement in different scenarios (regular and OSR
 *          compilations, reducible and irreducible CFGs).
 * @library /test/lib /
 * @run main/othervm -Xbatch compiler.codegen.TestGCMStorePlacement regularReducible1
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler.codegen.TestGCMStorePlacement:: compiler.codegen.TestGCMStorePlacement regularReducible2
 * @run main/othervm -Xcomp -XX:CompileOnly=compiler.codegen.TestGCMStorePlacement:: compiler.codegen.TestGCMStorePlacement regularReducible3
 * @run main/othervm -Xcomp -XX:CompileOnly=compiler.codegen.TestGCMStorePlacement:: compiler.codegen.TestGCMStorePlacement regularReducible4
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler.codegen.TestGCMStorePlacement:: compiler.codegen.TestGCMStorePlacement osrReducible1
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler.codegen.TestGCMStorePlacement:: compiler.codegen.TestGCMStorePlacement osrReducible2
 * @run main/othervm -Xbatch compiler.codegen.TestGCMStorePlacement osrIrreducible1
 */

public class TestGCMStorePlacement {

    static int intCounter;
    static long longCounter;

    static void testRegularReducible1() {
        // This should not be placed into the loop.
        intCounter++;
        int acc = 0;
        for (int i = 0; i < 50; i++) {
            if (i % 2 == 0) {
                acc += 1;
            }
        }
        return;
    }

    static void testRegularReducible2() {
        int i;
        for (i = 22; i < 384; i++)
            longCounter += 1;
        switch (i % 9) {
        case 49:
            int i17 = 70;
            while (i17 > 0) {
                longCounter = 42;
                switch (9) {
                case 97:
                    break;
                case 11398:
                    for (int i18 = 1; ; );
                default:
                }
            }
        }
    }

    static void testRegularReducible3() {
        int i = 0, i23, i27 = 184;
        for (int i21 = 0; i21 < 100; i21++) {
            i23 = 1;
            longCounter += 1;
            while (++i23 < 190)
                switch (i23 % 10) {
                case 86:
                    continue;
                case 0:
                    i += 76.854F;
                    for (; i27 < 1; i27++);
                }
        }
    }

    static void testRegularReducible4() {
        int i16 = 0, i17;
        longCounter += 1;
        while (++i16 < 100) {
            i17 = 0;
            while (++i17 < 200) {
                switch ((i16 * 5) + 123) {
                case 129:
                    break;
                case 149:
                    break;
                default:
                }
            }
        }
    }

    public static void bar() {
        int[] a = new int[0];
        long sum = 0;
        for (int j = 0; j < 0; j++) {
            sum += (a[j] / (j + 1) + a[j] % (j + 1));
        }
    }
    static void foo() {
        bar();
    }

    static void testOsrReducible1() {
        int count = 100;
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                foo();
                try {
                    count = (100000 / count);
                } catch (Exception e) {}
                switch (0) {
                case 0:
                    for (int k = 0; k < 2; k++) {
                        count = 0;
                    }
                    longCounter += 1;
                }
            }
        }
    }

    static void testOsrReducible2() {
        System.out.println();
        boolean cond = false;
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                intCounter = 42;
                if (cond) {
                    break;
                }
                for (int k = 0; k < 2; k++);
            }
        }
    }

    static void testOsrIrreducible1() {
        for (int i = 0; i < 30; i++) {
            switch (i % 3) {
            case 0:
                for (int j = 0; j < 50; j++) {
                    // OSR enters here.
                    for (int k = 0; k < 7000; k++) {}
                    if (i % 2 == 0) {
                        break;
                    }
                }
                // This should not be placed outside the "case 0" block.
                intCounter++;
                break;
            case 1:
                break;
            case 2:
                break;
            }
        }
        return;
    }

    public static void main(String[] args) {
        switch (args[0]) {
        case "regularReducible1":
            for (int i = 0; i < 100_000; i++) {
                intCounter = 0;
                testRegularReducible1();
                Asserts.assertEQ(intCounter, 1);
            }
            break;
        case "regularReducible2":
            longCounter = 0;
            testRegularReducible2();
            Asserts.assertEQ(longCounter, 362L);
            break;
        case "regularReducible3":
            for (int i = 0; i < 10; i++) {
                longCounter = 0;
                testRegularReducible3();
                Asserts.assertEQ(longCounter, 100L);
            }
            break;
        case "regularReducible4":
            for (int i = 0; i < 10; i++) {
                longCounter = 0;
                testRegularReducible4();
                Asserts.assertEQ(longCounter, 1L);
            }
            break;
        case "osrReducible1":
            longCounter = 0;
            testOsrReducible1();
            Asserts.assertEQ(longCounter, 10000L);
            break;
        case "osrReducible2":
            intCounter = 0;
            testOsrReducible2();
            Asserts.assertEQ(intCounter, 42);
            break;
        case "osrIrreducible1":
            intCounter = 0;
            testOsrIrreducible1();
            Asserts.assertEQ(intCounter, 10);
            break;
        default:
            System.out.println("invalid mode");
        }
    }
}
