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
 * @bug 7192246
 * @summary check that well-formed MI hierarchies behaves well w.r.t. method resolution (i.e. no ambiguities)
 * @author  Maurizio Cimadamore
 * @compile Pos06.java
 */

class Pos06 {
     interface A {
         default void m() { Pos06.impl(this); }
     }

     interface B extends A {
         default void m() { Pos06.impl(this); }
     }

     static class X implements A, B { }

     void test(X x) {
         x.m();
         ((A)x).m();
         ((B)x).m();
     }

     static void impl(Object a) { }
}
