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
 * @author maddox
 *
 * @run compile QualifiedThisAndSuper_3.java
 * @run main QualifiedThisAndSuper_3
 */

class AS {
    String s = "ass";
    private String t = "ast";
    protected String u = "asu";
    String m() { return "asm"; }
    private String n() { return "asn"; }
    protected String o() { return "aso"; }

    static String xs = "xass";
    static private String xt = "xast";
    static protected String xu = "xasu";
    static String xm() { return "xasm"; }
    static private String xn() { return "xasn"; }
    static protected String xo() { return "xaso"; }
}

class BS {
    String s = "bss";
    private String t = "bst";
    protected String u = "bsu";
    String m() { return "bsm"; }
    private String n() { return "bsn"; }
    protected String o() { return "bso"; }
}

class CS {
    String s = "css";
    private String t = "cst";
    protected String u = "csu";
    String m() { return "csm"; }
    private String n() { return "csn"; }
    protected String o() { return "cso"; }
}

public class QualifiedThisAndSuper_3 extends AS {

    void check(String expr, String result, String expected) {
        if (!result.equals(expected)) {
            throw new Error("Evaluated "+ expr +
                            " : result " + result + ", expected " + expected);
        }
    }


    QualifiedThisAndSuper_3() { super(); }
    String s = "as";
    private String t = "at";
    protected String u = "au";
    String m() { return "am"; }
    private String n() { return "an"; }
    protected String o() { return "ao"; }

    static String xs = "xas";
    static private String xt = "xat";
    static protected String xu = "xau";
    static String xm() { return "xam"; }
    static private String xn() { return "xan"; }
    static protected String xo() { return "xao"; }

