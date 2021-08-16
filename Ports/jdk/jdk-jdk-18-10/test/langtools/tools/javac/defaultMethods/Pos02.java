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
 * @bug 7192246
 * @summary test for explicit resolution of ambiguous default methods
 * @author  Maurizio Cimadamore
 * @compile Pos02.java
 */

class Pos02 {
    interface IA { default int m() { return Pos02.m1(this); } }
    interface IB { default int m() { return Pos02.m2(this); } }

    static class A implements IA {}
    static class B implements IB {}

    static class AB implements IA, IB {
        public int m() { return 0; }
        void test() {
            AB.this.m();
            IA.super.m();
        }
    }

    static int m1(IA a) { return 0; }
    static int m2(IB b) { return 0; }
}
