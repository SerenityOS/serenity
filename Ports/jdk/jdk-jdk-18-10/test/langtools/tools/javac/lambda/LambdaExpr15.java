/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  check that nested inner class in statement lambdas don't get corrupted return statements
 * @run main LambdaExpr15
 */
public class LambdaExpr15 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface Block<T> {
       void apply(T t);
    }

    public static void main(String[] args) {
        //anon class
        Block<Object> ba1 = t -> {
            new Object() {
                String get() { return ""; }
            };
            assertTrue((Integer)t == 1);
        };
        ba1.apply(1);

        //local class
        Block<Object> ba2 = t -> {
            class A {
                String get() { return ""; }
            };
            new A();
            assertTrue((Integer)t == 2);
        };
        ba2.apply(2);
        assertTrue(assertionCount == 2);
    }
}
