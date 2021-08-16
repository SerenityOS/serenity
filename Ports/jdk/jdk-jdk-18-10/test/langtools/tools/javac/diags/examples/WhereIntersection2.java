/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

// key: compiler.err.intf.expected.here
// key: compiler.misc.inconvertible.types
// key: compiler.misc.where.description.typevar
// key: compiler.misc.where.typevar
// key: compiler.misc.intersection.type
// key: compiler.misc.where.description.intersection
// key: compiler.misc.where.intersection
// key: compiler.err.prob.found.req
// options: --diags=formatterOptions=where
// run: simple

class WhereIntersection2 {
    interface I1 {}
    interface I2 {}
    class A implements I1, I2 {}
    class B implements I1, I2 {}
    class Test {
        <Z extends A&B> Z m(Z z1, Z z2) { return null; }
        <T extends I1 & I2> T m2(){
            return m(new A(), new B());
        }
    }
}
