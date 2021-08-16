/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4717181
 * @summary javac treats inherited abstract method as an overrider
 * @author gafter
 *
 * @compile T4717181b.java
 */

class T4717181b {
    static class E1 extends Exception {}
    static class E2 extends Exception {}
    static class E3 extends Exception {}
    static abstract class Ta {
        public abstract void m() throws E1, E2;
    }
    interface Tb {
        void m() throws E2, E3;
    }
    static abstract class Tc extends Ta implements Tb {
        {
            try {
                m(); // intersect throws clauses
            } catch (E2 e2) {
            }
        }
    }
}
