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
 * @bug 4403168
 * @summary Verify correct type of result of ?: operator when constant folding occurs
 * @author gafter
 *
 * @run main FoldConditional
 */

public class FoldConditional {
    static void f(double x) {
        // OK
    }
    static void f(int x) {
        throw new Error();
    }
    public static void main(String args[]){
        int x=5;
        String value0 = ("value is " + 9.0).intern();
        String value1 = ("value is " + ((x> 4)?9:9.9)).intern();
        String value2 = ("value is " + ( true ? 9 : (9.9+x) )).intern();
        f(true ? 9 : 9.9);
        f(true ? 9 : (9.9 + x));
        if (value0 != value1) throw new Error();
        if (value0 != value2) throw new Error();
    }
}
