/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6500343
 * @summary compiler generates bad code when translating conditional expressions
 * @author Maurizio Cimadamore
 *
 */

public class T6500343a {
    static class Base {}
    static interface I {}
    static class A1 extends Base implements I {}
    static class A2 extends Base implements I {}

    static Object crash(I i, A1 a1, A2 a2, boolean b1, boolean b2) {
        return b1 ? i : b2 ? a2 : a1;
        // lub(I, lub(A1, A2)) ==> lub(I, Base&I) ==> I (doesn't compile on 1.4 ok >1.5)
    }

    public static void main(String[] args) {
        T6500343a.crash(new A1(), new A1(), new A2(), true, false);
        T6500343a.crash(new A1(), new A1(), new A2(), false, true);
        T6500343a.crash(new A1(), new A1(), new A2(), false, false);
        T6500343a.crash(new A1(), new A1(), new A2(), true, true);
    }
}

