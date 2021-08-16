/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8147527
 * @summary Qualified "super" boxed unary post-operation using a type variable.
 * @compile QualBoxedPostOp2.java
 * @run main QualBoxedPostOp2
 */
public class QualBoxedPostOp2<T> extends Parent2<Integer> {
    public static void main(String[] args) {
        new QualBoxedPostOp2().testAll();
    }

    private void testAll() {
        super.i = 10;

        equals(new Inner().testParent(), 10);
        equals(super.i, 11);
    }

    private void equals(int a, int b) {
        if (a != b) throw new Error();
    }

    T i;

    class Inner {
        private Integer testParent() {
            return QualBoxedPostOp2.super.i++;
        }
    }
}

class Parent2<T> {
    protected T i;
}
