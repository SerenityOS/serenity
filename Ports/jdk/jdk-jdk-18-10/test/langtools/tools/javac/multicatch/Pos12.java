/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8013163
 * @author  sogoel
 * @summary Child exception can be caught in a parents catch block but not sibling exceptions.
 * @run main Pos12
 */

/*
 * For this test:
 *    RuntimeException
 *    |
 *    A
 *   / \
 *  Ab Ac
 * This test throws an Ab and catches it as an A(parent).
 * The exception, although catch as an A, is rethrown and eventually
 * caught as an Ab. Before that it is NOT caught as an Ac.
 */

public class Pos12 {

    public static void main(String... args) {
        try {
            new Pos12().test();
        } catch (A exception) {
            try {
                try {
                    throw exception; // used to throw A, now throws Ab
                } catch (Ac cException) { // This should NOT catch sibling exception Ab
                    throw new RuntimeException("FAIL: Should not be caught in catch Ac");
                }
            } catch (Ab | Ac bcException) {
                if (bcException instanceof Ac) {
                    throw new RuntimeException("FAIL: Sibling exception Ab not caught as expected");
                } else if (bcException instanceof Ab) {
                    System.out.println("PASS");
                }
            }
        }
    }

    public void test() { throw new Ab(); }

    static class A extends RuntimeException {}

    // Test class
    static class Ab extends A {}

    // Test class
    static class Ac extends A {}
}

