/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8042741
 * @summary Java 8 compiler throws NullPointerException depending location in source file
 * @compile A.java PositionTest.java
 */

public class PositionTest extends A {
    <E extends Exception> void test(SAM<E> r) throws E {
        test(() -> { System.err.println(str); });
    }
    interface SAM<E extends Exception> {
        public void run() throws E;
    }
    void f() {
        try {
            test(() -> {
                    test(() -> {
                            try {
                                test(() -> { System.err.println(str); });
                                System.err.println(str);
                            } catch (Exception e) {}
                            System.err.println(str);
                        });
                    System.err.println(str);
                });
        } catch (Exception e) { }
    }
    void g() throws Exception {
        test(() -> {
                try {
                    try {
                        test(() -> { System.err.println(str); });
                    } catch (Exception e) {}
                    System.err.println(str);
                } catch (Exception e) {}
                System.err.println(str);
            });
    }
}
