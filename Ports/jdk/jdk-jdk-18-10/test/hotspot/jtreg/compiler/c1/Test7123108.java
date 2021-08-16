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

/**
 * @test
 * @bug 7123108
 * @summary C1 crashes with assert(if_state != NULL) failed: states do not match up
 *
 * @run main/othervm -Xcomp compiler.c1.Test7123108
 */

package compiler.c1;

public class Test7123108 {

    static class Test_Class_0 {
        final static byte var_2 = 67;
        byte var_3;
    }

    Object var_25 = "kgfpyhcms";
    static long var_27 = 6899666748616086528L;

    static float func_1()
    {
        return 0.0F;
    }

    private void test()
    {
        "dlwq".charAt(((short)'x' > var_27 | func_1() <= (((Test_Class_0)var_25).var_3) ? true : true) ? Test_Class_0.var_2 & (short)-1.1173839E38F : 'Y');
    }

    public static void main(String[] args)
    {
        Test7123108 t = new Test7123108();
        try {
            t.test();
        } catch (Throwable e) { }
    }
}
