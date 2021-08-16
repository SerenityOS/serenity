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
 * @bug 4147520
 * @summary Verify correct implementation of qualified 'this' and 'super'.
 *
 * @run compile QualifiedThisAndSuper_2.java
 * @run main QualifiedThisAndSuper_2
 */

import p1.*;

public class QualifiedThisAndSuper_2 {

    void check(String expr, String result, String expected) {
        if (!result.equals(expected)) {
            throw new Error("Evaluated "+ expr +
                            " : result " + result + ", expected " + expected);
        }
    }

    public class A extends p1.AS {
        A() { super(); }
        String s = "as";
        private String t = "at";
        protected String u = "au";
        String m() { return "am"; }
        private String n() { return "an"; }
        protected String o() { return "ao"; }
        public class B extends p1.BS {
            B() { super(); }
            String s = "bs";
            private String t = "bt";
            protected String u = "bu";
            String m() { return "bm"; }
            private String n() { return "bn"; }
            protected String o() { return "bo"; }
            public class C extends p1.CS {
                C() { super(); }
                String s = "cs";
                private String t = "ct";
                protected String u = "cu";
                String m() { return "cm"; }
                private String n() { return "cn"; }
                protected String o() { return "co"; }
                void test() {

                    check("this.m()", this.m(), "cm");

                    check("A.this.m()", A.this.m(), "am");
                    check("B.this.m()", B.this.m(), "bm");
                    check("C.this.m()", C.this.m(), "cm");

                    //---

                    check("this.n()", this.n(), "cn");

                    check("A.this.n()", A.this.n(), "an");
                    check("B.this.n()", B.this.n(), "bn");
                    check("C.this.n()", C.this.n(), "cn");

                    //---

                    check("this.o()", this.o(), "co");

                    check("A.this.o()", A.this.o(), "ao");
                    check("B.this.o()", B.this.o(), "bo");
                    check("C.this.o()", C.this.o(), "co");

                    check("super.o()", super.o(), "cso");

                    check("A.super.o()", A.super.o(), "aso");
                    check("B.super.o()", B.super.o(), "bso");
                    check("C.super.o()", C.super.o(), "cso");

                    // should re-use access methods.
                    check("A.super.o()", A.super.o(), "aso");
                    check("B.super.o()", B.super.o(), "bso");
                    check("C.super.o()", C.super.o(), "cso");

                    //---

                    check("this.s", this.s, "cs");

                    check("A.this.s", A.this.s, "as");
                    check("B.this.s", B.this.s, "bs");
                    check("C.this.s", C.this.s, "cs");

                    //---

                    check("this.t", this.t, "ct");

                    check("A.this.t", A.this.t, "at");
                    check("B.this.t", B.this.t, "bt");
                    check("C.this.t", C.this.t, "ct");

                    //---

                    check("this.u", this.u, "cu");

                    check("A.this.u", A.this.u, "au");
                    check("B.this.u", B.this.u, "bu");
                    check("C.this.u", C.this.u, "cu");

                    //---

                    check("super.u", super.u, "csu");

                    check("A.super.u", A.super.u, "asu");
                    check("B.super.u", B.super.u, "bsu");
                    check("C.super.u", C.super.u, "csu");

                    //---

                    A.this.s = "foo";
                    System.out.println(A.this.s);
                    check("A.this.s", A.this.s, "foo");
                    B.this.s = "bar";
                    System.out.println(B.this.s);
                    check("B.this.s", B.this.s, "bar");
                    C.this.s = "baz";
                    System.out.println(C.this.s);
                    check("C.this.s", C.this.s, "baz");

                    A.this.t = "foo";
                    System.out.println(A.this.t);
                    check("A.this.t", A.this.t, "foo");
                    B.this.t = "bar";
                    System.out.println(B.this.t);
                    check("B.this.t", B.this.t, "bar");
                    C.this.t = "baz";
                    System.out.println(C.this.t);
                    check("C.this.t", C.this.t, "baz");

                    A.this.u = "foo";
                    System.out.println(A.this.u);
                    check("A.this.u", A.this.u, "foo");
                    B.this.u = "bar";
                    System.out.println(B.this.u);
                    check("B.this.u", B.this.u, "bar");
                    C.this.u = "baz";
                    System.out.println(C.this.u);
                    check("C.this.u", C.this.u, "baz");

                    A.super.u = "foo";
                    System.out.println(A.super.u);
                    check("A.super.u", A.super.u, "foo");
                    B.super.u = "bar";
                    System.out.println(B.super.u);
                    check("B.super.u", B.super.u, "bar");
                    C.super.u = "baz";
                    System.out.println(C.super.u);
                    check("C.super.u", C.super.u, "baz");

                }
            }
            void test() throws Exception {
                C c = new C();
                c.test();
            }
        }
        void test() throws Exception {
            B b = new B();
            b.test();
        }
    }

    public static void main(String[] args) throws Exception {
        A a = new QualifiedThisAndSuper_2().new A();
        a.test();
    }
}
