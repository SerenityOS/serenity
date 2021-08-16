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
 * @bug 6531090
 *
 * @summary Cannot access methods/fields of a captured type belonging to an intersection type
 * @author Maurizio Cimadamore
 *
 */
public class T6531090a {

    static class E {}

    static class F extends E implements I1 {}

    static interface I {}

    static interface I1 {}

    static class G extends F implements I {}

    static class C<T extends E & I> {
        T field;
    }

    public static void main(String... args) {
        test(new C<G>());
    }

    static <W extends F> void test(C<? extends W> arg) {
        F vf = arg.field;
        I vi = arg.field;
        I1 vi1 = arg.field;
        E ve = arg.field;
        W vt = arg.field;
    }
}
