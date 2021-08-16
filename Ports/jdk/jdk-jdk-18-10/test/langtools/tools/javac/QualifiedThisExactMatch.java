/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4102393 4133457
 * @summary Verify that <class>.this  matches <class> exactly, not a subclass.
 * @author maddox
 *
 * @run compile QualifiedThisExactMatch.java
 * @run main QualifiedThisExactMatch
 */

public class QualifiedThisExactMatch {

    void test() throws Exception {}

    void checkIt1() throws Exception {
        QualifiedThisExactMatch z = new QualifiedThisExactMatch() {
            void test() throws Exception {
                if (this == QualifiedThisExactMatch.this) {
                    throw new Exception("anonymous");
                }
            }
        };
        z.test();
    }

    //---

    class A  {
        Object getThisA() { return A.this; }
        class B extends A {
            Object getThisA() { return A.this; }
            Object getThisB() { return B.this; }
        }
    }

    void check(Object x, Object y) throws Exception {
        if (x != y) {
            throw new Exception("named");
        }
    }

    void checkIt2 () throws Exception {
        A a = new A();
        A.B b = a.new B();

        check(a, a.getThisA());
        check(a, b.getThisA());
        check(b, b.getThisB());
    }

    //---

    public static void main(String[] s) throws Exception {
        QualifiedThisExactMatch x = new QualifiedThisExactMatch();
        x.checkIt1();
        x.checkIt2();
    }
}
