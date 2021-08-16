/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8154587
 * @summary Check that invokeinterface of clone() works properly where clone() is
 *          an abstract method in a superinterface and also overridden locally.
 * @run main AbstractClone
 */

public class AbstractClone {

    interface I1 {
        Object clone();
    }

    interface I2 extends I1 { }

    static class C implements I2 {
        public Object clone() {
            return "In C's clone()";
        }
    }

    static Object test(I2 i) { return i.clone(); }

    public static void main(String[] args) {
        String s = (String)test(new C());
        if (!s.equals("In C's clone()")) {
            throw new RuntimeException("Wrong clone() called");
        }
    }
}
