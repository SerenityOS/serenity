/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4654698
 * @summary Verify that expected methods are returned for star inheritance.
 */

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;

// D.m
interface A1 extends B1, C1 {}
interface B1 extends D1 {}
interface C1 extends D1 {}
interface D1 { void m(); }

// A.m
interface A2 extends B2, C2 { void m(); }
interface B2 extends D2 {}
interface C2 extends D2 {}
interface D2 { void m(); }

// B.m, C.m
interface A3 extends B3, C3 {}
interface B3 extends D3 { void m(); }
interface C3 extends D3 { void m(); }
interface D3 { void m() ; }

// B.m
interface A4 extends B4, C4 {}
interface B4 extends D4 { void m(); }
interface C4 extends D4 {}
interface D4 { void m(); }

public class StarInheritance {
    private static int n = 1;

    private static void test(Method [] ma, ArrayList expect) {
        System.out.println("Test " + n++);

        if (expect.size() != ma.length) {
            System.err.println("  found methods: " + Arrays.asList(ma));
            System.err.println("  expected locations: " + expect);
            throw new RuntimeException("found = " + ma.length
                                       +"; expected = " + expect.size());
        }

        for (int i = 0; i < ma.length; i++) {
            Method m = ma[i];
            System.out.println("  " + m.toString());
            int n;
            if (m.getName().equals("m")
                && (n = expect.indexOf(m.getDeclaringClass())) != -1) {
                expect.remove(n);
            } else {
                throw new RuntimeException("unable to locate method in class: "
                                           + m.getDeclaringClass());
            }
        }
    }

    public static void main(String [] args) {
        Class [] l1 = {D1.class};
        test(A1.class.getMethods(), new ArrayList(Arrays.asList(l1)));

        Class [] l2 = {A2.class};
        test(A2.class.getMethods(), new ArrayList(Arrays.asList(l2)));

        Class [] l3 = {B3.class, C3.class};
        test(A3.class.getMethods(), new ArrayList(Arrays.asList(l3)));

        Class [] l4 = {B4.class};
        test(A4.class.getMethods(), new ArrayList(Arrays.asList(l4)));
    }
}
