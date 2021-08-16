/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7023703
 * @summary Valid code doesn't compile
 * @compile T7023703pos.java
 */

class T7023703pos {

    void testForLoop() {
        final int bug;
        for (;"a".equals("b");) {
            final int item = 0;
        }
        bug = 0; //ok
    }

    void testForEachLoop(boolean cond, java.util.Collection<Integer> c) {
        final int bug;
        for (Integer i : c) {
            if (cond) {
                final int item = 0;
            }
        }
        bug = 0; //ok
    }

    void testWhileLoop() {
        final int bug;
        while ("a".equals("b")) {
            final int item = 0;
        }
        bug = 0; //ok
    }

    void testDoWhileLoop() {
        final int bug;
        do {
            final int item = 0;
        } while ("a".equals("b"));
        bug = 0; //ok
    }

    private static class Inner {
        private final int a, b, c, d, e;

        public Inner() {
            a = b = c = d = e = 0;
        }
    }
}
