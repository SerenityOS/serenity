/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8074803
 * @summary Incorrect name clash error
 *
 * @compile NameClashTest.java
 */

public class NameClashTest {

    String log = "";

    interface A1 {
        A1 m(String s);
    }

    abstract class A2 implements A1 {
        public abstract A2 m(String s);
    }

    interface B1 {
        A1 m(String s);
    }

    interface B2 extends B1 {
        A2 m(String s);
    }

    abstract class C extends A2 implements B2 {}

    class D extends C {

        public A2 m(String s) {
            log += s;
            return null;
        }
    }

    public static void main(String[] args) {
        NameClashTest nct = new NameClashTest();
        A1 a1 = nct.new D();
        a1.m("A1.m ");
        A2 a2 = nct.new D();
        a2.m("A2.m ");
        B1 b1 = nct.new D();
        b1.m("B1.m ");
        B2 b2 = nct.new D();
        b2.m("B2.m ");
        C c = nct.new D();
        c.m("C.m ");
        D d = nct.new D();
        d.m("D.m ");
        if (!nct.log.equals("A1.m A2.m B1.m B2.m C.m D.m "))
            throw new AssertionError("unexpected output: " + nct.log);
    }
}
