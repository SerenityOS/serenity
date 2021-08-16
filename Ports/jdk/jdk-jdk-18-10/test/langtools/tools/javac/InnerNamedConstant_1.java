/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4095568
 * @summary Allow static final variables with constant initializers
 * in inner classes.
 * @author William Maddox (maddox)
 *
 * @compile InnerNamedConstant_1.java
 * @run main InnerNamedConstant_1
 */

public class InnerNamedConstant_1 {
    static class Inner1 {
        static final int x = 1;
        static final int y = x * 5;
        static final String z = "foobar";
    }

    class Inner2 {
        static final int x = 1;
        static final int y = x * 5;
        static final String z = "foobar";
    }

    public static void main(String[] args) {
        System.out.println(Inner1.x + " " + Inner1.y + " " + Inner1.z);
        InnerNamedConstant_1 outer = new InnerNamedConstant_1();
        Inner2 inner2 = outer.new Inner2();
        System.out.println(inner2.x + " " + inner2.y + " " + inner2.z);
    }
}