    public class B extends BS {
        B() { super(); }
        String s = "bs";
        private String t = "bt";
        protected String u = "bu";
        String m() { return "bm"; }
        private String n() { return "bn"; }
        protected String o() { return "bo"; }
        public class C extends CS {
            C() { super(); }
            String s = "cs";
            private String t = "ct";
            protected String u = "cu";
            String m() { return "cm"; }
            private String n() { return "cn"; }
            protected String o() { return "co"; }
            void test() {

                check("QualifiedThisAndSuper_3.super.xm()", QualifiedThisAndSuper_3.super.xm(), "xasm");
                // Private to another package-member class: not accessible
                // check("QualifiedThisAndSuper_3.super.xn()", QualifiedThisAndSuper_3.super.xn(), "xasn");
                check("QualifiedThisAndSuper_3.super.xo()", QualifiedThisAndSuper_3.super.xo(), "xaso");

                check("QualifiedThisAndSuper_3.super.xs", QualifiedThisAndSuper_3.super.xs, "xass");
                // Private to another package-member class: not accessible
                // check("QualifiedThisAndSuper_3.super.xt", QualifiedThisAndSuper_3.super.xt, "xast");
                check("QualifiedThisAndSuper_3.super.xu", QualifiedThisAndSuper_3.super.xu, "xasu");

                check("QualifiedThisAndSuper_3.this.xm()", QualifiedThisAndSuper_3.this.xm(), "xam");
                check("QualifiedThisAndSuper_3.this.xn()", QualifiedThisAndSuper_3.this.xn(), "xan");
                check("QualifiedThisAndSuper_3.this.xo()", QualifiedThisAndSuper_3.this.xo(), "xao");

                check("QualifiedThisAndSuper_3.this.xs", QualifiedThisAndSuper_3.this.xs, "xas");
                check("QualifiedThisAndSuper_3.this.xt", QualifiedThisAndSuper_3.this.xt, "xat");
                check("QualifiedThisAndSuper_3.this.xu", QualifiedThisAndSuper_3.this.xu, "xau");

                //---

                check("this.m()", this.m(), "cm");

                check("QualifiedThisAndSuper_3.this.m()", QualifiedThisAndSuper_3.this.m(), "am");
                check("B.this.m()", B.this.m(), "bm");
                check("C.this.m()", C.this.m(), "cm");

                check("super.m()", super.m(), "csm");

                check("QualifiedThisAndSuper_3.super.m()", QualifiedThisAndSuper_3.super.m(), "asm");
                check("B.super.m()", B.super.m(), "bsm");
                check("C.super.m()", C.super.m(), "csm");

                // should re-use access methods.
                check("QualifiedThisAndSuper_3.super.m()", QualifiedThisAndSuper_3.super.m(), "asm");
                check("B.super.m()", B.super.m(), "bsm");
                check("C.super.m()", C.super.m(), "csm");

                //---

                check("this.n()", this.n(), "cn");

                check("QualifiedThisAndSuper_3.this.n()", QualifiedThisAndSuper_3.this.n(), "an");
                check("B.this.n()", B.this.n(), "bn");
                check("C.this.n()", C.this.n(), "cn");

                /*****
                check("super.n()", super.n(), "csn");

                check("QualifiedThisAndSuper_3.super.n()", QualifiedThisAndSuper_3.super.n(), "asn");
                check("B.super.n()", B.super.n(), "bsn");
                check("C.super.n()", C.super.n(), "csn");

                // should re-use access methods.
                check("QualifiedThisAndSuper_3.super.n()", QualifiedThisAndSuper_3.super.n(), "asn");
                check("B.super.n()", B.super.n(), "bsn");
                check("C.super.n()", C.super.n(), "csn");
                *****/

                //---

                check("this.o()", this.o(), "co");

                check("QualifiedThisAndSuper_3.this.o()", QualifiedThisAndSuper_3.this.o(), "ao");
                check("B.this.o()", B.this.o(), "bo");
                check("C.this.o()", C.this.o(), "co");

                check("super.o()", super.o(), "cso");

                check("QualifiedThisAndSuper_3.super.o()", QualifiedThisAndSuper_3.super.o(), "aso");
                check("B.super.o()", B.super.o(), "bso");
                check("C.super.o()", C.super.o(), "cso");

                // should re-use access methods.
                check("QualifiedThisAndSuper_3.super.o()", QualifiedThisAndSuper_3.super.o(), "aso");
                check("B.super.o()", B.super.o(), "bso");
                check("C.super.o()", C.super.o(), "cso");

                //---

                check("this.s", this.s, "cs");

                check("QualifiedThisAndSuper_3.this.s", QualifiedThisAndSuper_3.this.s, "as");
                check("B.this.s", B.this.s, "bs");
                check("C.this.s", C.this.s, "cs");

                //---

                check("this.t", this.t, "ct");

                check("QualifiedThisAndSuper_3.this.t", QualifiedThisAndSuper_3.this.t, "at");
                check("B.this.t", B.this.t, "bt");
                check("C.this.t", C.this.t, "ct");

                //---

                check("this.u", this.u, "cu");

                check("QualifiedThisAndSuper_3.this.u", QualifiedThisAndSuper_3.this.u, "au");
                check("B.this.u", B.this.u, "bu");
                check("C.this.u", C.this.u, "cu");

                //---

                check("super.s", super.s, "css");

                check("QualifiedThisAndSuper_3.super.s", QualifiedThisAndSuper_3.super.s, "ass");
                check("B.super.s", B.super.s, "bss");
                check("C.super.s", C.super.s, "css");

                //---

                /*****
                check("super.t", super.t, "cst");

                check("QualifiedThisAndSuper_3.super.t", QualifiedThisAndSuper_3.super.t, "ast");
                check("B.super.t", B.super.t, "bst");
                check("C.super.t", C.super.t, "cst");
                *****/

                //---

                check("super.u", super.u, "csu");

                check("QualifiedThisAndSuper_3.super.u", QualifiedThisAndSuper_3.super.u, "asu");
                check("B.super.u", B.super.u, "bsu");
                check("C.super.u", C.super.u, "csu");

                //---

                QualifiedThisAndSuper_3.this.s = "foo";
                System.out.println(QualifiedThisAndSuper_3.this.s);
                check("QualifiedThisAndSuper_3.this.s", QualifiedThisAndSuper_3.this.s, "foo");
                B.this.s = "bar";
                System.out.println(B.this.s);
                check("B.this.s", B.this.s, "bar");
                C.this.s = "baz";
                System.out.println(C.this.s);
                check("C.this.s", C.this.s, "baz");

                QualifiedThisAndSuper_3.this.t = "foo";
                System.out.println(QualifiedThisAndSuper_3.this.t);
                check("QualifiedThisAndSuper_3.this.t", QualifiedThisAndSuper_3.this.t, "foo");
                B.this.t = "bar";
                System.out.println(B.this.t);
                check("B.this.t", B.this.t, "bar");
                C.this.t = "baz";
                System.out.println(C.this.t);
                check("C.this.t", C.this.t, "baz");

                QualifiedThisAndSuper_3.this.u = "foo";
                System.out.println(QualifiedThisAndSuper_3.this.u);
                check("QualifiedThisAndSuper_3.this.u", QualifiedThisAndSuper_3.this.u, "foo");
                B.this.u = "bar";
                System.out.println(B.this.u);
                check("B.this.u", B.this.u, "bar");
                C.this.u = "baz";
                System.out.println(C.this.u);
                check("C.this.u", C.this.u, "baz");

                QualifiedThisAndSuper_3.super.s = "foo";
                System.out.println(QualifiedThisAndSuper_3.super.s);
                check("QualifiedThisAndSuper_3.super.s", QualifiedThisAndSuper_3.super.s, "foo");
                B.super.s = "bar";
                System.out.println(B.super.s);
                check("B.super.s", B.super.s, "bar");
                C.super.s = "baz";
                System.out.println(C.super.s);
                check("C.super.s", C.super.s, "baz");

                /*****
                QualifiedThisAndSuper_3.super.t = "foo";
                System.out.println(QualifiedThisAndSuper_3.super.t);
                check("QualifiedThisAndSuper_3.super.t", QualifiedThisAndSuper_3.super.t, "foo");
                B.super.t = "bar";
                System.out.println(B.super.t);
                check("B.super.t", B.super.t, "bar");
                C.super.t = "baz";
                System.out.println(C.super.t);
                check("C.super.t", C.super.t, "baz");
                *****/

                QualifiedThisAndSuper_3.super.u = "foo";
                System.out.println(QualifiedThisAndSuper_3.super.u);
                check("QualifiedThisAndSuper_3.super.u", QualifiedThisAndSuper_3.super.u, "foo");
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

    public static void main(String[] args) throws Exception {
        QualifiedThisAndSuper_3 a = new QualifiedThisAndSuper_3();
        a.test();
    }
}
