/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6877254
 * @summary Implement StoreCMNode::Ideal to promote its OopStore above the MergeMem
 *
 * @run main/othervm -Xcomp compiler.c2.Test6877254
 */

package compiler.c2;

public class Test6877254 {
    static byte var_1;
    static String var_2 = "";
    static byte var_3;
    static float var_4 = 0;

    public static void main(String[] args) {
        int i = 0;

        for (String var_tmp = var_2; i < 11; var_1 = 0, i++) {
            var_2 = var_2;
            var_4 *= (var_4 *= (var_3 = 0));
        }

        System.out.println("var_1 = " + var_1);
        System.out.println("var_2 = " + var_2);
        System.out.println("var_3 = " + var_3);
        System.out.println("var_4 = " + var_4);
    }
}
