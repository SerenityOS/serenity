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
 * @bug 4459133
 * @summary Forward reference legal or illegal?
 * @author gafter
 *
 * @compile UseBeforeDeclaration.java
 */

class UseBeforeDeclaration {
    static {
        x = 100; // ok - assignment
//      int y = ((x)) + 1; // error - read before declaration
        int v = ((x)) = 3; // ok - x at left hand side of assignment
        int z = UseBeforeDeclaration.x * 2; // ok - not accessed via simple name
        Object o = new Object(){
                void foo(){x++;} // ok - occurs in a different class
                {x++;} // ok - occurs in a different class
            };
    }
    {
        j = 200; // ok - assignment
//      j = j + 1; // error - right hand side reads before declaration
//      int k = j = j + 1; // error - right hand side reads before declaration
        int n = j = 300; // ok - j at left hand side of assignment
//      int h = j++; // error - read before declaration
        int l = this.j * 3; // ok - not accessed via simple name
        Object o = new Object(){
                void foo(){j++;} // ok - occurs in a different class
                { j = j + 1;} // ok - occurs in a different class
            };
    }
    int w = x= 3; // ok - x at left hand side of assignment
    int p = x; // ok - instance initializers may access static fields
    static int u = (new Object(){int bar(){return x;}}).bar(); // ok - occurs in a different class
    static int x;
    int m = j = 4; // ok - j at left hand side of assignment
    int o = (new Object(){int bar(){return j;}}).bar(); // ok - occurs in a different class
    int j;
}
